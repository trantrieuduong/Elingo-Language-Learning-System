# Dùng module `file` từ module khác

## Nguyên tắc

- Không gọi `R2Service`, không phụ thuộc ngược vào `file`.
- Client gửi `verifiedFileKey` (key ở `verified/`).
- Module sở hữu dữ liệu: kiểm tra nhóm định dạng → đổi sang key `uploads/` → lưu CSDL → phát event.
- CSDL **luôn** lưu key `uploads/`. Lưu `verified/` là sai: file ở vùng chờ sẽ tự xoá, ảnh hỏng vĩnh viễn.

```java
String uploadsKey = FileKey.toUploads(verifiedKey); // không dùng replace("verified/", "uploads/")
```

---

## 1. Kiểm tra nhóm định dạng

Đuôi file trong key chính là mime type đã được xác thực nên đọc nhóm từ key không tốn request R2.

### Ở DTO: `@AllowedMediaKind`

Chặn sớm, trả 400. Cần `@Valid` trên `@RequestBody`.

```java
public record UpdateAvatarRequest(
        @NotBlank @AllowedMediaKind(MediaKind.IMAGE) String avatarKey
) {}

public record CreatePostRequest(
        @NotBlank String content,
        List<@NotBlank @AllowedMediaKind({MediaKind.IMAGE, MediaKind.VIDEO}) String> mediaKeys
) {}
```

### Ở service: chốt chặn cuối

Annotation không thay thế bước này. Luôn kiểm tra lại trong transaction, **trước `save()`**.

```java
private String requireMediaKind(String verifiedKey, MediaKind... allowed) {
    MediaKind actual = FileKey.mediaKindOf(verifiedKey);
    if (actual == null || !List.of(allowed).contains(actual)) {
        throw new AppException(AppError.INVALID_FILE_TYPE);
    }
    return FileKey.toUploads(verifiedKey);
}
```

Ném lỗi ở đây thì chưa ghi gì, file vẫn ở `verified/` và tự xoá sau.

Trần dung lượng nằm trong `MediaKind.maxBytes()`: `IMAGE` 5 MB, `VIDEO` 100 MB, `AUDIO` 25 MB.

---

## 2. `FileAttachedEvent` — gắn file mới

Phát **một event cho mỗi file**, ngay sau khi ghi entity. Listener copy `verified/` → `uploads/` sau commit; rollback thì không copy gì.

```java
@Transactional
public Long createPost(CreatePostRequest request, Long userId) {
    List<String> verifiedKeys = request.mediaKeys();
    List<String> uploadsKeys = verifiedKeys.stream()
            .map(k -> requireMediaKind(k, MediaKind.IMAGE, MediaKind.VIDEO))
            .toList();

    Post post = postRepository.save(new Post(request.content(), userId));

    for (int i = 0; i < uploadsKeys.size(); i++) {
        post.getMedia().add(new PostMedia(post, uploadsKeys.get(i)));
        publisher.publishEvent(new FileAttachedEvent(verifiedKeys.get(i), uploadsKeys.get(i)));
    }
    return post.getId();
}
```

---

## 3. `FileDeletedEvent` — xoá file cũ

Gom key rồi phát **một event** cho cả danh sách. Event tự chuẩn hoá `null` thành list rỗng và copy list. Chỉ phát sau khi đã cập nhật hoặc xoá entity.

**Thay file** (lấy `oldKey` trước khi ghi đè, `FileReplacedEvent` đã bị xoá):

```java
@Transactional
public void updateAvatar(Long userId, String verifiedKey) {
    String newKey = requireMediaKind(verifiedKey, MediaKind.IMAGE);
    User user = userRepository.findById(userId).orElseThrow();
    String oldKey = user.getAvatarKey();

    user.setAvatarKey(newKey);
    userRepository.save(user);

    publisher.publishEvent(new FileAttachedEvent(verifiedKey, newKey));
    if (oldKey != null && !oldKey.isBlank()) {
        publisher.publishEvent(new FileDeletedEvent(List.of(oldKey)));
    }
}
```

**Xoá bản ghi:**

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