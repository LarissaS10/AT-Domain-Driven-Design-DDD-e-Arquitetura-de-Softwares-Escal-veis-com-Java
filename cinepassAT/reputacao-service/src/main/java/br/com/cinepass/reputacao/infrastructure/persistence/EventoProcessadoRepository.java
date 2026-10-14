package br.com.cinepass.reputacao.infrastructure.persistence;

import br.com.cinepass.reputacao.domain.EventoProcessado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EventoProcessadoRepository extends JpaRepository<EventoProcessado, UUID> {
}