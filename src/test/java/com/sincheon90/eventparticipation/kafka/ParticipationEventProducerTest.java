package com.sincheon90.eventparticipation.kafka;

import com.sincheon90.eventparticipation.domain.participation.ParticipationResultStatus;
import com.sincheon90.eventparticipation.kafka.producer.ParticipationEventProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDateTime;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ParticipationEventProducerTest {

    private static final Long PARTICIPATION_ID = 1L;
    private static final Long EVENT_ID = 10L;
    private static final Long MISSION_ID = 20L;
    private static final Long USER_ID = 100L;

    private static final String MESSAGE_KEY = "10:20:100";

    @Mock
    private KafkaTemplate<String, ParticipationResultEvent> kafkaTemplate;

    private ParticipationEventProducer participationEventProducer;

    @BeforeEach
    void setUp() {
        participationEventProducer =
                new ParticipationEventProducer(kafkaTemplate);
    }

    @Test
    @DisplayName("参加結果イベントを正しいTopic・Key・MessageでKafkaに送信する")
    void sendParticipationResultEvent() {
        // given
        ParticipationResultEvent event =
                new ParticipationResultEvent(
                        PARTICIPATION_ID,
                        EVENT_ID,
                        MISSION_ID,
                        USER_ID,
                        ParticipationResultStatus.SUCCESS,
                        "Participation completed",
                        LocalDateTime.of(2026, 7, 13, 10, 0)
                );

        // when
        participationEventProducer.send(event);

        // then
        verify(kafkaTemplate).send(
                KafkaTopics.PARTICIPATION_EVENT,
                MESSAGE_KEY,
                event
        );
    }
}