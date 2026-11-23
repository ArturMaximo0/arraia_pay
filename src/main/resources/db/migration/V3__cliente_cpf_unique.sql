ALTER TABLE cliente
  MODIFY documento VARCHAR(30) NOT NULL,
  ADD CONSTRAINT uk_cliente_documento UNIQUE (documento);
