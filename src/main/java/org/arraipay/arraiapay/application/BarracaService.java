package org.arraipay.arraiapay.application;

import org.arraipay.arraiapay.api.exception.DominioException;
import org.arraipay.arraiapay.domain.Barraca;
import org.arraipay.arraiapay.repository.BarracaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BarracaService {
    private final BarracaRepository barracaRepository;

    public BarracaService(BarracaRepository barracaRepository) { this.barracaRepository = barracaRepository; }

    @Transactional
    public Barraca criar(String nome, String responsavel, Boolean ativa) {
        if (nome == null || nome.isBlank()) throw new DominioException("E20", "Nome da barraca obrigatorio.");
        if (barracaRepository.existsByNomeIgnoreCase(nome.trim())) throw new DominioException("E20", "Barraca ja cadastrada.");
        Barraca barraca = new Barraca(nome.trim(), responsavel);
        if (ativa != null) barraca.setAtiva(ativa);
        return barracaRepository.save(barraca);
    }

    public Barraca buscar(Long id) {
        return barracaRepository.findById(id).orElseThrow(() -> new DominioException("E08", "Barraca nao encontrada."));
    }

    public List<Barraca> listar(Boolean ativa) {
        return barracaRepository.findAll().stream()
                .filter(b -> ativa == null || b.isAtiva() == ativa)
                .toList();
    }

    @Transactional
    public Barraca atualizar(Long id, String nome, String responsavel, Boolean ativa) {
        Barraca barraca = buscar(id);
        if (nome != null) barraca.setNome(nome);
        if (responsavel != null) barraca.setResponsavel(responsavel);
        if (ativa != null) barraca.setAtiva(ativa);
        return barracaRepository.save(barraca);
    }
}
