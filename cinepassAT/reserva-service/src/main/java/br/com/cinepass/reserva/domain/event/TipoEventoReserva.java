package br.com.cinepass.reserva.domain.event;

/** Tipos de evento que o agregado Reserva pode registrar. O nome vira o "eventType" da mensagem no Kafka. */
public enum TipoEventoReserva {
    RESERVA_CRIADA("ReservaCriada"),
    PAGAMENTO_DA_RESERVA_CONFIRMADO("PagamentoDaReservaConfirmado"),
    EMISSAO_DE_INGRESSO_INICIADA("EmissaoDeIngressoIniciada"),
    RESERVA_CONFIRMADA("ReservaConfirmada"),
    RESERVA_CANCELADA("ReservaCancelada");

    private final String nome;

    TipoEventoReserva(String nome) { this.nome = nome; }

    public String nome() { return nome; }
}