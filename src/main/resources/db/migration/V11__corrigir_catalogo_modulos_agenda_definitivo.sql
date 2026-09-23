-- FASE 12 - Correção definitiva do catálogo de módulos.
-- Algumas bases antigas possuem um CHECK legado gerado pelo PostgreSQL
-- (ex.: empresa_modulos_modulo_check). A V10 tentou localizar o CHECK pela
-- expressão, mas uma base pode possuir uma expressão equivalente com outra
-- representação. Aqui identificamos o CHECK pela coluna modulo,
-- e não pelo nome ou texto da expressão.

DO $$
DECLARE
    c RECORD;
BEGIN
    FOR c IN
        SELECT con.conname
        FROM pg_constraint con
        JOIN pg_class rel ON rel.oid = con.conrelid
        JOIN pg_namespace ns ON ns.oid = rel.relnamespace
        JOIN pg_attribute att
          ON att.attrelid = rel.oid
         AND att.attname = 'modulo'
        WHERE ns.nspname = 'public'
          AND rel.relname = 'empresa_modulos'
          AND con.contype = 'c'
          AND att.attnum = ANY (con.conkey)
    LOOP
        EXECUTE format(
            'ALTER TABLE public.empresa_modulos DROP CONSTRAINT IF EXISTS %I',
            c.conname
        );
    END LOOP;
END $$;

ALTER TABLE public.empresa_modulos
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

-- Garante o módulo AGENDA para empresas existentes.
-- Não altera módulos já configurados.
INSERT INTO public.empresa_modulos (empresa_id, modulo, ativo)
SELECT e.id, 'AGENDA', TRUE
FROM public.empresas e
ON CONFLICT (empresa_id, modulo) DO NOTHING;
