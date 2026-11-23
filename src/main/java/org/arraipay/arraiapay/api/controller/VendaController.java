package org.arraipay.arraiapay.api.controller;

import org.arraipay.arraiapay.application.VendaService;
import org.arraipay.arraiapay.api.dto.Requests.EstornoRequest;
import org.arraipay.arraiapay.api.dto.Requests.VendaRequest;
import org.arraipay.arraiapay.api.dto.Requests.VendaValorRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.arraipay.arraiapay.application.EstornoService;

@RestController
@RequestMapping("/api/v1/vendas")
public class VendaController {
    private final VendaService vendaService;
    private final EstornoService estornoService;

    public VendaController(VendaService vendaService, EstornoService estornoService) {
        this.vendaService = vendaService;
        this.estornoService = estornoService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('BARRACA','ADMINISTRADOR')")
    @Transactional
    public Object criar(@RequestHeader("Idempotency-Key") String chaveIdempotencia,
                        @RequestBody VendaRequest request) {
        return resposta(vendaService.registrar(request, chaveIdempotencia));
    }

    @PostMapping("/valor")
    @PreAuthorize("hasAnyRole('BARRACA','ADMINISTRADOR')")
    @Transactional
    public Object criarPorValor(@RequestHeader("Idempotency-Key") String chaveIdempotencia,
                                @RequestBody VendaValorRequest request) {
        return resposta(vendaService.registrarPorValor(request, chaveIdempotencia));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','BARRACA')")
    @Transactional(readOnly = true)
    public Object buscar(@PathVariable("id") Long id) {
        return resposta(vendaService.buscar(id));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','BARRACA')")
    @Transactional(readOnly = true)
    public Object listar(@RequestParam(name = "inicio", required = false) String inicio,
                         @RequestParam(name = "fim", required = false) String fim,
                         @RequestParam(name = "idCartao", required = false) Long idCartao,
                         @RequestParam(name = "idBarraca", required = false) Long idBarraca,
                         @RequestParam(name = "status", required = false) String status,
                         @RequestParam(name = "page", defaultValue = "0") int page,
                         @RequestParam(name = "size", defaultValue = "20") int size,
                         @RequestParam(name = "sort", defaultValue = "dataHora,desc") String sort) {
        return vendaService.listar().stream().map(this::resposta).toList();
    }

    @PostMapping("/{id}/estorno")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Transactional
    public Object estornar(@PathVariable("id") Long id,
                           @RequestHeader("Idempotency-Key") String chave,
                           @RequestBody EstornoRequest request) {
        return resposta(estornoService.estornarVenda(id, request.justificativa(), chave));
    }

    private java.util.Map<String, Object> resposta(org.arraipay.arraiapay.domain.Venda venda) {
        return java.util.Map.of(
                "idVenda", venda.getIdVenda(),
                "idCartao", venda.getCartao().getIdCartao(),
                "nomeCliente", venda.getCartao().getCliente().getNome(),
                "clienteAtivo", venda.getCartao().getCliente().isAtivo(),
                "idBarraca", venda.getBarraca().getIdBarraca(),
                "nomeBarraca", venda.getBarraca().getNome(),
                "valorTotal", venda.getValorTotal(),
                "status", venda.getStatus().name(),
                "dataHora", venda.getDataHora().toString(),
                "itens", venda.getItens().stream().map(item -> java.util.Map.of(
                        "produto", item.getProduto().getNome(),
                        "quantidade", item.getQuantidade(),
                        "precoUnitario", item.getPrecoUnitario(),
                        "subtotal", item.getSubtotal())).toList());
    }
}
