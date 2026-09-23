ALTER TABLE configuracoes_whatsapp_business
    ADD COLUMN IF NOT EXISTS modo_integracao VARCHAR(20) NOT NULL DEFAULT 'META_CLOUD';

ALTER TABLE configuracoes_whatsapp_business
    ADD COLUMN IF NOT EXISTS n8n_base_url VARCHAR(500);

ALTER TABLE configuracoes_whatsapp_business
    ADD COLUMN IF NOT EXISTS n8n_webhook_path VARCHAR(500);

ALTER TABLE configuracoes_whatsapp_business
    ADD COLUMN IF NOT EXISTS n8n_integration_key VARCHAR(100);

ALTER TABLE configuracoes_whatsapp_business
    ADD COLUMN IF NOT EXISTS n8n_token_encrypted VARCHAR(4096);

CREATE UNIQUE INDEX IF NOT EXISTS uk_whatsapp_n8n_integration_key
    ON configuracoes_whatsapp_business(n8n_integration_key)
    WHERE n8n_integration_key IS NOT NULL;

ALTER TABLE configuracoes_whatsapp_business
    ALTER COLUMN phone_number_id DROP NOT NULL;

ALTER TABLE configuracoes_whatsapp_business
    ALTER COLUMN access_token_encrypted DROP NOT NULL;

ALTER TABLE configuracoes_whatsapp_business
    ALTER COLUMN api_version DROP NOT NULL;

ALTER TABLE configuracoes_whatsapp_business
    ALTER COLUMN api_base_url DROP NOT NULL;
