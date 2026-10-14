package br.com.cinepass.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class CorrelationIdFilter implements GlobalFilter, Ordered {
    private static final Logger log = LoggerFactory.getLogger(CorrelationIdFilter.class);
    private static final String CABECALHO = "X-Correlation-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String recebido = exchange.getRequest().getHeaders().getFirst(CABECALHO);
        String correlationId = (recebido == null || recebido.isBlank()) ? UUID.randomUUID().toString() : recebido;
        ServerHttpRequest request = exchange.getRequest().mutate().header(CABECALHO, correlationId).build();
        log.info("Requisição recebida no gateway: {} {} correlationId={}",
                request.getMethod(), request.getURI().getPath(), correlationId);
        return chain.filter(exchange.mutate().request(request).build());
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}