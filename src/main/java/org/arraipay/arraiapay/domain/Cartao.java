package org.arraipay.arraiapay.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Convert;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.arraipay.arraiapay.domain.enums.StatusCartao;
import org.arraipay.arraiapay.domain.enums.StatusCartaoConverter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "cartao")
public class Cartao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cartao")
    private Long idCartao;

    @Column(name = "codigo_qr", nullable = false, unique = true, length = 50)
    private String codigoQr;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_cliente", nullable = false)
    private Cliente cliente;

    @Convert(converter = StatusCartaoConverter.class)
    @Column(nullable = false, length = 20)
    private StatusCartao status = StatusCartao.ATIVO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cartao_anterior")
    private Cartao cartaoAnterior;

    @Column(name = "data_criacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    @Version
    private long versao;

    protected Cartao() { }

    public Cartao(Cliente cliente) {
        this.cliente = cliente;
    }

    @PrePersist
    void prepararCadastro() {
        if (codigoQr == null || codigoQr.isBlank()) codigoQr = UUID.randomUUID().toString();
        if (dataCriacao == null) dataCriacao = LocalDateTime.now();
        if (status == null) status = StatusCartao.ATIVO;
    }

    public Long getIdCartao() { return idCartao; }
    public String getCodigoQr() { return codigoQr; }
    public void setCodigoQr(String codigoQr) { this.codigoQr = codigoQr; }
    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }
    public StatusCartao getStatus() { return status; }
    public void setStatus(StatusCartao status) { this.status = status; }
    public Cartao getCartaoAnterior() { return cartaoAnterior; }
    public void setCartaoAnterior(Cartao cartaoAnterior) { this.cartaoAnterior = cartaoAnterior; }
    public LocalDateTime getDataCriacao() { return dataCriacao; }
    public long getVersao() { return versao; }
}
