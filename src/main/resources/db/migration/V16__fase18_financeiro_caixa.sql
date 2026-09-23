CREATE TABLE IF NOT EXISTS lancamentos_financeiros (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL,
    atendimento_id BIGINT NULL,
    cliente_id BIGINT NULL,
    tipo VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    forma_pagamento VARCHAR(30) NULL,
    valor NUMERIC(12,2) NOT NULL,
    descricao VARCHAR(200) NOT NULL,
    data_movimento DATE NOT NULL,
    data_vencimento DATE NULL,
    pago_em TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_fin_empresa FOREIGN KEY (empresa_id) REFERENCES empresas(id),
    CONSTRAINT fk_fin_atendimento FOREIGN KEY (atendimento_id) REFERENCES atendimentos(id),
    CONSTRAINT fk_fin_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id),
    CONSTRAINT ck_fin_tipo CHECK (tipo IN ('RECEITA','DESPESA')),
    CONSTRAINT ck_fin_status CHECK (status IN ('PENDENTE','PAGO','CANCELADO')),
    CONSTRAINT ck_fin_valor CHECK (valor > 0)
);
CREATE INDEX IF NOT EXISTS idx_fin_empresa_data ON lancamentos_financeiros(empresa_id,data_movimento);
CREATE INDEX IF NOT EXISTS idx_fin_empresa_status ON lancamentos_financeiros(empresa_id,status);
CREATE INDEX IF NOT EXISTS idx_fin_empresa_cliente ON lancamentos_financeiros(empresa_id,cliente_id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_fin_receita_atendimento ON lancamentos_financeiros(atendimento_id) WHERE atendimento_id IS NOT NULL AND tipo='RECEITA';
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name='empresa_modulos') THEN
    EXECUTE 'ALTER TABLE empresa_modulos DROP CONSTRAINT IF EXISTS empresa_modulos_modulo_check';
    IF NOT EXISTS (SELECT 1 FROM pg_constraint c JOIN pg_class t ON t.oid=c.conrelid WHERE t.relname='empresa_modulos' AND pg_get_constraintdef(c.oid) LIKE '%FINANCEIRO%') THEN
      EXECUTE $mig$ALTER TABLE empresa_modulos ADD CONSTRAINT empresa_modulos_modulo_check CHECK (modulo IN ('CLIENTES','PRODUTOS','ESTOQUE','MEGA_HAIR','EQUIPE','HISTORICO','AGENDA','SERVICOS','FINANCEIRO'))$mig$;
    END IF;
    EXECUTE 'INSERT INTO empresa_modulos (empresa_id, modulo, ativo) SELECT e.id, ''FINANCEIRO'', false FROM empresas e WHERE NOT EXISTS (SELECT 1 FROM empresa_modulos em WHERE em.empresa_id=e.id AND em.modulo=''FINANCEIRO'')';
  END IF;
END $$;
