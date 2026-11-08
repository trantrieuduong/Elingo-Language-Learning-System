package com.elingo.community.moderation.service;

import com.elingo.community.entity.ModerationAction;
import com.elingo.community.moderation.model.ModerationResult;
import com.elingo.community.moderation.model.PostForModeration;
import com.elingo.community.moderation.stage.ModerationStage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class ModerationPipelineTest {

    @Mock
    private ModerationStage stage1;

    @Mock
    private ModerationStage stage2;

    @Mock
    private ModerationStage stage3;

    // ========================================================================
    // run
    // ========================================================================
    @Nested
    @DisplayName("run")
    class RunTests {

        @Test
        @DisplayName("Should return clean when all stages are empty or clean")
        void run_WhenAllStagesClean_ReturnsClean() {
            ModerationPipeline pipeline = new ModerationPipeline(List.of(stage1, stage2));
            PostForModeration post = new PostForModeration(1L, 10L, "Title", "Content", List.of());

            when(stage1.evaluate(post)).thenReturn(Optional.empty());
            when(stage2.name()).thenReturn("STAGE_2");
            when(stage2.evaluate(post)).thenReturn(Optional.of(ModerationResult.clean()));

            ModerationResult result = pipeline.run(post);

            assertThat(result.action()).isEqualTo(ModerationAction.AUTO_APPROVE);
            assertThat(result.label()).isEqualTo("clean");
        }

        @Test
        @DisplayName("Should short-circuit and return immediately when a stage returns AUTO_REJECT")
        void run_WhenStageReturnsAutoReject_ShortCircuits() {
            ModerationPipeline pipeline = new ModerationPipeline(List.of(stage1, stage2, stage3));
            PostForModeration post = new PostForModeration(1L, 10L, "Title", "Content", List.of());

            ModerationResult rejectResult = new ModerationResult(ModerationAction.AUTO_REJECT, "hate", 0.95, null);
            when(stage1.name()).thenReturn("TEXT");
            when(stage1.evaluate(post)).thenReturn(Optional.of(rejectResult));

            ModerationResult result = pipeline.run(post);

            assertThat(result).isEqualTo(rejectResult);
            verify(stage1).evaluate(post);
            verifyNoInteractions(stage2);
            verifyNoInteractions(stage3);
        }

        @Test
        @DisplayName("Should continue evaluating remaining stages when a stage returns FLAG_FOR_REVIEW and return worst")
        void run_WhenStageReturnsFlagForReview_ContinuesAndReturnsWorst() {
            ModerationPipeline pipeline = new ModerationPipeline(List.of(stage1, stage2));
            PostForModeration post = new PostForModeration(1L, 10L, "Title", "Content", List.of());

            ModerationResult flagResult = new ModerationResult(ModerationAction.FLAG_FOR_REVIEW, "toxic", 0.60, null);
            ModerationResult cleanResult = ModerationResult.clean();

            when(stage1.name()).thenReturn("TEXT");
            when(stage1.evaluate(post)).thenReturn(Optional.of(flagResult));
            when(stage2.name()).thenReturn("IMAGE");
            when(stage2.evaluate(post)).thenReturn(Optional.of(cleanResult));

            ModerationResult result = pipeline.run(post);

            assertThat(result).isEqualTo(flagResult);
            verify(stage1).evaluate(post);
            verify(stage2).evaluate(post);
        }
    }
}
