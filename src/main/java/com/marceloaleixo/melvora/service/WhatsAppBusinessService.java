package com.marceloaleixo.melvora.service;

import com.marceloaleixo.melvora.dto.ComunicacaoRequests;
import com.marceloaleixo.melvora.entity.*;
import com.marceloaleixo.melvora.entity.enums.ModuloSistema;
import com.marceloaleixo.melvora.entity.enums.WhatsAppIntegrationMode;
import com.marceloaleixo.melvora.exception.RegraNegocioException;
import com.marceloaleixo.melvora.repository.*;
import com.marceloaleixo.melvora.tenant.TenantContext;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

@Service
public class WhatsAppBusinessService {
    private static final Pattern NON_DIGIT = Pattern.compile("\\D");
    private final ConfiguracaoWhatsAppBusinessRepository configRepository;
    private final ComunicacaoAgendadaRepository comunicacaoRepository;
    private final ModuloAcessoService moduloAcessoService;
    private final SecretCryptoService crypto;
    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper objectMapper;

    public WhatsAppBusinessService(ConfiguracaoWhatsAppBusinessRepository configRepository,
                                   ComunicacaoAgendadaRepository comunicacaoRepository,
                                   ModuloAcessoService moduloAcessoService,
                                   SecretCryptoService crypto,
                                   RestClient.Builder restClientBuilder,
                                   ObjectMapper objectMapper) {
        this.configRepository = configRepository;
        this.comunicacaoRepository = comunicacaoRepository;
        this.moduloAcessoService = moduloAcessoService;
        this.crypto = crypto;
        this.restClientBuilder = restClientBuilder;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public ConfiguracaoWhatsAppBusiness configuracao(Long empresaId, Empresa empresa) {
        return configRepository.findByEmpresaId(empresaId).orElseGet(() -> new ConfiguracaoWhatsAppBusiness(empresa));
    }

    @Transactional
    public void salvarConfiguracao(Long empresaId, Empresa empresa, ComunicacaoRequests.WhatsAppBusinessForm form) {
        moduloAcessoService.exigir(ModuloSistema.COMUNICACAO);
        if (!TenantContext.getRequired().equals(empresaId)) throw new RegraNegocioException("Empresa inválida.");

        ConfiguracaoWhatsAppBusiness cfg = configRepository.findByEmpresaId(empresaId)
                .orElseGet(() -> new ConfiguracaoWhatsAppBusiness(empresa));

        if (form.modoIntegracao() == WhatsAppIntegrationMode.N8N) {
            salvarN8n(cfg, form);
        } else {
            salvarMeta(cfg, form);
        }
        configRepository.save(cfg);
    }

    private void salvarMeta(ConfiguracaoWhatsAppBusiness cfg, ComunicacaoRequests.WhatsAppBusinessForm form) {
        String phoneNumberId = value(form.phoneNumberId());
        String token = value(form.accessToken());
        if (phoneNumberId.isBlank()) throw new RegraNegocioException("Informe o Phone Number ID.");
        if (token.isBlank() && blank(cfg.getAccessTokenEncrypted())) {
            throw new RegraNegocioException("Informe o token de acesso do WhatsApp Business.");
        }
        String encrypted = token.isBlank() ? cfg.getAccessTokenEncrypted() : crypto.encrypt(token);
        cfg.atualizarMeta(phoneNumberId, encrypted, normalizarVersao(form.apiVersion()),
                normalizarBase(form.apiBaseUrl()), form.ativa());
    }

    private void salvarN8n(ConfiguracaoWhatsAppBusiness cfg, ComunicacaoRequests.WhatsAppBusinessForm form) {
        String base = normalizarN8nBase(form.n8nBaseUrl());
        String path = normalizarWebhookPath(form.n8nWebhookPath());
        String token = value(form.n8nToken());
        String encrypted = token.isBlank() ? cfg.getN8nTokenEncrypted() : crypto.encrypt(token);
        if (blank(encrypted)) throw new RegraNegocioException("Informe o token de integração do n8n.");

        String key = cfg.getN8nIntegrationKey();
        if (blank(key)) key = UUID.randomUUID().toString().replace("-", "");
        cfg.atualizarN8n(base, path, key, encrypted, form.ativa());
    }

    @Transactional(readOnly = true)
    public String testarConexao(Long empresaId) {
        moduloAcessoService.exigir(ModuloSistema.COMUNICACAO);
        ConfiguracaoWhatsAppBusiness cfg = configRepository.findByEmpresaId(empresaId)
                .orElseThrow(() -> new RegraNegocioException("Configure a integração antes de testar a conexão."));
        validarTenant(empresaId);
        if (cfg.getModoIntegracao() == WhatsAppIntegrationMode.N8N) return testarN8n(cfg);
        return testarMeta(cfg);
    }

    private String testarN8n(ConfiguracaoWhatsAppBusiness cfg) {
        validarN8n(cfg);
        ObjectNode body = objectMapper.createObjectNode();
        body.put("evento", "MELVORA_TESTE_CONEXAO");
        body.put("empresaId", cfg.getEmpresaId());
        body.put("integrationKey", cfg.getN8nIntegrationKey());
        try {
            JsonNode resposta = n8nClient(cfg).post()
                    .uri(normalizarWebhookPath(cfg.getN8nWebhookPath()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(h -> h.setBearerAuth(crypto.decrypt(cfg.getN8nTokenEncrypted())))
                    .body(body)
                    .retrieve().body(JsonNode.class);
            if (resposta != null && resposta.has("ok") && !resposta.path("ok").asBoolean(true)) {
                throw new RegraNegocioException("O workflow n8n respondeu que a conexão não está disponível.");
            }
            return "Conexão com o n8n realizada com sucesso.";
        } catch (RestClientResponseException ex) {
            throw new RegraNegocioException("O n8n recusou a conexão (HTTP " + ex.getStatusCode().value() + "). Verifique URL, webhook, autenticação e o nó Respond to Webhook.");
        }
    }

    private String testarMeta(ConfiguracaoWhatsAppBusiness cfg) {
        validarMeta(cfg);
        try {
            JsonNode resposta = metaClient(cfg).get()
                    .uri(uriBuilder -> uriBuilder.path("/{phoneNumberId}").queryParam("fields", "display_phone_number,verified_name").build(cfg.getPhoneNumberId()))
                    .headers(h -> h.setBearerAuth(crypto.decrypt(cfg.getAccessTokenEncrypted())))
                    .retrieve().body(JsonNode.class);
            String nome = resposta != null && resposta.hasNonNull("verified_name") ? resposta.get("verified_name").asText() : "número configurado";
            String numero = resposta != null && resposta.hasNonNull("display_phone_number") ? resposta.get("display_phone_number").asText() : "número não informado";
            return "Conexão realizada com sucesso. " + nome + " — " + numero;
        } catch (RestClientResponseException ex) {
            throw new RegraNegocioException("WhatsApp Business recusou a conexão (HTTP " + ex.getStatusCode().value() + "). Verifique Phone Number ID, token e versão da API.");
        }
    }

    @Transactional
    public void enviar(Long comunicacaoId, Long empresaId) {
        moduloAcessoService.exigir(ModuloSistema.COMUNICACAO);
        validarTenant(empresaId);
        ComunicacaoAgendada c = comunicacaoRepository.findByIdAndEmpresaId(comunicacaoId, empresaId)
                .orElseThrow(() -> new RegraNegocioException("Comunicação não encontrada."));
        if (c.getStatus() != ComunicacaoAgendada.Status.PENDENTE && c.getStatus() != ComunicacaoAgendada.Status.ABERTA) {
            throw new RegraNegocioException("Somente comunicações pendentes podem ser enviadas.");
        }
        ConfiguracaoWhatsAppBusiness cfg = configRepository.findByEmpresaId(empresaId)
                .orElseThrow(() -> new RegraNegocioException("WhatsApp Business não configurado."));
        try {
            String messageId = cfg.getModoIntegracao() == WhatsAppIntegrationMode.N8N
                    ? enviarViaN8n(cfg, c)
                    : enviarViaMeta(cfg, c.getAgendamento().getCliente().getTelefone(), c.getMensagem());
            c.marcarEnviada(messageId);
        } catch (RestClientResponseException ex) {
            c.marcarErro("HTTP " + ex.getStatusCode().value() + ": " + extrairErro(ex.getResponseBodyAsString()));
            throw new RegraNegocioException("Falha ao enviar a mensagem pelo WhatsApp.");
        }
    }

    @Scheduled(fixedDelayString = "PT1M")
    @Transactional
    public void processarEnviosAutomaticos() {
        LocalDateTime agora = LocalDateTime.now();
        List<ComunicacaoAgendada> fila = comunicacaoRepository.findByStatusAndDataHoraEnvioLessThanEqual(
                ComunicacaoAgendada.Status.PENDENTE, agora);
        for (ComunicacaoAgendada c : fila) {
            Long empresaId = c.getEmpresa().getId();
            ConfiguracaoWhatsAppBusiness cfg = configRepository.findByEmpresaId(empresaId).orElse(null);
            if (cfg == null || !cfg.isAtiva()) continue;
            try {
                String messageId = cfg.getModoIntegracao() == WhatsAppIntegrationMode.N8N
                        ? enviarViaN8n(cfg, c)
                        : enviarViaMeta(cfg, c.getAgendamento().getCliente().getTelefone(), c.getMensagem());
                c.marcarEnviada(messageId);
            } catch (Exception ex) {
                c.marcarErro(mensagemErro(ex));
            }
        }
    }

    private String enviarViaN8n(ConfiguracaoWhatsAppBusiness cfg, ComunicacaoAgendada c) {
        validarN8n(cfg);
        String telefone = normalizarNumero(c.getAgendamento().getCliente().getTelefone());
        ObjectNode body = objectMapper.createObjectNode();
        body.put("evento", "MELVORA_WHATSAPP_ENVIAR");
        body.put("empresaId", cfg.getEmpresaId());
        body.put("integrationKey", cfg.getN8nIntegrationKey());
        body.put("comunicacaoId", c.getId());
        body.put("tipo", c.getTipo().name());
        body.put("telefone", telefone);
        body.put("mensagem", c.getMensagem());
        body.put("callbackUrl", "/webhooks/n8n/whatsapp/" + cfg.getN8nIntegrationKey());
        ObjectNode cliente = body.putObject("cliente");
        cliente.put("id", c.getAgendamento().getCliente().getId());
        cliente.put("nome", c.getAgendamento().getCliente().getNome());

        JsonNode resposta = n8nClient(cfg).post()
                .uri(normalizarWebhookPath(cfg.getN8nWebhookPath()))
                .contentType(MediaType.APPLICATION_JSON)
                .headers(h -> h.setBearerAuth(crypto.decrypt(cfg.getN8nTokenEncrypted())))
                .body(body)
                .retrieve().body(JsonNode.class);
        String providerId = resposta == null ? null : resposta.path("providerMessageId").asText(null);
        if (blank(providerId)) providerId = "n8n:" + c.getId();
        return providerId;
    }

    private String enviarViaMeta(ConfiguracaoWhatsAppBusiness cfg, String telefone, String mensagem) {
        validarMeta(cfg);
        String numero = normalizarNumero(telefone);
        ObjectNode body = objectMapper.createObjectNode();
        body.put("messaging_product", "whatsapp");
        body.put("to", numero);
        body.put("type", "text");
        ObjectNode text = body.putObject("text");
        text.put("preview_url", false);
        text.put("body", mensagem == null ? "" : mensagem);
        JsonNode resposta = metaClient(cfg).post()
                .uri("/{phoneNumberId}/messages", cfg.getPhoneNumberId())
                .contentType(MediaType.APPLICATION_JSON)
                .headers(h -> h.setBearerAuth(crypto.decrypt(cfg.getAccessTokenEncrypted())))
                .body(body)
                .retrieve().body(JsonNode.class);
        if (resposta == null || !resposta.has("messages") || !resposta.get("messages").isArray() || resposta.get("messages").isEmpty()) {
            throw new RegraNegocioException("WhatsApp Business não retornou o ID da mensagem.");
        }
        return resposta.get("messages").get(0).path("id").asText(null);
    }

    @Transactional
    public void atualizarStatusViaN8n(String integrationKey, String authorizationHeader, Long comunicacaoId,
                                      String status, String providerMessageId, String erro) {
        ConfiguracaoWhatsAppBusiness cfg = configRepository.findByN8nIntegrationKey(integrationKey)
                .orElseThrow(() -> new RegraNegocioException("Integração n8n não encontrada."));
        validarTokenCallback(cfg, authorizationHeader);
        ComunicacaoAgendada c = comunicacaoRepository.findByIdAndEmpresaId(comunicacaoId, cfg.getEmpresaId())
                .orElseThrow(() -> new RegraNegocioException("Comunicação não encontrada para esta integração."));
        if (providerMessageId != null && !providerMessageId.isBlank()) c.marcarEnviada(providerMessageId);
        switch (status == null ? "" : status.toUpperCase()) {
            case "ENVIADA", "SENT" -> { if (providerMessageId != null && !providerMessageId.isBlank()) c.marcarEnviada(providerMessageId); }
            case "ENTREGUE", "DELIVERED" -> c.marcarEntregue();
            case "LIDA", "READ" -> c.marcarLida();
            case "ERRO", "FAILED", "ERROR" -> c.marcarErro(erro);
            case "CANCELADA", "CANCELED", "CANCELLED" -> c.cancelar();
            default -> throw new RegraNegocioException("Status de WhatsApp inválido: " + status);
        }
    }

    public String callbackPath(ConfiguracaoWhatsAppBusiness cfg) {
        return cfg.getN8nIntegrationKey() == null ? "" : "/webhooks/n8n/whatsapp/" + cfg.getN8nIntegrationKey();
    }

    private void validarTenant(Long empresaId) {
        if (!TenantContext.getRequired().equals(empresaId)) throw new RegraNegocioException("Empresa inválida.");
    }

    private void validarMeta(ConfiguracaoWhatsAppBusiness cfg) {
        if (cfg == null || blank(cfg.getPhoneNumberId()) || blank(cfg.getAccessTokenEncrypted())) {
            throw new RegraNegocioException("Configuração da WhatsApp Business Cloud incompleta.");
        }
    }

    private void validarN8n(ConfiguracaoWhatsAppBusiness cfg) {
        if (cfg == null || blank(cfg.getN8nBaseUrl()) || blank(cfg.getN8nWebhookPath()) || blank(cfg.getN8nIntegrationKey()) || blank(cfg.getN8nTokenEncrypted())) {
            throw new RegraNegocioException("Configuração do n8n incompleta.");
        }
    }

    private void validarTokenCallback(ConfiguracaoWhatsAppBusiness cfg, String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new RegraNegocioException("Autenticação da integração n8n inválida.");
        }
        String provided = authorizationHeader.substring(7).trim();
        String expected = crypto.decrypt(cfg.getN8nTokenEncrypted());
        if (!MessageDigest.isEqual(provided.getBytes(StandardCharsets.UTF_8), expected.getBytes(StandardCharsets.UTF_8))) {
            throw new RegraNegocioException("Autenticação da integração n8n inválida.");
        }
    }

    private RestClient metaClient(ConfiguracaoWhatsAppBusiness cfg) {
        return restClientBuilder.baseUrl(normalizarBase(cfg.getApiBaseUrl()) + "/" + normalizarVersao(cfg.getApiVersion())).build();
    }

    private RestClient n8nClient(ConfiguracaoWhatsAppBusiness cfg) {
        return restClientBuilder.baseUrl(normalizarN8nBase(cfg.getN8nBaseUrl())).build();
    }

    private String normalizarNumero(String telefone) {
        if (telefone == null || telefone.isBlank()) throw new RegraNegocioException("A cliente não possui telefone cadastrado.");
        String numero = NON_DIGIT.matcher(telefone).replaceAll("");
        if (numero.length() == 10 || numero.length() == 11) numero = "55" + numero;
        if (!numero.startsWith("55") || numero.length() < 12 || numero.length() > 13) throw new RegraNegocioException("Telefone inválido para WhatsApp Business.");
        return numero;
    }

    private String normalizarVersao(String valor) {
        String v = value(valor);
        if (v.isBlank()) v = "v23.0";
        if (!v.matches("v\\d+\\.\\d+")) throw new RegraNegocioException("Versão da Graph API inválida.");
        return v;
    }

    private String normalizarBase(String valor) {
        String v = value(valor);
        if (v.isBlank()) v = "https://graph.facebook.com";
        if (!v.startsWith("https://")) throw new RegraNegocioException("A URL da API deve usar HTTPS.");
        return v.replaceAll("/+$", "");
    }

    private String normalizarN8nBase(String valor) {
        String v = value(valor);
        if (v.isBlank()) throw new RegraNegocioException("Informe a URL base do n8n.");
        if (!v.startsWith("https://")) throw new RegraNegocioException("A URL do n8n deve usar HTTPS.");
        return v.replaceAll("/+$", "");
    }

    private String normalizarWebhookPath(String valor) {
        String v = value(valor);
        if (v.isBlank()) throw new RegraNegocioException("Informe o caminho do webhook do n8n.");
        if (!v.startsWith("/")) v = "/" + v;
        if (v.contains("..") || v.contains(" ")) throw new RegraNegocioException("Caminho do webhook inválido.");
        return v;
    }

    private String extrairErro(String body) {
        try {
            JsonNode n = objectMapper.readTree(body);
            JsonNode error = n.path("error");
            if (error.hasNonNull("message")) return error.get("message").asText();
            if (n.hasNonNull("message")) return n.get("message").asText();
        } catch (Exception ignored) {}
        return "Resposta de erro não detalhada.";
    }

    private String mensagemErro(Exception ex) {
        if (ex instanceof RestClientResponseException r) return "HTTP " + r.getStatusCode().value() + ": " + extrairErro(r.getResponseBodyAsString());
        return ex.getMessage() == null ? "Erro ao enviar mensagem." : ex.getMessage();
    }

    private static String value(String v) { return v == null ? "" : v.trim(); }
    private static boolean blank(String v) { return v == null || v.isBlank(); }
}
