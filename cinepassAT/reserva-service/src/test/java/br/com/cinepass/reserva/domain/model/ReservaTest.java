package br.com.cinepass.reserva.domain.model;

import br.com.cinepass.reserva.domain.event.TipoEventoReserva;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ReservaTest {
    @Test
    void devePercorrerFluxoFeliz() {
        var reserva=Reserva.criar(new ClienteId(UUID.randomUUID()), new SessaoId(UUID.randomUUID()), List.of(new AssentoId("A1")), new Dinheiro(new BigDecimal("39.90")));
        reserva.aguardarPagamento();
        reserva.confirmarPagamento(UUID.randomUUID());
        reserva.iniciarEmissaoIngresso();
        reserva.confirmar(UUID.randomUUID());
        assertEquals(StatusReserva.CONFIRMADA, reserva.getStatus());
    }

    private Reserva nova() {
        return Reserva.criar(new ClienteId(UUID.randomUUID()), new SessaoId(UUID.randomUUID()), List.of(new AssentoId("A1")), new Dinheiro(new BigDecimal("39.90")));
    }

    @Test
    void deveRegistrarUmEventoParaCadaPassoDoFluxoFeliz() {
        var reserva = nova();
        reserva.aguardarPagamento();
        reserva.confirmarPagamento(UUID.randomUUID());
        reserva.iniciarEmissaoIngresso();
        reserva.confirmar(UUID.randomUUID());
        assertEquals(List.of(TipoEventoReserva.RESERVA_CRIADA, TipoEventoReserva.PAGAMENTO_DA_RESERVA_CONFIRMADO,
                        TipoEventoReserva.EMISSAO_DE_INGRESSO_INICIADA, TipoEventoReserva.RESERVA_CONFIRMADA),
                reserva.eventosPendentes().stream().map(e -> e.tipo()).toList());
        assertEquals("CONFIRMADA", reserva.eventosPendentes().get(3).status());
    }

    @Test
    void cancelarDuasVezesGeraApenasUmEvento() {
        var reserva = nova();
        reserva.limparEventos();
        reserva.cancelar();
        reserva.cancelar();
        assertEquals(1, reserva.eventosPendentes().size());
        assertEquals(TipoEventoReserva.RESERVA_CANCELADA, reserva.eventosPendentes().get(0).tipo());
    }

    @Test
    void restaurarNaoGeraEventos() {
        var r = nova();
        var restaurada = Reserva.restaurar(r.getId(), r.getClienteId(), r.getSessaoId(), r.getAssentos(), r.getValorTotal(),
                StatusReserva.CRIADA, null, null, r.getCriadaEm());
        assertEquals(0, restaurada.eventosPendentes().size());
    }
}