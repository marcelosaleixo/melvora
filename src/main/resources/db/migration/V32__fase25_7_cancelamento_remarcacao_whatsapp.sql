CREATE TABLE IF NOT EXISTS whatsapp_agendamento_acoes_pendentes (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL,
    cliente_id BIGINT NOT NULL,
    agendamento_id BIGINT NOT NULL,
    telefone VARCHAR(30) NOT NULL,
    acao VARCHAR(20) NOT NULL,
    status VARCHAR(45) NOT NULL,
    data_desejada DATE,
    opcoes_json TEXT,
    profissional_id BIGINT,
    inicio_selecionado TIMESTAMP,
    fim_selecionado TIMESTAMP,
    expira_em TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_wa_agenda_acao_empresa FOREIGN KEY (empresa_id) REFERENCES empresas(id),
    CONSTRAINT fk_wa_agenda_acao_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id),
    CONSTRAINT fk_wa_agenda_acao_agendamento FOREIGN KEY (agendamento_id) REFERENCES agendamentos(id),
    CONSTRAINT ck_wa_agenda_acao_acao CHECK (acao IN ('CANCELAR','REMARCAR')),
    CONSTRAINT ck_wa_agenda_acao_status CHECK (status IN (
        'AGUARDANDO_CONFIRMACAO_CANCELAMENTO',
        'AGUARDANDO_DATA_REAGENDAMENTO',
        'AGUARDANDO_HORARIO_REAGENDAMENTO',
        'AGUARDANDO_CONFIRMACAO_REAGENDAMENTO',
        'CONCLUIDA','EXPIRADA','CANCELADA'
    ))
);
CREATE INDEX IF NOT EXISTS idx_wa_agenda_acao_empresa_telefone_status
    ON whatsapp_agendamento_acoes_pendentes(empresa_id, telefone, status);
CREATE INDEX IF NOT EXISTS idx_wa_agenda_acao_expira
    ON whatsapp_agendamento_acoes_pendentes(status, expira_em);
