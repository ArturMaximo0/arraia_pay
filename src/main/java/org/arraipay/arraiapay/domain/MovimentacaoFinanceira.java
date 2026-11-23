package org.arraipay.arraiapay.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.arraipay.arraiapay.domain.enums.TipoMovimentacao;
import org.arraipay.arraiapay.domain.enums.TipoMovimentacaoConverter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "movimentacao_financeira")
public class MovimentacaoFinanceira {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_movimentacao")
    private Long idMovimentacao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_carteira", nullable = false)
    private Carteira carteira;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cartao")
    private Cartao cartao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_cliente", nullable = false)
    private Cliente cliente;

    @Convert(converter = TipoMovimentacaoConverter.class)
    @Column(name = "tipo", nullable = false, length = 30)
    private TipoMovimentacao tipo;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @Column(name = "saldo_anterior", nullable = false, precision = 10, scale = 2)
    private BigDecimal saldoAnterior;

    @Column(name = "saldo_posterior", nullable = false, precision = 10, scale = 2)
    private BigDecimal saldoPosterior;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuarioResponsavel;

    @Column(name = "id_operacao", length = 100)
    private String idOperacao;

    @Column(length = 500)
    private String justificativa;

    @Column(name = "data_hora", nullable = false, updatable = false)
    private LocalDateTime dataHora;

    protected MovimentacaoFinanceira() {}

    @PrePersist
    void preparar() { if (dataHora == null) dataHora = LocalDateTime.now(); }

    public MovimentacaoFinanceira(Carteira carteira, Cartao cartao, TipoMovimentacao tipo,
                                  BigDecimal valor, BigDecimal saldoAnterior, BigDecimal saldoPosterior,
                                  Usuario usuarioResponsavel, String idOperacao, String justificativa) {
        this.carteira = carteira;
        this.cartao = cartao;
        this.cliente = carteira.getCliente();
        this.tipo = tipo;
        this.valor = valor;
        this.saldoAnterior = saldoAnterior;
        this.saldoPosterior = saldoPosterior;
        this.usuarioResponsavel = usuarioResponsavel;
        this.idOperacao = idOperacao;
        this.justificativa = justificativa;
    }

    public Long getIdMovimentacao() { return idMovimentacao; }
    public Carteira getCarteira() { return carteira; }
    public Cartao getCartao() { return cartao; }
    public Cliente getCliente() { return cliente; }
    public TipoMovimentacao getTipo() { return tipo; }
    public BigDecimal getValor() { return valor; }
    public BigDecimal getSaldoAnterior() { return saldoAnterior; }
    public BigDecimal getSaldoPosterior() { return saldoPosterior; }
    public Usuario getUsuarioResponsavel() { return usuarioResponsavel; }
    public String getIdOperacao() { return idOperacao; }
    public String getJustificativa() { return justificativa; }
    public LocalDateTime getDataHora() { return dataHora; }
}
