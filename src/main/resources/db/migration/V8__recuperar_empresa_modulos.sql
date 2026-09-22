-- Recuperação idempotente da tabela de licenciamento.
-- Necessária para bancos em que V6/V7 constam no flyway_schema_history,
-- mas empresa_modulos não existe mais ou ficou incompleta.

CREATE TABLE IF NOT EXISTS empresa_modulos (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT,
    modulo VARCHAR(40),
    ativo BOOLEAN NOT NULL DEFAULT TRUE
);

ALTER TABLE empresa_modulos
    ADD COLUMN IF NOT EXISTS id BIGSERIAL;

ALTER TABLE empresa_modulos
    ADD COLUMN IF NOT EXISTS empresa_id BIGINT;

ALTER TABLE empresa_modulos
    ADD COLUMN IF NOT EXISTS modulo VARCHAR(40);

ALTER TABLE empresa_modulos
    ADD COLUMN IF NOT EXISTS ativo BOOLEAN NOT NULL DEFAULT TRUE;

-- Remove registros inválidos antes de aplicar as restrições estruturais.
DELETE FROM empresa_modulos em
WHERE em.empresa_id IS NULL
   OR em.modulo IS NULL
   OR em.modulo NOT IN (
        'CLIENTES', 'PRODUTOS', 'ESTOQUE',
        'MEGA_HAIR', 'EQUIPE', 'HISTORICO'
   );

-- Mantém apenas um registro por empresa/módulo, preservando o estado
-- ativo de pelo menos um registro quando houver duplicidade.
DELETE FROM empresa_modulos em
USING empresa_modulos duplicado
WHERE em.empresa_id = duplicado.empresa_id
  AND em.modulo = duplicado.modulo
  AND em.id > duplicado.id;

ALTER TABLE empresa_modulos
    ALTER COLUMN empresa_id SET NOT NULL,
    ALTER COLUMN modulo SET NOT NULL,
    ALTER COLUMN ativo SET DEFAULT TRUE,
    ALTER COLUMN ativo SET NOT NULL;

-- FK da empresa.
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
            FOREIGN KEY (empresa_id)
            REFERENCES empresas(id)
            ON DELETE CASCADE;
    END IF;
END $$;

-- Regra do catálogo de módulos.
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

-- Índice único evita dois licenciamentos para o mesmo módulo da mesma empresa.
CREATE UNIQUE INDEX IF NOT EXISTS ux_empresa_modulo_empresa_modulo
    ON empresa_modulos (empresa_id, modulo);

CREATE INDEX IF NOT EXISTS idx_empresa_modulo_empresa
    ON empresa_modulos (empresa_id);

CREATE INDEX IF NOT EXISTS idx_empresa_modulo_empresa_ativo
    ON empresa_modulos (empresa_id, ativo);

-- Recria o catálogo para todas as empresas já existentes sem sobrescrever
-- configurações que já tenham sido definidas.
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
