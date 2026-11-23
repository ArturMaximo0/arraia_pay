package org.arraipay.arraiapay.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "cartao_qrcode")
public class CartaoQrCode {
    @Id
    @Column(name = "id_cartao")
    private Long idCartao;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_cartao", nullable = false)
    private Cartao cartao;

    @Lob
    @Column(name = "imagem_png", nullable = false, columnDefinition = "MEDIUMBLOB")
    private byte[] imagemPng;

    @Column(name = "data_geracao", nullable = false)
    private LocalDateTime dataGeracao;

    protected CartaoQrCode() {}

    public CartaoQrCode(Cartao cartao, byte[] imagemPng) {
        this.cartao = cartao;
        this.imagemPng = imagemPng;
        this.dataGeracao = LocalDateTime.now();
    }

    public Long getIdCartao() { return idCartao; }
    public byte[] getImagemPng() { return imagemPng; }
    public LocalDateTime getDataGeracao() { return dataGeracao; }
}
