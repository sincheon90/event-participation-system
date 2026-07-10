package com.sincheon90.eventparticipation.domain.participation;

import com.sincheon90.eventparticipation.domain.event.Event;
import com.sincheon90.eventparticipation.domain.event.EventRepository;
import com.sincheon90.eventparticipation.domain.event.Mission;
import com.sincheon90.eventparticipation.domain.event.MissionRepository;
import com.sincheon90.eventparticipation.domain.user.User;
import com.sincheon90.eventparticipation.domain.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ParticipationResultLogRepositoryTest {

    @Autowired
    private ParticipationResultLogRepository participationResultLogRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private MissionRepository missionRepository;

    @Test
    void saveParticipationResultLog() {
        // given
        User user = userRepository.save(
                User.builder()
                        .name("test-user")
                        .build()
        );

        Event event = eventRepository.save(
                Event.builder()
                        .title("test-event")
                        .build()
        );

        Mission mission = missionRepository.save(
                Mission.builder()
                        .eventId(event.getId())
                        .title("test-mission")
                        .build()
        );

        ParticipationResultLog log = ParticipationResultLog.builder()
                .userId(user.getId())
                .eventId(event.getId())
                .missionId(mission.getId())
                .resultType("PARTICIPATION_SUCCESS")
                .status(ParticipationResultStatus.SUCCESS)
                .message("Participation completed")
                .build();

        // when
        ParticipationResultLog saved =
                participationResultLogRepository.saveAndFlush(log);

        ParticipationResultLog found =
                participationResultLogRepository.findById(saved.getId())
                        .orElseThrow();

        // then
        assertThat(found.getId()).isNotNull();
        assertThat(found.getResultType())
                .isEqualTo("PARTICIPATION_SUCCESS");
        assertThat(found.getStatus())
                .isEqualTo(ParticipationResultStatus.SUCCESS);
        assertThat(found.getMessage())
                .isEqualTo("Participation completed");

        assertThat(found.getUserId())
                .isEqualTo(user.getId());
        assertThat(found.getEventId())
                .isEqualTo(event.getId());
        assertThat(found.getMissionId())
                .isEqualTo(mission.getId());

        assertThat(found.getCreatedAt()).isNotNull();
    }

    @Test
    void saveDuplicateParticipationResultLog() {
        // given
        User user = userRepository.save(
                User.builder()
                        .name("duplicate-user")
                        .build()
        );

        Event event = eventRepository.save(
                Event.builder()
                        .title("duplicate-event")
                        .build()
        );

        Mission mission = missionRepository.save(
                Mission.builder()
                        .eventId(event.getId())
                        .title("duplicate-mission")
                        .build()
        );

        ParticipationResultLog log = ParticipationResultLog.builder()
                .userId(user.getId())
                .eventId(event.getId())
                .missionId(mission.getId())
                .resultType("PARTICIPATION_DUPLICATE")
                .status(ParticipationResultStatus.DUPLICATE)
                .message("User already participated in this mission")
                .build();

        // when
        ParticipationResultLog saved =
                participationResultLogRepository.saveAndFlush(log);

        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getResultType())
                .isEqualTo("PARTICIPATION_DUPLICATE");
        assertThat(saved.getStatus())
                .isEqualTo(ParticipationResultStatus.DUPLICATE);
    }
}


