package com.sincheon90.eventparticipation.kafka;

import com.sincheon90.eventparticipation.domain.participation.ParticipationResultStatus;

import java.time.LocalDateTime;

public record ParticipationResultEvent(
        Long participationId,
        Long eventId,
        Long missionId,
        Long userId,
        ParticipationResultStatus resultStatus,
        String message,
        LocalDateTime occurredAt
) {
}