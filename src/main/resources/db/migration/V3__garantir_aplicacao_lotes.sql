-- Migration de segurança para bases locais que possam ter recebido
-- uma versão anterior da V2 sem a tabela de vínculo.
CREATE TABLE IF NOT EXISTS aplicacao_lotes (
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
    CONSTRAINT uk_aplicacao_lote UNIQUE (aplicacao_id, lote_id)
);

CREATE INDEX IF NOT EXISTS idx_aplicacao_lotes_aplicacao
    ON aplicacao_lotes(aplicacao_id);

CREATE INDEX IF NOT EXISTS idx_aplicacao_lotes_lote
    ON aplicacao_lotes(lote_id);
