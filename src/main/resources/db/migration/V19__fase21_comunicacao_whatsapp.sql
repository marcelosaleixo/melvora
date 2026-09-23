-- FASE 21 - Comunicação e WhatsApp
-- Módulo comercial para links seguros de WhatsApp, confirmações e lembretes.
DO $$
DECLARE c record;
BEGIN
  FOR c IN
    SELECT con.conname
      FROM pg_constraint con
      JOIN pg_class rel ON rel.oid = con.conrelid
      JOIN pg_namespace nsp ON nsp.oid = rel.relnamespace
     WHERE rel.relname = 'empresa_modulos'
       AND nsp.nspname = current_schema()
       AND con.contype = 'c'
       AND pg_get_constraintdef(con.oid) ILIKE '%modulo%'
  LOOP
    EXECUTE format('ALTER TABLE empresa_modulos DROP CONSTRAINT IF EXISTS %I', c.conname);
  END LOOP;
END $$;

ALTER TABLE empresa_modulos
ADD CONSTRAINT empresa_modulos_modulo_check CHECK (modulo IN (
 'CLIENTES','PRODUTOS','ESTOQUE','MEGA_HAIR','EQUIPE','HISTORICO','AGENDA','FINANCEIRO','SERVICOS','COMISSOES','RELATORIOS','COMUNICACAO'
));

INSERT INTO empresa_modulos (empresa_id, modulo, ativo)
SELECT e.id, 'COMUNICACAO', false
FROM empresas e
WHERE NOT EXISTS (SELECT 1 FROM empresa_modulos em WHERE em.empresa_id=e.id AND em.modulo='COMUNICACAO');
