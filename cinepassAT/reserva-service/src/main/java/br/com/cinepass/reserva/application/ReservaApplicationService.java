package br.com.cinepass.reserva.application;

import br.com.cinepass.reserva.application.port.IngressoGateway;
import br.com.cinepass.reserva.application.port.PagamentoGateway;
import br.com.cinepass.reserva.domain.model.*;
import br.com.cinepass.reserva.domain.repository.ReservaRepository;
import br.com.cinepass.reserva.domain.repository.SessaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ReservaApplicationService {
    private final ReservaRepository reservaRepository;
    private final SessaoRepository sessaoRepository;
    private final PagamentoGateway pagamentoGateway;
    private final IngressoGateway ingressoGateway;

    public ReservaApplicationService(ReservaRepository reservaRepository, SessaoRepository sessaoRepository,
                                     PagamentoGateway pagamentoGateway, IngressoGateway ingressoGateway) {
        this.reservaRepository=reservaRepository; this.sessaoRepository=sessaoRepository;
        this.pagamentoGateway=pagamentoGateway; this.ingressoGateway=ingressoGateway;
    }

    /**
     * Esta transação é propositalmente longa para a aula. Ela protege apenas o banco do reserva-service.
     * Os commits realizados em pagamento-service e ingresso-service não participam desta transação local.
     */
    @Transactional
    //ACID
    public ReservaDetalhe realizar(RealizarReservaCommand command) {
        Sessao sessao = sessaoRepository.buscarPorId(new SessaoId(command.sessaoId()))
                .orElseThrow(() -> new SessaoNaoEncontradaException(command.sessaoId()));

        List<AssentoId> assentos = command.assentos().stream().map(AssentoId::new).toList();
        sessao.reservar(assentos);
        sessaoRepository.salvar(sessao);

        Reserva reserva = Reserva.criar(new ClienteId(command.clienteId()), sessao.getId(), assentos, sessao.getPreco().multiplicar(assentos.size()));
        reserva.aguardarPagamento();
        reservaRepository.salvar(reserva);

        PagamentoGateway.PagamentoResultado pagamento = pagamentoGateway.cobrar(
                reserva.getId().valor(), reserva.getValorTotal().valor(), command.simularRecusaPagamento());

        if (!"APROVADO".equals(pagamento.status())) {
            throw new PagamentoRecusadoException(reserva.getId().valor());
        }

        reserva.confirmarPagamento(pagamento.pagamentoId());
        reserva.iniciarEmissaoIngresso();
        reservaRepository.salvar(reserva);

        try {
            IngressoGateway.IngressoResultado ingresso = ingressoGateway.emitir(
                    reserva.getId().valor(), reserva.getSessaoId().valor(),
                    reserva.getAssentos().stream().map(AssentoId::valor).toList(), command.simularFalhaIngresso());
            reserva.confirmar(ingresso.ingressoId());
            return ReservaDetalhe.de(reservaRepository.salvar(reserva));
        } catch (FalhaIntegracaoException ex) {
            // NÃO compensamos ainda. Essa lacuna é intencional: será resolvida quando ensinarmos Saga.
            throw new FalhaProcessamentoReservaException(reserva.getId().valor(), pagamento.pagamentoId(), ex);
        }
    }
    @Transactional
    public ReservaDetalhe confirmarPagamento(UUID reservaId, UUID pagamentoId){
        Reserva reserva = reservaRepository
                .buscarPorId(new ReservaId(reservaId))
                .orElseThrow(
                        () -> new ReservaNaoEncontradaException(
                                reservaId
                        )
                );
        reserva.confirmarPagamento(pagamentoId);
        return ReservaDetalhe.de(reservaRepository.salvar(reserva));
    }
    @Transactional
    public ReservaDetalhe cancelar(UUID reservaId){
        Reserva reserva = reservaRepository
                .buscarPorId(new ReservaId(reservaId))
                .orElseThrow(
                        () -> new ReservaNaoEncontradaException(
                                reservaId
                        )
                );
        Sessao sessao = sessaoRepository
                .buscarPorId(
                        reserva.getSessaoId()
                )
                .orElseThrow(
                        () -> new SessaoNaoEncontradaException(
                                reserva.getSessaoId().valor()
                        )
                );
        reserva.cancelar();
        sessao.liberar(reserva.getAssentos());
        sessaoRepository.salvar(sessao);
        reservaRepository.salvar(reserva);
        return ReservaDetalhe.de(reserva);

    }
    @Transactional
    public ReservaDetalhe iniciarEmissaoIngresso(UUID reservaId){
        Reserva reserva = reservaRepository
                .buscarPorId(new ReservaId(reservaId))
                .orElseThrow(
                        () -> new ReservaNaoEncontradaException(
                                reservaId
                        )
                );
        reserva.iniciarEmissaoIngresso();
        return ReservaDetalhe.de(reservaRepository.salvar(reserva));

    }
    @Transactional
    public ReservaDetalhe confirmar(UUID reservaId, UUID ingressoId){
        Reserva reserva = reservaRepository
                .buscarPorId(new ReservaId(reservaId))
                .orElseThrow(
                        () -> new ReservaNaoEncontradaException(
                                reservaId
                        )
                );
        reserva.confirmar(ingressoId);
        return ReservaDetalhe.de(reservaRepository.salvar(reserva));

    }
    @Transactional
    public ReservaDetalhe iniciar(RealizarReservaCommand command) {
        Sessao sessao = sessaoRepository
                .buscarPorId(
                        new SessaoId(command.sessaoId())
                )
                .orElseThrow(
                        () -> new SessaoNaoEncontradaException(
                                command.sessaoId()
                        )
                );

        List<AssentoId> assentos =
                command.assentos()
                        .stream()
                        .map(AssentoId::new)
                        .toList();

        sessao.reservar(assentos);

        sessaoRepository.salvar(sessao);

        Reserva reserva = Reserva.criar(
                new ClienteId(command.clienteId()),
                sessao.getId(),
                assentos,
                sessao.getPreco()
                        .multiplicar(assentos.size())
        );

        reserva.aguardarPagamento();

        reservaRepository.salvar(reserva);

        return ReservaDetalhe.de(reserva);
    }

    @Transactional(readOnly = true)
    public ReservaDetalhe buscar(UUID id) {
        return reservaRepository.buscarPorId(new ReservaId(id)).map(ReservaDetalhe::de)
                .orElseThrow(() -> new ReservaNaoEncontradaException(id));
    }

    @Transactional(readOnly = true)
    public List<ReservaDetalhe> listar() { return reservaRepository.listar().stream().map(ReservaDetalhe::de).toList(); }
}
