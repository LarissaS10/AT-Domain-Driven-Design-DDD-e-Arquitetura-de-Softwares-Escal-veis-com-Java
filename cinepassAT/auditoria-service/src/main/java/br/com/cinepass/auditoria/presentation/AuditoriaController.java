package br.com.cinepass.auditoria.presentation;

import br.com.cinepass.auditoria.application.AuditoriaService;
import br.com.cinepass.auditoria.domain.RegistroAuditoria;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/auditoria")
public class AuditoriaController {
    private final AuditoriaService service;

    public AuditoriaController(AuditoriaService service) {
        this.service = service;
    }

   @GetMapping
    public List<RegistroResponse> consultar(@RequestParam(required = false) UUID reservaId,
                                            @RequestParam(required = false) String correlationId,
                                            @RequestParam(required = false) String eventType) {
        return service.consultar(reservaId, correlationId, eventType).stream().map(RegistroResponse::de).toList();
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<RegistroResponse> buscar(@PathVariable UUID eventId) {
        return service.buscar(eventId).map(RegistroResponse::de).map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    public record RegistroResponse(UUID eventId, String eventType, UUID reservaId, Instant occurredAt,
                                   String correlationId, String producer, Instant recebidoEm, String conteudo) {
        static RegistroResponse de(RegistroAuditoria r) {
            return new RegistroResponse(r.getEventId(), r.getEventType(), r.getAggregateId(), r.getOccurredAt(),
                    r.getCorrelationId(), r.getProducer(), r.getRecebidoEm(), r.getConteudo());
        }
    }
}