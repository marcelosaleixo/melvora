-- FASE 17 - Atendimento e histórico geral de serviços.
-- Um atendimento é criado automaticamente quando um agendamento é concluído.
CREATE TABLE IF NOT EXISTS atendimentos (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL,
    agendamento_id BIGINT NOT NULL,
    cliente_id BIGINT NOT NULL,
    profissional_id BIGINT NOT NULL,
    tipo VARCHAR(40) NOT NULL,
    servico_nome VARCHAR(150),
    preco_tabela NUMERIC(12,2),
    valor_cobrado NUMERIC(12,2) NOT NULL,
    duracao_minutos INTEGER NOT NULL,
    data_hora_inicio TIMESTAMP NOT NULL,
    data_hora_fim TIMESTAMP NOT NULL,
    observacoes VARCHAR(2000),
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_atendimento_empresa FOREIGN KEY (empresa_id) REFERENCES empresas(id),
    CONSTRAINT fk_atendimento_agendamento FOREIGN KEY (agendamento_id) REFERENCES agendamentos(id),
    CONSTRAINT fk_atendimento_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id),
    CONSTRAINT fk_atendimento_profissional FOREIGN KEY (profissional_id) REFERENCES usuarios(id),
    CONSTRAINT uk_atendimento_agendamento UNIQUE (agendamento_id),
    CONSTRAINT ck_atendimento_valor CHECK (valor_cobrado >= 0),
    CONSTRAINT ck_atendimento_duracao CHECK (duracao_minutos BETWEEN 1 AND 720),
    CONSTRAINT ck_atendimento_datas CHECK (data_hora_fim > data_hora_inicio)
);

CREATE INDEX IF NOT EXISTS idx_atendimento_empresa_data ON atendimentos(empresa_id, data_hora_inicio);
CREATE INDEX IF NOT EXISTS idx_atendimento_empresa_cliente ON atendimentos(empresa_id, cliente_id, data_hora_inicio);
CREATE INDEX IF NOT EXISTS idx_atendimento_empresa_profissional ON atendimentos(empresa_id, profissional_id, data_hora_inicio);
