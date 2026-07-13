package com.sincheon90.eventparticipation.kafka;

import com.sincheon90.eventparticipation.domain.participation.ParticipationResultStatus;
import com.sincheon90.eventparticipation.kafka.consumer.ParticipationEventConsumer;
import com.sincheon90.eventparticipation.kafka.consumer.ParticipationResultEventHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ParticipationEventConsumerTest {

    @Mock
    private ParticipationResultEventHandler eventHandler;

    @InjectMocks
    private ParticipationEventConsumer participationEventConsumer;

    @Test
    @DisplayName("参加結果イベントを受信するとHandlerを呼び出す")
    void consume() {
        // given
        ParticipationResultEvent event =
                new ParticipationResultEvent(
                        1L,
                        1L,
                        10L,
                        100L,
                        ParticipationResultStatus.SUCCESS,
                        "Participation completed",
                        LocalDateTime.of(2026, 7, 13, 10, 0)
                );

        // when
        participationEventConsumer.consume(event);

        // then
        verify(eventHandler).handle(event);
    }
}
