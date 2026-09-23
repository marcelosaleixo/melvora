package com.marceloaleixo.melvora.entity.enums;

public enum WhatsAppIntegrationMode {
    META_CLOUD("WhatsApp Business Cloud API"),
    N8N("n8n + WhatsApp Business");

    private final String label;

    WhatsAppIntegrationMode(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
