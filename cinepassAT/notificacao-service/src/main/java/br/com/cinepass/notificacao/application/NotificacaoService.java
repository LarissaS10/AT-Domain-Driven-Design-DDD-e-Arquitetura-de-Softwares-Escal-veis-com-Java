package br.com.cinepass.notificacao.application;

import br.com.cinepass.notificacao.domain.Notificacao;
import br.com.cinepass.notificacao.infrastructure.persistence.NotificacaoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class NotificacaoService {
    private static final Logger log = LoggerFactory.getLogger(NotificacaoService.class);

    private final NotificacaoRepository repository;
    private final JsonMapper jsonMapper;

    public NotificacaoService(NotificacaoRepository repository, JsonMapper jsonMapper) {
        this.repository = repository;
        this.jsonMapper = jsonMapper;
    }

    @Transactional
    public void processar(String mensagemJson) {
        try {
            JsonNode json = jsonMapper.readTree(mensagemJson);
            UUID eventId = UUID.fromString(json.get("eventId").asString());
            String tipo = json.get("eventType").asString();
            UUID reservaId = UUID.fromString(json.get("aggregateId").asString());
            JsonNode payload = json.get("payload");
            UUID clienteId = UUID.fromString(payload.get("clienteId").asString());

            log.info("Notificação: evento recebido. eventId={} tipo={} reserva={} destinatário={}",
                    eventId, tipo, reservaId, clienteId);

            if (repository.existsById(eventId)) {
                log.warn("Notificação: eventId={} reserva={} destinatário={} resultado=IGNORADO_DUPLICADO",
                        eventId, reservaId, clienteId);
                return;
            }

            Notificacao notificacao = montar(eventId, tipo, reservaId, clienteId, payload);
            if (notificacao == null) {
                log.info("Notificação: eventId={} tipo={} reserva={} destinatário={} resultado=IGNORADO_SEM_NOTIFICACAO",
                        eventId, tipo, reservaId, clienteId);
                return;
            }

            repository.save(notificacao);
            log.info("Notificação: eventId={} tipo={} reserva={} destinatário={} canal={} resultado=ENVIADA",
                    eventId, tipo, reservaId, clienteId, notificacao.getCanal());
        } catch (RuntimeException e) {
            log.error("Notificação: falha ao processar mensagem. resultado=FALHA erro={}", e.getMessage(), e);
            throw e; // o Kafka tenta de novo
        }
    }

    /** Só dois momentos do ciclo de vida geram notificação. Os demais eventos são ignorados. */
    private Notificacao montar(UUID eventId, String tipo, UUID reservaId, UUID clienteId, JsonNode payload) {
        List<String> assentos = new ArrayList<>();
        for (JsonNode assento : payload.get("assentos")) {
            assentos.add(assento.asString());
        }
        String valor = payload.get("valorTotal").get("valor").asString();

        String assunto;
        String texto;
        switch (tipo) {
            case "ReservaConfirmada" -> {
                assunto = "Reserva confirmada";
                texto = "Sua reserva " + reservaId + " foi confirmada. Assentos: " + String.join(", ", assentos)
                        + ". Valor: R$ " + valor + ".";
            }
            case "ReservaCancelada" -> {
                assunto = "Reserva cancelada";
                texto = "Sua reserva " + reservaId + " foi cancelada. Os assentos " + String.join(", ", assentos)
                        + " foram liberados.";
            }
            default -> {
                return null;
            }
        }
        return new Notificacao(eventId, reservaId, clienteId, "EMAIL", tipo, assunto, texto, "ENVIADA", Instant.now());
    }

    @Transactional(readOnly = true)
    public List<Notificacao> consultar(UUID reservaId) {
        return reservaId != null
                ? repository.findByReservaIdOrderByCriadaEmAsc(reservaId)
                : repository.findAllByOrderByCriadaEmDesc();
    }
}