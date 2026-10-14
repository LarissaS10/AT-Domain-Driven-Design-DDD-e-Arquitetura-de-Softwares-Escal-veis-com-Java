package br.com.cinepass.notificacao.infrastructure.persistence;

import br.com.cinepass.notificacao.domain.Notificacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificacaoRepository extends JpaRepository<Notificacao, UUID> {
    List<Notificacao> findAllByOrderByCriadaEmDesc();

    List<Notificacao> findByReservaIdOrderByCriadaEmAsc(UUID reservaId);
}