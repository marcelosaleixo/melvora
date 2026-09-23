-- FASE 13: vínculo seguro entre atendimentos concluídos e registros operacionais de Mega Hair.
ALTER TABLE aplicacoes_mega_hair ADD COLUMN IF NOT EXISTS agendamento_id BIGINT;
ALTER TABLE aplicacoes_mega_hair DROP CONSTRAINT IF EXISTS fk_aplicacao_agendamento;
ALTER TABLE aplicacoes_mega_hair ADD CONSTRAINT fk_aplicacao_agendamento FOREIGN KEY (agendamento_id) REFERENCES agendamentos(id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_aplicacao_agendamento ON aplicacoes_mega_hair(agendamento_id) WHERE agendamento_id IS NOT NULL;

ALTER TABLE manutencoes_mega_hair ADD COLUMN IF NOT EXISTS agendamento_id BIGINT;
ALTER TABLE manutencoes_mega_hair DROP CONSTRAINT IF EXISTS fk_manutencao_agendamento;
ALTER TABLE manutencoes_mega_hair ADD CONSTRAINT fk_manutencao_agendamento FOREIGN KEY (agendamento_id) REFERENCES agendamentos(id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_manutencao_agendamento ON manutencoes_mega_hair(agendamento_id) WHERE agendamento_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_aplicacao_agendamento ON aplicacoes_mega_hair(agendamento_id);
CREATE INDEX IF NOT EXISTS idx_manutencao_agendamento ON manutencoes_mega_hair(agendamento_id);
