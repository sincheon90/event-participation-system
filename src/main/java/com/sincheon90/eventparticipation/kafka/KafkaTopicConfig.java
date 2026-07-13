package com.sincheon90.eventparticipation.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic participationCreatedTopic() {
        return TopicBuilder
                .name(KafkaTopics.PARTICIPATION_EVENT)
                .partitions(3)
                .replicas(2)
                .build();
    }
}