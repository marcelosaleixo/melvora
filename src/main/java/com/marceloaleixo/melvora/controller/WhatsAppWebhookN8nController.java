package com.marceloaleixo.melvora.controller;

import com.marceloaleixo.melvora.exception.RegraNegocioException;
import com.marceloaleixo.melvora.service.WhatsAppBusinessService;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;

@RestController
@RequestMapping("/webhooks/n8n/whatsapp")
public class WhatsAppWebhookN8nController {
    private final WhatsAppBusinessService service;

    public WhatsAppWebhookN8nController(WhatsAppBusinessService service) {
        this.service = service;
    }

    @PostMapping("/{integrationKey}")
    public ResponseEntity<Map<String, Object>> status(
            @PathVariable String integrationKey,
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestBody JsonNode payload) {
        if (payload == null || !payload.hasNonNull("comunicacaoId")) {
            throw new RegraNegocioException("comunicacaoId é obrigatório.");
        }
        Long comunicacaoId = payload.path("comunicacaoId").asLong(0);
        if (comunicacaoId <= 0) throw new RegraNegocioException("comunicacaoId inválido.");
        service.atualizarStatusViaN8n(
                integrationKey,
                authorization,
                comunicacaoId,
                payload.path("status").asText("ENVIADA"),
                payload.path("providerMessageId").asText(null),
                payload.path("erro").asText(null));
        return ResponseEntity.ok(Map.of("ok", true));
    }
}
