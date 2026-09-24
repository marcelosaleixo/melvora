package com.marceloaleixo.melvora.controller;

import com.marceloaleixo.melvora.exception.RegraNegocioException;
import com.marceloaleixo.melvora.service.WhatsAppBusinessService;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/webhooks/wuzapi")
public class WhatsAppWebhookWuzApiController {
    private final WhatsAppBusinessService service;
    private final ObjectMapper objectMapper;

    public WhatsAppWebhookWuzApiController(WhatsAppBusinessService service, ObjectMapper objectMapper) {
        this.service = service;
        this.objectMapper = objectMapper;
    }

    @PostMapping(value = "/{integrationKey}", consumes = {"application/json", "application/x-www-form-urlencoded", "text/plain"})
    public ResponseEntity<Void> receber(
            @PathVariable String integrationKey,
            @RequestHeader(name = "x-hmac-signature", required = false) String signature,
            @RequestBody(required = false) String rawBody) {
        if (rawBody == null || rawBody.isBlank()) throw new RegraNegocioException("Payload WuzAPI vazio.");
        JsonNode payload = parsePayload(rawBody);
        service.processarWebhookWuzapi(integrationKey, signature, rawBody, payload);
        return ResponseEntity.ok().build();
    }

    private JsonNode parsePayload(String rawBody) {
        String trimmed = rawBody.trim();
        try {
            if (trimmed.startsWith("{") || trimmed.startsWith("[")) return objectMapper.readTree(trimmed);
            for (String part : trimmed.split("&")) {
                int separator = part.indexOf('=');
                if (separator <= 0) continue;
                String key = URLDecoder.decode(part.substring(0, separator), StandardCharsets.UTF_8);
                if ("jsonData".equals(key)) {
                    String json = URLDecoder.decode(part.substring(separator + 1), StandardCharsets.UTF_8);
                    return objectMapper.readTree(json);
                }
            }
        } catch (Exception ex) {
            throw new RegraNegocioException("Payload do webhook WuzAPI inválido.");
        }
        throw new RegraNegocioException("Formato de webhook WuzAPI não suportado.");
    }
}
