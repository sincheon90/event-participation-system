package com.sincheon90.eventparticipation.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Entity
@Table(
        name="participation",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"event_id", "mission_id", "user_id"})
        }
)
public class Participation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id")
    private Long eventId;

    @Column(name = "mission_id")
    private Long missionId;

    @Column(name = "user_id")
    private Long userId;

    private LocalDateTime createdAt;


    public Participation(Long userId, Long eventId, Long missionId) {
        this.userId = userId;
        this.eventId = eventId;
        this.missionId = missionId;
    }

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
