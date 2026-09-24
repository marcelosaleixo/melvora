CREATE TABLE IF NOT EXISTS whatsapp_reservas_pendentes (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL,
    cliente_id BIGINT NOT NULL,
    servico_id BIGINT NOT NULL,
    telefone VARCHAR(30) NOT NULL,
    data_desejada DATE NOT NULL,
    opcoes_json TEXT NOT NULL,
    profissional_id BIGINT,
    inicio_selecionado TIMESTAMP,
    fim_selecionado TIMESTAMP,
    status VARCHAR(30) NOT NULL,
    expira_em TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_whatsapp_reserva_empresa FOREIGN KEY (empresa_id) REFERENCES empresas(id),
    CONSTRAINT fk_whatsapp_reserva_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id),
    CONSTRAINT fk_whatsapp_reserva_servico FOREIGN KEY (servico_id) REFERENCES servicos(id),
    CONSTRAINT ck_whatsapp_reserva_status CHECK (status IN ('AGUARDANDO_HORARIO','AGUARDANDO_CONFIRMACAO','CONFIRMADA','EXPIRADA','CANCELADA'))
);
CREATE INDEX IF NOT EXISTS idx_whatsapp_reserva_empresa_telefone_status ON whatsapp_reservas_pendentes(empresa_id, telefone, status);
CREATE INDEX IF NOT EXISTS idx_whatsapp_reserva_expira ON whatsapp_reservas_pendentes(status, expira_em);
