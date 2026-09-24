CREATE TABLE IF NOT EXISTS avaliacoes_atendimento (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL,
    atendimento_id BIGINT NOT NULL,
    cliente_id BIGINT NOT NULL,
    profissional_id BIGINT NOT NULL,
    public_token VARCHAR(80) NOT NULL,
    nota INTEGER NULL,
    comentario VARCHAR(1000) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    respondida_at TIMESTAMP NULL,
    CONSTRAINT uk_avaliacao_atendimento UNIQUE (atendimento_id),
    CONSTRAINT uk_avaliacao_token UNIQUE (public_token),
    CONSTRAINT ck_avaliacao_nota CHECK (nota IS NULL OR nota BETWEEN 1 AND 5),
    CONSTRAINT fk_avaliacao_empresa FOREIGN KEY (empresa_id) REFERENCES empresas(id),
    CONSTRAINT fk_avaliacao_atendimento FOREIGN KEY (atendimento_id) REFERENCES atendimentos(id),
    CONSTRAINT fk_avaliacao_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id),
    CONSTRAINT fk_avaliacao_profissional FOREIGN KEY (profissional_id) REFERENCES usuarios(id)
);
CREATE INDEX IF NOT EXISTS idx_avaliacao_empresa_data ON avaliacoes_atendimento(empresa_id, created_at);
CREATE INDEX IF NOT EXISTS idx_avaliacao_empresa_nota ON avaliacoes_atendimento(empresa_id, nota);
