-- FASE 25.4: automação assistida de conversas WhatsApp.
CREATE TABLE IF NOT EXISTS configuracoes_whatsapp_automacao (
    empresa_id BIGINT PRIMARY KEY,
    ativa BOOLEAN NOT NULL DEFAULT FALSE,
    responder_saudacao BOOLEAN NOT NULL DEFAULT TRUE,
    responder_servicos BOOLEAN NOT NULL DEFAULT TRUE,
    responder_preco BOOLEAN NOT NULL DEFAULT TRUE,
    responder_agendamento BOOLEAN NOT NULL DEFAULT TRUE,
    encaminhar_humano BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_whatsapp_automacao_empresa FOREIGN KEY (empresa_id) REFERENCES empresas(id) ON DELETE CASCADE
);

ALTER TABLE whatsapp_mensagens
    ADD COLUMN IF NOT EXISTS automacao_intencao VARCHAR(30),
    ADD COLUMN IF NOT EXISTS automacao_processada_em TIMESTAMP NULL,
    ADD COLUMN IF NOT EXISTS automacao_resposta_enviada BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_whatsapp_mensagem_automacao_pendente
    ON whatsapp_mensagens (direcao, automacao_processada_em, recebido_em);
