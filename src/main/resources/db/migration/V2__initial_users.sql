INSERT INTO barraca (nome, responsavel, ativa)
SELECT 'Barraca Central', 'Responsavel da Barraca', TRUE
WHERE NOT EXISTS (SELECT 1 FROM barraca WHERE nome = 'Barraca Central');

INSERT INTO usuarios (nome, login, senha_hash, perfil, id_barraca, ativo)
SELECT 'Administrador', 'admin', '$2a$10$XyJIp0CsZrvoULYqG4fyjeDLaT16Hs627EzDw8bfdhC3lCHs0nyd6', 'administrador', NULL, TRUE
WHERE NOT EXISTS (SELECT 1 FROM usuarios WHERE login = 'admin');

INSERT INTO usuarios (nome, login, senha_hash, perfil, id_barraca, ativo)
SELECT 'Caixa', 'caixa', '$2a$10$ddD4BMA3j.q0Rs8lQz6TnubwvCdD5mi.XdXv8Py2BtYY5buslMaKi', 'caixa', NULL, TRUE
WHERE NOT EXISTS (SELECT 1 FROM usuarios WHERE login = 'caixa');

INSERT INTO usuarios (nome, login, senha_hash, perfil, id_barraca, ativo)
SELECT 'Operador da Barraca', 'barraca', '$2a$10$Cvu7UtG7pci2hzp7A3kAlenfd5uWyX16fAg6NpKq3.YSxZ0ed1Doa', 'barraca',
       (SELECT id_barraca FROM barraca WHERE nome = 'Barraca Central'), TRUE
WHERE NOT EXISTS (SELECT 1 FROM usuarios WHERE login = 'barraca');
