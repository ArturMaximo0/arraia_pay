package org.arraipay.arraiapay.application;

import org.arraipay.arraiapay.api.dto.Requests.ClienteRequest;
import org.arraipay.arraiapay.api.exception.DominioException;
import org.arraipay.arraiapay.domain.Carteira;
import org.arraipay.arraiapay.domain.Cliente;
import org.arraipay.arraiapay.repository.CarteiraRepository;
import org.arraipay.arraiapay.repository.ClienteRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClienteService {
    private final ClienteRepository clienteRepository;
    private final CarteiraRepository carteiraRepository;

    public ClienteService(ClienteRepository clienteRepository, CarteiraRepository carteiraRepository) {
        this.clienteRepository = clienteRepository;
        this.carteiraRepository = carteiraRepository;
    }

    @Transactional
    public Cliente criar(ClienteRequest request) {
        if (request.nome() == null || request.nome().isBlank()) throw new DominioException("E15", "Nome do cliente obrigatorio.");
        String cpf = validarEFormatarCpf(request.documento());
        String telefone = validarEFormatarTelefone(request.telefone());
        if (clienteRepository.existsByDocumento(cpf)) {
            throw new DominioException("E15", "Ja existe um cliente cadastrado com este CPF.");
        }
        Cliente cliente = new Cliente(request.nome().trim());
        cliente.setDocumento(cpf);
        cliente.setTelefone(telefone);
        cliente = clienteRepository.save(cliente);
        Carteira carteira = carteiraRepository.save(new Carteira(cliente));
        cliente.setCarteira(carteira);
        return cliente;
    }

    public Page<Cliente> listar(Boolean ativo, Pageable pageable) {
        if (ativo == null) return clienteRepository.findAll(pageable);
        return clienteRepository.findByAtivo(ativo, pageable);
    }

    public Cliente buscar(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new DominioException("E16", "Cliente nao encontrado."));
    }

    @Transactional
    public Cliente atualizar(Long id, ClienteRequest request) {
        Cliente cliente = buscar(id);
        if (request.nome() != null) cliente.setNome(request.nome());
        if (request.documento() != null) {
            String cpf = validarEFormatarCpf(request.documento());
            if (!cpf.equals(cliente.getDocumento()) && clienteRepository.existsByDocumento(cpf)) {
                throw new DominioException("E15", "Ja existe um cliente cadastrado com este CPF.");
            }
            cliente.setDocumento(cpf);
        }
        if (request.telefone() != null) cliente.setTelefone(validarEFormatarTelefone(request.telefone()));
        if (request.ativo() != null) cliente.setAtivo(request.ativo());
        return clienteRepository.save(cliente);
    }

    @Transactional
    public Cliente arquivar(Long id) {
        Cliente cliente = buscar(id);
        cliente.setAtivo(false);
        return clienteRepository.save(cliente);
    }

    private String apenasDigitos(String valor) {
        return valor == null ? "" : valor.replaceAll("\\D", "");
    }

    private String validarEFormatarCpf(String valor) {
        String cpf = apenasDigitos(valor);
        if (cpf.length() != 11 || cpf.matches("(\\d)\\1{10}") || !cpfValido(cpf)) {
            throw new DominioException("E15", "CPF invalido.");
        }
        return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "."
                + cpf.substring(6, 9) + "-" + cpf.substring(9);
    }

    private boolean cpfValido(String cpf) {
        int[] digitos = cpf.chars().map(Character::getNumericValue).toArray();
        int soma = 0;
        for (int i = 0; i < 9; i++) soma += digitos[i] * (10 - i);
        int primeiro = (soma * 10) % 11;
        if (primeiro == 10) primeiro = 0;
        if (primeiro != digitos[9]) return false;
        soma = 0;
        for (int i = 0; i < 10; i++) soma += digitos[i] * (11 - i);
        int segundo = (soma * 10) % 11;
        if (segundo == 10) segundo = 0;
        return segundo == digitos[10];
    }

    private String validarEFormatarTelefone(String valor) {
        String telefone = apenasDigitos(valor);
        if (telefone.length() != 10 && telefone.length() != 11) {
            throw new DominioException("E15", "Telefone invalido.");
        }
        if (telefone.length() == 10) {
            return "(" + telefone.substring(0, 2) + ") " + telefone.substring(2, 6)
                    + "-" + telefone.substring(6);
        }
        return "(" + telefone.substring(0, 2) + ") " + telefone.substring(2, 7)
                + "-" + telefone.substring(7);
    }
}
