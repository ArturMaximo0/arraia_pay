package org.arraipay.arraiapay.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;

@Entity
@Table(name = "carteira")
public class Carteira {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_carteira")
    private Long idCarteira;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_cliente", nullable = false, unique = true)
    private Cliente cliente;

    @Column(name = "saldo_atual", nullable = false, precision = 10, scale = 2)
    private BigDecimal saldoAtual = BigDecimal.ZERO;

    @Column(nullable = false)
    private boolean ativa = true;

    @Version
    private long versao;

    protected Carteira() {}

    public Carteira(Cliente cliente) { this.cliente = cliente; }

    public Long getIdCarteira() { return idCarteira; }
    public Cliente getCliente() { return cliente; }
    public BigDecimal getSaldoAtual() { return saldoAtual; }
    public void setSaldoAtual(BigDecimal saldoAtual) { this.saldoAtual = saldoAtual; }
    public boolean isAtiva() { return ativa; }
    public void setAtiva(boolean ativa) { this.ativa = ativa; }
    public long getVersao() { return versao; }
}
