# Dùng module `file` từ module khác

> Tài liệu này chỉ nói **cách một module sử dụng module `file`**: gọi API upload và thao tác với hai event.

---

## 1. Nguyên tắc

Module sở hữu dữ liệu **không gọi `R2Service`**, cũng không phụ thuộc ngược vào `file`. Chỉ làm:

1. Nhận `verifiedFileKey` từ client.
2. Kiểm tra key đó có đúng nhóm định dạng không, **trước khi ghi entity**.
3. Tính key `uploads/` bằng `FileKey.toUploads(...)`, ghi key đó vào CSDL, phát event để module `file` lo phần còn lại.

```java
String uploadsKey = FileKey.toUploads(request.verifiedFileKey());
```

Đây là hàm thuần trong `com.elingo.file.util` — không phải bean, không I/O. Dùng nó thay vì
`key.replace("verified/", "uploads/")`.

> **Sai lầm:** lưu `verifiedFileKey` vào CSDL. File đó nằm ở vùng chờ, tự xoá ⇒ ảnh hỏng vĩnh viễn. CSDL **luôn** lưu key `uploads/`.

---

## Chặn file sai chỗ — `MediaKind`

`POST /files/verifications` chỉ xác nhận file **là media hợp lệ** (magic bytes khớp đúng loại
client khai, và loại đó nằm trong allowlist chung của ảnh/video/audio). Nó không biết file sắp
đi vào đâu, nên không chặn được "đặt video làm ảnh đại diện". Việc đó là của module sở hữu
dữ liệu.

Đuôi file trong key **chính là mime type đã xác thực**: `verifyUploadedFile` kiểm tra magic
bytes, bắt buộc khớp loại đã khai, rồi mới sinh key bằng `MediaKind.extensionOf(actualType)`;
mọi thành phần của key đều do server quyết định. Nên đọc ngược nhóm từ đuôi là đọc lại kết
quả kiểm tra đó — **không tốn một request R2 nào**:

```java
MediaKind kind = FileKey.mediaKindOf(verifiedFileKey);
```

`MediaKind` ở `com.elingo.common.enums` (`IMAGE` / `VIDEO` / `AUDIO`).
Đặt ở `common` vì `user`, `community`, `speaking`, `learning` đều cần — đây là từ vựng
dùng chung, không phải của riêng `file`. Nó cũng là **nguồn duy nhất** cho danh sách mime type,
đuôi file và trần dung lượng.

**Kiểm tra bắt buộc trong transaction, trước `save()`:**

```java
if (FileKey.mediaKindOf(verifiedFileKey) != MediaKind.IMAGE) {
    throw new AppException(AppError.INVALID_FILE_TYPE);
}
```

Ném lỗi ở đây ⇒ chưa gì được ghi, request fail sạch, file vẫn nằm ở `verified/` và tự
xoá sau. Không cần dọn. Các ví dụ ở §2 và §3 đều có sẵn dòng này.

## Giới hạn dung lượng

Trần nằm trong chính `MediaKind.maxBytes()`, **mỗi nhóm một trần**:

| Nhóm | Trần |
|---|---|
| `IMAGE` | 5 MB |
| `VIDEO` | 100 MB |
| `AUDIO` | 25 MB |

Muốn đổi trần hay thêm định dạng thì chỉ sửa `MediaKind`; không còn biến cấu hình riêng nào
cho dung lượng.

---

## 2. Gắn file mới — `FileAttachedEvent`

Kiểm tra nhóm định dạng **trước `save()`**, rồi phát một event cho mỗi file, ngay sau khi ghi
entity. `@TransactionalEventListener` lo phần chờ commit.

```java
@Transactional
public Long createPost(CreatePostRequest request, Long userId) {
    // Bài viết nhận ảnh và video; sai nhóm thì ném lỗi khi chưa ghi gì.
    List<String> verifiedKeys = request.mediaKeys();
    List<String> uploadsKeys = verifiedKeys.stream()
            .map(key -> requireMediaKind(key, MediaKind.IMAGE, MediaKind.VIDEO))
            .toList();

    Post post = postRepository.save(new Post(request.content(), userId));

    for (int i = 0; i < uploadsKeys.size(); i++) {
        post.getMedia().add(new PostMedia(post, uploadsKeys.get(i)));   // CSDL chỉ lưu key uploads/
        publisher.publishEvent(new FileAttachedEvent(verifiedKeys.get(i), uploadsKeys.get(i)));
    }
    return post.getId();
}

/** Trả key uploads/ nếu key đúng nhóm được phép; ném INVALID_FILE_TYPE nếu sai hoặc không có đuôi hợp lệ. */
private String requireMediaKind(String verifiedKey, MediaKind... allowed) {
    MediaKind actual = FileKey.mediaKindOf(verifiedKey);
    if (actual == null || !List.of(allowed).contains(actual)) {
        throw new AppException(AppError.INVALID_FILE_TYPE);
    }
    return FileKey.toUploads(verifiedKey);
}
```

Listener copy `verified/` → `uploads/` **sau khi commit**. Nếu transaction rollback thì không
copy gì, file vẫn nằm ở `verified/` và tự xoá sau.

Một lần commit có nhiều file thì phát nhiều event — mỗi `FileAttachedEvent` mang đúng một cặp
key.

`actual == null` phải chặn luôn: key không có đuôi hợp lệ là key lạ, không phải file hợp lệ
mà thiếu thông tin.

---

## 3. Thay file — `FileDeletedEvent` (xóa file cũ)

Lấy `oldKey` **trước khi** ghi đè. Nếu có file cũ, phát `FileDeletedEvent` sau khi đã cập nhật
entity và commit thành công. Nếu không có file cũ (`oldKey` rỗng), listener tự bỏ qua.

`FileReplacedEvent` đã bị loại bỏ (deprecated → xóa) — việc thay file giờ được xử lý bằng hai
bước rõ ràng:
- `FileAttachedEvent` (cho file mới) chuyển từ `verified/` → `uploads/`.
- `FileDeletedEvent` (cho file cũ) xóa bản `uploads/` cũ khi nó không còn ai trỏ tới.

```java
@Transactional
public void updateAvatar(Long userId, String verifiedKey) {
    String newKey = requireMediaKind(verifiedKey, MediaKind.IMAGE); // fail sớm, chưa đọc entity
    User user = userRepository.findById(userId).orElseThrow();
    String oldKey = user.getAvatarKey();

    user.setAvatarKey(newKey);          // CSDL lưu uploads/, không phải verified/
    userRepository.save(user);

    publisher.publishEvent(new FileAttachedEvent(verifiedKey, newKey));
    if (oldKey != null && !oldKey.isBlank()) {
        publisher.publishEvent(new FileDeletedEvent(List.of(oldKey)));
    }
}
```

> Lưu ý: `FileReplacedEvent` đã bị xóa khỏi codebase. Nếu còn module nào tham chiếu đến nó,
> cần đổi thành phát `FileDeletedEvent` cho `oldKey` và `FileAttachedEvent` cho file mới.

---

## 4. Xoá bản ghi — `FileDeletedEvent`

Gom key của các dòng con rồi phát **một** event, đừng phát N event.

```java
@Transactional
public void deletePost(Long postId, Long currentUserId) {
    Post post = postRepository.findById(postId).orElseThrow();
    // ...kiểm tra quyền...

    List<String> keys = post.getMedia().stream().map(PostMedia::getFileKey).toList();

    postRepository.delete(post);

    publisher.publishEvent(new FileDeletedEvent(keys));
}
```

`FileDeletedEvent` tự chuẩn hoá `null` thành danh sách rỗng và sao chép danh sách, nên sau khi
phát, module khác không sửa được danh sách đó nữa.

Do key phẳng (`uploads/{userId}/{uuid}.{ext}`), xoá bài nhiều ảnh nghĩa là gom key từ
bảng con chứ không phải dò cả thư mục — chính xác hơn và không sợ xoá nhầm.

---
