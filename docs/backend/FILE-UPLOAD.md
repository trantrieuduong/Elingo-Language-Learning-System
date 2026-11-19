# Hướng dẫn tích hợp Module File trong Elingo

Tài liệu này hướng dẫn cách các module nghiệp vụ (`user`, `community`, `vocabulary`, `admin`...) tương tác với module `file` và lưu trữ Cloudflare R2 một cách an toàn và đúng kiến trúc.

---

## 1. Nguyên tắc cốt lõi

- **Không gọi trực tiếp `R2Service`** từ các service nghiệp vụ thông thường (trừ listener nội bộ của module file).
- **Phân tách 4 phân vùng lưu trữ trên R2**:
  ```text
  staging/{userId}/{uuid}.{ext}     File vừa tải lên, chưa kiểm tra định dạng
  verified/{userId}/{uuid}.{ext}    File đã qua kiểm tra magic bytes, chưa ai dùng
  uploads/{userId}/{uuid}.{ext}     File người dùng lưu trữ vĩnh viễn (đã được bản ghi tham chiếu)
  system/{module}/{uuid}.{ext}      File hệ thống lưu trữ vĩnh viễn (decks, cards, phonetics, badges...)
  ```
- **CSDL luôn lưu key vĩnh viễn** (`uploads/...` cho User hoặc `system/...` cho Hệ thống). Tuyệt đối không lưu key `verified/` hay `staging/` vào CSDL vì các file ở vùng chờ sẽ bị bucket lifecycle tự động xóa.
- **Tên file không đổi khi promote sang vùng vĩnh viễn**: Tên file `{uuid}.{ext}` được giữ nguyên từ `verified/` sang `uploads/` hoặc `system/{module}/`.

---

## 2. Kiểm tra định dạng đa phương tiện

Đuôi file trong key ở vùng `verified/` chính là mime type đã được xác thực qua magic bytes, nên kiểm tra loại file từ key không tốn request đến R2.

### 2.1 Ở tầng DTO: `@AllowedMediaKind`
Chặn sớm ngay từ request, trả về HTTP 400. Yêu cầu có `@Valid` trên `@RequestBody`:
```java
public record UpdateAvatarRequest(
        @NotBlank @AllowedMediaKind(MediaKind.IMAGE) String avatarKey
) {}

public record CreatePostRequest(
        @NotBlank String content,
        List<@NotBlank @AllowedMediaKind({MediaKind.IMAGE, MediaKind.VIDEO}) String> mediaKeys
) {}
```

### 2.2 Ở tầng Service: Chốt chặn kiểm tra
Thực hiện kiểm tra trong transaction, **trước khi gọi `save()`**.

- **Đối với tài nguyên người dùng (User Assets)**:
  ```java
  private String requireUserMediaKind(String verifiedKey, MediaKind... allowed) {
      MediaKind actual = FileKey.mediaKindOf(verifiedKey);
      if (actual == null || !List.of(allowed).contains(actual)) {
          throw new AppException(AppError.INVALID_FILE_TYPE);
      }
      return FileKey.toUploads(verifiedKey); // uploads/{userId}/{uuid}.{ext}
  }
  ```

- **Đối với tài nguyên hệ thống của Admin (System Assets)**:
  ```java
  private String requireSystemMediaKind(String verifiedKey, SystemFileModule module) {
      if (module == null) {
          throw new AppException(AppError.INVALID_REQUEST);
      }
      MediaKind actual = FileKey.mediaKindOf(verifiedKey);
      if (actual == null || !module.supports(actual)) {
          throw new AppException(AppError.INVALID_FILE_TYPE);
      }
      return FileKey.toSystem(verifiedKey, module); // system/{module}/{uuid}.{ext}
  }
  ```

Trần dung lượng theo cấu hình `MediaKind.maxBytes()`: `IMAGE` 5 MB, `VIDEO` 100 MB, `AUDIO` 25 MB.

---

## 3. Cơ chế đồng bộ tệp qua sự kiện (File Promotion Events)

Sau khi bản ghi được commit vào CSDL (`AFTER_COMMIT`), `FileLifecycleListener` bắt sự kiện và thực hiện copy từ `verified/` sang vùng lưu trữ vĩnh viễn tương ứng.

### 3.1 `FileAttachedEvent` — Dành cho tài nguyên người dùng
Chuyển file từ `verified/{userId}/{uuid}.{ext}` sang `uploads/{userId}/{uuid}.{ext}`:
```java
@Transactional
public Long createPost(CreatePostRequest request, Long userId) {
    List<String> verifiedKeys = request.mediaKeys();
    List<String> uploadsKeys = verifiedKeys.stream()
            .map(k -> requireUserMediaKind(k, MediaKind.IMAGE, MediaKind.VIDEO))
            .toList();

    Post post = postRepository.save(new Post(request.content(), userId));

    for (int i = 0; i < uploadsKeys.size(); i++) {
        post.getMedia().add(new PostMedia(post, uploadsKeys.get(i)));
        publisher.publishEvent(new FileAttachedEvent(verifiedKeys.get(i), uploadsKeys.get(i)));
    }
    return post.getId();
}
```

### 3.2 `SystemFileAttachedEvent` — Dành cho tài nguyên hệ thống
Chuyển file từ `verified/{adminUserId}/{uuid}.{ext}` sang `system/{module}/{uuid}.{ext}`:
```java
@Transactional
public DeckResponse createSystemDeck(SystemDeckRequest request) {
    String verifiedKey = request.coverImageUrl();
    String systemKey = requireSystemMediaKind(verifiedKey, SystemFileModule.DECKS);

    Deck deck = deckMapper.toDeck(request);
    deck.setCoverImageUrl(systemKey);
    deck.setOwnerType(OwnerType.SYSTEM);
    Deck saved = deckRepository.save(deck);

    publisher.publishEvent(new SystemFileAttachedEvent(verifiedKey, systemKey));
    return deckMapper.toDeckResponse(saved);
}
```

---

## 4. `FileDeletedEvent` — Xóa tệp cũ

Gom danh sách key và phát một sự kiện duy nhất. Chỉ phát sự kiện sau khi cập nhật hoặc xóa entity thành công.

### 4.1 Thay thế file của người dùng
```java
@Transactional
public void updateAvatar(Long userId, String verifiedKey) {
    String newKey = requireUserMediaKind(verifiedKey, MediaKind.IMAGE);
    User user = userRepository.findById(userId).orElseThrow();
    String oldKey = user.getAvatarKey();

    user.setAvatarKey(newKey);
    userRepository.save(user);

    publisher.publishEvent(new FileAttachedEvent(verifiedKey, newKey));
    if (oldKey != null && FileKey.isInZone(oldKey, FileKey.UPLOADS_PREFIX)) {
        publisher.publishEvent(new FileDeletedEvent(List.of(oldKey)));
    }
}
```

### 4.2 Xóa bản ghi
```java
@Transactional
public void deletePost(Long postId, Long currentUserId) {
    Post post = postRepository.findById(postId).orElseThrow();
    // ...kiểm tra quyền...

    List<String> keys = post.getMedia().stream().map(PostMedia::getFileKey).toList();
    postRepository.delete(post);

    if (!keys.isEmpty()) {
        publisher.publishEvent(new FileDeletedEvent(keys));
    }
}
```

---

## 5. Dùng tiện ích dùng chung `MediaKeyResolver` (Module File)

Module `file` cung cấp sẵn component `com.elingo.file.util.MediaKeyResolver` để mọi module (`user`, `community`, `vocabulary`, `admin`, `gamification`...) chuẩn hóa toàn bộ luồng tạo, cập nhật và phát sự kiện chỉ trong 2 bước đơn giản:

```java
// 1. Tạo mới tài nguyên người dùng:
var avatar = mediaKeyResolver.forCreate(request.avatarKey(), MediaKind.IMAGE);
user.setAvatarUrl(avatar.key());
userRepository.save(user);
mediaKeyResolver.publish(avatar); // Tự động phát FileAttachedEvent

// 2. Tạo mới tài nguyên hệ thống (Admin):
var cover = mediaKeyResolver.forCreate(request.coverImageUrl(), SystemFileModule.DECKS);
deck.setCoverImageUrl(cover.key());
deckRepository.save(deck);
mediaKeyResolver.publish(cover); // Tự động phát SystemFileAttachedEvent

// 3. Cập nhật tài nguyên (tự động xử lý giữ nguyên file hoặc thay file mới):
var cover = mediaKeyResolver.forUpdate(request.coverImageUrl(), deck.getCoverImageUrl(), SystemFileModule.DECKS);
deck.setCoverImageUrl(cover.key());
deckRepository.save(deck);
mediaKeyResolver.publish(cover); // Tự động phát SystemFileAttachedEvent (nếu đổi) và FileDeletedEvent (nếu có file cũ cần dọn)
```

### Quy tắc quan trọng khi cập nhật (Update in-place):
- **Giữ nguyên file cũ**: Nếu client gửi lại `newKey == oldKey`, `MediaKeyResolver` kiểm tra qua `FileKey.isPermanentKey`. Cả key thuộc `uploads/` lẫn `system/` (bao gồm cả các đường dẫn phân cấp sâu như `system/decks/common-1000/animals/image/...`) đều được công nhận là hợp lệ và giữ nguyên.
- **Dọn dẹp file cũ khi thay thế hoặc xóa**: Khi thay thế file mới hoặc xóa file (bằng cách gửi key mới từ `verified/` hoặc `null`), bất kỳ file cũ nào thuộc các vùng lưu trữ vĩnh viễn (`uploads/` hoặc `system/`) đều được đưa vào danh sách dọn dẹp và phát `FileDeletedEvent` để xóa vĩnh viễn trên R2.
