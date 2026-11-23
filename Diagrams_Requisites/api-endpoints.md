# API REST — ArraiPay

Base: `/api/v1`

## Autenticação

- `POST /auth/login`
- `GET /auth/me`

## Perfis

- `ADMINISTRADOR`
- `CAIXA`
- `BARRACA`

## Recursos principais

- `/clientes`
- `/usuarios`
- `/barracas`
- `/cartoes`
- `/produtos`
- `/recargas`
- `/vendas`
- `/relatorios`

Toda operação financeira exige `Idempotency-Key` em cabeçalho.

O caixa usa recargas. A barraca usa vendas. Somente o administrador pode executar estornos.
O sistema não controla estoque nesta versão.
