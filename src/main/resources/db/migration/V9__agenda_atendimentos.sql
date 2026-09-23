-- FASE 12: Agenda multiempresa com prevenção de conflitos concorrentes.
CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE IF NOT EXISTS agendamentos (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL,
    cliente_id BIGINT NOT NULL,
    profissional_id BIGINT NOT NULL,
    tipo VARCHAR(40) NOT NULL,
    data_hora_inicio TIMESTAMP NOT NULL,
    data_hora_fim TIMESTAMP NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'AGENDADO',
    observacoes VARCHAR(2000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_agendamento_empresa FOREIGN KEY (empresa_id) REFERENCES empresas(id),
    CONSTRAINT fk_agendamento_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id),
    CONSTRAINT fk_agendamento_profissional FOREIGN KEY (profissional_id) REFERENCES usuarios(id),
    CONSTRAINT ck_agendamento_horario CHECK (data_hora_fim > data_hora_inicio),
    CONSTRAINT ck_agendamento_tipo CHECK (tipo IN ('APLICACAO_MEGA_HAIR','MANUTENCAO_MEGA_HAIR','OUTRO')),
    CONSTRAINT ck_agendamento_status CHECK (status IN ('AGENDADO','CONFIRMADO','EM_ATENDIMENTO','CONCLUIDO','CANCELADO','FALTOU'))
);

CREATE INDEX IF NOT EXISTS idx_agendamento_empresa_inicio
    ON agendamentos (empresa_id, data_hora_inicio);
CREATE INDEX IF NOT EXISTS idx_agendamento_empresa_profissional_inicio
    ON agendamentos (empresa_id, profissional_id, data_hora_inicio);
CREATE INDEX IF NOT EXISTS idx_agendamento_empresa_cliente_inicio
    ON agendamentos (empresa_id, cliente_id, data_hora_inicio);

-- Impede sobreposição do mesmo profissional em atendimentos que ocupam horário.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'ex_agendamento_profissional_horario'
          AND conrelid = 'agendamentos'::regclass
    ) THEN
        ALTER TABLE agendamentos
            ADD CONSTRAINT ex_agendamento_profissional_horario
            EXCLUDE USING gist (
                empresa_id WITH =,
                profissional_id WITH =,
                tsrange(data_hora_inicio, data_hora_fim, '[)') WITH &&
            ) WHERE (status IN ('AGENDADO','CONFIRMADO','EM_ATENDIMENTO'));
    END IF;
END $$;

-- Impede que a mesma cliente seja marcada em dois atendimentos simultâneos.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'ex_agendamento_cliente_horario'
          AND conrelid = 'agendamentos'::regclass
    ) THEN
        ALTER TABLE agendamentos
            ADD CONSTRAINT ex_agendamento_cliente_horario
            EXCLUDE USING gist (
                empresa_id WITH =,
                cliente_id WITH =,
                tsrange(data_hora_inicio, data_hora_fim, '[)') WITH &&
            ) WHERE (status IN ('AGENDADO','CONFIRMADO','EM_ATENDIMENTO'));
    END IF;
END $$;

-- Atualiza primeiro o catálogo de módulos da tabela de licenciamento.
ALTER TABLE empresa_modulos DROP CONSTRAINT IF EXISTS ck_empresa_modulo_nome;
ALTER TABLE empresa_modulos
    ADD CONSTRAINT ck_empresa_modulo_nome
    CHECK (modulo IN ('CLIENTES','PRODUTOS','ESTOQUE','MEGA_HAIR','EQUIPE','HISTORICO','AGENDA'));

-- O novo módulo fica disponível para empresas já existentes sem alterar
-- as configurações que futuramente forem administradas pelo SUPER_ADMIN.
INSERT INTO empresa_modulos (empresa_id, modulo, ativo)
SELECT id, 'AGENDA', TRUE FROM empresas
ON CONFLICT (empresa_id, modulo) DO NOTHING;
