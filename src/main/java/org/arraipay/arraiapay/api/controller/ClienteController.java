package org.arraipay.arraiapay.api.controller;

import org.arraipay.arraiapay.application.ClienteService;
import org.arraipay.arraiapay.api.dto.Requests.ClienteRequest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/clientes")
public class ClienteController {
    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) { this.clienteService = clienteService; }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Object criar(@RequestBody ClienteRequest request) {
        return resposta(clienteService.criar(request));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Object listar(@RequestParam(required = false) Boolean ativo,
                         @RequestParam(defaultValue = "0") int page,
                         @RequestParam(defaultValue = "20") int size) {
        return clienteService.listar(ativo, PageRequest.of(page, size, Sort.by("idCliente").ascending()))
                .map(this::resposta);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Object buscar(@PathVariable Long id) { return resposta(clienteService.buscar(id)); }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Object atualizar(@PathVariable Long id, @RequestBody ClienteRequest request) {
        return resposta(clienteService.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Object arquivar(@PathVariable Long id) {
        return resposta(clienteService.arquivar(id));
    }

    private java.util.Map<String, Object> resposta(org.arraipay.arraiapay.domain.Cliente cliente) {
        return java.util.Map.of(
                "idCliente", cliente.getIdCliente(),
                "nome", cliente.getNome(),
                "documento", cliente.getDocumento() == null ? "" : cliente.getDocumento(),
                "telefone", cliente.getTelefone() == null ? "" : cliente.getTelefone(),
                "ativo", cliente.isAtivo(),
                "saldoAtual", cliente.getCarteira() == null
                        ? java.math.BigDecimal.ZERO : cliente.getCarteira().getSaldoAtual());
    }
}
