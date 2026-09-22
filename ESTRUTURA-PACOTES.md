# Melvora — Arquitetura de pacotes

O projeto utiliza organização por responsabilidade técnica:

```text
com.marceloaleixo.melvora
├── config
├── controller
├── dto
├── entity
│   └── enums
├── exception
├── repository
├── security
├── service
└── tenant
```

## Responsabilidades

- `entity`: entidades JPA persistidas no PostgreSQL.
- `entity.enums`: enums do domínio.
- `repository`: acesso ao banco via Spring Data JPA.
- `service`: regras de negócio e transações.
- `controller`: endpoints HTTP e páginas web.
- `dto`: objetos de entrada/saída da API.
- `security`: autenticação, autorização e filtro de segurança.
- `tenant`: contexto da empresa autenticada.
- `config`: configurações e bootstrap da aplicação.
- `exception`: exceções de negócio e tratamento global.

## Regra de multitenancy

Entidades de negócio possuem `empresa_id` e os repositories recebem o `empresaId` obtido do `TenantContext`.

O frontend nunca informa qual tenant deve ser usado.

## Mega Hair

O fluxo implementado é:

```text
Produto Mega Hair
       ↓
Lote
       ↓
Estoque
       ↓
Aplicação na cliente
       ↓
Manutenção
       ↓
Histórico
```

A aplicação consome o estoque dentro da mesma transação e usa bloqueio pessimista no lote para evitar consumo concorrente do mesmo saldo.

## Banco local

```text
Host: localhost
Porta: 5432
Banco: melvora
Usuário: postgres
```

Crie o banco:

```sql
CREATE DATABASE melvora;
```

As migrations são executadas pelo Flyway.

## Bootstrap do administrador

PowerShell:

```powershell
$env:MELVORA_BOOTSTRAP_ADMIN_EMAIL="admin@melvora.local"
$env:MELVORA_BOOTSTRAP_ADMIN_PASSWORD="TroqueEstaSenha#2026"
$env:MELVORA_BOOTSTRAP_COMPANY_NAME="Meu Salão"
.\mvnw.cmd spring-boot:run
```

A senha de bootstrap deve possuir pelo menos 12 caracteres.
