package org.arraipay.arraiapay.application;

import org.arraipay.arraiapay.api.exception.DominioException;
import org.arraipay.arraiapay.domain.Usuario;
import org.arraipay.arraiapay.domain.enums.PerfilUsuario;
import org.arraipay.arraiapay.repository.ItemVendaRepository;
import org.arraipay.arraiapay.repository.MovimentacaoFinanceiraRepository;
import org.arraipay.arraiapay.repository.RecargaRepository;
import org.arraipay.arraiapay.repository.VendaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HistoricoServiceTests {
    @Mock private UsuarioAtual usuarioAtual;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private MovimentacaoFinanceiraRepository movimentacaoRepository;
    @Mock private ItemVendaRepository itemVendaRepository;
    @Mock private RecargaRepository recargaRepository;
    @Mock private VendaRepository vendaRepository;

    @InjectMocks private HistoricoService historicoService;

    @Test
    void clearsAllOperationHistoryOnlyAfterAdminPasswordMatches() {
        Usuario admin = new Usuario("Admin", "admin", "encoded", PerfilUsuario.ADMINISTRADOR);
        when(usuarioAtual.get()).thenReturn(admin);
        when(passwordEncoder.matches("correct-password", "encoded")).thenReturn(true);

        historicoService.limpar("correct-password");

        verify(movimentacaoRepository).deleteAllInBatch();
        verify(itemVendaRepository).deleteAllInBatch();
        verify(recargaRepository).deleteAllInBatch();
        verify(vendaRepository).deleteAllInBatch();
    }

    @Test
    void preservesHistoryWhenAdminPasswordDoesNotMatch() {
        Usuario admin = new Usuario("Admin", "admin", "encoded", PerfilUsuario.ADMINISTRADOR);
        when(usuarioAtual.get()).thenReturn(admin);
        when(passwordEncoder.matches("wrong-password", "encoded")).thenReturn(false);

        assertThrows(DominioException.class, () -> historicoService.limpar("wrong-password"));

        verify(movimentacaoRepository, never()).deleteAllInBatch();
        verify(itemVendaRepository, never()).deleteAllInBatch();
        verify(recargaRepository, never()).deleteAllInBatch();
        verify(vendaRepository, never()).deleteAllInBatch();
    }
}
