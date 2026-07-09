package com.sincheon90.eventparticipation.domain.event;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MissionRepository extends JpaRepository<Mission, Long> {

    boolean existsByIdAndEventId(Long id, Long eventId);
}
