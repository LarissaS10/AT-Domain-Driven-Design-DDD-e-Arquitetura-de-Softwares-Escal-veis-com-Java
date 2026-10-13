package br.com.cinepass.ingresso.application;

import br.com.cinepass.ingresso.domain.model.*;
import br.com.cinepass.ingresso.domain.repository.IngressoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class IngressoApplicationService {
    private final IngressoRepository repository;

    public IngressoApplicationService(IngressoRepository repository) { this.repository = repository; }

    @Transactional
    public Ingresso emitir(EmitirIngressoCommand command) {
        if (command.simularFalha()) {
            throw new EmissaoIngressoException("Falha proposital na emissão do ingresso para demonstrar transação distribuída.");
        }
        Ingresso ingresso = Ingresso.emitir(new ReservaId(command.reservaId()), new SessaoId(command.sessaoId()), command.assentos());
        return repository.salvar(ingresso);
    }

    @Transactional
    public Ingresso cancelar(UUID ingressoId) {
        Ingresso ingresso = repository.buscarPorId(new IngressoId(ingressoId))
                .orElseThrow(() -> new IngressoNaoEncontradoException(ingressoId));
        ingresso.cancelar();
        return repository.salvar(ingresso);
    }

    @Transactional(readOnly = true)
    public Ingresso buscarPorReserva(UUID reservaId) {
        return repository.buscarPorReserva(new ReservaId(reservaId))
                .orElseThrow(() -> new IngressoNaoEncontradoException(reservaId));
    }
}
