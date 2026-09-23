CREATE TABLE IF NOT EXISTS configuracoes_comissao (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL,
    profissional_id BIGINT NOT NULL,
    percentual NUMERIC(5,2) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_config_comissao_empresa FOREIGN KEY (empresa_id) REFERENCES empresas(id),
    CONSTRAINT fk_config_comissao_profissional FOREIGN KEY (profissional_id) REFERENCES usuarios(id),
    CONSTRAINT uk_config_comissao_empresa_profissional UNIQUE (empresa_id, profissional_id),
    CONSTRAINT ck_config_comissao_percentual CHECK (percentual >= 0 AND percentual <= 100)
);
CREATE INDEX IF NOT EXISTS idx_config_comissao_empresa ON configuracoes_comissao(empresa_id);

CREATE TABLE IF NOT EXISTS comissoes (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL,
    profissional_id BIGINT NOT NULL,
    lancamento_id BIGINT NOT NULL,
    atendimento_id BIGINT NULL,
    base_calculo NUMERIC(12,2) NOT NULL,
    percentual NUMERIC(5,2) NOT NULL,
    valor NUMERIC(12,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    data_comissao DATE NOT NULL,
    pago_em TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_comissao_empresa FOREIGN KEY (empresa_id) REFERENCES empresas(id),
    CONSTRAINT fk_comissao_profissional FOREIGN KEY (profissional_id) REFERENCES usuarios(id),
    CONSTRAINT fk_comissao_lancamento FOREIGN KEY (lancamento_id) REFERENCES lancamentos_financeiros(id),
    CONSTRAINT fk_comissao_atendimento FOREIGN KEY (atendimento_id) REFERENCES atendimentos(id),
    CONSTRAINT uk_comissao_lancamento UNIQUE (lancamento_id),
    CONSTRAINT ck_comissao_status CHECK (status IN ('PENDENTE','PAGO','CANCELADO')),
    CONSTRAINT ck_comissao_percentual CHECK (percentual >= 0 AND percentual <= 100),
    CONSTRAINT ck_comissao_valor CHECK (valor > 0),
    CONSTRAINT ck_comissao_base CHECK (base_calculo > 0)
);
CREATE INDEX IF NOT EXISTS idx_comissao_empresa_data ON comissoes(empresa_id,data_comissao);
CREATE INDEX IF NOT EXISTS idx_comissao_empresa_profissional ON comissoes(empresa_id,profissional_id,status);
CREATE INDEX IF NOT EXISTS idx_comissao_empresa_status ON comissoes(empresa_id,status);

DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name='empresa_modulos') THEN
    EXECUTE 'ALTER TABLE empresa_modulos DROP CONSTRAINT IF EXISTS empresa_modulos_modulo_check';
    EXECUTE $mig$ALTER TABLE empresa_modulos ADD CONSTRAINT empresa_modulos_modulo_check CHECK (modulo IN ('CLIENTES','PRODUTOS','ESTOQUE','MEGA_HAIR','EQUIPE','HISTORICO','AGENDA','SERVICOS','FINANCEIRO','COMISSOES'))$mig$;
    EXECUTE 'INSERT INTO empresa_modulos (empresa_id, modulo, ativo) SELECT e.id, ''COMISSOES'', false FROM empresas e WHERE NOT EXISTS (SELECT 1 FROM empresa_modulos em WHERE em.empresa_id=e.id AND em.modulo=''COMISSOES'')';
  END IF;
END $$;
