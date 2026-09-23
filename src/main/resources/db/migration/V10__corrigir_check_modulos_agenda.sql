-- FASE 12 - Correção do catálogo de módulos.
-- Algumas bases possuem um CHECK legado com nome gerado automaticamente
-- (por exemplo: empresa_modulos_modulo_check). Esse CHECK não conhecia AGENDA
-- e permanecia ativo mesmo após a criação do novo CHECK em V9.
--
-- A correção é feita dinamicamente pelo conteúdo da regra, e não pelo nome,
-- para funcionar tanto em bases antigas quanto nas bases que usam os nomes
-- ck_empresa_modulo_nome / empresa_modulos_modulo_check.

DO $$
DECLARE
    constraint_record RECORD;
BEGIN
    FOR constraint_record IN
        SELECT con.conname
        FROM pg_constraint con
        JOIN pg_class rel ON rel.oid = con.conrelid
        JOIN pg_namespace ns ON ns.oid = rel.relnamespace
        WHERE ns.nspname = current_schema()
          AND rel.relname = 'empresa_modulos'
          AND con.contype = 'c'
          AND pg_get_constraintdef(con.oid) ILIKE '%modulo%'
          AND pg_get_constraintdef(con.oid) ILIKE '%CLIENTES%'
          AND pg_get_constraintdef(con.oid) ILIKE '%HISTORICO%'
    LOOP
        EXECUTE format(
            'ALTER TABLE empresa_modulos DROP CONSTRAINT IF EXISTS %I',
            constraint_record.conname
        );
    END LOOP;
END $$;

ALTER TABLE empresa_modulos
    ADD CONSTRAINT ck_empresa_modulo_nome
    CHECK (
        modulo IN (
            'CLIENTES',
            'PRODUTOS',
            'ESTOQUE',
            'MEGA_HAIR',
            'EQUIPE',
            'HISTORICO',
            'AGENDA'
        )
    );

-- Garante o registro de Agenda para empresas existentes sem alterar
-- configurações já salvas pelo SUPER_ADMIN.
INSERT INTO empresa_modulos (empresa_id, modulo, ativo)
SELECT e.id, 'AGENDA', TRUE
FROM empresas e
ON CONFLICT (empresa_id, modulo) DO NOTHING;
