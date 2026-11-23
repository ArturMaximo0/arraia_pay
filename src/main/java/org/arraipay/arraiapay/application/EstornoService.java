package org.arraipay.arraiapay.application;

import org.arraipay.arraiapay.api.exception.DominioException;
import org.arraipay.arraiapay.domain.Carteira;
import org.arraipay.arraiapay.domain.MovimentacaoFinanceira;
import org.arraipay.arraiapay.domain.OperacaoIdempotente;
import org.arraipay.arraiapay.domain.Recarga;
import org.arraipay.arraiapay.domain.Usuario;
import org.arraipay.arraiapay.domain.Venda;
import org.arraipay.arraiapay.domain.enums.StatusRecarga;
import org.arraipay.arraiapay.domain.enums.StatusVenda;
import org.arraipay.arraiapay.domain.enums.TipoMovimentacao;
import org.arraipay.arraiapay.repository.CarteiraRepository;
import org.arraipay.arraiapay.repository.MovimentacaoFinanceiraRepository;
import org.arraipay.arraiapay.repository.OperacaoIdempotenteRepository;
import org.arraipay.arraiapay.repository.RecargaRepository;
import org.arraipay.arraiapay.repository.VendaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class EstornoService {
    private final RecargaRepository recargaRepository;
    private final VendaRepository vendaRepository;
    private final CarteiraRepository carteiraRepository;
    private final MovimentacaoFinanceiraRepository movimentacaoRepository;
    private final OperacaoIdempotenteRepository operacaoRepository;
    private final UsuarioAtual usuarioAtual;

    public EstornoService(RecargaRepository recargaRepository, VendaRepository vendaRepository,
                          CarteiraRepository carteiraRepository,
                          MovimentacaoFinanceiraRepository movimentacaoRepository,
                          OperacaoIdempotenteRepository operacaoRepository,
                          UsuarioAtual usuarioAtual) {
        this.recargaRepository = recargaRepository;
        this.vendaRepository = vendaRepository;
        this.carteiraRepository = carteiraRepository;
        this.movimentacaoRepository = movimentacaoRepository;
        this.operacaoRepository = operacaoRepository;
        this.usuarioAtual = usuarioAtual;
    }

    @Transactional
    public Recarga estornarRecarga(Long idRecarga, String justificativa, String chaveIdempotencia) {
        Usuario administrador = usuarioAtual.get();
        if (chaveIdempotencia == null || chaveIdempotencia.isBlank()) throw new DominioException("E12", "Idempotency-Key obrigatoria.");
        if (justificativa == null || justificativa.isBlank()) throw new DominioException("E10", "Justificativa obrigatoria.");

        Recarga recarga = recargaRepository.findById(idRecarga)
                .orElseThrow(() -> new DominioException("E16", "Recarga nao encontrada."));
        if (recarga.getStatus() == StatusRecarga.ESTORNADA) throw new DominioException("E11", "Recarga ja estornada.");

        String payload = "RECARGA|" + idRecarga + "|" + justificativa;
        String hash = HashUtil.sha256(payload);
        var existente = operacaoRepository.findByUsuarioIdAndTipoOperacaoAndChaveIdempotencia(
                administrador.getIdUsuario(), "ESTORNO_RECARGA", chaveIdempotencia);
        if (existente.isPresent()) {
            if (!existente.get().getPayloadHash().equals(hash)) throw new DominioException("E12", "Chave idempotente reutilizada com conteudo diferente.");
            return recarga;
        }

        Carteira carteira = carteiraRepository.findByIdForUpdate(recarga.getCarteira().getIdCarteira())
                .orElseThrow(() -> new DominioException("E19", "Carteira nao encontrada."));
        if (carteira.getSaldoAtual().compareTo(recarga.getValor()) < 0) {
            throw new DominioException("E13", "Estorno impedido: o saldo da carteira ficaria negativo.");
        }

        BigDecimal anterior = carteira.getSaldoAtual();
        BigDecimal posterior = anterior.subtract(recarga.getValor());
        carteira.setSaldoAtual(posterior);
        recarga.setStatus(StatusRecarga.ESTORNADA);
        recarga.setAutorizador(administrador);
        recarga.setJustificativaEstorno(justificativa);
        recargaRepository.save(recarga);

        movimentacaoRepository.save(new MovimentacaoFinanceira(
                carteira, recarga.getCartao(), TipoMovimentacao.ESTORNO_RECARGA, recarga.getValor(),
                anterior, posterior, administrador, recarga.getIdRecarga().toString(), justificativa));
        operacaoRepository.save(new OperacaoIdempotente(
                administrador.getIdUsuario(), "ESTORNO_RECARGA", chaveIdempotencia, hash, 200, recarga.getIdRecarga().toString()));
        return recarga;
    }

    @Transactional
    public Venda estornarVenda(Long idVenda, String justificativa, String chaveIdempotencia) {
        Usuario administrador = usuarioAtual.get();
        if (chaveIdempotencia == null || chaveIdempotencia.isBlank()) throw new DominioException("E12", "Idempotency-Key obrigatoria.");
        if (justificativa == null || justificativa.isBlank()) throw new DominioException("E10", "Justificativa obrigatoria.");

        Venda venda = vendaRepository.findById(idVenda)
                .orElseThrow(() -> new DominioException("E16", "Venda nao encontrada."));
        if (venda.getStatus() == StatusVenda.ESTORNADA) throw new DominioException("E11", "Venda ja estornada.");

        String payload = "VENDA|" + idVenda + "|" + justificativa;
        String hash = HashUtil.sha256(payload);
        var existente = operacaoRepository.findByUsuarioIdAndTipoOperacaoAndChaveIdempotencia(
                administrador.getIdUsuario(), "ESTORNO_VENDA", chaveIdempotencia);
        if (existente.isPresent()) {
            if (!existente.get().getPayloadHash().equals(hash)) throw new DominioException("E12", "Chave idempotente reutilizada com conteudo diferente.");
            return venda;
        }

        Carteira carteira = carteiraRepository.findByIdForUpdate(venda.getCarteira().getIdCarteira())
                .orElseThrow(() -> new DominioException("E19", "Carteira nao encontrada."));
        BigDecimal anterior = carteira.getSaldoAtual();
        BigDecimal posterior = anterior.add(venda.getValorTotal());
        carteira.setSaldoAtual(posterior);

        venda.setStatus(StatusVenda.ESTORNADA);
        venda.setAutorizador(administrador);
        venda.setJustificativaEstorno(justificativa);
        vendaRepository.save(venda);
        movimentacaoRepository.save(new MovimentacaoFinanceira(
                carteira, venda.getCartao(), TipoMovimentacao.ESTORNO_VENDA, venda.getValorTotal(),
                anterior, posterior, administrador, venda.getIdVenda().toString(), justificativa));
        operacaoRepository.save(new OperacaoIdempotente(
                administrador.getIdUsuario(), "ESTORNO_VENDA", chaveIdempotencia, hash, 200, venda.getIdVenda().toString()));
        return venda;
    }
}
