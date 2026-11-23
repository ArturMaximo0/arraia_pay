CREATE TABLE IF NOT EXISTS cartao_qrcode (
    id_cartao BIGINT NOT NULL PRIMARY KEY,
    imagem_png MEDIUMBLOB NOT NULL,
    data_geracao DATETIME NOT NULL,
    CONSTRAINT fk_cartao_qrcode_cartao
        FOREIGN KEY (id_cartao) REFERENCES cartao(id_cartao)
);
