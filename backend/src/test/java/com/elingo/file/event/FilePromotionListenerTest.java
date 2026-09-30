package com.elingo.file.event;

import com.elingo.common.event.FileAttachedEvent;
import com.elingo.file.service.R2Service;
import com.elingo.file.util.FileKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Kiểm tra {@link FilePromotionListener} chuyển file sang vùng vĩnh viễn sau khi commit.
 *
 * <p>Cố tình <b>không</b> kế thừa {@code BaseIntegrationTest}: lớp đó bọc mỗi test trong
 * {@code @Transactional} nên transaction luôn rollback, mà {@code AFTER_COMMIT} thì
 * không bao giờ chạy. Test này tự commit bằng {@link TransactionTemplate} để thấy đúng
 * hành vi cần kiểm.
 *
 * <p>Test quan trọng nhất là {@code testRollback_DoesNotPromote}: nó giữ cho
 * {@code uploads/} không bao giờ chứa file mà CSDL chưa tham chiếu — đó là toàn bộ ý nghĩa
 * của việc thêm vùng {@code verified/}.
 */
@SpringBootTest
@ActiveProfiles("test")
@Tag("integration")
class FilePromotionListenerTest {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockitoBean
    private R2Service r2Service;

    private void inCommittedTransaction(Runnable action) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> action.run());
    }

    private void inRolledBackTransaction(Runnable action) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            action.run();
            status.setRollbackOnly();
        });
    }

    @Test
    @DisplayName("A committed attachment promotes the file with both keys")
    void testCommit_PromotesFile() {
        String verifiedKey = "verified/12/a.png";
        String uploadsKey = FileKey.toUploads(verifiedKey);

        inCommittedTransaction(() -> eventPublisher.publishEvent(
                new FileAttachedEvent(verifiedKey, uploadsKey)));

        verify(r2Service, times(1)).promoteToUploads(verifiedKey, uploadsKey);
    }

    @Test
    @DisplayName("A rolled-back transaction promotes nothing — uploads/ never holds a file no record points to")
    void testRollback_DoesNotPromote() {
        inRolledBackTransaction(() -> eventPublisher.publishEvent(
                new FileAttachedEvent("verified/12/a.png", "uploads/12/a.png")));

        // Promote trước khi commit sẽ để lại file mồ côi ở vùng không có quy tắc vòng đời
        // dọn — đúng lỗi mà vùng verified/ sinh ra để chặn.
        verifyNoInteractions(r2Service);
    }

    @Test
    @DisplayName("A blank verified key touches no R2 call")
    void testBlankVerifiedKey_PromotesNothing() {
        inCommittedTransaction(() -> eventPublisher.publishEvent(
                new FileAttachedEvent("   ", "uploads/12/a.png")));

        verifyNoInteractions(r2Service);
    }

    @Test
    @DisplayName("A blank uploads key touches no R2 call")
    void testBlankUploadsKey_PromotesNothing() {
        inCommittedTransaction(() -> eventPublisher.publishEvent(
                new FileAttachedEvent("verified/12/a.png", "")));

        verifyNoInteractions(r2Service);
    }

    @Test
    @DisplayName("A null key is normalized to empty, no NullPointerException")
    void testNullKeys_PromoteNothing() {
        inCommittedTransaction(() -> eventPublisher.publishEvent(
                new FileAttachedEvent(null, null)));

        verifyNoInteractions(r2Service);
    }

    @Test
    @DisplayName("The source is passed first and the destination second")
    void testPromote_ArgumentOrder() {
        String verifiedKey = "verified/12/a.png";
        String uploadsKey = "uploads/12/a.png";

        inCommittedTransaction(() -> eventPublisher.publishEvent(
                new FileAttachedEvent(verifiedKey, uploadsKey)));

        // Đảo hai key là copy ngược hướng: sẽ ghi đè bản trong uploads/ bằng bản chờ và
        // xoá mất bản vĩnh viễn mà CSDL đang trỏ tới.
        verify(r2Service, times(1)).promoteToUploads(verifiedKey, uploadsKey);
        verify(r2Service, never()).promoteToUploads(uploadsKey, verifiedKey);
    }

    @Test
    @DisplayName("Promoting many attachments in one transaction promotes each of them")
    void testManyAttachments_PromoteEach() {
        inCommittedTransaction(() -> {
            eventPublisher.publishEvent(new FileAttachedEvent("verified/12/a.png", "uploads/12/a.png"));
            eventPublisher.publishEvent(new FileAttachedEvent("verified/12/b.jpg", "uploads/12/b.jpg"));
            eventPublisher.publishEvent(new FileAttachedEvent("verified/12/c.webp", "uploads/12/c.webp"));
        });

        verify(r2Service, times(1)).promoteToUploads("verified/12/a.png", "uploads/12/a.png");
        verify(r2Service, times(1)).promoteToUploads("verified/12/b.jpg", "uploads/12/b.jpg");
        verify(r2Service, times(1)).promoteToUploads("verified/12/c.webp", "uploads/12/c.webp");
    }

    @Test
    @DisplayName("A promote failure does not break the committed operation — the error is swallowed")
    void testPromoteFailure_IsSwallowed() {
        doThrow(new RuntimeException("R2 unreachable"))
                .when(r2Service).promoteToUploads(any(), any());

        // Không ném ra ngoài: caller đã nhận response 200 trước khi listener chạy.
        inCommittedTransaction(() -> eventPublisher.publishEvent(
                new FileAttachedEvent("verified/12/a.png", "uploads/12/a.png")));

        verify(r2Service, times(1)).promoteToUploads("verified/12/a.png", "uploads/12/a.png");
    }

    /**
     * Listener phải nuốt lỗi, nên không cái gì ném ra được — nếu test này đỏ nghĩa là đã
     * có exception lọt ra ngoài {@code catch}.
     */
    @Test
    @DisplayName("No exception escapes the listener even when R2 fails")
    void testListener_NeverThrowsToCaller() {
        doThrow(new RuntimeException("R2 unreachable"))
                .when(r2Service).promoteToUploads(any(), any());

        assertThatCode(() -> inCommittedTransaction(() -> eventPublisher.publishEvent(
                new FileAttachedEvent("verified/12/a.png", "uploads/12/a.png"))))
                .doesNotThrowAnyException();
    }
}
