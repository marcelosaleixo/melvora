# Correção definitiva do schema da FASE 25.12

O banco apresentado no log possuía apenas as colunas `retencao_ativa`, `retencao_dias_sem_retorno` e `retencao_mensagem`.
A V38 foi criada para garantir também `retencao_automatica_ativa`, `retencao_cooldown_dias`,
`retencao_horario_inicio`, `retencao_horario_fim` e `retencao_max_envios_dia`.

A V38 é idempotente e usa `ADD COLUMN IF NOT EXISTS`.

1. Pare a aplicação.
2. Use esta versão do projeto.
3. Execute `mvnw.cmd clean`.
4. Execute `mvnw.cmd spring-boot:run`.
5. Confirme no log a execução da migration V38.
6. Confirme no PostgreSQL que existem as 8 colunas com prefixo `retencao_`.

Não altere manualmente a tabela `flyway_schema_history` e não execute `DROP SCHEMA`.
