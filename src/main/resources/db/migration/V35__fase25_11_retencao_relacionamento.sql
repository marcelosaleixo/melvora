ALTER TABLE configuracoes_comunicacao
    ADD COLUMN IF NOT EXISTS retencao_ativa BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS retencao_dias_sem_retorno INTEGER NOT NULL DEFAULT 60,
    ADD COLUMN IF NOT EXISTS retencao_mensagem VARCHAR(2000) NOT NULL DEFAULT 'Olá, {cliente}! Sentimos sua falta no Melvora. Já faz {dias} dias desde seu último atendimento, em {ultima_data}. Quando quiser, estamos aqui para cuidar de você novamente!';

ALTER TABLE configuracoes_comunicacao
    DROP CONSTRAINT IF EXISTS configuracoes_comunicacao_retencao_dias_check;
ALTER TABLE configuracoes_comunicacao
    ADD CONSTRAINT configuracoes_comunicacao_retencao_dias_check CHECK (retencao_dias_sem_retorno BETWEEN 15 AND 365);

CREATE INDEX IF NOT EXISTS idx_atendimento_empresa_cliente_data
    ON atendimentos (empresa_id, cliente_id, data_hora_inicio);
