package com.elingo.community.moderation.stage;

import com.elingo.community.moderation.model.ModerationResult;
import com.elingo.community.moderation.model.PostForModeration;

import java.util.Optional;

/** Thứ tự chạy do @Order trên từng bean quyết định (số nhỏ chạy trước).
 * Thêm stage mới = thêm 1 @Component. */
public interface ModerationStage {

    String name();

    /** @return empty nếu stage không có gì để kiểm tra,
     * nếu có trả về duy nhất kết quả tệ theo các mức: reject -> review -> approved */
    Optional<ModerationResult> evaluate(PostForModeration post);
}
