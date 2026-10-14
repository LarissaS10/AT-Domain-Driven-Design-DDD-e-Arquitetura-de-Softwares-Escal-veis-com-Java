package br.com.cinepass.reserva.infrastructure.http;

import br.com.cinepass.reserva.application.FalhaIntegracaoException;
import br.com.cinepass.reserva.application.LogContexto;
import br.com.cinepass.reserva.application.port.PagamentoGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class PagamentoHttpGateway implements PagamentoGateway {
    private static final Logger log = LoggerFactory.getLogger(PagamentoHttpGateway.class);
    private static final String CABECALHO_CORRELACAO = "X-Correlation-Id";

    private final RestClient restClient;

    public PagamentoHttpGateway(@LoadBalanced RestClient.Builder builder) {
        this.restClient = builder.baseUrl("http://PAGAMENTO-SERVICE").build();
    }

    @Override
    public PagamentoResultado cobrar(UUID reservaId, BigDecimal valor, boolean simularRecusa) {
        try (LogContexto ignorado = LogContexto.abrir(reservaId)) {
            log.info("Chamando pagamento-service: cobrar reservaId={}", reservaId);
            PagamentoResponse response = restClient.post()
                    .uri("/api/pagamentos")
                    .header(CABECALHO_CORRELACAO, LogContexto.correlacao(reservaId))
                    .body(new PagamentoRequest(reservaId, valor, simularRecusa))
                    .retrieve()
                    .body(PagamentoResponse.class);
            if (response == null) throw new FalhaIntegracaoException("Pagamento respondeu sem corpo.", null);
            log.info("pagamento-service respondeu: reservaId={} pagamentoId={} status={}", reservaId, response.pagamentoId(), response.status());
            return new PagamentoResultado(response.pagamentoId(), response.status());
        } catch (RestClientException ex) {
            log.error("Falha ao chamar pagamento-service: reservaId={} erro={}", reservaId, ex.getClass().getSimpleName());
            throw new FalhaIntegracaoException("Falha ao chamar pagamento-service.", ex);
        }
    }

    @Override
    public PagamentoResultado estornar(UUID pagamentoId) {
        try {
            log.info("Chamando pagamento-service: estornar pagamentoId={}", pagamentoId);
            PagamentoResponse response = restClient.post()
                    .uri("/api/pagamentos/{id}/estorno", pagamentoId)
                    .header(CABECALHO_CORRELACAO, LogContexto.correlacao())
                    .retrieve()
                    .body(PagamentoResponse.class);

            if (response == null) {
                throw new FalhaIntegracaoException("Pagamento respondeu sem corpo no estorno.", null);
            }

            log.info("pagamento-service respondeu: estorno pagamentoId={} status={}", response.pagamentoId(), response.status());
            return new PagamentoResultado(response.pagamentoId(), response.status());

        } catch (RestClientException ex) {
            log.error("Falha ao estornar pagamento: pagamentoId={} erro={}", pagamentoId, ex.getClass().getSimpleName());
            throw new FalhaIntegracaoException("Falha ao estornar pagamento.", ex);
        }
    }

    private record PagamentoRequest(UUID reservaId, BigDecimal valor, boolean simularRecusa) {}
    private record PagamentoResponse(UUID pagamentoId, UUID reservaId, BigDecimal valor, String status) {}
}