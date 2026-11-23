package org.arraipay.arraiapay.api.controller;

import org.arraipay.arraiapay.application.BarracaService;
import org.arraipay.arraiapay.api.dto.Requests.BarracaRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/v1/barracas")
public class BarracaController {
    private final BarracaService barracaService;
    public BarracaController(BarracaService barracaService) { this.barracaService = barracaService; }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Object criar(@RequestBody BarracaRequest request) {
        return barracaService.criar(request.nome(), request.responsavel(), request.ativa());
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Object listar(@RequestParam(name = "ativa", required = false) Boolean ativa,
                         @RequestParam(name = "page", defaultValue = "0") int page,
                         @RequestParam(name = "size", defaultValue = "20") int size,
                         @RequestParam(name = "sort", defaultValue = "nome,asc") String sort) {
        return barracaService.listar(ativa);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Object buscar(@PathVariable("id") Long id) {
        return barracaService.buscar(id);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Object atualizar(@PathVariable("id") Long id, @RequestBody BarracaRequest request) {
        return barracaService.atualizar(id, request.nome(), request.responsavel(), request.ativa());
    }
}
