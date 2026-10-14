package br.com.cinepass.reputacao.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

/** Histórico/reputação de um cliente. As regras de pontuação ficam aqui, dentro da entidade. */
@Entity
@Table(name = "reputacao_cliente")
public class ReputacaoCliente {
    private static final int PONTOS_POR_RESERVA_CONFIRMADA = 10;
    private static final int PONTOS_PERDIDOS_POR_CANCELAMENTO = 5;

    @Id
    private UUID clienteId;
    @Column(nullable = false)
    private int reservasConfirmadas;
    @Column(nullable = false)
    private int reservasCanceladas;
    @Column(nullable = false)
    private int pontos;
    @Column(nullable = false)
    private Instant atualizadoEm;
    @Version
    private Long versao;

    protected ReputacaoCliente() {
    }

    public ReputacaoCliente(UUID clienteId) {
        this.clienteId = clienteId;
        this.atualizadoEm = Instant.now();
    }

    public void registrarReservaConfirmada() {
        reservasConfirmadas++;
        pontos += PONTOS_POR_RESERVA_CONFIRMADA;
        atualizadoEm = Instant.now();
    }

    public void registrarReservaCancelada() {
        reservasCanceladas++;
        pontos = Math.max(0, pontos - PONTOS_PERDIDOS_POR_CANCELAMENTO);
        atualizadoEm = Instant.now();
    }

    public UUID getClienteId() { return clienteId; }
    public int getReservasConfirmadas() { return reservasConfirmadas; }
    public int getReservasCanceladas() { return reservasCanceladas; }
    public int getPontos() { return pontos; }
    public Instant getAtualizadoEm() { return atualizadoEm; }
}