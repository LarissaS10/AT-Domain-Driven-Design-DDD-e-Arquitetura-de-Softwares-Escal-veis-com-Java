package br.com.cinepass.reserva.domain.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Evento de domínio da Reserva: um fato que JÁ aconteceu.
 * É criado dentro do agregado Reserva (nunca fala com Kafka nem com banco).
 * Guarda uma "foto" da reserva no momento em que o fato ocorreu.
 */
public record ReservaEvento(
        UUID eventId,
        TipoEventoReserva tipo,
        Instant ocorridoEm,
        UUID reservaId,
        UUID clienteId,
        UUID sessaoId,
        List<String> assentos,
        BigDecimal valorTotal,
        String status,
        UUID pagamentoId,
        UUID ingressoId) {
}