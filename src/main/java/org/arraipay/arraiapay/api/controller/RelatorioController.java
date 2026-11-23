package org.arraipay.arraiapay.api.controller;

import org.arraipay.arraiapay.application.RelatorioService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/relatorios")
public class RelatorioController {
    private final RelatorioService relatorioService;

    public RelatorioController(RelatorioService relatorioService) {
        this.relatorioService = relatorioService;
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Object dashboard() {
        return relatorioService.dashboard();
    }

    @GetMapping("/vendas-por-barraca")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Object vendasPorBarraca() {
        return relatorioService.vendasPorBarraca();
    }
}
