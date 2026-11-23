package org.arraipay.arraiapay.application;

import org.arraipay.arraiapay.api.exception.DominioException;
import org.arraipay.arraiapay.domain.enums.PerfilUsuario;
import org.arraipay.arraiapay.repository.ItemVendaRepository;
import org.arraipay.arraiapay.repository.MovimentacaoFinanceiraRepository;
import org.arraipay.arraiapay.repository.RecargaRepository;
import org.arraipay.arraiapay.repository.VendaRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HistoricoService {
    private final UsuarioAtual usuarioAtual;
    private final PasswordEncoder passwordEncoder;
    private final MovimentacaoFinanceiraRepository movimentacaoRepository;
    private final ItemVendaRepository itemVendaRepository;
    private final RecargaRepository recargaRepository;
    private final VendaRepository vendaRepository;

    public HistoricoService(UsuarioAtual usuarioAtual, PasswordEncoder passwordEncoder,
                            MovimentacaoFinanceiraRepository movimentacaoRepository,
                            ItemVendaRepository itemVendaRepository,
                            RecargaRepository recargaRepository,
                            VendaRepository vendaRepository) {
        this.usuarioAtual = usuarioAtual;
        this.passwordEncoder = passwordEncoder;
        this.movimentacaoRepository = movimentacaoRepository;
        this.itemVendaRepository = itemVendaRepository;
        this.recargaRepository = recargaRepository;
        this.vendaRepository = vendaRepository;
    }

    @Transactional
    public void limpar(String senhaAdmin) {
        var administrador = usuarioAtual.get();
        if (!administrador.isAtivo() || administrador.getPerfil() != PerfilUsuario.ADMINISTRADOR
                || senhaAdmin == null || !passwordEncoder.matches(senhaAdmin, administrador.getSenhaHash())) {
            throw new DominioException("E23", "Senha de administrador invalida.");
        }

        movimentacaoRepository.deleteAllInBatch();
        itemVendaRepository.deleteAllInBatch();
        recargaRepository.deleteAllInBatch();
        vendaRepository.deleteAllInBatch();
    }
}
