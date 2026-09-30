# Dùng module `file` từ module khác

> Tài liệu này chỉ nói **cách một module sử dụng module `file`**: gọi API upload và thao tác
> với ba event. Thiết kế ba vùng trên bucket (`staging/` → `verified/` → `uploads/`), cơ
> chế magic bytes và quy tắc vòng đời đã nêu ở
> [`docs/project-architecture/backend-architecture.md` §16](../docs/project-architecture/backend-architecture.md).
> Luồng tổng quát: [`File_Upload_Sequence.puml`](../docs/diagrams/sequence-diagram/File_Upload_Sequence.puml).

---

## 1. Nguyên tắc

Module sở hữu dữ liệu **không gọi `R2Service`**, cũng không phụ thuộc ngược vào `file`.
Nó làm đúng ba việc:

1. Nhận `verifiedFileKey` từ client.
2. Tính key `uploads/` bằng `FileKey.toUploads(...)`, ghi key đó vào CSDL.
3. Phát event để module `file` lo phần còn lại.

```java
String uploadsKey = FileKey.toUploads(request.verifiedFileKey());
```

Đây là hàm thuần trong `com.elingo.file.util` — không phải bean, không I/O. Dùng nó thay vì
`key.replace("verified/", "uploads/")`.

> **Sai lầm chết người:** lưu `verifiedFileKey` vào CSDL. File đó nằm ở vùng chờ, tự xoá
> sau 7 ngày ⇒ ảnh hỏng vĩnh viễn. CSDL **luôn** lưu key `uploads/`.

---

## 2. Gắn file mới vào bản ghi — `FileAttachedEvent`

Phát **bên trong** transaction, ngay sau khi đã ghi entity. `@TransactionalEventListener`
lo phần chờ commit.

```java
@Transactional
public Long createPost(CreatePostRequest request, Long userId) {
    Post post = new Post(request.content(), userId);
    postRepository.save(post);

    if (request.verifiedFileKey() != null && !request.verifiedFileKey().isBlank()) {
        String uploadsKey = FileKey.toUploads(request.verifiedFileKey());
        post.getMedia().add(new PostMedia(post, uploadsKey));

        publisher.publishEvent(
                new FileAttachedEvent(request.verifiedFileKey(), uploadsKey));
    }
    return post.getId();
}
```

Listener sẽ copy `verified/` → `uploads/` **sau khi commit**. Nếu transaction rollback thì
không copy gì, file vẫn nằm ở `verified/` và tự xoá sau 7 ngày.

Một lần commit có nhiều file thì phát nhiều event — mỗi `FileAttachedEvent` mang đúng một
cặp key.

---

## 3. Thay file — `FileReplacedEvent`

Lấy key cũ **trước khi** ghi đè, rồi phát event sau khi entity đã cập nhật.

```java
@Transactional
public void updateAvatar(Long userId, String verifiedFileKey) {
    User user = userRepository.findById(userId).orElseThrow();

    String oldKey = user.getAvatarKey();               // lấy trước
    String newKey = FileKey.toUploads(verifiedFileKey);

    user.setAvatarKey(newKey);
    userRepository.save(user);

    publisher.publishEvent(new FileReplacedEvent(oldKey, newKey));
}
```

Lần đầu gán ảnh thì `oldKey` là `null` hoặc rỗng — listener tự bỏ qua, không cần if.

Bản ghi có nhiều ảnh mà chỉ thay một ảnh: phát một `FileReplacedEvent` cho ảnh đó.

---

## 4. Xoá bản ghi — `FileDeletedEvent`

Event mang **danh sách** key. Gom key từ các dòng con rồi phát **một** event, đừng phát N
event.

```java
@Transactional
public void deletePost(Long postId, Long currentUserId) {
    Post post = postRepository.findById(postId).orElseThrow();
    // ...kiểm tra quyền...

    List<String> keys = postMediaRepository.findByPostId(postId).stream()
            .map(PostMedia::getFileKey)
            .toList();

    postRepository.delete(post);

    publisher.publishEvent(new FileDeletedEvent(keys));
}
```

`FileDeletedEvent` tự chuẩn hoá `null` thành danh sách rỗng và sao chép danh sách, nên sau khi
phát, module khác không sửa được danh sách đó nữa.

Do key phẳng (`uploads/{userId}/{uuid}.{ext}`), xoá bài nhiều ảnh nghĩa là gom key từ
`post_media` chứ không phải dò cả thư mục — chính xác hơn và không sợ xoá nhầm.

---

## 5. Quy tắc cần nhớ

| Nên | Không nên |
|---|---|
| Ghi key `uploads/` vào CSDL | Ghi key `verified/` — file bị quy tắc vòng đời xoá sau 7 ngày |
| `FileKey.toUploads(verifiedKey)` | `key.replace("verified/", "uploads/")` |
| Phát event trong transaction, sau khi ghi entity | Phát trước khi ghi entity |
| Gom key rồi phát một `FileDeletedEvent` | Phát N event cho N key |
| Lấy `oldKey` trước khi ghi đè | Đọc `oldKey` sau khi đã ghi — luôn ra `null` |
| Phụ thuộc `FileKey` (utility thuần) | Inject `R2Service` — phụ thuộc ngược vào `file` |

**Giả định: mỗi file chỉ được một bản ghi tham chiếu.** Tham chiếu lần hai sẽ tìm key ở
`verified/` mà nó đã sang `uploads/` ⇒ hỏng. Hiện tại mỗi lần verify sinh UUID mới nên
chắc chắn không xảy ra.

**Lỗi R2 không làm hỏng request.** Cả hai listener đều `@TransactionalEventListener` +
`AFTER_COMMIT` và nuốt lỗi. Bắt lỗi quanh *từng key* để một key hỏng không chặn các key còn
lại. Đừng bọc lại phần phát event trong try/catch — không cần.

---

## 6. Chưa làm

- Chưa module nào (`user`, `community`, `report`…) phát event — cần endpoint cập nhật/xoá file.
- Chưa giới hạn tần suất xin URL, chưa giới hạn số file của một bản ghi.
- Chưa kiểm duyệt nội dung ảnh.
- Chưa có job sửa file mồ côi: quét vùng `verified/`, copy lại key đang được CSDL tham chiếu
  mà `uploads/` còn thiếu.
