-- FASE 25.3: central de conversas WhatsApp e controle de leitura.
ALTER TABLE whatsapp_mensagens
    ADD COLUMN IF NOT EXISTS lida_em TIMESTAMP NULL;

CREATE INDEX IF NOT EXISTS idx_whatsapp_mensagem_empresa_lida
    ON whatsapp_mensagens (empresa_id, lida_em, direcao, recebido_em DESC);
