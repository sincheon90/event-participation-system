package com.sincheon90.eventparticipation.domain.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class MissionRepositoryTest {

    @Autowired
    private MissionRepository missionRepository;

    @Test
    @DisplayName("イベントIDとミッションIDでミッションの存在を確認できる")
    void existsByIdAndEventId() {
        // given
        Mission mission = new Mission(1L, "QRチェックイン", "CHECK_IN");
        Mission saved = missionRepository.saveAndFlush(mission);

        // when
        boolean exists = missionRepository.existsByIdAndEventId(saved.getId(), 1L);
        boolean notExistsByEventId = missionRepository.existsByIdAndEventId(saved.getId(), 2L);
        boolean notExistsByMissionId = missionRepository.existsByIdAndEventId(999L, 1L);

        // then
        assertThat(exists).isTrue();
        assertThat(notExistsByEventId).isFalse();
        assertThat(notExistsByMissionId).isFalse();
    }
}