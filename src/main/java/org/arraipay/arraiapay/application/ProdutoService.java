package org.arraipay.arraiapay.application;

import org.arraipay.arraiapay.api.exception.DominioException;
import org.arraipay.arraiapay.domain.Barraca;
import org.arraipay.arraiapay.domain.Produto;
import org.arraipay.arraiapay.domain.enums.CategoriaProduto;
import org.arraipay.arraiapay.repository.BarracaRepository;
import org.arraipay.arraiapay.repository.ProdutoRepository;
import org.arraipay.arraiapay.domain.enums.PerfilUsuario;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ProdutoService {
    private final ProdutoRepository produtoRepository;
    private final BarracaRepository barracaRepository;
    private final UsuarioAtual usuarioAtual;

    public ProdutoService(ProdutoRepository produtoRepository, BarracaRepository barracaRepository,
                          UsuarioAtual usuarioAtual) {
        this.produtoRepository = produtoRepository;
        this.barracaRepository = barracaRepository;
        this.usuarioAtual = usuarioAtual;
    }

    @Transactional
    public Produto criar(Long idBarraca, String nome, String categoria, String descricao,
                         BigDecimal preco, Boolean ativo) {
        Barraca barraca = barracaRepository.findById(idBarraca)
                .orElseThrow(() -> new DominioException("E08", "Barraca nao encontrada."));
        if (nome == null || nome.isBlank()) throw new DominioException("E06", "Nome do produto obrigatorio.");
        if (preco == null || preco.compareTo(BigDecimal.ZERO) <= 0) throw new DominioException("E06", "Preco deve ser positivo.");
        CategoriaProduto categoriaProduto = categoria == null ? CategoriaProduto.OUTRO : CategoriaProduto.valueOf(categoria.toUpperCase());
        Produto produto = new Produto(barraca, nome.trim(), categoriaProduto, preco);
        produto.setDescricao(descricao);
        if (ativo != null) produto.setAtivo(ativo);
        return produtoRepository.save(produto);
    }

    public Produto buscar(Long id) {
        return produtoRepository.findById(id).orElseThrow(() -> new DominioException("E06", "Produto nao encontrado."));
    }

    public List<Produto> cardapio(Long idBarraca) {
        var usuario = usuarioAtual.get();
        if (usuario.getPerfil() == PerfilUsuario.BARRACA
                && (usuario.getBarraca() == null
                || !usuario.getBarraca().getIdBarraca().equals(idBarraca))) {
            throw new DominioException("E09", "Acesso permitido somente a produtos da propria barraca.");
        }
        return produtoRepository.findByBarraca_IdBarracaAndAtivoTrue(idBarraca);
    }

    @Transactional
    public Produto atualizar(Long id, String nome, String categoria, BigDecimal preco, String descricao, Boolean ativo) {
        Produto produto = buscar(id);
        if (nome != null) produto.setNome(nome);
        if (categoria != null) produto.setCategoria(CategoriaProduto.valueOf(categoria.toUpperCase()));
        if (preco != null) {
            if (preco.compareTo(BigDecimal.ZERO) <= 0) throw new DominioException("E06", "Preco deve ser positivo.");
            produto.setPreco(preco);
        }
        if (descricao != null) produto.setDescricao(descricao);
        if (ativo != null) produto.setAtivo(ativo);
        return produtoRepository.save(produto);
    }
}
