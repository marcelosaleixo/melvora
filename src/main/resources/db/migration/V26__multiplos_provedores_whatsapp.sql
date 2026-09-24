-- FASE 25: preparação para múltiplos provedores WhatsApp.
-- A configuração anterior do n8n é preservada para compatibilidade.

ALTER TABLE configuracoes_whatsapp_business
    ADD COLUMN IF NOT EXISTS evolution_base_url VARCHAR(500);

ALTER TABLE configuracoes_whatsapp_business
    ADD COLUMN IF NOT EXISTS evolution_api_key_encrypted VARCHAR(4096);

ALTER TABLE configuracoes_whatsapp_business
    ADD COLUMN IF NOT EXISTS evolution_instance VARCHAR(150);

ALTER TABLE configuracoes_whatsapp_business
    ADD COLUMN IF NOT EXISTS wuzapi_base_url VARCHAR(500);

ALTER TABLE configuracoes_whatsapp_business
    ADD COLUMN IF NOT EXISTS wuzapi_token_encrypted VARCHAR(4096);

-- Não há dados de credenciais nesta migration: tokens são gravados somente
-- pelo serviço após criptografia AES-256-GCM.
