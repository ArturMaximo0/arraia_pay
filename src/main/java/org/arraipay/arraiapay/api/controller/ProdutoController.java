package org.arraipay.arraiapay.api.controller;

import org.arraipay.arraiapay.application.ProdutoService;
import org.arraipay.arraiapay.api.dto.Requests.ProdutoRequest;
import org.arraipay.arraiapay.api.dto.Requests.ProdutoUpdateRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class ProdutoController {
    private final ProdutoService produtoService;

    public ProdutoController(ProdutoService produtoService) {
        this.produtoService = produtoService;
    }

    @GetMapping("/barracas/{idBarraca}/cardapio")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','CAIXA','BARRACA')")
    @Transactional(readOnly = true)
    public Object cardapio(@PathVariable Long idBarraca) {
        return produtoService.cardapio(idBarraca).stream().map(this::resposta).toList();
    }

    @PostMapping("/produtos")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Transactional
    public Object criar(@RequestBody ProdutoRequest request) {
        return resposta(produtoService.criar(request.idBarraca(), request.nome(), request.categoria(),
                request.descricao(), request.preco(), request.ativo()));
    }

    @GetMapping("/produtos/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','BARRACA')")
    @Transactional(readOnly = true)
    public Object buscar(@PathVariable Long id) {
        return resposta(produtoService.buscar(id));
    }

    @PatchMapping("/produtos/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Transactional
    public Object atualizar(@PathVariable Long id, @RequestBody ProdutoUpdateRequest request) {
        return resposta(produtoService.atualizar(id, request.nome(), request.categoria(),
                request.preco(), request.descricao(), request.ativo()));
    }

    private java.util.Map<String, Object> resposta(org.arraipay.arraiapay.domain.Produto produto) {
        return java.util.Map.of(
                "idProduto", produto.getIdProduto(),
                "idBarraca", produto.getBarraca().getIdBarraca(),
                "nomeBarraca", produto.getBarraca().getNome(),
                "nome", produto.getNome(),
                "categoria", produto.getCategoria().name(),
                "descricao", produto.getDescricao() == null ? "" : produto.getDescricao(),
                "preco", produto.getPreco(),
                "ativo", produto.isAtivo());
    }
}
