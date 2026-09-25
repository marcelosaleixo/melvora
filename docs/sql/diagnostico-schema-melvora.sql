-- ============================================================
-- DIAGNÓSTICO DO SCHEMA MELVORA
-- Execute no PostgreSQL do ambiente que está apresentando erro.
-- Este script NÃO altera dados nem estrutura.
-- ============================================================

-- 1) Versões aplicadas pelo Flyway
SELECT installed_rank,
       version,
       description,
       type,
       script,
       checksum,
       installed_on,
       success
FROM flyway_schema_history
ORDER BY installed_rank;

-- 2) Tabelas esperadas pelo domínio atual
WITH esperadas(nome) AS (
    VALUES
      ('empresas'),
      ('usuarios'),
      ('produtos'),
      ('produtos_mega_hair'),
      ('clientes'),
      ('lotes_mega_hair'),
      ('aplicacoes_mega_hair'),
      ('aplicacao_lotes'),
      ('movimentacoes_estoque'),
      ('manutencoes_mega_hair'),
      ('empresa_modulos'),
      ('agendamentos'),
      ('servicos'),
      ('servicos_profissionais'),
      ('atendimentos'),
      ('lancamentos_financeiros'),
      ('configuracoes_comissao'),
      ('comissoes'),
      ('configuracoes_comunicacao'),
      ('comunicacoes_agendadas'),
      ('templates_whatsapp'),
      ('configuracoes_whatsapp_business'),
      ('configuracoes_whatsapp_automacao'),
      ('whatsapp_mensagens'),
      ('agenda_horarios_funcionamento'),
      ('whatsapp_reservas_pendentes'),
      ('whatsapp_agendamento_acoes_pendentes'),
      ('whatsapp_confirmacoes_pendentes'),
      ('avaliacoes_atendimento'),
      ('preferencias_comunicacao_contato'),
      ('retencao_envios'),
      ('campanhas_comunicacao'),
      ('campanhas_envios')
)
SELECT e.nome,
       CASE WHEN c.table_name IS NULL THEN 'AUSENTE' ELSE 'OK' END AS status
FROM esperadas e
LEFT JOIN information_schema.tables c
       ON c.table_schema = 'public'
      AND c.table_name = e.nome
ORDER BY e.nome;

-- 3) Estrutura específica que motivou a V40
SELECT column_name,
       data_type,
       is_nullable,
       column_default
FROM information_schema.columns
WHERE table_schema = 'public'
  AND table_name = 'agenda_horarios_funcionamento'
ORDER BY ordinal_position;

-- 4) Constraints da tabela de horários
SELECT con.conname,
       pg_get_constraintdef(con.oid) AS definicao
FROM pg_constraint con
JOIN pg_class rel ON rel.oid = con.conrelid
JOIN pg_namespace nsp ON nsp.oid = rel.relnamespace
WHERE nsp.nspname = 'public'
  AND rel.relname = 'agenda_horarios_funcionamento'
ORDER BY con.conname;
