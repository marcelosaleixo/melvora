-- ADMIN e SUPER_ADMIN são perfis protegidos e não podem ser desativados pela aplicação.
-- Corrige registros existentes que tenham ficado inativos antes desta regra.
UPDATE usuarios
SET ativo = TRUE
WHERE role IN ('ADMIN', 'SUPER_ADMIN')
  AND ativo = FALSE;
