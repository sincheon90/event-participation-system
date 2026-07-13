package com.sincheon90.eventparticipation.kafka.producer;

import com.sincheon90.eventparticipation.kafka.KafkaTopics;
import com.sincheon90.eventparticipation.kafka.ParticipationResultEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ParticipationEventProducer {

    private final KafkaTemplate<String, ParticipationResultEvent> kafkaTemplate;

    public void send(ParticipationResultEvent event) {
        kafkaTemplate.send(
                KafkaTopics.PARTICIPATION_EVENT,
                createKey(event),
                event
        );
    }

    private String createKey(ParticipationResultEvent event) {
        return event.eventId() + ":" + event.missionId() + ":" + event.userId();
    }
}