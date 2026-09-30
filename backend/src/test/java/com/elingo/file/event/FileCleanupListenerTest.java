package com.elingo.file.event;

import com.elingo.common.event.FileDeletedEvent;
import com.elingo.common.event.FileReplacedEvent;
import com.elingo.file.service.R2Service;
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

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Kiểm tra {@link FileCleanupListener} nghe event và xoá sau khi commit.
 *
 * <p>Cố tình <b>không</b> kế thừa {@code BaseIntegrationTest}: lớp đó bọc mỗi test trong
 * {@code @Transactional} nên transaction luôn rollback, mà {@code AFTER_COMMIT} thì
 * không bao giờ chạy. Test này tự commit bằng {@link TransactionTemplate} để thấy đúng
 * hành vi cần kiểm.
 *
 * <p>Event không mang {@code userId}: {@code deleteFile} chỉ chạy nội bộ sau khi module
 * sở hữu đã xác nhận bản ghi không còn tham chiếu file đó, nên không cần dẫn thêm
 * {@code userId} để kiểm tra quyền.
 */
@SpringBootTest
@ActiveProfiles("test")
@Tag("integration")
class FileCleanupListenerTest {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockitoBean
    private R2Service r2Service;

    private void inCommittedTransaction(Runnable action) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> action.run());
    }

    @Test
    @DisplayName("Replacing a file deletes the old key after commit")
    void testFileReplaced_DeletesOldKey() {
        inCommittedTransaction(() -> eventPublisher.publishEvent(
                new FileReplacedEvent("uploads/12/old.png", "uploads/12/new.png")));

        verify(r2Service, times(1)).deleteFile("uploads/12/old.png");
        verify(r2Service, never()).deleteFile("uploads/12/new.png");
    }

    @Test
    @DisplayName("First-time file assignment (no old key) deletes nothing")
    void testFileReplaced_WithoutOldKey_DeletesNothing() {
        inCommittedTransaction(() -> eventPublisher.publishEvent(
                new FileReplacedEvent(null, "uploads/12/first.png")));

        verifyNoInteractions(r2Service);
    }

    @Test
    @DisplayName("A blank old key also deletes nothing — no R2 call with a meaningless key")
    void testFileReplaced_WithBlankOldKey_DeletesNothing() {
        inCommittedTransaction(() -> eventPublisher.publishEvent(
                new FileReplacedEvent("   ", "uploads/12/first.png")));

        verifyNoInteractions(r2Service);
    }

    @Test
    @DisplayName("Deleting a file removes the given key after commit")
    void testFileDeleted_DeletesKey() {
        inCommittedTransaction(() -> eventPublisher.publishEvent(
                new FileDeletedEvent(List.of("uploads/12/gone.png"))));

        verify(r2Service, times(1)).deleteFile("uploads/12/gone.png");
    }

    @Test
    @DisplayName("Deleting a record with many images: one event deletes every key in a single run")
    void testFileDeleted_DeletesEveryKeyInList() {
        List<String> keys = List.of("uploads/12/a.png", "uploads/12/b.jpg", "uploads/12/c.webp");

        inCommittedTransaction(() -> eventPublisher.publishEvent(new FileDeletedEvent(keys)));

        verify(r2Service, times(1)).deleteFile("uploads/12/a.png");
        verify(r2Service, times(1)).deleteFile("uploads/12/b.jpg");
        verify(r2Service, times(1)).deleteFile("uploads/12/c.webp");
        // deleteFile là lời gọi duy nhất listener được phép thực hiện — không thừa lời gọi nào.
        verify(r2Service, times(keys.size())).deleteFile(any());
    }

    @Test
    @DisplayName("An empty key list never touches R2")
    void testFileDeleted_WithEmptyList_DeletesNothing() {
        inCommittedTransaction(() -> eventPublisher.publishEvent(
                new FileDeletedEvent(List.of())));

        verifyNoInteractions(r2Service);
    }

    @Test
    @DisplayName("A null list is normalized to empty, no NullPointerException")
    void testFileDeleted_WithNullList_DeletesNothing() {
        inCommittedTransaction(() -> eventPublisher.publishEvent(
                new FileDeletedEvent(null)));

        verifyNoInteractions(r2Service);
    }

    @Test
    @DisplayName("The key list is an immutable copy — the publisher cannot mutate it afterwards")
    void testFileDeleted_KeysAreDefensiveCopy() {
        List<String> mutable = new ArrayList<>(List.of("uploads/12/a.png"));
        FileDeletedEvent event = new FileDeletedEvent(mutable);

        mutable.add("uploads/12/b.png");

        assertThat(event.fileKeys()).containsExactly("uploads/12/a.png");
    }

    @Test
    @DisplayName("A rolled-back transaction deletes no file — a still-referenced file is never lost")
    void testRollback_DeletesNothing() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            eventPublisher.publishEvent(
                    new FileReplacedEvent("uploads/12/old.png", "uploads/12/new.png"));
            eventPublisher.publishEvent(new FileDeletedEvent(
                    List.of("uploads/12/a.png", "uploads/12/b.png")));
            status.setRollbackOnly();
        });

        verifyNoInteractions(r2Service);
    }

    @Test
    @DisplayName("A delete failure does not break the committed operation — the error is swallowed")
    void testDeleteFailure_IsSwallowed() {
        doThrow(new RuntimeException("R2 unreachable"))
                .when(r2Service).deleteFile(any());

        // Không ném ra ngoài: caller đã nhận response 200 trước khi listener chạy.
        inCommittedTransaction(() -> eventPublisher.publishEvent(
                new FileDeletedEvent(List.of("uploads/12/gone.png"))));

        verify(r2Service, times(1)).deleteFile("uploads/12/gone.png");
    }

    @Test
    @DisplayName("One failing key must not block the remaining keys in the same list")
    void testDeleteFailure_OneKeyDoesNotBlockTheRest() {
        doThrow(new RuntimeException("R2 unreachable"))
                .when(r2Service).deleteFile("uploads/12/bad.png");

        inCommittedTransaction(() -> eventPublisher.publishEvent(new FileDeletedEvent(
                List.of("uploads/12/good.png", "uploads/12/bad.png", "uploads/12/also-good.png"))));

        // Key hỏng chỉ làm mất chính nó, các key sau vẫn phải được dọn.
        verify(r2Service, times(1)).deleteFile("uploads/12/good.png");
        verify(r2Service, times(1)).deleteFile("uploads/12/bad.png");
        verify(r2Service, times(1)).deleteFile("uploads/12/also-good.png");
    }

    /**
     * Listener phải nuốt lỗi, nên không cái gì ném ra được — nếu test này đỏ nghĩa là đã
     * có exception lọt ra ngoài {@code catch}.
     */
    @Test
    @DisplayName("No exception escapes the listener even when every R2 call fails")
    void testListener_NeverThrowsToCaller() {
        doThrow(new RuntimeException("R2 unreachable")).when(r2Service).deleteFile(any());

        assertThatCode(() -> inCommittedTransaction(() -> eventPublisher.publishEvent(
                new FileDeletedEvent(List.of("a.png", "b.png", "c.png")))))
                .doesNotThrowAnyException();
    }
}
