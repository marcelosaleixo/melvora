CREATE TABLE IF NOT EXISTS campanhas_comunicacao (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL,
    nome VARCHAR(120) NOT NULL,
    segmento VARCHAR(40) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'RASCUNHO',
    dias_sem_retorno INTEGER,
    servico_id BIGINT,
    mensagem VARCHAR(4000) NOT NULL,
    cooldown_dias INTEGER NOT NULL DEFAULT 30,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMP,
    finished_at TIMESTAMP,
    CONSTRAINT fk_campanha_empresa FOREIGN KEY (empresa_id) REFERENCES empresas(id),
    CONSTRAINT fk_campanha_servico FOREIGN KEY (servico_id) REFERENCES servicos(id),
    CONSTRAINT ck_campanha_segmento CHECK (segmento IN ('INATIVAS','SEM_PROXIMO_AGENDAMENTO','SERVICO_REALIZADO')),
    CONSTRAINT ck_campanha_status CHECK (status IN ('RASCUNHO','ENVIANDO','CONCLUIDA','CANCELADA')),
    CONSTRAINT ck_campanha_dias CHECK (dias_sem_retorno IS NULL OR (dias_sem_retorno BETWEEN 1 AND 365)),
    CONSTRAINT ck_campanha_cooldown CHECK (cooldown_dias BETWEEN 1 AND 365)
);

CREATE INDEX IF NOT EXISTS idx_campanha_empresa_status ON campanhas_comunicacao(empresa_id,status);
CREATE INDEX IF NOT EXISTS idx_campanha_empresa_criada ON campanhas_comunicacao(empresa_id,created_at);

CREATE TABLE IF NOT EXISTS campanhas_envios (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL,
    campanha_id BIGINT NOT NULL,
    cliente_id BIGINT NOT NULL,
    telefone VARCHAR(30) NOT NULL,
    mensagem VARCHAR(4000) NOT NULL,
    status VARCHAR(20) NOT NULL,
    data_hora_envio TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    provider_message_id VARCHAR(200),
    erro VARCHAR(2000),
    CONSTRAINT fk_camp_envio_empresa FOREIGN KEY (empresa_id) REFERENCES empresas(id),
    CONSTRAINT fk_camp_envio_campanha FOREIGN KEY (campanha_id) REFERENCES campanhas_comunicacao(id) ON DELETE CASCADE,
    CONSTRAINT fk_camp_envio_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id),
    CONSTRAINT uk_campanha_envio_cliente UNIQUE (campanha_id,cliente_id),
    CONSTRAINT ck_campanha_envio_status CHECK (status IN ('PENDENTE','ENVIADA','ERRO','BLOQUEADA'))
);

CREATE INDEX IF NOT EXISTS idx_campanha_envio_empresa_status ON campanhas_envios(empresa_id,status);
CREATE INDEX IF NOT EXISTS idx_campanha_envio_cliente_data ON campanhas_envios(empresa_id,cliente_id,data_hora_envio);
