package br.com.cinepass.reputacao.presentation;

import br.com.cinepass.reputacao.application.ReputacaoService;
import br.com.cinepass.reputacao.domain.ReputacaoCliente;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reputacao")
public class ReputacaoController {
    private final ReputacaoService service;

    public ReputacaoController(ReputacaoService service) {
        this.service = service;
    }

    @GetMapping
    public List<ReputacaoResponse> listar() {
        return service.listar().stream().map(ReputacaoResponse::de).toList();
    }

    @GetMapping("/{clienteId}")
    public ResponseEntity<ReputacaoResponse> buscar(@PathVariable UUID clienteId) {
        return service.buscar(clienteId).map(ReputacaoResponse::de).map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    public record ReputacaoResponse(UUID clienteId, int reservasConfirmadas, int reservasCanceladas, int pontos,
                                    Instant atualizadoEm) {
        static ReputacaoResponse de(ReputacaoCliente r) {
            return new ReputacaoResponse(r.getClienteId(), r.getReservasConfirmadas(), r.getReservasCanceladas(),
                    r.getPontos(), r.getAtualizadoEm());
        }
    }
}