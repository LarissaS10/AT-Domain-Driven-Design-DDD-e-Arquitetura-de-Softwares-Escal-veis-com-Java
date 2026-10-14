package br.com.cinepass.reputacao.infrastructure.persistence;

import br.com.cinepass.reputacao.domain.ReputacaoCliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ReputacaoClienteRepository extends JpaRepository<ReputacaoCliente, UUID> {
}