package com.sincheon90.eventparticipation.service;

import com.sincheon90.eventparticipation.api.dto.ParticipationRequest;
import com.sincheon90.eventparticipation.api.dto.ParticipationResponse;
import com.sincheon90.eventparticipation.domain.event.EventRepository;
import com.sincheon90.eventparticipation.domain.event.MissionRepository;
import com.sincheon90.eventparticipation.domain.participation.Participation;
import com.sincheon90.eventparticipation.domain.participation.ParticipationRepository;
import com.sincheon90.eventparticipation.domain.user.UserRepository;
import com.sincheon90.eventparticipation.kafka.ParticipationResultEvent;
import com.sincheon90.eventparticipation.kafka.producer.ParticipationEventProducer;
import com.sincheon90.eventparticipation.redis.ParticipationRedisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ParticipationServiceTest {

    private static final Long EVENT_ID = 1L;
    private static final Long MISSION_ID = 10L;
    private static final Long USER_ID = 100L;
    private static final Long PARTICIPATION_ID = 1L;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private MissionRepository missionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ParticipationRepository participationRepository;

    @Mock
    private ParticipationRedisService participationRedisService;

    @Mock
    private ParticipationEventProducer participationEventProducer;

    @InjectMocks
    private ParticipationService participationService;

    private ParticipationRequest request;

    @BeforeEach
    void setUp() {
        request = new ParticipationRequest();
        ReflectionTestUtils.setField(request, "userId", 100L);
    }

    @Test
    @DisplayName("イベントが存在しない場合、NOT_FOUNDを返してKafkaに結果を送信する")
    void eventNotFound() {
        // given
        given(eventRepository.existsById(EVENT_ID))
                .willReturn(false);

        ArgumentCaptor<ParticipationResultEvent> eventCaptor =
                ArgumentCaptor.forClass(ParticipationResultEvent.class);

        // when
        ParticipationResponse response =
                participationService.participate(
                        EVENT_ID,
                        MISSION_ID,
                        request
                );

        // then
        assertThat(response.getStatus().name())
                .isEqualTo("NOT_FOUND");

        assertThat(response.getMessage())
                .isEqualTo("Event not found");

        verify(participationEventProducer)
                .send(eventCaptor.capture());

        ParticipationResultEvent sentEvent =
                eventCaptor.getValue();

        assertThat(sentEvent.participationId())
                .isNull();

        assertThat(sentEvent.eventId())
                .isEqualTo(EVENT_ID);

        assertThat(sentEvent.missionId())
                .isEqualTo(MISSION_ID);

        assertThat(sentEvent.userId())
                .isEqualTo(USER_ID);

        assertThat(sentEvent.resultStatus().name())
                .isEqualTo("NOT_FOUND");

        assertThat(sentEvent.message())
                .isEqualTo("Event not found");

        assertThat(sentEvent.occurredAt())
                .isNotNull();

        verifyNoInteractions(
                missionRepository,
                userRepository,
                participationRedisService,
                participationRepository
        );
    }

    @Test
    @DisplayName("対象イベントにミッションが存在しない場合、NOT_FOUNDを返してKafkaに結果を送信する")
    void missionNotFound() {
        // given
        given(eventRepository.existsById(EVENT_ID))
                .willReturn(true);

        given(missionRepository.existsByIdAndEventId(
                MISSION_ID,
                EVENT_ID
        )).willReturn(false);

        ArgumentCaptor<ParticipationResultEvent> eventCaptor =
                ArgumentCaptor.forClass(ParticipationResultEvent.class);

        // when
        ParticipationResponse response =
                participationService.participate(
                        EVENT_ID,
                        MISSION_ID,
                        request
                );

        // then
        assertThat(response.getStatus().name())
                .isEqualTo("NOT_FOUND");

        assertThat(response.getMessage())
                .isEqualTo("Mission not found");

        verify(participationEventProducer)
                .send(eventCaptor.capture());

        ParticipationResultEvent sentEvent =
                eventCaptor.getValue();

        assertThat(sentEvent.participationId())
                .isNull();

        assertThat(sentEvent.resultStatus().name())
                .isEqualTo("NOT_FOUND");

        assertThat(sentEvent.message())
                .isEqualTo("Mission not found");

        verifyNoInteractions(
                userRepository,
                participationRedisService,
                participationRepository
        );
    }

    @Test
    @DisplayName("ユーザーが存在しない場合、NOT_FOUNDを返してKafkaに結果を送信する")
    void userNotFound() {
        // given
        given(eventRepository.existsById(EVENT_ID))
                .willReturn(true);

        given(missionRepository.existsByIdAndEventId(
                MISSION_ID,
                EVENT_ID
        )).willReturn(true);

        given(userRepository.existsById(USER_ID))
                .willReturn(false);

        ArgumentCaptor<ParticipationResultEvent> eventCaptor =
                ArgumentCaptor.forClass(ParticipationResultEvent.class);

        // when
        ParticipationResponse response =
                participationService.participate(
                        EVENT_ID,
                        MISSION_ID,
                        request
                );

        // then
        assertThat(response.getStatus().name())
                .isEqualTo("NOT_FOUND");

        assertThat(response.getMessage())
                .isEqualTo("User not found");

        verify(participationEventProducer)
                .send(eventCaptor.capture());

        ParticipationResultEvent sentEvent =
                eventCaptor.getValue();

        assertThat(sentEvent.participationId())
                .isNull();

        assertThat(sentEvent.resultStatus().name())
                .isEqualTo("NOT_FOUND");

        assertThat(sentEvent.message())
                .isEqualTo("User not found");

        verify(
                participationRedisService,
                never()
        ).tryLock(any(), any(), any());

        verify(
                participationRepository,
                never()
        ).saveAndFlush(any());
    }

    @Test
    @DisplayName("Redisで重複と判断された場合、DUPLICATEを返してKafkaに結果を送信する")
    void duplicateByRedis() {
        // given
        given(eventRepository.existsById(EVENT_ID))
                .willReturn(true);

        given(missionRepository.existsByIdAndEventId(
                MISSION_ID,
                EVENT_ID
        )).willReturn(true);

        given(userRepository.existsById(USER_ID))
                .willReturn(true);

        given(participationRedisService.tryLock(
                EVENT_ID,
                MISSION_ID,
                USER_ID
        )).willReturn(false);

        ArgumentCaptor<ParticipationResultEvent> eventCaptor =
                ArgumentCaptor.forClass(ParticipationResultEvent.class);

        // when
        ParticipationResponse response =
                participationService.participate(
                        EVENT_ID,
                        MISSION_ID,
                        request
                );

        // then
        assertThat(response.getParticipationId())
                .isNull();

        assertThat(response.getStatus().name())
                .isEqualTo("DUPLICATE");

        assertThat(response.getMessage())
                .isEqualTo(
                        "User already participated in this mission"
                );

        verify(participationEventProducer)
                .send(eventCaptor.capture());

        ParticipationResultEvent sentEvent =
                eventCaptor.getValue();

        assertThat(sentEvent.participationId())
                .isNull();

        assertThat(sentEvent.resultStatus().name())
                .isEqualTo("DUPLICATE");

        verify(
                participationRepository,
                never()
        ).saveAndFlush(any());
    }

    @Test
    @DisplayName("すべての対象が存在する場合、参加情報を保存してSUCCESS結果をKafkaに送信する")
    void participationSuccess() {
        // given
        given(eventRepository.existsById(EVENT_ID))
                .willReturn(true);

        given(missionRepository.existsByIdAndEventId(
                MISSION_ID,
                EVENT_ID
        )).willReturn(true);

        given(userRepository.existsById(USER_ID))
                .willReturn(true);

        given(participationRedisService.tryLock(
                EVENT_ID,
                MISSION_ID,
                USER_ID
        )).willReturn(true);

        given(participationRepository.saveAndFlush(
                any(Participation.class)
        )).willAnswer(invocation -> {
            Participation participation = invocation.getArgument(0);

            ReflectionTestUtils.setField(
                    participation,
                    "id",
                    PARTICIPATION_ID
            );

            return participation;
        });

        ArgumentCaptor<Participation> participationCaptor =
                ArgumentCaptor.forClass(Participation.class);

        ArgumentCaptor<ParticipationResultEvent> eventCaptor =
                ArgumentCaptor.forClass(ParticipationResultEvent.class);

        // when
        ParticipationResponse response =
                participationService.participate(
                        EVENT_ID,
                        MISSION_ID,
                        request
                );

        // then
        assertThat(response.getParticipationId())
                .isEqualTo(PARTICIPATION_ID);

        assertThat(response.getStatus().name())
                .isEqualTo("SUCCESS");

        assertThat(response.getMessage())
                .isEqualTo("Participation completed");

        verify(participationRepository)
                .saveAndFlush(participationCaptor.capture());

        Participation savedParticipation =
                participationCaptor.getValue();

        assertThat(savedParticipation.getEventId())
                .isEqualTo(EVENT_ID);

        assertThat(savedParticipation.getMissionId())
                .isEqualTo(MISSION_ID);

        assertThat(savedParticipation.getUserId())
                .isEqualTo(USER_ID);

        verify(participationEventProducer)
                .send(eventCaptor.capture());

        ParticipationResultEvent sentEvent =
                eventCaptor.getValue();

        assertThat(sentEvent.participationId())
                .isEqualTo(PARTICIPATION_ID);

        assertThat(sentEvent.eventId())
                .isEqualTo(EVENT_ID);

        assertThat(sentEvent.missionId())
                .isEqualTo(MISSION_ID);

        assertThat(sentEvent.userId())
                .isEqualTo(USER_ID);

        assertThat(sentEvent.resultStatus().name())
                .isEqualTo("SUCCESS");

        assertThat(sentEvent.message())
                .isEqualTo("Participation completed");

        assertThat(sentEvent.occurredAt())
                .isNotNull();
    }

    @Test
    @DisplayName("DBのUnique制約に違反した場合、DUPLICATE結果をKafkaに送信する")
    void duplicateParticipation() {
        // given
        given(eventRepository.existsById(EVENT_ID))
                .willReturn(true);

        given(missionRepository.existsByIdAndEventId(
                MISSION_ID,
                EVENT_ID
        )).willReturn(true);

        given(userRepository.existsById(USER_ID))
                .willReturn(true);

        given(participationRedisService.tryLock(
                EVENT_ID,
                MISSION_ID,
                USER_ID
        )).willReturn(true);

        given(participationRepository.saveAndFlush(
                any(Participation.class)
        )).willThrow(
                new DataIntegrityViolationException(
                        "Duplicate participation"
                )
        );

        ArgumentCaptor<ParticipationResultEvent> eventCaptor =
                ArgumentCaptor.forClass(ParticipationResultEvent.class);

        // when
        ParticipationResponse response =
                participationService.participate(
                        EVENT_ID,
                        MISSION_ID,
                        request
                );

        // then
        assertThat(response.getParticipationId())
                .isNull();

        assertThat(response.getStatus().name())
                .isEqualTo("DUPLICATE");

        assertThat(response.getMessage())
                .isEqualTo(
                        "User already participated in this mission"
                );

        verify(participationEventProducer)
                .send(eventCaptor.capture());

        ParticipationResultEvent sentEvent =
                eventCaptor.getValue();

        assertThat(sentEvent.participationId())
                .isNull();

        assertThat(sentEvent.eventId())
                .isEqualTo(EVENT_ID);

        assertThat(sentEvent.missionId())
                .isEqualTo(MISSION_ID);

        assertThat(sentEvent.userId())
                .isEqualTo(USER_ID);

        assertThat(sentEvent.resultStatus().name())
                .isEqualTo("DUPLICATE");

        assertThat(sentEvent.message())
                .isEqualTo(
                        "User already participated in this mission"
                );
    }
}