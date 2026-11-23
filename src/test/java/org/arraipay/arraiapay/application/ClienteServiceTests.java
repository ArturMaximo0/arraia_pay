package org.arraipay.arraiapay.application;

import org.arraipay.arraiapay.domain.Cliente;
import org.arraipay.arraiapay.repository.CarteiraRepository;
import org.arraipay.arraiapay.repository.ClienteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTests {
    @Mock private ClienteRepository clienteRepository;
    @Mock private CarteiraRepository carteiraRepository;

    @InjectMocks private ClienteService clienteService;

    @Test
    void archivesClientWithoutDeletingItsRecord() {
        Cliente cliente = new Cliente("Cliente");
        when(clienteRepository.findById(42L)).thenReturn(Optional.of(cliente));
        when(clienteRepository.save(cliente)).thenReturn(cliente);

        Cliente arquivado = clienteService.arquivar(42L);

        assertFalse(arquivado.isAtivo());
        verify(clienteRepository).save(cliente);
    }
}
