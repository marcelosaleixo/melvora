-- FASE 25.8 - confirmação interativa e lembretes WhatsApp
CREATE TABLE IF NOT EXISTS whatsapp_confirmacoes_pendentes (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL,
    cliente_id BIGINT NOT NULL,
    agendamento_id BIGINT NOT NULL,
    telefone VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'AGUARDANDO_RESPOSTA',
    expira_em TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_wa_confirmacao_empresa FOREIGN KEY (empresa_id) REFERENCES empresas(id) ON DELETE CASCADE,
    CONSTRAINT fk_wa_confirmacao_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id) ON DELETE CASCADE,
    CONSTRAINT fk_wa_confirmacao_agendamento FOREIGN KEY (agendamento_id) REFERENCES agendamentos(id) ON DELETE CASCADE,
    CONSTRAINT uk_wa_confirmacao_agendamento UNIQUE (agendamento_id),
    CONSTRAINT ck_wa_confirmacao_status CHECK (status IN ('AGUARDANDO_RESPOSTA','CONFIRMADA','RECUSADA','EXPIRADA'))
);
CREATE INDEX IF NOT EXISTS idx_wa_confirmacao_empresa_telefone_status
    ON whatsapp_confirmacoes_pendentes(empresa_id, telefone, status);
CREATE INDEX IF NOT EXISTS idx_wa_confirmacao_expira
    ON whatsapp_confirmacoes_pendentes(status, expira_em);
