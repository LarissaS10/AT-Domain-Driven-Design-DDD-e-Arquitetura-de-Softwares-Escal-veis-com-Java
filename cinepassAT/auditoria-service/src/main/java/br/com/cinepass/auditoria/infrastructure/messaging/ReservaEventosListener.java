package br.com.cinepass.auditoria.infrastructure.messaging;

import br.com.cinepass.auditoria.application.AuditoriaService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ReservaEventosListener {
    private final AuditoriaService auditoriaService;

    public ReservaEventosListener(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @KafkaListener(topics = "reserva-eventos")
    public void ouvir(ConsumerRecord<String, String> registro) {
        auditoriaService.registrar(registro.value(), registro.topic(), registro.partition(), registro.offset());
    }
}