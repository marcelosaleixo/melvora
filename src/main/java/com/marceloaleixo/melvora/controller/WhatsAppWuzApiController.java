package com.marceloaleixo.melvora.controller;

import com.marceloaleixo.melvora.entity.enums.ModuloSistema;
import com.marceloaleixo.melvora.exception.RegraNegocioException;
import com.marceloaleixo.melvora.service.WhatsAppBusinessService;
import com.marceloaleixo.melvora.service.ModuloAcessoService;
import com.marceloaleixo.melvora.tenant.TenantContext;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/comunicacao/whatsapp-business/wuzapi")
public class WhatsAppWuzApiController {
    private final WhatsAppBusinessService service;
    private final ModuloAcessoService modulo;
    private final ObjectMapper objectMapper;

    public WhatsAppWuzApiController(WhatsAppBusinessService service, ModuloAcessoService modulo, ObjectMapper objectMapper) {
        this.service = service;
        this.modulo = modulo;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/conectar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> conectar() {
        exigirAdmin();
        return ResponseEntity.ok(service.conectarWuzapi(TenantContext.getRequired()));
    }

    @GetMapping("/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> status() {
        exigirAdmin();
        return ResponseEntity.ok(service.statusWuzapi(TenantContext.getRequired()));
    }

    @GetMapping("/qr")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> qr() {
        exigirAdmin();
        return ResponseEntity.ok(Map.of("qrCode", service.qrWuzapi(TenantContext.getRequired())));
    }

    @PostMapping("/desconectar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> desconectar(@RequestParam(defaultValue = "false") boolean logout) {
        exigirAdmin();
        return ResponseEntity.ok(Map.of("mensagem", service.desconectarWuzapi(TenantContext.getRequired(), logout)));
    }

    private void exigirAdmin() {
        modulo.exigir(ModuloSistema.COMUNICACAO);
    }
}
