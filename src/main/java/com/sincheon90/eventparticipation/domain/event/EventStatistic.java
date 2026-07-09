package com.sincheon90.eventparticipation.domain.event;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "event_statistics",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"event_id", "mission_id"})
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventStatistic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long eventId;

    private Long missionId;

    private Long participationCount;

    private LocalDateTime updatedAt;

    public EventStatistic(Long eventId, Long missionId) {
        this.eventId = eventId;
        this.missionId = missionId;
        this.participationCount = 0L;
    }

    public void increaseParticipationCount() {
        this.participationCount++;
    }

    @PrePersist
    public void prePersist() {
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

}
