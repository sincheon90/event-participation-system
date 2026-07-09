package com.sincheon90.eventparticipation.domain.notification;

import jakarta.persistence.Enumerated;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "notification_logs")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long participationId;

    private Long userId;

    private Long eventId;

    private Long missionId;

    private String notificationType;

    @Enumerated(EnumType.STRING)
    private NotificationStatus status;

    @Column(columnDefinition = "TEXT")
    private String message;

    private LocalDateTime processedAt;

    public NotificationLog(
            Long participationId,
            Long userId,
            Long eventId,
            Long missionId,
            String notificationType,
            NotificationStatus status,
            String message
    ) {
        this.participationId = participationId;
        this.userId = userId;
        this.eventId = eventId;
        this.missionId = missionId;
        this.notificationType = notificationType;
        this.status = status;
        this.message = message;
    }

    @PrePersist
    public void prePersist() {
        this.processedAt = LocalDateTime.now();
    }
}
