package br.com.cinepass.reserva.application;

import org.slf4j.MDC;

import java.util.UUID;

public final class LogContexto implements AutoCloseable {
    public static final String CORRELATION_ID = "correlationId";
    public static final String RESERVA_ID = "reservaId";

    private final String correlationAnterior;
    private final String reservaAnterior;

    private LogContexto(UUID reservaId) {
        this.correlationAnterior = MDC.get(CORRELATION_ID);
        this.reservaAnterior = MDC.get(RESERVA_ID);
        if (reservaId != null) {
            definir(reservaId);
        }
    }

    public static LogContexto abrir(UUID reservaId) {
        return new LogContexto(reservaId);
    }

    public static void definir(UUID reservaId) {
        MDC.put(RESERVA_ID, reservaId.toString());
        if (MDC.get(CORRELATION_ID) == null) {
            MDC.put(CORRELATION_ID, reservaId.toString());
        }
    }

    public static String correlacao() {
        String atual = MDC.get(CORRELATION_ID);
        return atual != null ? atual : UUID.randomUUID().toString();
    }

    public static String correlacao(UUID reservaId) {
        String atual = MDC.get(CORRELATION_ID);
        return atual != null ? atual : reservaId.toString();
    }

    @Override
    public void close() {
        restaurar(CORRELATION_ID, correlationAnterior);
        restaurar(RESERVA_ID, reservaAnterior);
    }

    private static void restaurar(String chave, String valor) {
        if (valor == null) {
            MDC.remove(chave);
        } else {
            MDC.put(chave, valor);
        }
    }
}