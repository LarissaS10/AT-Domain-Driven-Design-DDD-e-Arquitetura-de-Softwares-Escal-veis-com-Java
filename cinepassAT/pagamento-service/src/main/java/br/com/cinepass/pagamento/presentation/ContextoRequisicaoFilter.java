package br.com.cinepass.pagamento.presentation;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class ContextoRequisicaoFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(ContextoRequisicaoFilter.class);
    private static final String CABECALHO = "X-Correlation-Id";

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/actuator");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String recebido = request.getHeader(CABECALHO);
        String correlationId = (recebido == null || recebido.isBlank()) ? UUID.randomUUID().toString() : recebido;
        MDC.put("correlationId", correlationId);
        response.setHeader(CABECALHO, correlationId);
        long inicio = System.currentTimeMillis();
        log.info("Requisição recebida: {} {}", request.getMethod(), request.getRequestURI());
        try {
            chain.doFilter(request, response);
            int status = response.getStatus();
            long duracao = System.currentTimeMillis() - inicio;
            if (status >= 500) {
                log.error("Requisição concluída com erro: {} {} status={} duracaoMs={}", request.getMethod(), request.getRequestURI(), status, duracao);
            } else if (status >= 400) {
                log.warn("Requisição concluída com recusa: {} {} status={} duracaoMs={}", request.getMethod(), request.getRequestURI(), status, duracao);
            } else {
                log.info("Requisição concluída: {} {} status={} duracaoMs={}", request.getMethod(), request.getRequestURI(), status, duracao);
            }
        } catch (Exception ex) {
            log.error("Requisição falhou: {} {} duracaoMs={} erro={}", request.getMethod(), request.getRequestURI(),
                    System.currentTimeMillis() - inicio, ex.getClass().getSimpleName());
            throw ex;
        } finally {
            MDC.remove("correlationId");
            MDC.remove("reservaId");
            MDC.remove("eventId");
        }
    }
}