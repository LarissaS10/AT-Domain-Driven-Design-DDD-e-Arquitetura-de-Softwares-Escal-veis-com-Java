package br.com.cinepass.reputacao.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "evento_processado")
public class EventoProcessado {
    @Id
    private UUID eventId;
    private String eventType;
    private Instant processadoEm;

    protected EventoProcessado() {
    }

    public EventoProcessado(UUID eventId, String eventType, Instant processadoEm) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.processadoEm = processadoEm;
    }

    public UUID getEventId() { return eventId; }
    public String getEventType() { return eventType; }
    public Instant getProcessadoEm() { return processadoEm; }
}