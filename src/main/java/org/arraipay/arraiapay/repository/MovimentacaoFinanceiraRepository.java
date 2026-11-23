package org.arraipay.arraiapay.repository;

import org.arraipay.arraiapay.domain.MovimentacaoFinanceira;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MovimentacaoFinanceiraRepository extends JpaRepository<MovimentacaoFinanceira, Long> {
    List<MovimentacaoFinanceira> findByCarteira_IdCarteiraOrderByDataHoraDesc(Long idCarteira);
}
