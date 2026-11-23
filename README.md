# ArraiPay

Aplicação web para créditos digitais em festas escolares.

## Tecnologias

- Java 21
- Spring Boot
- Spring Data JPA
- Spring Security
- Flyway
- MySQL 8.0 / MySQL Connector/J
- React + Vite + PWA

## Base de dados

Para muitos acessos simultâneos, use o banco persistente MySQL 8.0:

```bash
cd /home/artur/copilot-worktrees/arraia_pay/arturmaximo0-fluffy-adventure
docker-compose up -d mysql
```

O volume `arraiapay_mysql` mantém clientes, cartões, recargas e vendas entre reinícios.

O schema oficial é criado por `src/main/resources/db/migration/V1__initial_schema.sql`.
No banco deste projeto, a configuração de baseline considera que schemas não vazios já estão na versão 3 e aplica as migrations V4 e V5 para armazenar imagens de QR Code. Bancos novos continuam executando todas as migrations.

Configure a conexão por variáveis de ambiente:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`

Configuração padrão:

```yaml
db: jdbc:mysql://localhost:3306/arraiapay
usuario: arraiapay
senha: arraiapay
```

## Logins iniciais

| Perfil | Login | Senha |
|---|---|---|
| Administrador | `admin` | `admin123` |
| Caixa | `caixa` | `caixa123` |
| Barraca | `barraca` | `barraca123` |

Altere estas senhas antes de usar em produção.


```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 \
sh mvnw spring-boot:run -Dspring-boot.run.profiles=dev-h2
```

Ele inicia o banco H2 em memória e cria automaticamente os usuários da tabela acima. Não existe um login chamado `dev-h2`; use `admin`, `caixa` ou `barraca` no frontend em `http://localhost:5173`.

## Executar backend

Desenvolvimento imediato com H2:

```bash
cd /home/artur/copilot-worktrees/arraia_pay/arturmaximo0-fluffy-adventure
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 sh mvnw spring-boot:run -Dspring-boot.run.profiles=dev-h2
```

Execução com MySQL 8.0:

```bash
cd /home/artur/copilot-worktrees/arraia_pay/arturmaximo0-fluffy-adventure
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 sh mvnw spring-boot:run
```

## Executar frontend

```bash
cd frontend
npm install
npm run dev
```

Abra `https://localhost:5173` ou o endereço HTTPS mostrado pelo Vite na rede local. O certificado local pode pedir confirmação no navegador na primeira vez.

Use sempre esse endereco para entrar e testar os tres perfis. O frontend encaminha `/api` para `http://localhost:8080`; entrar diretamente no backend cria uma sessao separada no navegador.

Cada guia aberta no navegador pode manter um perfil diferente. Os logins ficam isolados por guia; trocar o perfil em uma guia não altera as demais.

No cadastro de cliente, o CPF e o telefone são obrigatórios. Os campos aceitam apenas números digitados e exibem automaticamente a pontuação padrão brasileira. CPF inválido e telefone com menos de 10 ou mais de 11 dígitos não podem ser cadastrados.

### Testar a camera pelo celular

1. Mantenha computador e celular na mesma rede.
2. Execute o frontend com `npm run dev`.
3. No celular, abra o endereço HTTPS `https://IP_DO_COMPUTADOR:5173`.
4. Aceite o certificado local do navegador.
5. Permita o acesso à câmera quando Caixa ou Barraca pedir.
6. Use o botão `Ler QR`; o escaneamento preenche e valida o cartão automaticamente.

### QR Codes dos cartões

Na area **Cartoes para teste** do painel Administrador, clique em **Ver QR** ao lado do codigo. O primeiro acesso gera a imagem PNG pela API QRGen; ela fica armazenada no banco vinculada ao cartao e os proximos acessos reutilizam a imagem persistida. Cada cartao tem seu proprio codigo e QR, inclusive quando um cliente recebe uma segunda via.

Para testar a camera com o QR de um cartao, abra o QR no painel e leia-o no perfil Caixa ou Barraca. A imagem codifica o codigo do cartao mostrado na mesma tela.

No painel **Clientes e saldos**, **Excluir** arquiva o cliente e bloqueia novas operacoes, preservando saldo, cartoes e dados financeiros para consulta. As operacoes de clientes arquivados aparecem identificadas como **Cliente excluido**. Em **Visao geral**, o botao **Limpar historico de todos** remove recargas, vendas, itens e movimentacoes; exige a senha atual do administrador e preserva os cadastros e saldos.

No perfil `dev-h2`, sao criados uma barraca e um cliente com cartao de demonstracao. Entre como `admin` para ver o codigo QR do cartao. No caixa, valide o codigo e faca uma recarga; depois, entre como `barraca`, informe o valor total da compra e conclua a venda. O painel permite atualizar os totais e a atividade.

O H2 de desenvolvimento e em memoria: os dados de teste sao recriados quando o backend reinicia. A sessao de login, por outro lado, continua ativa ao atualizar a pagina, enquanto o backend permanecer rodando.

## Produção

```bash
cd frontend
npm run build
```

O build será colocado em `src/main/resources/static` e servido pelo Spring Boot na mesma origem.
