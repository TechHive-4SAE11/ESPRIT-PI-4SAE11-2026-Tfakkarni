package org.techhive.medicalservice.service.coaching;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.techhive.medicalservice.dto.coaching.CoachingNotificationResponse;
import org.techhive.medicalservice.entity.MedicalFolder;
import org.techhive.medicalservice.entity.coaching.CoachingGoal;
import org.techhive.medicalservice.entity.coaching.CoachingNotification;
import org.techhive.medicalservice.repository.CoachingNotificationRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CoachingNotificationService {

    private final CoachingNotificationRepository coachingNotificationRepository;

    @Transactional
    public void notifyUser(String recipientUserId,
                           MedicalFolder folder,
                           CoachingGoal goal,
                           String eventType,
                           String title,
                           String message) {
        if (recipientUserId == null || recipientUserId.isBlank() || folder == null) {
            return;
        }
        coachingNotificationRepository.save(CoachingNotification.builder()
                .recipientUserId(recipientUserId)
                .medicalFolder(folder)
                .coachingGoal(goal)
                .eventType(eventType)
                .title(title)
                .message(message)
                .build());
    }

    @Transactional(readOnly = true)
    public List<CoachingNotificationResponse> listMyNotifications(String userId) {
        return coachingNotificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Long> unreadCount(String userId) {
        return Map.of("count", coachingNotificationRepository.countByRecipientUserIdAndReadFalse(userId));
    }

    @Transactional
    public CoachingNotificationResponse markRead(String userId, Long notificationId) {
        CoachingNotification notif = coachingNotificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
        if (!notif.getRecipientUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed");
        }
        if (!notif.isRead()) {
            notif.setRead(true);
            notif.setReadAt(LocalDateTime.now());
            notif = coachingNotificationRepository.save(notif);
        }
        return toResponse(notif);
    }

    @Transactional
    public void markAllRead(String userId) {
        List<CoachingNotification> list = coachingNotificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(userId);
        LocalDateTime now = LocalDateTime.now();
        boolean changed = false;
        for (CoachingNotification n : list) {
            if (!n.isRead()) {
                n.setRead(true);
                n.setReadAt(now);
                changed = true;
            }
        }
        if (changed) {
            coachingNotificationRepository.saveAll(list);
        }
    }

    private CoachingNotificationResponse toResponse(CoachingNotification n) {
        return CoachingNotificationResponse.builder()
                .id(n.getId())
                .folderId(n.getMedicalFolder().getId())
                .goalId(n.getCoachingGoal() != null ? n.getCoachingGoal().getId() : null)
                .eventType(n.getEventType())
                .title(n.getTitle())
                .message(n.getMessage())
                .read(n.isRead())
                .createdAt(n.getCreatedAt())
                .readAt(n.getReadAt())
                .build();
    }
}
