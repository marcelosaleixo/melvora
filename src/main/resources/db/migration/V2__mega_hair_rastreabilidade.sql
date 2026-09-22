CREATE TABLE clientes (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL REFERENCES empresas(id),
    nome VARCHAR(150) NOT NULL,
    telefone VARCHAR(30),
    email VARCHAR(180),
    observacoes VARCHAR(1000),
    ativo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_cliente_empresa ON clientes(empresa_id);
CREATE INDEX idx_cliente_empresa_nome ON clientes(empresa_id, nome);

CREATE TABLE lotes_mega_hair (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL REFERENCES empresas(id),
    produto_id BIGINT NOT NULL REFERENCES produtos(id),
    codigo VARCHAR(60) NOT NULL,
    quantidade_inicial INTEGER NOT NULL
        CHECK (quantidade_inicial BETWEEN 1 AND 100000),
    quantidade_disponivel INTEGER NOT NULL
        CHECK (quantidade_disponivel BETWEEN 0 AND 100000),
    peso_por_unidade_gramas NUMERIC(10,2) NOT NULL
        CHECK (peso_por_unidade_gramas > 0),
    comprimento_cm INTEGER NOT NULL
        CHECK (comprimento_cm BETWEEN 1 AND 300),
    tipo_fio VARCHAR(30) NOT NULL
        CHECK (tipo_fio IN ('LISO','ONDULADO','CACHEADO','CRESPO')),
    cor VARCHAR(80) NOT NULL,
    metodo VARCHAR(40) NOT NULL
        CHECK (metodo IN (
            'FITA_ADESIVA','QUERATINA','MICROLINK',
            'NANO_LINK','TIC_TAC','ENTRELECADO','OUTRO'
        )),
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT uk_lote_empresa_codigo
        UNIQUE (empresa_id, codigo),

    CONSTRAINT ck_lote_disponivel_le_inicial
        CHECK (quantidade_disponivel <= quantidade_inicial)
);

CREATE INDEX idx_lote_empresa ON lotes_mega_hair(empresa_id);
CREATE INDEX idx_lote_empresa_produto
    ON lotes_mega_hair(empresa_id, produto_id);

CREATE TABLE aplicacoes_mega_hair (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL REFERENCES empresas(id),
    cliente_id BIGINT NOT NULL REFERENCES clientes(id),
    profissional_id BIGINT NOT NULL REFERENCES usuarios(id),
    data_aplicacao DATE NOT NULL,
    observacoes VARCHAR(2000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_aplicacao_empresa_cliente
    ON aplicacoes_mega_hair(empresa_id, cliente_id);

CREATE INDEX idx_aplicacao_empresa_data
    ON aplicacoes_mega_hair(empresa_id, data_aplicacao);

CREATE TABLE aplicacao_lotes (
    id BIGSERIAL PRIMARY KEY,
    aplicacao_id BIGINT NOT NULL
        REFERENCES aplicacoes_mega_hair(id) ON DELETE CASCADE,
    lote_id BIGINT NOT NULL
        REFERENCES lotes_mega_hair(id),
    quantidade INTEGER NOT NULL CHECK (quantidade > 0),
    peso_total_gramas NUMERIC(12,2) NOT NULL
        CHECK (peso_total_gramas > 0),
    comprimento_cm INTEGER NOT NULL
        CHECK (comprimento_cm BETWEEN 1 AND 300),
    tipo_fio VARCHAR(30) NOT NULL,
    cor VARCHAR(80) NOT NULL,
    metodo VARCHAR(40) NOT NULL,

    CONSTRAINT uk_aplicacao_lote
        UNIQUE (aplicacao_id, lote_id)
);

CREATE INDEX idx_aplicacao_lotes_aplicacao
    ON aplicacao_lotes(aplicacao_id);

CREATE INDEX idx_aplicacao_lotes_lote
    ON aplicacao_lotes(lote_id);

CREATE TABLE movimentacoes_estoque (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL REFERENCES empresas(id),
    lote_id BIGINT NOT NULL REFERENCES lotes_mega_hair(id),
    aplicacao_id BIGINT REFERENCES aplicacoes_mega_hair(id),
    tipo VARCHAR(20) NOT NULL
        CHECK (tipo IN ('ENTRADA','SAIDA','AJUSTE')),
    quantidade INTEGER NOT NULL CHECK (quantidade > 0),
    saldo_apos INTEGER NOT NULL CHECK (saldo_apos >= 0),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_mov_empresa_data
    ON movimentacoes_estoque(empresa_id, created_at);

CREATE INDEX idx_mov_lote
    ON movimentacoes_estoque(lote_id);

CREATE TABLE manutencoes_mega_hair (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL REFERENCES empresas(id),
    cliente_id BIGINT NOT NULL REFERENCES clientes(id),
    aplicacao_id BIGINT NOT NULL
        REFERENCES aplicacoes_mega_hair(id),
    profissional_id BIGINT NOT NULL REFERENCES usuarios(id),
    data_manutencao DATE NOT NULL,
    tipo VARCHAR(30) NOT NULL
        CHECK (tipo IN ('REAPLICACAO','REMOCAO','MANUTENCAO')),
    observacoes VARCHAR(2000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_manut_empresa_cliente
    ON manutencoes_mega_hair(empresa_id, cliente_id);

CREATE INDEX idx_manut_aplicacao
    ON manutencoes_mega_hair(aplicacao_id);
