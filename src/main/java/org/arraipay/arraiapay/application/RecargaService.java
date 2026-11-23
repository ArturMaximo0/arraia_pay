package org.arraipay.arraiapay.application;

import org.arraipay.arraiapay.api.exception.DominioException;
import org.arraipay.arraiapay.domain.Carteira;
import org.arraipay.arraiapay.domain.Cartao;
import org.arraipay.arraiapay.domain.MovimentacaoFinanceira;
import org.arraipay.arraiapay.domain.OperacaoIdempotente;
import org.arraipay.arraiapay.domain.Recarga;
import org.arraipay.arraiapay.domain.Usuario;
import org.arraipay.arraiapay.domain.enums.FormaPagamento;
import org.arraipay.arraiapay.domain.enums.StatusCartao;
import org.arraipay.arraiapay.domain.enums.TipoMovimentacao;
import org.arraipay.arraiapay.repository.CarteiraRepository;
import org.arraipay.arraiapay.repository.CartaoRepository;
import org.arraipay.arraiapay.repository.MovimentacaoFinanceiraRepository;
import org.arraipay.arraiapay.repository.OperacaoIdempotenteRepository;
import org.arraipay.arraiapay.repository.RecargaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class RecargaService {
    private final CartaoRepository cartaoRepository;
    private final CarteiraRepository carteiraRepository;
    private final RecargaRepository recargaRepository;
    private final MovimentacaoFinanceiraRepository movimentacaoRepository;
    private final OperacaoIdempotenteRepository operacaoRepository;
    private final UsuarioAtual usuarioAtual;

    public RecargaService(CartaoRepository cartaoRepository, CarteiraRepository carteiraRepository,
                          RecargaRepository recargaRepository,
                          MovimentacaoFinanceiraRepository movimentacaoRepository,
                          OperacaoIdempotenteRepository operacaoRepository,
                          UsuarioAtual usuarioAtual) {
        this.cartaoRepository = cartaoRepository;
        this.carteiraRepository = carteiraRepository;
        this.recargaRepository = recargaRepository;
        this.movimentacaoRepository = movimentacaoRepository;
        this.operacaoRepository = operacaoRepository;
        this.usuarioAtual = usuarioAtual;
    }

    @Transactional
    public Recarga registrar(Long idCartao, BigDecimal valor, String formaPagamento, String chaveIdempotencia) {
        if (chaveIdempotencia == null || chaveIdempotencia.isBlank()) throw new DominioException("E12", "Idempotency-Key obrigatoria.");
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) throw new DominioException("E05", "Valor de recarga deve ser positivo.");

        Usuario usuario = usuarioAtual.get();
        String payload = idCartao + "|" + valor.toPlainString() + "|" + formaPagamento;
        String hash = HashUtil.sha256(payload);

        var existente = operacaoRepository.findByUsuarioIdAndTipoOperacaoAndChaveIdempotencia(
                usuario.getIdUsuario(), "RECARGA", chaveIdempotencia);
        if (existente.isPresent()) {
            if (!existente.get().getPayloadHash().equals(hash)) throw new DominioException("E12", "Chave idempotente reutilizada com conteudo diferente.");
            return recargaRepository.findByChaveUnica(chaveIdempotencia)
                    .orElseThrow(() -> new DominioException("E14", "Resultado idempotente indisponivel."));
        }

        Cartao cartao = cartaoRepository.findById(idCartao)
                .orElseThrow(() -> new DominioException("E01", "Cartao nao encontrado."));
        if (cartao.getStatus() != StatusCartao.ATIVO) throw new DominioException("E02", "Cartao indisponivel.");
        if (!cartao.getCliente().isAtivo()) throw new DominioException("E03", "Cliente indisponivel.");

        Carteira carteira = carteiraRepository.findByIdForUpdate(cartao.getCliente().getCarteira().getIdCarteira())
                .orElseThrow(() -> new DominioException("E19", "Carteira nao encontrada."));
        if (!carteira.isAtiva()) throw new DominioException("E19", "Carteira inativa.");

        FormaPagamento forma;
        try { forma = FormaPagamento.valueOf(formaPagamento.toUpperCase()); }
        catch (Exception exception) { throw new DominioException("E05", "Forma de pagamento invalida."); }

        BigDecimal anterior = carteira.getSaldoAtual();
        BigDecimal posterior = anterior.add(valor);
        carteira.setSaldoAtual(posterior);

        Recarga recarga = new Recarga(cartao, carteira, valor, forma, usuario);
        recarga.setChaveUnica(chaveIdempotencia);
        recarga = recargaRepository.save(recarga);

        MovimentacaoFinanceira movimentacao = new MovimentacaoFinanceira(
                carteira, cartao, TipoMovimentacao.RECARGA, valor,
                anterior, posterior, usuario, recarga.getIdRecarga().toString(), null);
        movimentacaoRepository.save(movimentacao);

        operacaoRepository.save(new OperacaoIdempotente(
                usuario.getIdUsuario(), "RECARGA", chaveIdempotencia, hash, 201, recarga.getIdRecarga().toString()));
        return recarga;
    }

    public java.util.List<Recarga> listar() {
        Usuario usuario = usuarioAtual.get();
        if (usuario.getPerfil().name().equals("ADMINISTRADOR") || usuario.getPerfil().name().equals("CAIXA")) {
            return recargaRepository.findAll();
        }
        throw new DominioException("E09", "Sem permissao para consultar recargas.");
    }

    public Recarga buscar(Long id) {
        return recargaRepository.findById(id)
                .orElseThrow(() -> new DominioException("E16", "Recarga nao encontrada."));
    }
}
