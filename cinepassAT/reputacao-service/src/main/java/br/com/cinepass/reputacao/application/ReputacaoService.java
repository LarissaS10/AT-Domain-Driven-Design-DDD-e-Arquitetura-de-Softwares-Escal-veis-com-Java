package br.com.cinepass.reputacao.application;

import br.com.cinepass.reputacao.domain.EventoProcessado;
import br.com.cinepass.reputacao.domain.ReputacaoCliente;
import br.com.cinepass.reputacao.infrastructure.persistence.EventoProcessadoRepository;
import br.com.cinepass.reputacao.infrastructure.persistence.ReputacaoClienteRepository;
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
public class ReputacaoService {
    private static final Logger log = LoggerFactory.getLogger(ReputacaoService.class);

    private final ReputacaoClienteRepository reputacaoRepository;
    private final EventoProcessadoRepository eventoRepository;
    private final JsonMapper jsonMapper;

    public ReputacaoService(ReputacaoClienteRepository reputacaoRepository, EventoProcessadoRepository eventoRepository,
                            JsonMapper jsonMapper) {
        this.reputacaoRepository = reputacaoRepository;
        this.eventoRepository = eventoRepository;
        this.jsonMapper = jsonMapper;
    }

    @Transactional
    public void processar(String mensagemJson) {
        JsonNode json = jsonMapper.readTree(mensagemJson);
        UUID eventId = UUID.fromString(json.get("eventId").asString());
        String tipo = json.get("eventType").asString();
        UUID reservaId = UUID.fromString(json.get("aggregateId").asString());
        UUID clienteId = UUID.fromString(json.get("payload").get("clienteId").asString());

        log.info("Reputação: evento recebido. eventId={} tipo={} reserva={} cliente={}", eventId, tipo, reservaId, clienteId);

        if (!"ReservaConfirmada".equals(tipo) && !"ReservaCancelada".equals(tipo)) {
            log.info("Reputação: eventId={} tipo={} resultado=IGNORADO_SEM_IMPACTO", eventId, tipo);
            return;
        }
        if (eventoRepository.existsById(eventId)) {
            log.warn("Reputação: eventId={} cliente={} resultado=IGNORADO_DUPLICADO", eventId, clienteId);
            return;
        }

        ReputacaoCliente reputacao = reputacaoRepository.findById(clienteId)
                .orElseGet(() -> new ReputacaoCliente(clienteId));
        if ("ReservaConfirmada".equals(tipo)) {
            reputacao.registrarReservaConfirmada();
        } else {
            reputacao.registrarReservaCancelada();
        }
        reputacaoRepository.save(reputacao);
        eventoRepository.save(new EventoProcessado(eventId, tipo, Instant.now()));

        log.info("Reputação: eventId={} cliente={} resultado=ATUALIZADA confirmadas={} canceladas={} pontos={}",
                eventId, clienteId, reputacao.getReservasConfirmadas(), reputacao.getReservasCanceladas(),
                reputacao.getPontos());
    }

    @Transactional(readOnly = true)
    public Optional<ReputacaoCliente> buscar(UUID clienteId) {
        return reputacaoRepository.findById(clienteId);
    }

    @Transactional(readOnly = true)
    public List<ReputacaoCliente> listar() {
        return reputacaoRepository.findAll();
    }
}