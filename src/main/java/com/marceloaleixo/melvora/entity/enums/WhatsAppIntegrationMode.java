package com.marceloaleixo.melvora.entity.enums;

public enum WhatsAppIntegrationMode {
    META_CLOUD("WhatsApp Business Cloud API"),
    EVOLUTION_API("Evolution API"),
    WUZAPI("WuzAPI Manager"),
    /** Mantido somente para compatibilidade com instalações anteriores. Não é exibido na nova tela. */
    N8N("n8n + WhatsApp Business");

    private final String label;

    WhatsAppIntegrationMode(String label) {
        this.label = label;
    }

    public String getLabel() { return label; }

    public boolean isLegacy() { return this == N8N; }
}
