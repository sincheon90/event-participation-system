package com.sincheon90.eventparticipation.domain.participation;

import jakarta.persistence.Enumerated;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "participation_result_logs")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ParticipationResultLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long participationId;

    private Long userId;

    private Long eventId;

    private Long missionId;

    private String resultType;

    @Enumerated(EnumType.STRING)
    private ParticipationResultStatus status;

    @Column(columnDefinition = "TEXT")
    private String message;

    private LocalDateTime eventCreatedAt;

    private LocalDateTime createdAt;

    @Builder
    public ParticipationResultLog(
            Long participationId,
            Long userId,
            Long eventId,
            Long missionId,
            String resultType,
            ParticipationResultStatus status,
            String message,
            LocalDateTime eventCreatedAt
    ) {
        this.participationId = participationId;
        this.userId = userId;
        this.eventId = eventId;
        this.missionId = missionId;
        this.resultType = resultType;
        this.status = status;
        this.message = message;
        this.eventCreatedAt = eventCreatedAt;
    }

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
