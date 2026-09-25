# Flyway + Hibernate Validate — política de schema do Melvora

## Regra definitiva

O **Flyway é a única ferramenta autorizada a criar ou alterar tabelas, colunas,
constraints e índices**.

O Hibernate fica em:

```properties
spring.jpa.hibernate.ddl-auto=validate
```

O `validate` não cria nada. Ele somente verifica se o schema físico é compatível
com as entidades JPA antes de a aplicação iniciar.

## Por que manter `validate`

Em um SaaS multitenant, permitir que o Hibernate execute `update` pode produzir
alterações estruturais fora do histórico do Flyway. Isso dificulta auditoria,
rollback e diagnóstico entre desenvolvimento, homologação e produção.

## Procedimento para uma nova funcionalidade

1. Criar/alterar a entidade Java.
2. Criar uma nova migration Flyway com a próxima versão.
3. Executar a migration em banco de desenvolvimento.
4. Confirmar que o Flyway registrou a versão em `flyway_schema_history`.
5. Iniciar a aplicação com `ddl-auto=validate`.
6. Somente depois publicar em produção.

## Banco legado com migration marcada, mas objeto ausente

Nunca editar `flyway_schema_history` manualmente.

Criar uma **migration de reparo idempotente**, como a V40, usando:

```sql
CREATE TABLE IF NOT EXISTS ...
ALTER TABLE ... ADD COLUMN IF NOT EXISTS ...
CREATE INDEX IF NOT EXISTS ...
```

somente quando a operação for segura para dados existentes.

## O que não fazer

```properties
spring.jpa.hibernate.ddl-auto=update
```

em produção.

Também não executar `DROP SCHEMA`, `flyway clean` ou apagar registros de
`flyway_schema_history` para contornar um erro de validação.
