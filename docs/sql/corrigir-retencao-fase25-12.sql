-- CORREÇÃO EMERGENCIAL - FASE 25.12
-- Use somente se precisar corrigir o banco imediatamente antes de reiniciar o Melvora.
-- Depois reinicie a aplicação para o Flyway registrar/aplicar a V38 normalmente.

ALTER TABLE configuracoes_comunicacao
    ADD COLUMN IF NOT EXISTS retencao_automatica_ativa BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS retencao_cooldown_dias INTEGER NOT NULL DEFAULT 30,
    ADD COLUMN IF NOT EXISTS retencao_horario_inicio TIME NOT NULL DEFAULT '09:00',
    ADD COLUMN IF NOT EXISTS retencao_horario_fim TIME NOT NULL DEFAULT '19:00',
    ADD COLUMN IF NOT EXISTS retencao_max_envios_dia INTEGER NOT NULL DEFAULT 20;

SELECT column_name
FROM information_schema.columns
WHERE table_schema = 'public'
  AND table_name = 'configuracoes_comunicacao'
  AND column_name LIKE 'retencao_%'
ORDER BY column_name;
