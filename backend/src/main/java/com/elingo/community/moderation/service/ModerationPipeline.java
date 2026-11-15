package com.elingo.community.moderation.service;

import com.elingo.community.entity.ModerationAction;
import com.elingo.community.moderation.model.ModerationResult;
import com.elingo.community.moderation.model.PostForModeration;
import com.elingo.community.moderation.stage.ModerationStage;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Chạy các stage theo thứ tự @Order (text -> ảnh -> video) và dừng ở stage đầu tiên không sạch
 */
@Slf4j(topic = "MODERATION-PIPELINE")
@Component
public class ModerationPipeline {

    private final List<ModerationStage> stages;

    public ModerationPipeline(List<ModerationStage> stages) {
        this.stages = stages;
    }

    public ModerationResult run(PostForModeration post) {
        ModerationResult worst = ModerationResult.clean();

        for (ModerationStage stage : stages) {
            Optional<ModerationResult> optResult = stage.evaluate(post);

            if (optResult.isEmpty()) continue;

            ModerationResult result = optResult.get();
            log.debug("Moderation stage evaluated stage={} label={} score={} action={}",
                    stage.name(), result.label(), result.score(), result.action());

            if (result.action().equals(ModerationAction.AUTO_REJECT)) {
                return result;
            }
            worst = worst.worseOf(result);
        }
        return worst;
    }
}
