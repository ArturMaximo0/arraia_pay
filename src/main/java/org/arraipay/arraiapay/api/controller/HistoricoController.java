package org.arraipay.arraiapay.api.controller;

import org.arraipay.arraiapay.api.dto.Requests.LimparHistoricoRequest;
import org.arraipay.arraiapay.application.HistoricoService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/historico")
public class HistoricoController {
    private final HistoricoService historicoService;

    public HistoricoController(HistoricoService historicoService) {
        this.historicoService = historicoService;
    }

    @PostMapping("/limpar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Map<String, String> limpar(@RequestBody LimparHistoricoRequest request) {
        historicoService.limpar(request.senhaAdmin());
        return Map.of("mensagem", "Historico de operacoes removido. Saldos e cadastros foram preservados.");
    }
}
