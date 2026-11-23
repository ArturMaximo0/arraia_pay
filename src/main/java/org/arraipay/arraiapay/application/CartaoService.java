package org.arraipay.arraiapay.application;

import org.arraipay.arraiapay.api.exception.DominioException;
import org.arraipay.arraiapay.domain.Cartao;
import org.arraipay.arraiapay.domain.Cliente;
import org.arraipay.arraiapay.domain.enums.StatusCartao;
import org.arraipay.arraiapay.repository.CartaoRepository;
import org.arraipay.arraiapay.repository.ClienteRepository;
import org.arraipay.arraiapay.repository.MovimentacaoFinanceiraRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartaoService {
    private final CartaoRepository cartaoRepository;
    private final ClienteRepository clienteRepository;
    private final MovimentacaoFinanceiraRepository movimentacaoRepository;

    public CartaoService(CartaoRepository cartaoRepository, ClienteRepository clienteRepository,
                         MovimentacaoFinanceiraRepository movimentacaoRepository) {
        this.cartaoRepository = cartaoRepository;
        this.clienteRepository = clienteRepository;
        this.movimentacaoRepository = movimentacaoRepository;
    }

    @Transactional
    public Cartao emitir(Long idCliente) {
        Cliente cliente = clienteRepository.findById(idCliente)
                .orElseThrow(() -> new DominioException("E16", "Cliente nao encontrado."));
        if (!cliente.isAtivo()) throw new DominioException("E17", "Cliente inativo.");
        if (cartaoRepository.findAll().stream()
                .anyMatch(cartao -> cartao.getCliente().getIdCliente().equals(idCliente)
                        && cartao.getStatus() == StatusCartao.ATIVO)) {
            throw new DominioException("E20", "Este cliente ja possui um cartao ativo.");
        }
        return cartaoRepository.save(new Cartao(cliente));
    }

    public java.util.List<Cartao> listar() {
        return cartaoRepository.findAll();
    }

    public Cartao buscar(Long id) {
        return cartaoRepository.findById(id)
                .orElseThrow(() -> new DominioException("E01", "Cartao nao encontrado."));
    }

    public Cartao validar(String codigoQr) {
        Cartao cartao = cartaoRepository.findByCodigoQr(codigoQr)
                .orElseThrow(() -> new DominioException("E01", "Cartao nao encontrado."));
        if (cartao.getStatus() != StatusCartao.ATIVO) throw new DominioException("E02", "Cartao indisponivel.");
        if (!cartao.getCliente().isAtivo()) throw new DominioException("E03", "Cliente indisponivel.");
        return cartao;
    }

    @Transactional
    public Cartao alterarStatus(Long id, StatusCartao status, String justificativa) {
        Cartao cartao = buscar(id);
        if (status == null) throw new DominioException("E18", "Status obrigatorio.");
        cartao.setStatus(status);
        return cartaoRepository.save(cartao);
    }

    @Transactional
    public Cartao segundaVia(Long id) {
        Cartao anterior = buscar(id);
        anterior.setStatus(StatusCartao.CANCELADO);
        cartaoRepository.save(anterior);
        Cartao novo = new Cartao(anterior.getCliente());
        novo.setCartaoAnterior(anterior);
        return cartaoRepository.save(novo);
    }
    public java.util.List<org.arraipay.arraiapay.domain.MovimentacaoFinanceira> extrato(Long idCartao) {
        Cartao cartao = buscar(idCartao);
        if (cartao.getCliente().getCarteira() == null) return java.util.List.of();
        return movimentacaoRepository.findByCarteira_IdCarteiraOrderByDataHoraDesc(
                cartao.getCliente().getCarteira().getIdCarteira());
    }

}