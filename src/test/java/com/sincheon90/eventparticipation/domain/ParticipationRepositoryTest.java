package com.sincheon90.eventparticipation.domain;

import com.sincheon90.eventparticipation.domain.participation.Participation;
import com.sincheon90.eventparticipation.domain.participation.ParticipationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
public class ParticipationRepositoryTest {

    @Autowired
    private ParticipationRepository participationRepository;

    @Test
    @DisplayName("参加情報を保存すると作成日時が自動で設定される")
    void saveParticipation() {
        // given
        Participation participation = new Participation(1L, 1L, 1L);

        // when
        Participation saved = participationRepository.save(participation);

        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUserId()).isEqualTo(1L);
        assertThat(saved.getEventId()).isEqualTo(1L);
        assertThat(saved.getMissionId()).isEqualTo(1L);
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("同一ユーザーは同じイベント・ミッションに重複参加できない")
    void duplicateParticipation() {
        // given
        Participation participation1 = new Participation(1L, 1L, 1L);
        Participation participation2 = new Participation(1L, 1L, 1L);

        participationRepository.saveAndFlush(participation1);

        // when & then
        assertThatThrownBy(() -> participationRepository.saveAndFlush(participation2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("ユーザー・イベント・ミッションで参加済みか確認できる")
    void existsByUserIdAndEventIdAndMissionId() {
        // given
        Participation participation = new Participation(1L, 1L, 1L);
        participationRepository.saveAndFlush(participation);

        // when
        boolean exists = participationRepository.existsByUserIdAndEventIdAndMissionId(1L, 1L, 1L);
        boolean notExists = participationRepository.existsByUserIdAndEventIdAndMissionId(2L, 1L, 1L);

        // then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }
}
