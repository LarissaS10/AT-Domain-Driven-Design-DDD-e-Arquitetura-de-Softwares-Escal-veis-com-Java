package br.com.cinepass.auditoria.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "registro_auditoria")
public class RegistroAuditoria {
    @Id
    private UUID eventId;
    @Column(nullable = false)
    private String eventType;
    @Column(nullable = false)
    private String aggregateType;
    @Column(nullable = false)
    private UUID aggregateId;
    @Column(nullable = false)
    private Instant occurredAt;
    @Column(nullable = false)
    private String correlationId;
    @Column(nullable = false)
    private String producer;
    @Column(nullable = false)
    private String topico;
    @Column(nullable = false)
    private int particaoKafka;
    @Column(nullable = false)
    private long offsetKafka;
    @Column(nullable = false, columnDefinition = "text")
    private String conteudo;
    @Column(nullable = false)
    private Instant recebidoEm;

    protected RegistroAuditoria() {
    }

    public RegistroAuditoria(UUID eventId, String eventType, String aggregateType, UUID aggregateId, Instant occurredAt,
                             String correlationId, String producer, String topico, int particaoKafka, long offsetKafka,
                             String conteudo, Instant recebidoEm) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.occurredAt = occurredAt;
        this.correlationId = correlationId;
        this.producer = producer;
        this.topico = topico;
        this.particaoKafka = particaoKafka;
        this.offsetKafka = offsetKafka;
        this.conteudo = conteudo;
        this.recebidoEm = recebidoEm;
    }

    public UUID getEventId() { return eventId; }
    public String getEventType() { return eventType; }
    public String getAggregateType() { return aggregateType; }
    public UUID getAggregateId() { return aggregateId; }
    public Instant getOccurredAt() { return occurredAt; }
    public String getCorrelationId() { return correlationId; }
    public String getProducer() { return producer; }
    public String getTopico() { return topico; }
    public int getParticaoKafka() { return particaoKafka; }
    public long getOffsetKafka() { return offsetKafka; }
    public String getConteudo() { return conteudo; }
    public Instant getRecebidoEm() { return recebidoEm; }
}