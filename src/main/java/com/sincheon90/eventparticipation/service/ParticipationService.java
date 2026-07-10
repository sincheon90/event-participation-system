package com.sincheon90.eventparticipation.service;

import com.sincheon90.eventparticipation.domain.participation.Participation;
import com.sincheon90.eventparticipation.api.dto.ParticipationRequest;
import com.sincheon90.eventparticipation.api.dto.ParticipationResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ParticipationService {

//    private final ParticipationRepository participationRepository;

    @Transactional
    public ParticipationResponse participate(Long eventId, Long missionId, ParticipationRequest request) {
        Participation participation = new Participation(request.getUserId(), eventId, missionId);

        return ParticipationResponse.success(participation.getId());
    }
}
