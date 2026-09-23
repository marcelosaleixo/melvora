-- FASE 16 - Correção definitiva do catálogo de módulos.
--
-- Algumas bases antigas possuem a constraint gerada automaticamente pelo
-- PostgreSQL, normalmente "empresa_modulos_modulo_check". A V13 tentou
-- localizar CHECKs pela coluna em pg_constraint.conkey, porém CHECKs podem
-- aparecer como constraints de expressão sem a coluna devidamente exposta
-- em conkey. Por isso esta migration localiza a regra pelo SQL da constraint.
--
-- Objetivo: permitir todos os módulos atualmente suportados, incluindo
-- SERVICOS, sem alterar os registros já licenciados.

DO $$
DECLARE
    c RECORD;
BEGIN
    FOR c IN
        SELECT con.conname
        FROM pg_constraint con
        JOIN pg_class rel ON rel.oid = con.conrelid
        JOIN pg_namespace ns ON ns.oid = rel.relnamespace
        WHERE ns.nspname = current_schema()
          AND rel.relname = 'empresa_modulos'
          AND con.contype = 'c'
          AND (
              pg_get_constraintdef(con.oid) ILIKE '%modulo%'
              OR con.conname = 'empresa_modulos_modulo_check'
              OR con.conname = 'ck_empresa_modulo_nome'
          )
    LOOP
        EXECUTE format(
            'ALTER TABLE %I.%I DROP CONSTRAINT IF EXISTS %I',
            current_schema(),
            'empresa_modulos',
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
            'AGENDA',
            'SERVICOS'
        )
    );

-- Garante que empresas existentes tenham o registro SERVICOS desativado.
-- O estado dos módulos existentes não é alterado.
INSERT INTO public.empresa_modulos (empresa_id, modulo, ativo)
SELECT e.id, 'SERVICOS', FALSE
FROM public.empresas e
ON CONFLICT (empresa_id, modulo) DO NOTHING;
