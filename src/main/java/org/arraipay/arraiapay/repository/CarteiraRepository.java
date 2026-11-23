package org.arraipay.arraiapay.repository;

import org.arraipay.arraiapay.domain.Carteira;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.Optional;

public interface CarteiraRepository extends JpaRepository<Carteira, Long> {
    Optional<Carteira> findByCliente_IdCliente(Long idCliente);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Carteira c where c.idCarteira = :id")
    Optional<Carteira> findByIdForUpdate(@Param("id") Long id);
}
