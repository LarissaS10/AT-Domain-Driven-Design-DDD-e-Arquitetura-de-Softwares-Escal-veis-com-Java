package br.com.cinepass.notificacao.presentation;

import br.com.cinepass.notificacao.application.NotificacaoService;
import br.com.cinepass.notificacao.domain.Notificacao;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notificacoes")
public class NotificacaoController {
    private final NotificacaoService service;

    public NotificacaoController(NotificacaoService service) {
        this.service = service;
    }

    @GetMapping
    public List<NotificacaoResponse> consultar(@RequestParam(required = false) UUID reservaId) {
        return service.consultar(reservaId).stream().map(NotificacaoResponse::de).toList();
    }

    public record NotificacaoResponse(UUID eventId, UUID reservaId, UUID destinatarioId, String canal, String tipoEvento,
                                      String assunto, String mensagem, String status, Instant criadaEm) {
        static NotificacaoResponse de(Notificacao n) {
            return new NotificacaoResponse(n.getEventId(), n.getReservaId(), n.getDestinatarioId(), n.getCanal(),
                    n.getTipoEvento(), n.getAssunto(), n.getMensagem(), n.getStatus(), n.getCriadaEm());
        }
    }
}