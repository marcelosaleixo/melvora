# Correção V47 - Schema de Retenção

A V37 recupera de forma idempotente as colunas e tabelas da FASE 25.11/25.12 quando V35/V36 constam como aplicadas, mas o schema físico está incompleto.

Também foi restaurado `spring.jpa.hibernate.ddl-auto=validate`, deixando o Flyway como fonte única do schema.
