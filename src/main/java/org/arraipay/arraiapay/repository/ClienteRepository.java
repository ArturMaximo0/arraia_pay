package org.arraipay.arraiapay.repository;

import org.arraipay.arraiapay.domain.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    boolean existsByDocumento(String documento);
    org.springframework.data.domain.Page<Cliente> findByAtivo(boolean ativo,
                                                                org.springframework.data.domain.Pageable pageable);
}
