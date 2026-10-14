package br.com.cinepass.auditoria.application;

import br.com.cinepass.auditoria.domain.RegistroAuditoria;
import br.com.cinepass.auditoria.infrastructure.persistence.RegistroAuditoriaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuditoriaService {
    private static final Logger log = LoggerFactory.getLogger(AuditoriaService.class);

    private final RegistroAuditoriaRepository repository;
    private final JsonMapper jsonMapper;

    public AuditoriaService(RegistroAuditoriaRepository repository, JsonMapper jsonMapper) {
        this.repository = repository;
        this.jsonMapper = jsonMapper;
    }

    @Transactional
    public void registrar(String mensagemJson, String topico, int particao, long offset) {
        JsonNode json = jsonMapper.readTree(mensagemJson);
        UUID eventId = UUID.fromString(json.get("eventId").asString());
        String eventType = json.get("eventType").asString();
        UUID reservaId = UUID.fromString(json.get("aggregateId").asString());
        String correlationId = json.get("correlationId").asString();

        if (repository.existsById(eventId)) {
            log.warn("Auditoria: evento {} ({}) já registrado, ignorando duplicado. reserva={}", eventId, eventType, reservaId);
            return;
        }

        repository.save(new RegistroAuditoria(
                eventId,
                eventType,
                json.get("aggregateType").asString(),
                reservaId,
                Instant.parse(json.get("occurredAt").asString()),
                correlationId,
                json.get("producer").asString(),
                topico,
                particao,
                offset,
                json.get("payload").toString(),
                Instant.now()));

        log.info("Auditoria: evento registrado. eventId={} tipo={} reserva={} correlationId={} partição={} offset={}",
                eventId, eventType, reservaId, correlationId, particao, offset);
    }

    @Transactional(readOnly = true)
    public List<RegistroAuditoria> consultar(UUID reservaId, String correlationId, String eventType) {
        if (reservaId != null) return repository.findByAggregateIdOrderByOccurredAtAsc(reservaId);
        if (correlationId != null) return repository.findByCorrelationIdOrderByOccurredAtAsc(correlationId);
        if (eventType != null) return repository.findByEventTypeOrderByOccurredAtDesc(eventType);
        return repository.findAllByOrderByOccurredAtDesc();
    }

    @Transactional(readOnly = true)
    public Optional<RegistroAuditoria> buscar(UUID eventId) {
        return repository.findById(eventId);
    }
}