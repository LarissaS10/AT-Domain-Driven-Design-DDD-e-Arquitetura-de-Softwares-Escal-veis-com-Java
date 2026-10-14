package br.com.cinepass.reserva.infrastructure.messaging;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {
    public static final String TOPICO_RESERVA_EVENTOS = "reserva-eventos";

    @Bean
    NewTopic reservaEventosTopic() {
        return TopicBuilder.name(TOPICO_RESERVA_EVENTOS).partitions(3).replicas(1).build();
    }
}