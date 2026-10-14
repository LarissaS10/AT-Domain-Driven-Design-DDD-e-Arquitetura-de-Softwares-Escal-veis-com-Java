package br.com.cinepass.notificacao.infrastructure.messaging;

import br.com.cinepass.notificacao.application.NotificacaoService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ReservaEventosListener {
    private final NotificacaoService service;

    public ReservaEventosListener(NotificacaoService service) {
        this.service = service;
    }

    @KafkaListener(topics = "reserva-eventos")
    public void ouvir(String mensagem) {
        service.processar(mensagem);
    }
}