package org.arraipay.arraiapay.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "operacao_idempotente",
       uniqueConstraints = @UniqueConstraint(columnNames = {"usuario_id", "tipo_operacao", "chave_idempotencia"}))
public class OperacaoIdempotente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "tipo_operacao", nullable = false, length = 30)
    private String tipoOperacao;

    @Column(name = "chave_idempotencia", nullable = false, length = 100)
    private String chaveIdempotencia;

    @Column(name = "payload_hash", nullable = false, length = 64)
    private String payloadHash;

    @Column(name = "resultado_status", nullable = false)
    private int resultadoStatus;

    @Column(name = "resultado_json", columnDefinition = "LONGTEXT")
    private String resultadoJson;

    protected OperacaoIdempotente() {}

    public OperacaoIdempotente(Long usuarioId, String tipoOperacao, String chaveIdempotencia,
                               String payloadHash, int resultadoStatus, String resultadoJson) {
        this.usuarioId = usuarioId;
        this.tipoOperacao = tipoOperacao;
        this.chaveIdempotencia = chaveIdempotencia;
        this.payloadHash = payloadHash;
        this.resultadoStatus = resultadoStatus;
        this.resultadoJson = resultadoJson;
    }

    public Long getId() { return id; }
    public Long getUsuarioId() { return usuarioId; }
    public String getTipoOperacao() { return tipoOperacao; }
    public String getChaveIdempotencia() { return chaveIdempotencia; }
    public String getPayloadHash() { return payloadHash; }
    public int getResultadoStatus() { return resultadoStatus; }
    public String getResultadoJson() { return resultadoJson; }
}
