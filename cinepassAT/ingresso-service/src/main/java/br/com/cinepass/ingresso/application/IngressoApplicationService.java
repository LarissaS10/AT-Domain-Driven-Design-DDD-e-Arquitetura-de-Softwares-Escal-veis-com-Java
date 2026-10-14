package br.com.cinepass.ingresso.application;

import br.com.cinepass.ingresso.domain.model.*;
import br.com.cinepass.ingresso.domain.repository.IngressoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class IngressoApplicationService {
    private static final Logger log = LoggerFactory.getLogger(IngressoApplicationService.class);

    private final IngressoRepository repository;

    public IngressoApplicationService(IngressoRepository repository) { this.repository = repository; }

    @Transactional
    public Ingresso emitir(EmitirIngressoCommand command) {
        try (MDC.MDCCloseable ignorado = MDC.putCloseable("reservaId", command.reservaId().toString())) {
            log.info("Início do processamento: emissao");
            try {
                if (command.simularFalha()) {
                    throw new EmissaoIngressoException("Falha proposital na emissão do ingresso para demonstrar transação distribuída.");
                }
                Ingresso ingresso = Ingresso.emitir(new ReservaId(command.reservaId()), new SessaoId(command.sessaoId()), command.assentos());
                Ingresso salvo = repository.salvar(ingresso);
                log.info("Conclusão do processamento: emissao ingressoId={} status={}", salvo.getId().valor(), salvo.getStatus().name());
                return salvo;
            } catch (RuntimeException ex) {
                log.error("Falha no processamento: emissao erro={} detalhe={}", ex.getClass().getSimpleName(), ex.getMessage());
                throw ex;
            }
        }
    }

    @Transactional
    public Ingresso cancelar(UUID ingressoId) {
        log.info("Início do processamento: cancelamento ingressoId={}", ingressoId);
        try {
            Ingresso ingresso = repository.buscarPorId(new IngressoId(ingressoId))
                    .orElseThrow(() -> new IngressoNaoEncontradoException(ingressoId));
            ingresso.cancelar();
            Ingresso salvo = repository.salvar(ingresso);
            log.info("Conclusão do processamento: cancelamento ingressoId={} status={}", ingressoId, salvo.getStatus().name());
            return salvo;
        } catch (RuntimeException ex) {
            log.error("Falha no processamento: cancelamento ingressoId={} erro={} detalhe={}", ingressoId, ex.getClass().getSimpleName(), ex.getMessage());
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public Ingresso buscarPorReserva(UUID reservaId) {
        return repository.buscarPorReserva(new ReservaId(reservaId))
                .orElseThrow(() -> new IngressoNaoEncontradoException(reservaId));
    }
}