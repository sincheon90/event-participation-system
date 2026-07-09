package com.sincheon90.eventparticipation.domain.participation;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ParticipationRepository extends JpaRepository<Participation, Long> {

    boolean existsByUserIdAndEventIdAndMissionId(Long userId, Long eventId, Long missionId);
}
