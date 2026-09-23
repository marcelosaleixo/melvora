-- FASE 16 - Gestão de Serviços e Procedimentos.
-- Cada serviço pertence a exatamente um tenant.
CREATE TABLE IF NOT EXISTS servicos (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL,
    nome VARCHAR(150) NOT NULL,
    descricao VARCHAR(1000),
    categoria VARCHAR(80) NOT NULL,
    duracao_minutos INTEGER NOT NULL,
    preco NUMERIC(12,2) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_servico_empresa FOREIGN KEY (empresa_id) REFERENCES empresas(id),
    CONSTRAINT ck_servico_duracao CHECK (duracao_minutos BETWEEN 15 AND 720),
    CONSTRAINT ck_servico_preco CHECK (preco >= 0)
);

CREATE INDEX IF NOT EXISTS idx_servico_empresa ON servicos(empresa_id);
CREATE INDEX IF NOT EXISTS idx_servico_empresa_ativo ON servicos(empresa_id, ativo);

CREATE TABLE IF NOT EXISTS servicos_profissionais (
    servico_id BIGINT NOT NULL,
    profissional_id BIGINT NOT NULL,
    CONSTRAINT pk_servicos_profissionais PRIMARY KEY (servico_id, profissional_id),
    CONSTRAINT fk_servico_profissional_servico FOREIGN KEY (servico_id) REFERENCES servicos(id) ON DELETE CASCADE,
    CONSTRAINT fk_servico_profissional_usuario FOREIGN KEY (profissional_id) REFERENCES usuarios(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_servico_profissional_usuario ON servicos_profissionais(profissional_id);

ALTER TABLE agendamentos ADD COLUMN IF NOT EXISTS servico_id BIGINT;
ALTER TABLE agendamentos DROP CONSTRAINT IF EXISTS fk_agendamento_servico;
ALTER TABLE agendamentos ADD CONSTRAINT fk_agendamento_servico FOREIGN KEY (servico_id) REFERENCES servicos(id);
CREATE INDEX IF NOT EXISTS idx_agendamento_empresa_servico_inicio ON agendamentos(empresa_id, servico_id, data_hora_inicio);

-- Atualiza o catálogo de módulos para bases que já possuem o CHECK legado.
DO $$
DECLARE c RECORD;
BEGIN
    FOR c IN
        SELECT con.conname
        FROM pg_constraint con
        JOIN pg_class rel ON rel.oid = con.conrelid
        JOIN pg_namespace ns ON ns.oid = rel.relnamespace
        JOIN pg_attribute att ON att.attrelid = rel.oid AND att.attname = 'modulo'
        WHERE ns.nspname = 'public'
          AND rel.relname = 'empresa_modulos'
          AND con.contype = 'c'
          AND att.attnum = ANY (con.conkey)
    LOOP
        EXECUTE format('ALTER TABLE public.empresa_modulos DROP CONSTRAINT IF EXISTS %I', c.conname);
    END LOOP;
END $$;

ALTER TABLE public.empresa_modulos
    ADD CONSTRAINT ck_empresa_modulo_nome
    CHECK (modulo IN ('CLIENTES','PRODUTOS','ESTOQUE','MEGA_HAIR','EQUIPE','HISTORICO','AGENDA','SERVICOS'));

INSERT INTO public.empresa_modulos (empresa_id, modulo, ativo)
SELECT e.id, 'SERVICOS', FALSE
FROM public.empresas e
ON CONFLICT (empresa_id, modulo) DO NOTHING;
