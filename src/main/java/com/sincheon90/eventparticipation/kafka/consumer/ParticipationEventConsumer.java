package com.sincheon90.eventparticipation.kafka.consumer;

import com.sincheon90.eventparticipation.kafka.KafkaTopics;
import com.sincheon90.eventparticipation.kafka.ParticipationResultEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile({"local", "worker"})
@RequiredArgsConstructor
public class ParticipationEventConsumer {
    private final ParticipationResultEventHandler eventHandler;

    @KafkaListener(
            topics = KafkaTopics.PARTICIPATION_EVENT,
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(ParticipationResultEvent event) {
        eventHandler.handle(event);

        log.info(
                "Participation result consumed: status={}, eventId={}, missionId={}, userId={}",
                event.resultStatus(),
                event.eventId(),
                event.missionId(),
                event.userId()
        );
    }
}