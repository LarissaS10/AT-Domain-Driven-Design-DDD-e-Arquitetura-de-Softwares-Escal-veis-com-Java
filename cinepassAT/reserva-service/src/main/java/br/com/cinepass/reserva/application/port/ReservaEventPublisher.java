package br.com.cinepass.reserva.application.port;

import br.com.cinepass.reserva.domain.event.ReservaEvento;

import java.util.List;

/** Porta de saída: a camada de aplicação só sabe que "alguém publica" os eventos (Kafka fica na infraestrutura). */
public interface ReservaEventPublisher {
    void publicar(List<ReservaEvento> eventos);
}