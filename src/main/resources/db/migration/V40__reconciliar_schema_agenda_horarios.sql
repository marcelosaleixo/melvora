-- ============================================================
-- MELVORA - Reparação idempotente do schema
--
-- Motivo:
-- Alguns bancos legados podem possuir a V30 registrada em
-- flyway_schema_history sem que a tabela física tenha sido criada.
-- Esta migration NÃO altera o histórico do Flyway e NÃO utiliza
-- Hibernate para criar objetos. Ela apenas reconcilia o objeto que
-- a entidade atual exige.
-- ============================================================

CREATE TABLE IF NOT EXISTS agenda_horarios_funcionamento (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL,
    dia_semana INTEGER NOT NULL,
    hora_inicio TIME NOT NULL,
    hora_fim TIME NOT NULL,
    intervalo_minutos INTEGER NOT NULL DEFAULT 15,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_agenda_horario_empresa_dia UNIQUE (empresa_id, dia_semana),
    CONSTRAINT fk_agenda_horario_empresa
        FOREIGN KEY (empresa_id) REFERENCES empresas(id) ON DELETE CASCADE,
    CONSTRAINT ck_agenda_horario_dia
        CHECK (dia_semana BETWEEN 1 AND 7),
    CONSTRAINT ck_agenda_horario_horas
        CHECK (hora_fim > hora_inicio),
    CONSTRAINT ck_agenda_horario_intervalo
        CHECK (intervalo_minutos BETWEEN 5 AND 240)
);

CREATE INDEX IF NOT EXISTS idx_agenda_horario_empresa
    ON agenda_horarios_funcionamento(empresa_id);
