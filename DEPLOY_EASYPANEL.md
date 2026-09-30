# Deploy Melvora no EasyPanel

## Variáveis essenciais
Configure as variáveis do arquivo `.env.example` no serviço da aplicação.
Use o hostname interno do serviço PostgreSQL no `DB_URL` (ex.: `app_postgres`),
desde que os serviços estejam na mesma rede Docker.

## Banco de dados
- Faça backup antes de atualizar uma base existente.
- A aplicação usa Flyway para aplicar migrations em `classpath:db/migration`.
- Hibernate está em `ddl-auto=validate`: ele valida entidades e tabelas, mas não cria tabelas.
- Em uma base já existente sem `flyway_schema_history`, não habilite o deploy sem revisar
  o estado do esquema e planejar o baseline/migração. Não apague dados para contornar erros.

## Build
Faça deploy com rebuild da imagem para garantir que o JAR seja recompilado com os recursos
atuais de `src/main/resources`.
