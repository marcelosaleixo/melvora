-- Garante unicidade lógica do nome da empresa sem alterar dados existentes.
-- A migration falha intencionalmente se o banco já possuir nomes duplicados,
-- evitando esconder inconsistências de cadastro.
CREATE UNIQUE INDEX IF NOT EXISTS ux_empresas_nome_fantasia_lower
    ON empresas (LOWER(nome_fantasia));
