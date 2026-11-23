package org.arraipay.arraiapay.application;

import org.arraipay.arraiapay.domain.Recarga;
import org.arraipay.arraiapay.domain.Venda;
import org.arraipay.arraiapay.repository.RecargaRepository;
import org.arraipay.arraiapay.repository.VendaRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class RelatorioService {
    private final RecargaRepository recargaRepository;
    private final VendaRepository vendaRepository;

    public RelatorioService(RecargaRepository recargaRepository, VendaRepository vendaRepository) {
        this.recargaRepository = recargaRepository;
        this.vendaRepository = vendaRepository;
    }

    public Map<String, Object> dashboard() {
        List<Recarga> recargas = recargaRepository.findAll();
        List<Venda> vendas = vendaRepository.findAll();

        BigDecimal totalRecargas = recargas.stream()
                .map(Recarga::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalVendas = vendas.stream()
                .filter(venda -> venda.getStatus().name().equals("CONCLUIDA"))
                .map(Venda::getValorTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("totalRecargas", totalRecargas);
        resultado.put("totalVendas", totalVendas);
        resultado.put("quantidadeRecargas", recargas.size());
        resultado.put("quantidadeVendas", vendas.size());
        return resultado;
    }

    public Map<String, Object> vendasPorBarraca() {
        Map<String, Object> resultado = new LinkedHashMap<>();
        Map<Long, BigDecimal> totais = new LinkedHashMap<>();
        for (Venda venda : vendaRepository.findAll()) {
            if (!venda.getStatus().name().equals("CONCLUIDA")) continue;
            totais.merge(venda.getBarraca().getIdBarraca(), venda.getValorTotal(), BigDecimal::add);
        }
        resultado.put("totaisPorBarraca", totais);
        return resultado;
    }
}
