CREATE TABLE IF NOT EXISTS configuracoes_whatsapp_business (
    empresa_id BIGINT PRIMARY KEY,
    phone_number_id VARCHAR(100) NOT NULL,
    access_token_encrypted VARCHAR(4096) NOT NULL,
    api_version VARCHAR(30) NOT NULL DEFAULT 'v23.0',
    api_base_url VARCHAR(500) NOT NULL DEFAULT 'https://graph.facebook.com',
    ativa BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_whatsapp_business_empresa FOREIGN KEY (empresa_id) REFERENCES empresas(id) ON DELETE CASCADE
);

ALTER TABLE comunicacoes_agendadas ADD COLUMN IF NOT EXISTS provider_message_id VARCHAR(200);
ALTER TABLE comunicacoes_agendadas ADD COLUMN IF NOT EXISTS provider_error VARCHAR(2000);
CREATE UNIQUE INDEX IF NOT EXISTS uk_comunicacao_provider_message ON comunicacoes_agendadas(provider_message_id) WHERE provider_message_id IS NOT NULL;

ALTER TABLE comunicacoes_agendadas DROP CONSTRAINT IF EXISTS ck_comunicacao_status;
ALTER TABLE comunicacoes_agendadas ADD CONSTRAINT ck_comunicacao_status CHECK (status IN ('PENDENTE','ABERTA','ENVIADA','ENTREGUE','LIDA','ERRO','CANCELADA','SEM_TELEFONE'));
