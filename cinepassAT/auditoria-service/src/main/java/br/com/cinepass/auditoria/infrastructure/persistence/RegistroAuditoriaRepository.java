package br.com.cinepass.auditoria.infrastructure.persistence;

import br.com.cinepass.auditoria.domain.RegistroAuditoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RegistroAuditoriaRepository extends JpaRepository<RegistroAuditoria, UUID> {
    List<RegistroAuditoria> findAllByOrderByOccurredAtDesc();

    List<RegistroAuditoria> findByAggregateIdOrderByOccurredAtAsc(UUID aggregateId);

    List<RegistroAuditoria> findByCorrelationIdOrderByOccurredAtAsc(String correlationId);

    List<RegistroAuditoria> findByEventTypeOrderByOccurredAtDesc(String eventType);
}