-- FASE 22 - Correção definitiva do schema de Templates WhatsApp.
-- A V20/V21 podem ter sido registradas pelo Flyway em bancos onde a tabela
-- não chegou a existir. Esta nova versão é executada em seguida e reconcilia
-- o schema sem alterar migrations já aplicadas.

CREATE TABLE IF NOT EXISTS templates_whatsapp (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL,
    nome VARCHAR(100) NOT NULL,
    mensagem VARCHAR(4000) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_template_whatsapp_empresa
        FOREIGN KEY (empresa_id) REFERENCES empresas(id) ON DELETE CASCADE,
    CONSTRAINT uk_template_whatsapp_empresa_nome
        UNIQUE (empresa_id, nome)
);

CREATE INDEX IF NOT EXISTS idx_template_whatsapp_empresa
    ON templates_whatsapp(empresa_id);

CREATE INDEX IF NOT EXISTS idx_template_whatsapp_empresa_ativo
    ON templates_whatsapp(empresa_id, ativo);
