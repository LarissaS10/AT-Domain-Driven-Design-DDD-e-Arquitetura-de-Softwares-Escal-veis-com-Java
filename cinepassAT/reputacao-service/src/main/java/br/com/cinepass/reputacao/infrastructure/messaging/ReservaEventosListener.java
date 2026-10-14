package br.com.cinepass.reputacao.infrastructure.messaging;

import br.com.cinepass.reputacao.application.ReputacaoService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ReservaEventosListener {
    private final ReputacaoService service;

    public ReservaEventosListener(ReputacaoService service) {
        this.service = service;
    }

    @KafkaListener(topics = "reserva-eventos")
    public void ouvir(String mensagem) {
        service.processar(mensagem);
    }
}