package org.arraipay.arraiapay.api.controller;

import org.arraipay.arraiapay.application.CartaoService;
import org.arraipay.arraiapay.api.dto.Requests.CartaoRequest;
import org.arraipay.arraiapay.api.dto.Requests.StatusCartaoRequest;
import org.arraipay.arraiapay.api.dto.Requests.ValidacaoCartaoRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.arraipay.arraiapay.application.CartaoQrCodeService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.arraipay.arraiapay.domain.enums.StatusCartao;

@RestController
@RequestMapping("/api/v1/cartoes")
public class CartaoController {
    private final CartaoService cartaoService;
    private final CartaoQrCodeService cartaoQrCodeService;

    public CartaoController(CartaoService cartaoService, CartaoQrCodeService cartaoQrCodeService) {
        this.cartaoService = cartaoService;
        this.cartaoQrCodeService = cartaoQrCodeService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Object emitir(@RequestBody CartaoRequest request) {
        return resposta(cartaoService.emitir(request.idCliente()));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Object listar() {
        return cartaoService.listar().stream().map(this::resposta).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','CAIXA','BARRACA')")
    public Object buscar(@PathVariable("id") Long id) {
        return resposta(cartaoService.buscar(id));
    }

    @GetMapping(value = "/{id}/qrcode", produces = MediaType.IMAGE_PNG_VALUE)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<byte[]> qrcode(@PathVariable("id") Long id) {
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .cacheControl(CacheControl.noStore())
                .body(cartaoQrCodeService.buscarOuGerar(id));
    }

    @PostMapping("/validacoes")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','CAIXA','BARRACA')")
    public Object validar(@RequestBody ValidacaoCartaoRequest request) {
        return resposta(cartaoService.validar(request.codigoQr()));
    }

    @GetMapping("/{id}/saldo")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','CAIXA','BARRACA')")
    public Object consultarSaldo(@PathVariable("id") Long id) {
        org.arraipay.arraiapay.domain.Cartao cartao = cartaoService.buscar(id);
        java.math.BigDecimal saldo = cartao.getCliente().getCarteira() == null
                ? java.math.BigDecimal.ZERO : cartao.getCliente().getCarteira().getSaldoAtual();
        return java.util.Map.of("saldoAtual", saldo, "status", cartao.getStatus().name());
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Object alterarStatus(@PathVariable("id") Long id, @RequestBody StatusCartaoRequest request) {
        StatusCartao status;
        if (request.status() == null) throw new org.arraipay.arraiapay.api.exception.DominioException("E18", "Status obrigatorio.");
        try { status = StatusCartao.valueOf(request.status().toUpperCase()); }
        catch (Exception exception) { throw new org.arraipay.arraiapay.api.exception.DominioException("E18", "Status invalido."); }
        return resposta(cartaoService.alterarStatus(id, status, request.justificativa()));
    }

    @PostMapping("/{id}/segunda-via")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Object segundaVia(@PathVariable("id") Long id) {
        return resposta(cartaoService.segundaVia(id));
    }

    @GetMapping("/{id}/extrato")
    public Object extrato(@PathVariable("id") Long id) {
        return cartaoService.extrato(id).stream().map(m -> java.util.Map.of(
                "id", m.getIdMovimentacao(),
                "tipo", m.getTipo().name(),
                "valor", m.getValor(),
                "saldoAnterior", m.getSaldoAnterior(),
                "saldoPosterior", m.getSaldoPosterior(),
                "dataHora", m.getDataHora().toString())).toList();
    }

    private java.util.Map<String, Object> resposta(org.arraipay.arraiapay.domain.Cartao cartao) {
        return java.util.Map.of(
                "idCartao", cartao.getIdCartao(),
                "codigoQr", cartao.getCodigoQr(),
                "status", cartao.getStatus().name(),
                "idCliente", cartao.getCliente().getIdCliente(),
                "nomeCliente", cartao.getCliente().getNome());
    }
}
