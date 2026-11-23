package org.arraipay.arraiapay.repository;

import org.arraipay.arraiapay.domain.OperacaoIdempotente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OperacaoIdempotenteRepository extends JpaRepository<OperacaoIdempotente, Long> {
    Optional<OperacaoIdempotente> findByUsuarioIdAndTipoOperacaoAndChaveIdempotencia(
            Long usuarioId, String tipoOperacao, String chaveIdempotencia);
}
