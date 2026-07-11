package com.sincheon90.eventparticipation.domain.participation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ParticipationRepositoryConstraintTest {

    @Autowired
    private ParticipationRepository participationRepository;

    @Test
    @DisplayName("同一ユーザー・イベント・ミッションの重複保存時に制約違反が発生する")
    void duplicateParticipationThrowsDataIntegrityViolationException() {
        // given
        Participation first = Participation.builder()
                .eventId(1L)
                .missionId(10L)
                .userId(100L)
                .build();

        Participation duplicate = Participation.builder()
                .eventId(1L)
                .missionId(10L)
                .userId(100L)
                .build();

        participationRepository.saveAndFlush(first);

        // when & then
        assertThatThrownBy(
                () -> participationRepository.saveAndFlush(duplicate)
        )
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}