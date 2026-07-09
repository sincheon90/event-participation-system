package com.sincheon90.eventparticipation.domain.point;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "point_histories")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private Long eventId;

    private Long missionId;

    private Integer point;

    private String reason;

    private LocalDateTime createdAt;

    public PointHistory(Long userId, Long eventId, Long missionId, Integer point, String reason) {
        this.userId = userId;
        this.eventId = eventId;
        this.missionId = missionId;
        this.point = point;
        this.reason = reason;
    }

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
