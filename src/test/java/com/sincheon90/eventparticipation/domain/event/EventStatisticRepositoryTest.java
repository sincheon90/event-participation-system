package com.sincheon90.eventparticipation.domain.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class EventStatisticRepositoryTest {

    @Autowired
    private EventStatisticRepository eventStatisticRepository;

    @Test
    @DisplayName("統計情報を保存すると更新日時が自動で設定される")
    void saveEventStatistic() {
        // given
        EventStatistic statistic = new EventStatistic(1L, 1L);

        // when
        EventStatistic saved = eventStatisticRepository.saveAndFlush(statistic);

        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getParticipationCount()).isEqualTo(0L);
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("参加数を1件増加して保存できる")
    void increaseParticipationCountAndSave() {
        // given
        EventStatistic statistic = new EventStatistic(1L, 1L);
        EventStatistic saved = eventStatisticRepository.saveAndFlush(statistic);

        LocalDateTime beforeUpdatedAt = saved.getUpdatedAt();

        // when
        saved.increaseParticipationCount();
        EventStatistic updated = eventStatisticRepository.saveAndFlush(saved);

        // then
        assertThat(updated.getParticipationCount()).isEqualTo(1L);
        assertThat(updated.getUpdatedAt()).isNotNull();
        assertThat(updated.getUpdatedAt()).isAfterOrEqualTo(beforeUpdatedAt);
    }
}
