ALTER TABLE configuracoes_comunicacao
    ADD COLUMN IF NOT EXISTS retencao_automatica_ativa BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS retencao_cooldown_dias INTEGER NOT NULL DEFAULT 30,
    ADD COLUMN IF NOT EXISTS retencao_horario_inicio TIME NOT NULL DEFAULT '09:00',
    ADD COLUMN IF NOT EXISTS retencao_horario_fim TIME NOT NULL DEFAULT '19:00',
    ADD COLUMN IF NOT EXISTS retencao_max_envios_dia INTEGER NOT NULL DEFAULT 20;

ALTER TABLE configuracoes_comunicacao DROP CONSTRAINT IF EXISTS configuracoes_comunicacao_retencao_cooldown_check;
ALTER TABLE configuracoes_comunicacao ADD CONSTRAINT configuracoes_comunicacao_retencao_cooldown_check CHECK (retencao_cooldown_dias BETWEEN 1 AND 365);
ALTER TABLE configuracoes_comunicacao DROP CONSTRAINT IF EXISTS configuracoes_comunicacao_retencao_max_envios_check;
ALTER TABLE configuracoes_comunicacao ADD CONSTRAINT configuracoes_comunicacao_retencao_max_envios_check CHECK (retencao_max_envios_dia BETWEEN 1 AND 500);
ALTER TABLE configuracoes_comunicacao DROP CONSTRAINT IF EXISTS configuracoes_comunicacao_retencao_horario_check;
ALTER TABLE configuracoes_comunicacao ADD CONSTRAINT configuracoes_comunicacao_retencao_horario_check CHECK (retencao_horario_inicio < retencao_horario_fim);

CREATE TABLE IF NOT EXISTS preferencias_comunicacao_contato (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL,
    cliente_id BIGINT NULL,
    telefone VARCHAR(30) NOT NULL,
    retencao_opt_in BOOLEAN NOT NULL DEFAULT FALSE,
    retencao_opt_in_at TIMESTAMP NULL,
    retencao_opt_out_at TIMESTAMP NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_pref_com_empresa_telefone UNIQUE (empresa_id, telefone),
    CONSTRAINT fk_pref_com_empresa FOREIGN KEY (empresa_id) REFERENCES empresas(id),
    CONSTRAINT fk_pref_com_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id)
);
CREATE INDEX IF NOT EXISTS idx_pref_com_empresa_cliente ON preferencias_comunicacao_contato(empresa_id, cliente_id);
CREATE INDEX IF NOT EXISTS idx_pref_com_empresa_optin ON preferencias_comunicacao_contato(empresa_id, retencao_opt_in);

CREATE TABLE IF NOT EXISTS retencao_envios (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL,
    cliente_id BIGINT NOT NULL,
    telefone VARCHAR(30) NOT NULL,
    data_hora_envio TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) NOT NULL,
    mensagem VARCHAR(4000) NOT NULL,
    provider_message_id VARCHAR(200),
    erro VARCHAR(2000),
    CONSTRAINT fk_ret_envio_empresa FOREIGN KEY (empresa_id) REFERENCES empresas(id),
    CONSTRAINT fk_ret_envio_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id),
    CONSTRAINT ck_ret_envio_status CHECK (status IN ('ENVIADA','ERRO','BLOQUEADA'))
);
CREATE INDEX IF NOT EXISTS idx_ret_envio_empresa_data ON retencao_envios(empresa_id, data_hora_envio);
CREATE INDEX IF NOT EXISTS idx_ret_envio_empresa_cliente ON retencao_envios(empresa_id, cliente_id, data_hora_envio);
