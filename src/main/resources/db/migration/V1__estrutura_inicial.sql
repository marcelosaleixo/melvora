CREATE TABLE empresas (
    id BIGSERIAL PRIMARY KEY,
    nome_fantasia VARCHAR(150) NOT NULL,
    ativa BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE usuarios (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT REFERENCES empresas(id),
    nome VARCHAR(150) NOT NULL,
    email VARCHAR(180) NOT NULL UNIQUE,
    senha_hash VARCHAR(100) NOT NULL,
    role VARCHAR(30) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT ck_usuario_role
        CHECK (role IN ('SUPER_ADMIN','ADMIN','PROFISSIONAL','RECEPCIONISTA'))
);

CREATE INDEX idx_usuario_empresa ON usuarios(empresa_id);
CREATE INDEX idx_usuario_email ON usuarios(email);

CREATE TABLE produtos (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL REFERENCES empresas(id),
    nome VARCHAR(150) NOT NULL,
    tipo VARCHAR(30) NOT NULL,
    preco_venda NUMERIC(12,2) NOT NULL CHECK (preco_venda >= 0),
    preco_custo NUMERIC(12,2) NOT NULL CHECK (preco_custo >= 0),
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT ck_produto_tipo
        CHECK (tipo IN ('COMUM','MEGA_HAIR'))
);

CREATE INDEX idx_produto_empresa ON produtos(empresa_id);

CREATE TABLE produtos_mega_hair (
    id BIGSERIAL PRIMARY KEY,
    produto_id BIGINT NOT NULL UNIQUE
        REFERENCES produtos(id) ON DELETE CASCADE,
    comprimento_cm INTEGER NOT NULL
        CHECK (comprimento_cm BETWEEN 1 AND 300),
    peso_gramas NUMERIC(10,2) NOT NULL
        CHECK (peso_gramas > 0),
    tipo_fio VARCHAR(30) NOT NULL
        CHECK (tipo_fio IN ('LISO','ONDULADO','CACHEADO','CRESPO')),
    cor VARCHAR(80) NOT NULL,
    metodo VARCHAR(40) NOT NULL
        CHECK (metodo IN (
            'FITA_ADESIVA','QUERATINA','MICROLINK',
            'NANO_LINK','TIC_TAC','ENTRELECADO','OUTRO'
        )),
    origem VARCHAR(100)
);
