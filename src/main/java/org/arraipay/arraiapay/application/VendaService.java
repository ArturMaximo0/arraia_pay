package org.arraipay.arraiapay.application;

import org.arraipay.arraiapay.api.exception.DominioException;
import org.arraipay.arraiapay.domain.Carteira;
import org.arraipay.arraiapay.domain.Barraca;
import org.arraipay.arraiapay.domain.Cartao;
import org.arraipay.arraiapay.domain.ItemVenda;
import org.arraipay.arraiapay.domain.MovimentacaoFinanceira;
import org.arraipay.arraiapay.domain.OperacaoIdempotente;
import org.arraipay.arraiapay.domain.Produto;
import org.arraipay.arraiapay.domain.Usuario;
import org.arraipay.arraiapay.domain.Venda;
import org.arraipay.arraiapay.domain.enums.PerfilUsuario;
import org.arraipay.arraiapay.domain.enums.StatusCartao;
import org.arraipay.arraiapay.domain.enums.TipoMovimentacao;
import org.arraipay.arraiapay.repository.CarteiraRepository;
import org.arraipay.arraiapay.repository.CartaoRepository;
import org.arraipay.arraiapay.repository.MovimentacaoFinanceiraRepository;
import org.arraipay.arraiapay.repository.OperacaoIdempotenteRepository;
import org.arraipay.arraiapay.repository.ProdutoRepository;
import org.arraipay.arraiapay.repository.BarracaRepository;
import org.arraipay.arraiapay.repository.VendaRepository;
import org.arraipay.arraiapay.api.dto.Requests.ItemVendaRequest;
import org.arraipay.arraiapay.api.dto.Requests.VendaRequest;
import org.arraipay.arraiapay.api.dto.Requests.VendaValorRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class VendaService {
    private final CartaoRepository cartaoRepository;
    private final CarteiraRepository carteiraRepository;
    private final ProdutoRepository produtoRepository;
    private final VendaRepository vendaRepository;
    private final MovimentacaoFinanceiraRepository movimentacaoRepository;
    private final OperacaoIdempotenteRepository operacaoRepository;
    private final UsuarioAtual usuarioAtual;
    private final BarracaRepository barracaRepository;

    public VendaService(CartaoRepository cartaoRepository, CarteiraRepository carteiraRepository,
                        ProdutoRepository produtoRepository, VendaRepository vendaRepository,
                        MovimentacaoFinanceiraRepository movimentacaoRepository,
                        OperacaoIdempotenteRepository operacaoRepository, UsuarioAtual usuarioAtual,
                        BarracaRepository barracaRepository) {
        this.cartaoRepository = cartaoRepository;
        this.carteiraRepository = carteiraRepository;
        this.produtoRepository = produtoRepository;
        this.vendaRepository = vendaRepository;
        this.movimentacaoRepository = movimentacaoRepository;
        this.operacaoRepository = operacaoRepository;
        this.usuarioAtual = usuarioAtual;
        this.barracaRepository = barracaRepository;
    }

    @Transactional
    public Venda registrar(VendaRequest request, String chaveIdempotencia) {
        if (chaveIdempotencia == null || chaveIdempotencia.isBlank()) throw new DominioException("E12", "Idempotency-Key obrigatoria.");
        if (request.itens() == null || request.itens().isEmpty()) throw new DominioException("E06", "Venda sem itens.");

        Usuario usuario = usuarioAtual.get();
        String payload = request.idCartao() + "|" + request.idBarraca() + "|" +
                request.itens().stream().map(i -> i.idProduto() + ":" + i.quantidade()).reduce("", (a, b) -> a + "|" + b);
        String hash = HashUtil.sha256(payload);

        var existente = operacaoRepository.findByUsuarioIdAndTipoOperacaoAndChaveIdempotencia(
                usuario.getIdUsuario(), "VENDA", chaveIdempotencia);
        if (existente.isPresent()) {
            if (!existente.get().getPayloadHash().equals(hash)) throw new DominioException("E12", "Chave idempotente reutilizada com conteudo diferente.");
            return vendaRepository.findByChaveUnica(chaveIdempotencia)
                    .orElseThrow(() -> new DominioException("E14", "Resultado idempotente indisponivel."));
        }

        if (usuario.getPerfil() == PerfilUsuario.BARRACA
                && (usuario.getBarraca() == null || !usuario.getBarraca().getIdBarraca().equals(request.idBarraca()))) {
            throw new DominioException("E09", "Operacao fora da barraca vinculada.");
        }

        Cartao cartao = cartaoRepository.findById(request.idCartao())
                .orElseThrow(() -> new DominioException("E01", "Cartao nao encontrado."));
        if (cartao.getStatus() != StatusCartao.ATIVO) throw new DominioException("E02", "Cartao indisponivel.");
        if (!cartao.getCliente().isAtivo()) throw new DominioException("E03", "Cliente indisponivel.");

        Carteira carteira = carteiraRepository.findByIdForUpdate(cartao.getCliente().getCarteira().getIdCarteira())
                .orElseThrow(() -> new DominioException("E19", "Carteira nao encontrada."));
        if (!carteira.isAtiva()) throw new DominioException("E19", "Carteira inativa.");

        org.arraipay.arraiapay.domain.Barraca barraca = usuario.getPerfil() == PerfilUsuario.BARRACA
                ? usuario.getBarraca()
                : barracaRepository.findById(request.idBarraca())
                    .orElseThrow(() -> new DominioException("E08", "Barraca nao encontrada."));
        Venda venda = new Venda(cartao, carteira, barraca, usuario, chaveIdempotencia);

        List<ItemVendaRequest> itens = request.itens();
        for (ItemVendaRequest item : itens) {
            if (item.quantidade() == null || item.quantidade() <= 0) throw new DominioException("E06", "Quantidade deve ser maior que zero.");
            Produto produto = produtoRepository.findById(item.idProduto())
                    .orElseThrow(() -> new DominioException("E06", "Produto nao encontrado."));
            if (!produto.isAtivo()) throw new DominioException("E06", "Produto indisponivel.");
            if (!produto.getBarraca().getIdBarraca().equals(request.idBarraca())) throw new DominioException("E09", "Produto pertence a outra barraca.");
            venda.adicionarItem(new ItemVenda(produto, item.quantidade(), produto.getPreco()));
        }

        if (carteira.getSaldoAtual().compareTo(venda.getValorTotal()) < 0) {
            throw new DominioException("E04", "Saldo insuficiente.");
        }

        BigDecimal anterior = carteira.getSaldoAtual();
        BigDecimal posterior = anterior.subtract(venda.getValorTotal());
        carteira.setSaldoAtual(posterior);

        venda = vendaRepository.save(venda);
        MovimentacaoFinanceira movimentacao = new MovimentacaoFinanceira(
                carteira, cartao, TipoMovimentacao.VENDA, venda.getValorTotal(),
                anterior, posterior, usuario, venda.getIdVenda().toString(), null);
        movimentacaoRepository.save(movimentacao);
        operacaoRepository.save(new OperacaoIdempotente(
                usuario.getIdUsuario(), "VENDA", chaveIdempotencia, hash, 201, venda.getIdVenda().toString()));
        return venda;
    }

    @Transactional
    public Venda registrarPorValor(VendaValorRequest request, String chaveIdempotencia) {
        if (chaveIdempotencia == null || chaveIdempotencia.isBlank()) {
            throw new DominioException("E12", "Idempotency-Key obrigatoria.");
        }
        if (request.valor() == null || request.valor().compareTo(BigDecimal.ZERO) <= 0) {
            throw new DominioException("E06", "Valor da venda deve ser positivo.");
        }

        Usuario usuario = usuarioAtual.get();
        String payload = request.idCartao() + "|" + request.idBarraca() + "|" + request.valor().toPlainString();
        String hash = HashUtil.sha256(payload);

        var existente = operacaoRepository.findByUsuarioIdAndTipoOperacaoAndChaveIdempotencia(
                usuario.getIdUsuario(), "VENDA", chaveIdempotencia);
        if (existente.isPresent()) {
            if (!existente.get().getPayloadHash().equals(hash)) {
                throw new DominioException("E12", "Chave idempotente reutilizada com conteudo diferente.");
            }
            return vendaRepository.findByChaveUnica(chaveIdempotencia)
                    .orElseThrow(() -> new DominioException("E14", "Resultado idempotente indisponivel."));
        }

        if (usuario.getPerfil() == PerfilUsuario.BARRACA
                && (usuario.getBarraca() == null
                || !usuario.getBarraca().getIdBarraca().equals(request.idBarraca()))) {
            throw new DominioException("E09", "Operacao fora da barraca vinculada.");
        }

        Cartao cartao = cartaoRepository.findById(request.idCartao())
                .orElseThrow(() -> new DominioException("E01", "Cartao nao encontrado."));
        if (cartao.getStatus() != StatusCartao.ATIVO) {
            throw new DominioException("E02", "Cartao indisponivel.");
        }
        if (!cartao.getCliente().isAtivo()) {
            throw new DominioException("E03", "Cliente indisponivel.");
        }

        Carteira carteira = carteiraRepository.findByIdForUpdate(
                        cartao.getCliente().getCarteira().getIdCarteira())
                .orElseThrow(() -> new DominioException("E19", "Carteira nao encontrada."));
        if (!carteira.isAtiva()) {
            throw new DominioException("E19", "Carteira inativa.");
        }
        if (carteira.getSaldoAtual().compareTo(request.valor()) < 0) {
            throw new DominioException("E04", "Saldo insuficiente.");
        }

        Barraca barraca = usuario.getPerfil() == PerfilUsuario.BARRACA
                ? usuario.getBarraca()
                : barracaRepository.findById(request.idBarraca())
                    .orElseThrow(() -> new DominioException("E08", "Barraca nao encontrada."));
        Venda venda = new Venda(cartao, carteira, barraca, usuario, chaveIdempotencia);
        venda.definirValorTotal(request.valor().setScale(2, java.math.RoundingMode.HALF_UP));

        BigDecimal anterior = carteira.getSaldoAtual();
        BigDecimal posterior = anterior.subtract(venda.getValorTotal());
        carteira.setSaldoAtual(posterior);

        venda = vendaRepository.save(venda);
        MovimentacaoFinanceira movimentacao = new MovimentacaoFinanceira(
                carteira, cartao, TipoMovimentacao.VENDA, venda.getValorTotal(),
                anterior, posterior, usuario, venda.getIdVenda().toString(), null);
        movimentacaoRepository.save(movimentacao);
        operacaoRepository.save(new OperacaoIdempotente(
                usuario.getIdUsuario(), "VENDA", chaveIdempotencia, hash, 201, venda.getIdVenda().toString()));
        return venda;
    }

    public java.util.List<Venda> listar() {
        Usuario usuario = usuarioAtual.get();
        if (usuario.getPerfil().name().equals("ADMINISTRADOR")) return vendaRepository.findAll();
        if (usuario.getPerfil().name().equals("BARRACA") && usuario.getBarraca() != null) {
            return vendaRepository.findAll().stream()
                    .filter(venda -> venda.getBarraca().getIdBarraca().equals(usuario.getBarraca().getIdBarraca()))
                    .toList();
        }
        throw new DominioException("E09", "Sem permissao para consultar vendas.");
    }

    public Venda buscar(Long id) {
        return vendaRepository.findById(id)
                .orElseThrow(() -> new DominioException("E16", "Venda nao encontrada."));
    }
}
