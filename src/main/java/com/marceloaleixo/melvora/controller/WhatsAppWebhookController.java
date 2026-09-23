package com.marceloaleixo.melvora.controller;

import com.marceloaleixo.melvora.entity.ComunicacaoAgendada;
import com.marceloaleixo.melvora.repository.ComunicacaoAgendadaRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;

@RestController
@RequestMapping("/webhooks/whatsapp")
public class WhatsAppWebhookController {
    private final ComunicacaoAgendadaRepository repository;
    private final String verifyToken;

    public WhatsAppWebhookController(ComunicacaoAgendadaRepository repository,
                                     @Value("${melvora.whatsapp.webhook-verify-token:}") String verifyToken) {
        this.repository = repository; this.verifyToken = verifyToken;
    }

    @GetMapping
    public ResponseEntity<String> verify(@RequestParam(name="hub.mode", required=false) String mode,
                                         @RequestParam(name="hub.verify_token", required=false) String token,
                                         @RequestParam(name="hub.challenge", required=false) String challenge) {
        if ("subscribe".equals(mode) && !verifyToken.isBlank() && verifyToken.equals(token)) return ResponseEntity.ok(challenge == null ? "" : challenge);
        return ResponseEntity.status(403).body("forbidden");
    }

    @PostMapping
    public ResponseEntity<Void> webhook(@RequestBody JsonNode payload) {
        JsonNode entries = payload.path("entry");
        if (entries.isArray()) for (JsonNode entry : entries) {
            JsonNode changes = entry.path("changes");
            if (changes.isArray()) for (JsonNode change : changes) processStatuses(change.path("value").path("statuses"));
        }
        return ResponseEntity.ok().build();
    }

    private void processStatuses(JsonNode statuses) {
        if (!statuses.isArray()) return;
        for (JsonNode status : statuses) {
            String id = status.path("id").asText(null);
            if (id == null) continue;
            repository.findByProviderMessageId(id).ifPresent(c -> atualizar(c, status));
        }
    }

    private void atualizar(ComunicacaoAgendada c, JsonNode status) {
        String estado = status.path("status").asText("");
        switch (estado) {
            case "delivered" -> c.marcarEntregue();
            case "read" -> c.marcarLida();
            case "failed" -> c.marcarErro(status.path("errors").toString());
            default -> { }
        }
        repository.save(c);
    }
}
