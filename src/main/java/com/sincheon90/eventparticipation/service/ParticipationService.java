package com.sincheon90.eventparticipation.service;

import com.sincheon90.eventparticipation.api.dto.ParticipationRequest;
import com.sincheon90.eventparticipation.api.dto.ParticipationResponse;
import com.sincheon90.eventparticipation.domain.event.EventRepository;
import com.sincheon90.eventparticipation.domain.event.MissionRepository;
import com.sincheon90.eventparticipation.domain.participation.Participation;
import com.sincheon90.eventparticipation.domain.participation.ParticipationRepository;
import com.sincheon90.eventparticipation.domain.user.UserRepository;
import com.sincheon90.eventparticipation.redis.ParticipationRedisService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ParticipationService {

    private final EventRepository eventRepository;

    private final MissionRepository missionRepository;

    private final UserRepository userRepository;

    private final ParticipationRepository participationRepository;

    private final ParticipationRedisService participationRedisService;

    @Transactional
    public ParticipationResponse participate(Long eventId, Long missionId, ParticipationRequest request) {
        ParticipationResponse result = process(eventId, missionId, request.getUserId());

        // Kafkaへ参加結果を非同期送信する

        return result;
    }

    private ParticipationResponse process(Long eventId, Long missionId, Long userId) {
        // イベントの存在確認
        if (!eventRepository.existsById(eventId)) {
            return ParticipationResponse.eventNotFound();
        }

        // 該当イベントのミッションの存在確認
        if (!missionRepository.existsByIdAndEventId(missionId, eventId)) {
            return ParticipationResponse.missionNotFound();
        }

        // ユーザーの存在確認
        if (!userRepository.existsById(userId)) {
            return ParticipationResponse.userNotFound();
        }

        // Redisによる重複確認
        if (!participationRedisService.tryLock(eventId, missionId, userId)) {
            return ParticipationResponse.duplicate();
        }

        // 参加データを生成する
        Participation participation = Participation.builder()
                .eventId(eventId)
                .missionId(missionId)
                .userId(userId)
                .build();

        // DBのUnique制約を最終的に利用する
        try {
            participationRepository.saveAndFlush(participation);

            return ParticipationResponse.success(participation.getId());
        } catch (DataIntegrityViolationException e) {

            return ParticipationResponse.duplicate();
        }
    }
}
