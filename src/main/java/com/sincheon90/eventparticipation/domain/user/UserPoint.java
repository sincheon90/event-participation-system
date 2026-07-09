package com.sincheon90.eventparticipation.domain.user;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "user_points",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id"})
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserPoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private Integer point;

    private LocalDateTime updatedAt;

    public UserPoint(Long userId) {
        this.userId = userId;
        this.point = 0;
    }

    public void addPoint(int point) {
        this.point += point;
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
