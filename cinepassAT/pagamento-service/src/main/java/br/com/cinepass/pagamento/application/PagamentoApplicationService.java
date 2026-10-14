package br.com.cinepass.pagamento.application;

import br.com.cinepass.pagamento.domain.model.Dinheiro;
import br.com.cinepass.pagamento.domain.model.Pagamento;
import br.com.cinepass.pagamento.domain.model.PagamentoId;
import br.com.cinepass.pagamento.domain.model.ReservaId;
import br.com.cinepass.pagamento.domain.repository.PagamentoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class PagamentoApplicationService {
    private static final Logger log = LoggerFactory.getLogger(PagamentoApplicationService.class);

    private final PagamentoRepository repository;

    public PagamentoApplicationService(PagamentoRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Pagamento processar(CriarPagamentoCommand command) {
        try (MDC.MDCCloseable ignorado = MDC.putCloseable("reservaId", command.reservaId().toString())) {
            log.info("Início do processamento: cobranca");
            try {
                Pagamento pagamento = Pagamento.solicitar(
                        new ReservaId(command.reservaId()),
                        new Dinheiro(command.valor())
                );

                if (command.simularRecusa()) {
                    pagamento.recusar();
                } else {
                    pagamento.aprovar();
                }

                Pagamento salvo = repository.salvar(pagamento);
                log.info("Conclusão do processamento: cobranca pagamentoId={} status={}", salvo.getId().valor(), salvo.getStatus().name());
                return salvo;
            } catch (RuntimeException ex) {
                log.error("Falha no processamento: cobranca erro={} detalhe={}", ex.getClass().getSimpleName(), ex.getMessage());
                throw ex;
            }
        }
    }

    @Transactional
    public Pagamento estornar(UUID pagamentoId) {
        log.info("Início do processamento: estorno pagamentoId={}", pagamentoId);
        try {
            Pagamento pagamento = repository.buscarPorId(new PagamentoId(pagamentoId))
                    .orElseThrow(() -> new PagamentoNaoEncontradoException(pagamentoId));
            pagamento.estornar();
            Pagamento salvo = repository.salvar(pagamento);
            log.info("Conclusão do processamento: estorno pagamentoId={} status={}", pagamentoId, salvo.getStatus().name());
            return salvo;
        } catch (RuntimeException ex) {
            log.error("Falha no processamento: estorno pagamentoId={} erro={} detalhe={}", pagamentoId, ex.getClass().getSimpleName(), ex.getMessage());
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public Pagamento buscar(UUID pagamentoId) {
        return repository.buscarPorId(new PagamentoId(pagamentoId))
                .orElseThrow(() -> new PagamentoNaoEncontradoException(pagamentoId));
    }

    @Transactional(readOnly = true)
    public Pagamento buscarPorReserva(UUID reservaId) {
        return repository.buscarPorReservaId(new ReservaId(reservaId))
                .orElseThrow(() -> new PagamentoNaoEncontradoException(reservaId));
    }
}