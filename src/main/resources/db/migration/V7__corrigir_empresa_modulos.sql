-- Corrige bases em que a migration V6 foi registrada como aplicada,
-- mas a tabela empresa_modulos não existe (ou foi removida manualmente).
-- Mantém o schema controlado pelo Flyway e torna a recuperação idempotente.

CREATE TABLE IF NOT EXISTS empresa_modulos (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL,
    modulo VARCHAR(40) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE
);

-- Colunas esperadas pela entidade JPA, caso a tabela tenha sido criada
-- parcialmente por uma versão anterior.
ALTER TABLE empresa_modulos
    ADD COLUMN IF NOT EXISTS empresa_id BIGINT;

ALTER TABLE empresa_modulos
    ADD COLUMN IF NOT EXISTS modulo VARCHAR(40);

ALTER TABLE empresa_modulos
    ADD COLUMN IF NOT EXISTS ativo BOOLEAN NOT NULL DEFAULT TRUE;

-- Impede duplicidade lógica de módulo por empresa.
CREATE UNIQUE INDEX IF NOT EXISTS ux_empresa_modulo_empresa_modulo
    ON empresa_modulos (empresa_id, modulo);

CREATE INDEX IF NOT EXISTS idx_empresa_modulo_empresa_ativo
    ON empresa_modulos (empresa_id, ativo);

-- FK somente se ainda não existir.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_empresa_modulo_empresa'
          AND conrelid = 'empresa_modulos'::regclass
    ) THEN
        ALTER TABLE empresa_modulos
            ADD CONSTRAINT fk_empresa_modulo_empresa
            FOREIGN KEY (empresa_id) REFERENCES empresas(id)
            ON DELETE CASCADE;
    END IF;
END $$;

-- Valores permitidos para o catálogo de módulos.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'ck_empresa_modulo_nome'
          AND conrelid = 'empresa_modulos'::regclass
    ) THEN
        ALTER TABLE empresa_modulos
            ADD CONSTRAINT ck_empresa_modulo_nome
            CHECK (modulo IN (
                'CLIENTES',
                'PRODUTOS',
                'ESTOQUE',
                'MEGA_HAIR',
                'EQUIPE',
                'HISTORICO'
            ));
    END IF;
END $$;

-- Garante que todas as empresas existentes tenham todos os registros
-- de licenciamento. A configuração ativa atual é preservada quando já existe.
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
