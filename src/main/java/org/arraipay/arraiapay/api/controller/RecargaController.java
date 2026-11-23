package org.arraipay.arraiapay.api.controller;

import org.arraipay.arraiapay.application.RecargaService;
import org.arraipay.arraiapay.api.dto.Requests.RecargaRequest;
import org.arraipay.arraiapay.api.dto.Requests.EstornoRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.arraipay.arraiapay.application.EstornoService;

@RestController
@RequestMapping("/api/v1/recargas")
public class RecargaController {
    private final RecargaService recargaService;
    private final EstornoService estornoService;

    public RecargaController(RecargaService recargaService, EstornoService estornoService) {
        this.recargaService = recargaService;
        this.estornoService = estornoService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('CAIXA','ADMINISTRADOR')")
    @Transactional
    public Object criar(@RequestHeader("Idempotency-Key") String chave,
                        @RequestBody RecargaRequest request) {
        return resposta(recargaService.registrar(request.idCartao(), request.valor(), request.formaPagamento(), chave));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','CAIXA')")
    @Transactional(readOnly = true)
    public Object buscar(@PathVariable("id") Long id) {
        return resposta(recargaService.buscar(id));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','CAIXA')")
    @Transactional(readOnly = true)
    public Object listar(@RequestParam(name = "inicio", required = false) String inicio,
                         @RequestParam(name = "fim", required = false) String fim,
                         @RequestParam(name = "idCartao", required = false) Long idCartao,
                         @RequestParam(name = "status", required = false) String status,
                         @RequestParam(name = "page", defaultValue = "0") int page,
                         @RequestParam(name = "size", defaultValue = "20") int size,
                         @RequestParam(name = "sort", defaultValue = "dataHora,desc") String sort) {
        return recargaService.listar().stream().map(this::resposta).toList();
    }

    @PostMapping("/{id}/estorno")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Transactional
    public Object estornar(@PathVariable("id") Long id,
                           @RequestHeader("Idempotency-Key") String chave,
                           @RequestBody EstornoRequest request) {
        return resposta(estornoService.estornarRecarga(id, request.justificativa(), chave));
    }

    private java.util.Map<String, Object> resposta(org.arraipay.arraiapay.domain.Recarga recarga) {
        return java.util.Map.of(
                "idRecarga", recarga.getIdRecarga(),
                "idCartao", recarga.getCartao().getIdCartao(),
                "nomeCliente", recarga.getCartao().getCliente().getNome(),
                "clienteAtivo", recarga.getCartao().getCliente().isAtivo(),
                "valor", recarga.getValor(),
                "formaPagamento", recarga.getFormaPagamento().name(),
                "status", recarga.getStatus().name(),
                "dataHora", recarga.getDataHora().toString());
    }
}
