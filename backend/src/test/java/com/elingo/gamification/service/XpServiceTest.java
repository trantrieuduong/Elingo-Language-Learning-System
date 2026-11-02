package com.elingo.gamification.service;

import com.elingo.common.event.LevelUpEvent;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.gamification.entity.XpEvent;
import com.elingo.gamification.entity.XpSource;
import com.elingo.gamification.repository.UserXpRepository;
import com.elingo.gamification.repository.XpEventRepository;
import com.elingo.gamification.service.impl.XpServiceImpl;
import com.elingo.user.entity.User;
import com.elingo.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class XpServiceTest {

    @Mock
    private UserXpRepository userXpRepository;

    @Mock
    private XpEventRepository xpEventRepository;

    @Mock
    private ApplicationEventPublisher publisher;

    @Mock
    private UserRepository userRepository;

    @Mock
    private LevelCatalog levelCatalog;

    @InjectMocks
    private XpServiceImpl xpService;

    private static final Long USER_ID = 1L;
    private static final Long REF_ID = 100L;
    private static final int AMOUNT = 50;

    private User testUser;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(xpService, "userXpRepository", userXpRepository);
        ReflectionTestUtils.setField(xpService, "xpEventRepository", xpEventRepository);
        ReflectionTestUtils.setField(xpService, "publisher", publisher);
        ReflectionTestUtils.setField(xpService, "userRepository", userRepository);
        ReflectionTestUtils.setField(xpService, "levelCatalog", levelCatalog);

        testUser = User.builder()
                .id(USER_ID)
                .username("testuser")
                .email("test@example.com")
                .build();
    }

    @Nested
    @DisplayName("addXp")
    class AddXpTests {

        @Test
        @DisplayName("Add experience points successfully with level increase")
        void addXp_Success_WithLevelUp() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(userXpRepository.findTotalXp(USER_ID)).thenReturn(120);
            when(levelCatalog.levelFor(120)).thenReturn(2);
            when(userXpRepository.raiseLevel(USER_ID, 2)).thenReturn(1);

            xpService.addXp(USER_ID, AMOUNT, XpSource.LESSON_DICTATION, REF_ID);

            verify(userXpRepository).upsertAddXp(USER_ID, AMOUNT);

            ArgumentCaptor<XpEvent> eventCaptor = ArgumentCaptor.forClass(XpEvent.class);
            verify(xpEventRepository).save(eventCaptor.capture());
            XpEvent savedEvent = eventCaptor.getValue();
            assertThat(savedEvent.getUser()).isEqualTo(testUser);
            assertThat(savedEvent.getSource()).isEqualTo(XpSource.LESSON_DICTATION);
            assertThat(savedEvent.getRefId()).isEqualTo(REF_ID);
            assertThat(savedEvent.getAmount()).isEqualTo(AMOUNT);

            verify(levelCatalog).levelFor(120);
            verify(userXpRepository).raiseLevel(USER_ID, 2);

            ArgumentCaptor<LevelUpEvent> levelUpCaptor = ArgumentCaptor.forClass(LevelUpEvent.class);
            verify(publisher).publishEvent(levelUpCaptor.capture());
            LevelUpEvent levelUpEvent = levelUpCaptor.getValue();
            assertThat(levelUpEvent.userId()).isEqualTo(USER_ID);
            assertThat(levelUpEvent.newLevel()).isEqualTo(2);
        }

        @Test
        @DisplayName("Add experience points successfully without level increase")
        void addXp_Success_WithoutLevelUp() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(userXpRepository.findTotalXp(USER_ID)).thenReturn(45);
            when(levelCatalog.levelFor(45)).thenReturn(1);
            when(userXpRepository.raiseLevel(USER_ID, 1)).thenReturn(0);

            xpService.addXp(USER_ID, AMOUNT, XpSource.FLASHCARD_REVIEW, REF_ID);

            verify(userXpRepository).upsertAddXp(USER_ID, AMOUNT);
            verify(xpEventRepository).save(any(XpEvent.class));
            verify(userXpRepository).raiseLevel(USER_ID, 1);
            verify(publisher, never()).publishEvent(any(LevelUpEvent.class));
        }

        @Test
        @DisplayName("Add experience points failed when user not found")
        void addXp_Fail_UserNotFound() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> xpService.addXp(USER_ID, AMOUNT, XpSource.BATTLE_WIN, REF_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.USER_NOT_FOUND));

            verify(userXpRepository, never()).upsertAddXp(anyLong(), anyInt());
            verify(xpEventRepository, never()).save(any(XpEvent.class));
        }

        @Test
        @DisplayName("Add experience points with zero amount successfully")
        void addXp_Success_ZeroAmount() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(userXpRepository.findTotalXp(USER_ID)).thenReturn(10);
            when(levelCatalog.levelFor(10)).thenReturn(1);
            when(userXpRepository.raiseLevel(USER_ID, 1)).thenReturn(0);

            xpService.addXp(USER_ID, 0, XpSource.LESSON_SHADOWING, null);

            verify(userXpRepository).upsertAddXp(USER_ID, 0);
            verify(xpEventRepository).save(any(XpEvent.class));
        }
    }
}
