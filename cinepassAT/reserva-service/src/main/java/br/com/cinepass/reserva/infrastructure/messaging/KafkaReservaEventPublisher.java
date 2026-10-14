package br.com.cinepass.reserva.infrastructure.messaging;

import br.com.cinepass.reserva.application.LogContexto;
import br.com.cinepass.reserva.application.port.ReservaEventPublisher;
import br.com.cinepass.reserva.domain.event.ReservaEvento;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class KafkaReservaEventPublisher implements ReservaEventPublisher {
    private static final Logger log = LoggerFactory.getLogger(KafkaReservaEventPublisher.class);
    static final String PRODUTOR = "reserva-service";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final JsonMapper jsonMapper;

    public KafkaReservaEventPublisher(KafkaTemplate<String, String> kafkaTemplate, JsonMapper jsonMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void publicar(List<ReservaEvento> eventos) {
        if (eventos.isEmpty()) return;
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() { enviar(eventos); }
            });
        } else {
            enviar(eventos);
        }
    }

    private void enviar(List<ReservaEvento> eventos) {
        for (ReservaEvento evento : eventos) {
            try {
                MensagemEvento mensagem = converter(evento);
                String chave = evento.reservaId().toString();
                ProducerRecord<String, String> registro = new ProducerRecord<>(
                        KafkaTopicConfig.TOPICO_RESERVA_EVENTOS, chave, jsonMapper.writeValueAsString(mensagem));
                registro.headers().add("eventType", mensagem.eventType().getBytes(StandardCharsets.UTF_8));
                registro.headers().add("eventId", mensagem.eventId().toString().getBytes(StandardCharsets.UTF_8));
                registro.headers().add("correlationId", mensagem.correlationId().getBytes(StandardCharsets.UTF_8));
                log.info("Publicando evento: tipo={} eventId={} reservaId={} correlationId={} topico={}",
                        mensagem.eventType(), mensagem.eventId(), chave, mensagem.correlationId(), KafkaTopicConfig.TOPICO_RESERVA_EVENTOS);
                kafkaTemplate.send(registro).whenComplete((resultado, erro) -> {
                    if (erro != null) {
                        log.error("Falha ao publicar evento: tipo={} eventId={} reservaId={} correlationId={} erro={}",
                                mensagem.eventType(), mensagem.eventId(), chave, mensagem.correlationId(), erro.getClass().getSimpleName());
                    } else {
                        log.info("Evento publicado: tipo={} eventId={} reservaId={} correlationId={} particao={} offset={}",
                                mensagem.eventType(), mensagem.eventId(), chave, mensagem.correlationId(),
                                resultado.getRecordMetadata().partition(), resultado.getRecordMetadata().offset());
                    }
                });
            } catch (Exception e) {
                log.error("Não foi possível publicar o evento: tipo={} eventId={} reservaId={} erro={}",
                        evento.tipo().nome(), evento.eventId(), evento.reservaId(), e.getClass().getSimpleName());
            }
        }
    }

    static MensagemEvento converter(ReservaEvento e) {
        var payload = new MensagemEvento.Payload(e.reservaId(), e.clienteId(), e.sessaoId(), e.assentos(),
                new MensagemEvento.Valor(e.valorTotal(), "BRL"), e.status(), e.pagamentoId(), e.ingressoId());
        return new MensagemEvento(e.eventId(), e.tipo().nome(), 1, "Reserva", e.reservaId(),
                e.ocorridoEm().toString(), LogContexto.correlacao(e.reservaId()), PRODUTOR, payload);
    }
}