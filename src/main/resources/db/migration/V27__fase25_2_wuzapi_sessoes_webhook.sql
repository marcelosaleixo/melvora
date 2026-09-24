-- FASE 25.2: sessão WuzAPI, QR Code, webhook seguro e identificação da instância.
ALTER TABLE configuracoes_whatsapp_business
    ADD COLUMN IF NOT EXISTS wuzapi_integration_key VARCHAR(100);

ALTER TABLE configuracoes_whatsapp_business
    ADD COLUMN IF NOT EXISTS wuzapi_hmac_secret_encrypted VARCHAR(4096);

ALTER TABLE configuracoes_whatsapp_business
    ADD COLUMN IF NOT EXISTS wuzapi_phone_jid VARCHAR(200);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'uk_whatsapp_wuzapi_integration_key'
    ) THEN
        ALTER TABLE configuracoes_whatsapp_business
            ADD CONSTRAINT uk_whatsapp_wuzapi_integration_key UNIQUE (wuzapi_integration_key);
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS whatsapp_mensagens (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL,
    cliente_id BIGINT NULL,
    provider_message_id VARCHAR(200) NOT NULL,
    direcao VARCHAR(10) NOT NULL,
    telefone VARCHAR(30) NOT NULL,
    push_name VARCHAR(150),
    mensagem TEXT,
    raw_payload TEXT NOT NULL,
    recebido_em TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_whatsapp_mensagem_empresa FOREIGN KEY (empresa_id) REFERENCES empresas(id),
    CONSTRAINT fk_whatsapp_mensagem_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id),
    CONSTRAINT ck_whatsapp_mensagem_direcao CHECK (direcao IN ('ENTRADA','SAIDA')),
    CONSTRAINT uk_whatsapp_mensagem_provider UNIQUE (empresa_id, provider_message_id)
);

CREATE INDEX IF NOT EXISTS idx_whatsapp_mensagem_empresa_data
    ON whatsapp_mensagens (empresa_id, recebido_em DESC);

CREATE INDEX IF NOT EXISTS idx_whatsapp_mensagem_empresa_cliente
    ON whatsapp_mensagens (empresa_id, cliente_id, recebido_em DESC);

CREATE INDEX IF NOT EXISTS idx_whatsapp_mensagem_empresa_telefone
    ON whatsapp_mensagens (empresa_id, telefone);
