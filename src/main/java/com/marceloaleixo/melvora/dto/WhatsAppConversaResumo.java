package com.marceloaleixo.melvora.dto;

import com.marceloaleixo.melvora.entity.WhatsAppMensagem;
import java.time.LocalDateTime;

public record WhatsAppConversaResumo(
        Long clienteId,
        String clienteNome,
        String telefone,
        String ultimaMensagem,
        WhatsAppMensagem.Direcao ultimaDirecao,
        LocalDateTime ultimaData,
        long naoLidas) {

    public String nomeExibicao() {
        return clienteNome == null || clienteNome.isBlank() ? telefone : clienteNome;
    }

    public boolean possuiCliente() {
        return clienteId != null;
    }
}
