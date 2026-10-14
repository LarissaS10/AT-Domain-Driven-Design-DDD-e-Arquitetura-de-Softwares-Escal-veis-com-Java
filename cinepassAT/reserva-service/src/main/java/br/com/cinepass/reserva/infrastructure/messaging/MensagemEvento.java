package br.com.cinepass.reserva.infrastructure.messaging;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record MensagemEvento(
        UUID eventId,
        String eventType,
        int eventVersion,
        String aggregateType,
        UUID aggregateId,
        String occurredAt,
        String correlationId,
        String producer,
        Payload payload) {

    public record Payload(
            UUID reservaId,
            UUID clienteId,
            UUID sessaoId,
            List<String> assentos,
            Valor valorTotal,
            String status,
            UUID pagamentoId,
            UUID ingressoId) {
    }

    public record Valor(BigDecimal valor, String moeda) {
    }
}