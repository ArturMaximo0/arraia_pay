package org.arraipay.arraiapay.repository;

import org.arraipay.arraiapay.domain.Recarga;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RecargaRepository extends JpaRepository<Recarga, Long> {
    Optional<Recarga> findByChaveUnica(String chaveUnica);
}
