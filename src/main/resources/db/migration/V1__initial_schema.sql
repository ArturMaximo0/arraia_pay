CREATE TABLE cliente (
  id_cliente BIGINT AUTO_INCREMENT PRIMARY KEY,
  nome VARCHAR(100) NOT NULL,
  documento VARCHAR(30),
  telefone VARCHAR(30),
  ativo BOOLEAN NOT NULL DEFAULT TRUE,
  data_cadastro DATETIME NOT NULL
) ENGINE=InnoDB;

CREATE TABLE barraca (
  id_barraca BIGINT AUTO_INCREMENT PRIMARY KEY,
  nome VARCHAR(100) NOT NULL UNIQUE,
  responsavel VARCHAR(100) NOT NULL,
  ativa BOOLEAN NOT NULL DEFAULT TRUE
) ENGINE=InnoDB;

CREATE TABLE usuarios (
  id_usuario BIGINT AUTO_INCREMENT PRIMARY KEY,
  nome VARCHAR(100) NOT NULL,
  login VARCHAR(100) NOT NULL UNIQUE,
  senha_hash VARCHAR(255) NOT NULL,
  perfil VARCHAR(20) NOT NULL,
  id_barraca BIGINT,
  ativo BOOLEAN NOT NULL DEFAULT TRUE,
  CONSTRAINT usuarios_barraca_fk FOREIGN KEY (id_barraca) REFERENCES barraca(id_barraca)
) ENGINE=InnoDB;

CREATE TABLE carteira (
  id_carteira BIGINT AUTO_INCREMENT PRIMARY KEY,
  id_cliente BIGINT NOT NULL UNIQUE,
  saldo_atual DECIMAL(10,2) NOT NULL DEFAULT 0,
  ativa BOOLEAN NOT NULL DEFAULT TRUE,
  versao BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT carteira_saldo_chk CHECK (saldo_atual >= 0),
  CONSTRAINT carteira_cliente_fk FOREIGN KEY (id_cliente) REFERENCES cliente(id_cliente)
) ENGINE=InnoDB;

CREATE TABLE cartao (
  id_cartao BIGINT AUTO_INCREMENT PRIMARY KEY,
  codigo_qr VARCHAR(50) NOT NULL UNIQUE,
  id_cliente BIGINT NOT NULL,
  status VARCHAR(20) NOT NULL,
  id_cartao_anterior BIGINT,
  data_criacao DATETIME NOT NULL,
  versao BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT cartao_cliente_fk FOREIGN KEY (id_cliente) REFERENCES cliente(id_cliente),
  CONSTRAINT cartao_anterior_fk FOREIGN KEY (id_cartao_anterior) REFERENCES cartao(id_cartao)
) ENGINE=InnoDB;

CREATE TABLE produto (
  id_produto BIGINT AUTO_INCREMENT PRIMARY KEY,
  id_barraca BIGINT NOT NULL,
  nome VARCHAR(100) NOT NULL,
  categoria VARCHAR(20),
  descricao VARCHAR(500),
  preco DECIMAL(10,2) NOT NULL,
  ativo BOOLEAN NOT NULL DEFAULT TRUE,
  CONSTRAINT produto_preco_chk CHECK (preco > 0),
  CONSTRAINT produto_barraca_fk FOREIGN KEY (id_barraca) REFERENCES barraca(id_barraca)
) ENGINE=InnoDB;

CREATE TABLE recarga (
  id_recarga BIGINT AUTO_INCREMENT PRIMARY KEY,
  id_cartao BIGINT NOT NULL,
  id_carteira BIGINT NOT NULL,
  valor DECIMAL(10,2) NOT NULL,
  forma_pagamento VARCHAR(20) NOT NULL,
  id_operador BIGINT NOT NULL,
  data_hora DATETIME NOT NULL,
  status VARCHAR(20) NOT NULL,
  chave_unica VARCHAR(100) NOT NULL UNIQUE,
  id_autorizador BIGINT,
  justificativa_estorno VARCHAR(500),
  CONSTRAINT recarga_valor_chk CHECK (valor > 0),
  CONSTRAINT recarga_cartao_fk FOREIGN KEY (id_cartao) REFERENCES cartao(id_cartao),
  CONSTRAINT recarga_carteira_fk FOREIGN KEY (id_carteira) REFERENCES carteira(id_carteira),
  CONSTRAINT recarga_operador_fk FOREIGN KEY (id_operador) REFERENCES usuarios(id_usuario)
) ENGINE=InnoDB;

CREATE TABLE venda (
  id_venda BIGINT AUTO_INCREMENT PRIMARY KEY,
  id_cartao BIGINT NOT NULL,
  id_carteira BIGINT NOT NULL,
  id_barraca BIGINT NOT NULL,
  id_operador BIGINT NOT NULL,
  valor_total DECIMAL(10,2) NOT NULL,
  data_hora DATETIME NOT NULL,
  status VARCHAR(20) NOT NULL,
  chave_unica VARCHAR(100) NOT NULL UNIQUE,
  id_autorizador BIGINT,
  justificativa_estorno VARCHAR(500),
  CONSTRAINT venda_valor_chk CHECK (valor_total > 0),
  CONSTRAINT venda_cartao_fk FOREIGN KEY (id_cartao) REFERENCES cartao(id_cartao),
  CONSTRAINT venda_carteira_fk FOREIGN KEY (id_carteira) REFERENCES carteira(id_carteira),
  CONSTRAINT venda_barraca_fk FOREIGN KEY (id_barraca) REFERENCES barraca(id_barraca),
  CONSTRAINT venda_operador_fk FOREIGN KEY (id_operador) REFERENCES usuarios(id_usuario)
) ENGINE=InnoDB;

CREATE TABLE item_venda (
  id_item BIGINT AUTO_INCREMENT PRIMARY KEY,
  id_venda BIGINT NOT NULL,
  id_produto BIGINT NOT NULL,
  quantidade INT NOT NULL,
  preco_unitario DECIMAL(10,2) NOT NULL,
  subtotal DECIMAL(10,2) NOT NULL,
  CONSTRAINT item_quantidade_chk CHECK (quantidade > 0),
  CONSTRAINT item_venda_fk FOREIGN KEY (id_venda) REFERENCES venda(id_venda),
  CONSTRAINT item_produto_fk FOREIGN KEY (id_produto) REFERENCES produto(id_produto)
) ENGINE=InnoDB;

CREATE TABLE movimentacao_financeira (
  id_movimentacao BIGINT AUTO_INCREMENT PRIMARY KEY,
  id_carteira BIGINT NOT NULL,
  id_cartao BIGINT,
  id_cliente BIGINT NOT NULL,
  tipo VARCHAR(30) NOT NULL,
  valor DECIMAL(10,2) NOT NULL,
  saldo_anterior DECIMAL(10,2) NOT NULL,
  saldo_posterior DECIMAL(10,2) NOT NULL,
  id_usuario BIGINT NOT NULL,
  id_operacao VARCHAR(100),
  justificativa VARCHAR(500),
  data_hora DATETIME NOT NULL,
  CONSTRAINT movimentacao_valor_chk CHECK (valor > 0),
  CONSTRAINT movimentacao_carteira_fk FOREIGN KEY (id_carteira) REFERENCES carteira(id_carteira),
  CONSTRAINT movimentacao_cartao_fk FOREIGN KEY (id_cartao) REFERENCES cartao(id_cartao),
  CONSTRAINT movimentacao_cliente_fk FOREIGN KEY (id_cliente) REFERENCES cliente(id_cliente),
  CONSTRAINT movimentacao_usuario_fk FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario)
) ENGINE=InnoDB;

CREATE TABLE operacao_idempotente (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  usuario_id BIGINT NOT NULL,
  tipo_operacao VARCHAR(30) NOT NULL,
  chave_idempotencia VARCHAR(100) NOT NULL,
  payload_hash VARCHAR(64) NOT NULL,
  resultado_status INT NOT NULL,
  resultado_json LONGTEXT,
  CONSTRAINT operacao_idempotente_unq UNIQUE (usuario_id, tipo_operacao, chave_idempotencia)
) ENGINE=InnoDB;


ALTER TABLE usuarios
  ADD CONSTRAINT usuarios_perfil_chk CHECK (perfil IN ('administrador','caixa','barraca')),
  ADD CONSTRAINT usuarios_barraca_regra_chk CHECK (
    (perfil = 'barraca' AND id_barraca IS NOT NULL)
    OR (perfil IN ('administrador','caixa') AND id_barraca IS NULL)
  );

ALTER TABLE cartao
  ADD CONSTRAINT cartao_status_chk CHECK (status IN ('ativo','bloqueado','cancelado'));

ALTER TABLE recarga
  ADD CONSTRAINT recarga_status_chk CHECK (status IN ('efetivada','estornada'));

ALTER TABLE venda
  ADD CONSTRAINT venda_status_chk CHECK (status IN ('concluida','estornada'));

ALTER TABLE movimentacao_financeira
  ADD CONSTRAINT movimentacao_tipo_chk CHECK (tipo IN ('recarga','venda','estorno_recarga','estorno_venda'));

CREATE INDEX idx_carteira_cliente ON carteira(id_cliente);
CREATE INDEX idx_cartao_cliente_status ON cartao(id_cliente, status);
CREATE INDEX idx_movimentacao_carteira_data ON movimentacao_financeira(id_carteira, data_hora);
CREATE INDEX idx_movimentacao_tipo_data ON movimentacao_financeira(tipo, data_hora);
CREATE INDEX idx_recarga_cartao_data ON recarga(id_cartao, data_hora);
CREATE INDEX idx_venda_barraca_data ON venda(id_barraca, data_hora);
CREATE INDEX idx_venda_cartao_data ON venda(id_cartao, data_hora);
