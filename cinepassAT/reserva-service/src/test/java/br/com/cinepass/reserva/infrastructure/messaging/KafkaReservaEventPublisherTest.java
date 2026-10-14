package br.com.cinepass.reserva.infrastructure.messaging;

import br.com.cinepass.reserva.domain.model.*;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KafkaReservaEventPublisherTest {
    @SuppressWarnings("unchecked")
    @Test
    void deveEnviarMensagemComChaveReservaIdEEnvelopeCompleto() throws Exception {
        KafkaTemplate<String, String> template = mock(KafkaTemplate.class);
        when(template.send(any(ProducerRecord.class))).thenReturn(new CompletableFuture<>());
        var publisher = new KafkaReservaEventPublisher(template, JsonMapper.builder().build());

        var reserva = Reserva.criar(new ClienteId(UUID.randomUUID()), new SessaoId(UUID.randomUUID()),
                List.of(new AssentoId("a1"), new AssentoId("A2")), new Dinheiro(new BigDecimal("79.80")));
        publisher.publicar(reserva.eventosPendentes()); // sem transação ativa: envia direto

        ArgumentCaptor<ProducerRecord<String, String>> captor = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(template).send(captor.capture());
        var registro = captor.getValue();
        assertEquals("reserva-eventos", registro.topic());
        assertEquals(reserva.getId().valor().toString(), registro.key());

        JsonNode json = JsonMapper.builder().build().readTree(registro.value());
        assertEquals("ReservaCriada", json.get("eventType").asString());
        assertEquals("Reserva", json.get("aggregateType").asString());
        assertEquals("reserva-service", json.get("producer").asString());
        assertEquals(reserva.getId().valor().toString(), json.get("correlationId").asString());
        assertEquals(reserva.getId().valor().toString(), json.get("aggregateId").asString());
        assertTrue(json.hasNonNull("eventId"));
        assertTrue(json.get("occurredAt").asString().endsWith("Z"));
        assertEquals(2, json.get("payload").get("assentos").size());
        assertEquals("BRL", json.get("payload").get("valorTotal").get("moeda").asString());
        assertEquals("CRIADA", json.get("payload").get("status").asString());
    }
}