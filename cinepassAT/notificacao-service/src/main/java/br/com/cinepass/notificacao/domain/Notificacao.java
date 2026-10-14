package br.com.cinepass.notificacao.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notificacao")
public class Notificacao {
    @Id
    private UUID eventId;
    @Column(nullable = false)
    private UUID reservaId;
    @Column(nullable = false)
    private UUID destinatarioId;
    @Column(nullable = false)
    private String canal;
    @Column(nullable = false)
    private String tipoEvento;
    @Column(nullable = false)
    private String assunto;
    @Column(nullable = false, columnDefinition = "text")
    private String mensagem;
    @Column(nullable = false)
    private String status;
    @Column(nullable = false)
    private Instant criadaEm;

    protected Notificacao() {
    }

    public Notificacao(UUID eventId, UUID reservaId, UUID destinatarioId, String canal, String tipoEvento,
                       String assunto, String mensagem, String status, Instant criadaEm) {
        this.eventId = eventId;
        this.reservaId = reservaId;
        this.destinatarioId = destinatarioId;
        this.canal = canal;
        this.tipoEvento = tipoEvento;
        this.assunto = assunto;
        this.mensagem = mensagem;
        this.status = status;
        this.criadaEm = criadaEm;
    }

    public UUID getEventId() { return eventId; }
    public UUID getReservaId() { return reservaId; }
    public UUID getDestinatarioId() { return destinatarioId; }
    public String getCanal() { return canal; }
    public String getTipoEvento() { return tipoEvento; }
    public String getAssunto() { return assunto; }
    public String getMensagem() { return mensagem; }
    public String getStatus() { return status; }
    public Instant getCriadaEm() { return criadaEm; }
}