# Modelo de domínio — ArraiPay

- `Cliente`: participante independente de cartão e usuário.
- `Carteira`: pertence ao cliente e mantém o saldo.
- `Cartao`: identifica o cliente e a carteira; não possui saldo próprio.
- `Usuario`: operador do sistema com perfil ADMINISTRADOR, CAIXA ou BARRACA.
- `Barraca`: unidade vinculada aos usuários BARRACA e aos produtos.
- `Produto`: cadastro sem controle de estoque nesta versão.
- `Recarga`: crédito na carteira.
- `Venda`: débito na carteira e referência aos itens vendidos.
- `ItemVenda`: quantidade e preço histórico praticado.
- `MovimentacaoFinanceira`: ledger auditável ligado à carteira.
- `OperacaoIdempotente`: proteção contra requisições financeiras duplicadas.

O cartão é opcional no ledger quando uma operação administrativa não utiliza cartão. Movimentações confirmadas não podem ser editadas ou excluídas.
