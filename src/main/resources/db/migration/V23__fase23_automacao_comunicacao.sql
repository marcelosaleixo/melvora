CREATE TABLE IF NOT EXISTS configuracoes_comunicacao (
    empresa_id BIGINT PRIMARY KEY,
    confirmacao_ativa BOOLEAN NOT NULL DEFAULT TRUE,
    confirmacao_minutos_antes INTEGER NOT NULL DEFAULT 1440,
    lembrete_ativo BOOLEAN NOT NULL DEFAULT TRUE,
    lembrete_minutos_antes INTEGER NOT NULL DEFAULT 120,
    pos_atendimento_ativo BOOLEAN NOT NULL DEFAULT FALSE,
    pos_atendimento_minutos_depois INTEGER NOT NULL DEFAULT 60,
    CONSTRAINT fk_config_comunicacao_empresa FOREIGN KEY (empresa_id) REFERENCES empresas(id) ON DELETE CASCADE,
    CONSTRAINT ck_config_confirmacao_minutos CHECK (confirmacao_minutos_antes BETWEEN 15 AND 10080),
    CONSTRAINT ck_config_lembrete_minutos CHECK (lembrete_minutos_antes BETWEEN 15 AND 10080),
    CONSTRAINT ck_config_pos_minutos CHECK (pos_atendimento_minutos_depois BETWEEN 0 AND 10080)
);

CREATE TABLE IF NOT EXISTS comunicacoes_agendadas (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL,
    agendamento_id BIGINT NOT NULL,
    tipo VARCHAR(30) NOT NULL,
    data_hora_envio TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    mensagem VARCHAR(4000) NOT NULL,
    whatsapp_url VARCHAR(5000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP,
    CONSTRAINT fk_comunicacao_empresa FOREIGN KEY (empresa_id) REFERENCES empresas(id) ON DELETE CASCADE,
    CONSTRAINT fk_comunicacao_agendamento FOREIGN KEY (agendamento_id) REFERENCES agendamentos(id) ON DELETE CASCADE,
    CONSTRAINT uk_comunicacao_agendamento_tipo UNIQUE (agendamento_id, tipo),
    CONSTRAINT ck_comunicacao_tipo CHECK (tipo IN ('CONFIRMACAO','LEMBRETE','POS_ATENDIMENTO')),
    CONSTRAINT ck_comunicacao_status CHECK (status IN ('PENDENTE','ABERTA','ENVIADA','CANCELADA','SEM_TELEFONE'))
);

CREATE INDEX IF NOT EXISTS idx_comunicacao_empresa_status ON comunicacoes_agendadas(empresa_id, status, data_hora_envio);
CREATE INDEX IF NOT EXISTS idx_comunicacao_agendamento ON comunicacoes_agendadas(agendamento_id);
