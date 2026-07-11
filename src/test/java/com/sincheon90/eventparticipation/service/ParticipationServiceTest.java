package com.sincheon90.eventparticipation.service;

import com.sincheon90.eventparticipation.api.dto.ParticipationRequest;
import com.sincheon90.eventparticipation.api.dto.ParticipationResponse;
import com.sincheon90.eventparticipation.domain.event.EventRepository;
import com.sincheon90.eventparticipation.domain.event.MissionRepository;
import com.sincheon90.eventparticipation.domain.participation.Participation;
import com.sincheon90.eventparticipation.domain.participation.ParticipationRepository;
import com.sincheon90.eventparticipation.domain.user.UserRepository;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ParticipationServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private MissionRepository missionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ParticipationRepository participationRepository;

    @InjectMocks
    private ParticipationService participationService;

    private ParticipationRequest request;

    @BeforeEach
    void setUp() {
        request = new ParticipationRequest();
        ReflectionTestUtils.setField(request, "userId", 100L);
    }

    @Test
    @DisplayName("イベントが存在しない場合、NOT_FOUNDを返す")
    void eventNotFound() {
        // given
        given(eventRepository.existsById(1L))
                .willReturn(false);

        // when
        ParticipationResponse response =
                participationService.participate(1L, 10L, request);

        // then
        assertThat(response.getStatus().name())
                .isEqualTo("NOT_FOUND");
        assertThat(response.getMessage())
                .isEqualTo("Event not found");

        verifyNoInteractions(
                missionRepository,
                userRepository,
                participationRepository
        );
    }

    @Test
    @DisplayName("対象イベントにミッションが存在しない場合、NOT_FOUNDを返す")
    void missionNotFound() {
        // given
        given(eventRepository.existsById(1L))
                .willReturn(true);

        given(missionRepository.existsByIdAndEventId(10L, 1L))
                .willReturn(false);

        // when
        ParticipationResponse response =
                participationService.participate(1L, 10L, request);

        // then
        assertThat(response.getStatus().name())
                .isEqualTo("NOT_FOUND");
        assertThat(response.getMessage())
                .isEqualTo("Mission not found");

        verifyNoInteractions(
                userRepository,
                participationRepository
        );
    }

    @Test
    @DisplayName("ユーザーが存在しない場合、NOT_FOUNDを返す")
    void userNotFound() {
        // given
        given(eventRepository.existsById(1L))
                .willReturn(true);

        given(missionRepository.existsByIdAndEventId(10L, 1L))
                .willReturn(true);

        given(userRepository.existsById(100L))
                .willReturn(false);

        // when
        ParticipationResponse response =
                participationService.participate(1L, 10L, request);

        // then
        assertThat(response.getStatus().name())
                .isEqualTo("NOT_FOUND");
        assertThat(response.getMessage())
                .isEqualTo("User not found");

        // ユーザーが存在しない場合、参加情報が保存されないことを確認する
        verify(
                participationRepository,
                never()
        ).saveAndFlush(any());
    }

    @Test
    @DisplayName("すべての対象が存在する場合、参加情報を保存してSUCCESSを返す")
    void participationSuccess() {
        // given
        given(eventRepository.existsById(1L))
                .willReturn(true);

        given(missionRepository.existsByIdAndEventId(10L, 1L))
                .willReturn(true);

        given(userRepository.existsById(100L))
                .willReturn(true);

        given(participationRepository.saveAndFlush(
                any(Participation.class)
        )).willAnswer(invocation -> {
            Participation participation = invocation.getArgument(0);

            // JPAが保存時にIDを設定する動作を再現する
            ReflectionTestUtils.setField(
                    participation,
                    "id",
                    1L
            );

            return participation;
        });

        // when
        ParticipationResponse response =
                participationService.participate(1L, 10L, request);

        // then
        assertThat(response.getParticipationId())
                .isEqualTo(1L);
        assertThat(response.getStatus().name())
                .isEqualTo("SUCCESS");
        assertThat(response.getMessage())
                .isEqualTo("Participation completed");

        ArgumentCaptor<Participation> captor =
                ArgumentCaptor.forClass(Participation.class);

        verify(participationRepository)
                .saveAndFlush(captor.capture());

        Participation savedParticipation = captor.getValue();

        assertThat(savedParticipation.getEventId())
                .isEqualTo(1L);
        assertThat(savedParticipation.getMissionId())
                .isEqualTo(10L);
        assertThat(savedParticipation.getUserId())
                .isEqualTo(100L);
    }

    @Test
    @DisplayName("DBのUnique制約に違反した場合、DUPLICATEを返す")
    void duplicateParticipation() {
        // given
        given(eventRepository.existsById(1L))
                .willReturn(true);

        given(missionRepository.existsByIdAndEventId(10L, 1L))
                .willReturn(true);

        given(userRepository.existsById(100L))
                .willReturn(true);

        given(participationRepository.saveAndFlush(
                any(Participation.class)
        )).willThrow(
                new DataIntegrityViolationException(
                        "Duplicate participation"
                )
        );

        // when
        ParticipationResponse response =
                participationService.participate(1L, 10L, request);

        // then
        assertThat(response.getParticipationId())
                .isNull();
        assertThat(response.getStatus().name())
                .isEqualTo("DUPLICATE");
        assertThat(response.getMessage())
                .isEqualTo(
                        "User already participated in this mission"
                );
    }
}