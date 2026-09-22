CREATE TABLE empresa_modulos (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL REFERENCES empresas(id) ON DELETE CASCADE,
    modulo VARCHAR(40) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_empresa_modulo UNIQUE (empresa_id, modulo),
    CONSTRAINT ck_empresa_modulo_nome CHECK (
        modulo IN ('CLIENTES','PRODUTOS','ESTOQUE','MEGA_HAIR','EQUIPE','HISTORICO')
    )
);

CREATE INDEX idx_empresa_modulo_empresa_ativo
    ON empresa_modulos (empresa_id, ativo);

-- Empresas existentes preservam o comportamento atual na primeira migração.
INSERT INTO empresa_modulos (empresa_id, modulo, ativo)
SELECT e.id, m.modulo, TRUE
FROM empresas e
CROSS JOIN (
    VALUES
        ('CLIENTES'),
        ('PRODUTOS'),
        ('ESTOQUE'),
        ('MEGA_HAIR'),
        ('EQUIPE'),
        ('HISTORICO')
) AS m(modulo)
ON CONFLICT (empresa_id, modulo) DO NOTHING;
