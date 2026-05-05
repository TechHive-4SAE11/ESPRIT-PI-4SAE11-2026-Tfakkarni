package org.techhive.medicalservice.service.coaching;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.techhive.medicalservice.dto.coaching.CoachingNotificationResponse;
import org.techhive.medicalservice.entity.MedicalFolder;
import org.techhive.medicalservice.entity.coaching.CoachingGoal;
import org.techhive.medicalservice.entity.coaching.CoachingNotification;
import org.techhive.medicalservice.repository.CoachingNotificationRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CoachingNotificationServiceTest {

    @Mock
    private CoachingNotificationRepository coachingNotificationRepository;

    private CoachingNotificationService service;

    @BeforeEach
    void setUp() {
        service = new CoachingNotificationService(coachingNotificationRepository);
    }

    @Test
    void notifyUserSkipsMissingRecipientOrFolderAndSavesValidNotification() {
        MedicalFolder folder = folder(10L);
        CoachingGoal goal = CoachingGoal.builder().id(20L).medicalFolder(folder).build();

        service.notifyUser(null, folder, goal, "GOAL", "Title", "Message");
        service.notifyUser("   ", folder, goal, "GOAL", "Title", "Message");
        service.notifyUser("patient-a", null, goal, "GOAL", "Title", "Message");

        verifyNoInteractions(coachingNotificationRepository);

        service.notifyUser("patient-a", folder, goal, "GOAL", "Title", "Message");

        ArgumentCaptor<CoachingNotification> captor = ArgumentCaptor.forClass(CoachingNotification.class);
        verify(coachingNotificationRepository).save(captor.capture());
        CoachingNotification saved = captor.getValue();
        assertEquals("patient-a", saved.getRecipientUserId());
        assertSame(folder, saved.getMedicalFolder());
        assertSame(goal, saved.getCoachingGoal());
        assertEquals("GOAL", saved.getEventType());
        assertEquals("Title", saved.getTitle());
        assertEquals("Message", saved.getMessage());
        assertFalse(saved.isRead());
    }

    @Test
    void listMyNotificationsMapsGoalAndNullGoalAndUnreadCountReturnsCount() {
        MedicalFolder folder = folder(10L);
        CoachingGoal goal = CoachingGoal.builder().id(20L).medicalFolder(folder).build();
        CoachingNotification withGoal = notification(1L, "patient-a", folder, goal, false, null);
        CoachingNotification withoutGoal = notification(2L, "patient-a", folder, null, true,
                LocalDateTime.of(2026, 4, 5, 6, 7));
        when(coachingNotificationRepository.findByRecipientUserIdOrderByCreatedAtDesc("patient-a"))
                .thenReturn(List.of(withGoal, withoutGoal));
        when(coachingNotificationRepository.countByRecipientUserIdAndReadFalse("patient-a")).thenReturn(3L);

        List<CoachingNotificationResponse> responses = service.listMyNotifications("patient-a");
        Map<String, Long> count = service.unreadCount("patient-a");

        assertEquals(2, responses.size());
        assertEquals(1L, responses.get(0).getId());
        assertEquals(10L, responses.get(0).getFolderId());
        assertEquals(20L, responses.get(0).getGoalId());
        assertEquals("GOAL", responses.get(0).getEventType());
        assertEquals("Title 1", responses.get(0).getTitle());
        assertFalse(responses.get(0).isRead());
        assertNull(responses.get(1).getGoalId());
        assertTrue(responses.get(1).isRead());
        assertEquals(LocalDateTime.of(2026, 4, 5, 6, 7), responses.get(1).getReadAt());
        assertEquals(3L, count.get("count"));
    }

    @Test
    void markReadThrowsNotFoundOrForbiddenAndSavesUnreadNotification() {
        when(coachingNotificationRepository.findById(404L)).thenReturn(Optional.empty());

        ResponseStatusException notFound = assertThrows(ResponseStatusException.class,
                () -> service.markRead("patient-a", 404L));
        assertEquals(HttpStatus.NOT_FOUND, notFound.getStatusCode());

        CoachingNotification otherUser = notification(2L, "patient-b", folder(10L), null, false, null);
        when(coachingNotificationRepository.findById(2L)).thenReturn(Optional.of(otherUser));
        ResponseStatusException forbidden = assertThrows(ResponseStatusException.class,
                () -> service.markRead("patient-a", 2L));
        assertEquals(HttpStatus.FORBIDDEN, forbidden.getStatusCode());

        CoachingNotification unread = notification(3L, "patient-a", folder(10L), null, false, null);
        when(coachingNotificationRepository.findById(3L)).thenReturn(Optional.of(unread));
        when(coachingNotificationRepository.save(unread)).thenReturn(unread);

        CoachingNotificationResponse response = service.markRead("patient-a", 3L);

        assertTrue(unread.isRead());
        assertNotNull(unread.getReadAt());
        assertEquals(3L, response.getId());
        assertTrue(response.isRead());
        verify(coachingNotificationRepository).save(unread);
    }

    @Test
    void markReadDoesNotSaveAlreadyReadNotification() {
        LocalDateTime readAt = LocalDateTime.of(2026, 3, 4, 5, 6);
        CoachingNotification alreadyRead = notification(3L, "patient-a", folder(10L), null, true, readAt);
        when(coachingNotificationRepository.findById(3L)).thenReturn(Optional.of(alreadyRead));

        CoachingNotificationResponse response = service.markRead("patient-a", 3L);

        assertEquals(readAt, response.getReadAt());
        assertTrue(response.isRead());
        verify(coachingNotificationRepository, never()).save(any());
    }

    @Test
    void markAllReadSavesOnlyWhenAnyUnreadChanged() {
        MedicalFolder folder = folder(10L);
        CoachingNotification unread = notification(1L, "patient-a", folder, null, false, null);
        CoachingNotification read = notification(2L, "patient-a", folder, null, true,
                LocalDateTime.of(2026, 1, 2, 3, 4));
        when(coachingNotificationRepository.findByRecipientUserIdOrderByCreatedAtDesc("patient-a"))
                .thenReturn(List.of(unread, read))
                .thenReturn(List.of(read));

        service.markAllRead("patient-a");
        assertTrue(unread.isRead());
        assertNotNull(unread.getReadAt());
        assertEquals(LocalDateTime.of(2026, 1, 2, 3, 4), read.getReadAt());
        verify(coachingNotificationRepository).saveAll(List.of(unread, read));

        service.markAllRead("patient-a");
        verifyNoMoreInteractions(coachingNotificationRepository);
    }

    private static MedicalFolder folder(Long id) {
        return MedicalFolder.builder()
                .id(id)
                .patientId("patient-a")
                .doctorId("doctor-a")
                .build();
    }

    private static CoachingNotification notification(Long id,
                                                     String recipientUserId,
                                                     MedicalFolder folder,
                                                     CoachingGoal goal,
                                                     boolean read,
                                                     LocalDateTime readAt) {
        return CoachingNotification.builder()
                .id(id)
                .recipientUserId(recipientUserId)
                .medicalFolder(folder)
                .coachingGoal(goal)
                .eventType("GOAL")
                .title("Title " + id)
                .message("Message " + id)
                .createdAt(LocalDateTime.of(2026, 1, id.intValue(), 8, 30))
                .read(read)
                .readAt(readAt)
                .build();
    }
}
