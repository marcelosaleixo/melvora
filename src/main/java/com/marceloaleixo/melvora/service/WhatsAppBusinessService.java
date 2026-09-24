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
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
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
    private final WhatsAppMensagemService whatsappMensagemService;
    private final String publicBaseUrl;

    public WhatsAppBusinessService(ConfiguracaoWhatsAppBusinessRepository configRepository,
                                   ComunicacaoAgendadaRepository comunicacaoRepository,
                                   ModuloAcessoService moduloAcessoService,
                                   SecretCryptoService crypto,
                                   RestClient.Builder restClientBuilder,
                                   ObjectMapper objectMapper,
                                   WhatsAppMensagemService whatsappMensagemService,
                                   @org.springframework.beans.factory.annotation.Value("${melvora.public-base-url:}") String publicBaseUrl) {
        this.configRepository = configRepository;
        this.comunicacaoRepository = comunicacaoRepository;
        this.moduloAcessoService = moduloAcessoService;
        this.crypto = crypto;
        this.restClientBuilder = restClientBuilder;
        this.objectMapper = objectMapper;
        this.whatsappMensagemService = whatsappMensagemService;
        this.publicBaseUrl = publicBaseUrl == null ? "" : publicBaseUrl.replaceAll("/+$", "");
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

        switch (form.modoIntegracao()) {
            case META_CLOUD -> salvarMeta(cfg, form);
            case EVOLUTION_API -> salvarEvolution(cfg, form);
            case WUZAPI -> salvarWuzapi(cfg, form);
            case N8N -> salvarN8n(cfg, form); // compatibilidade com instalações anteriores
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

    private void salvarEvolution(ConfiguracaoWhatsAppBusiness cfg, ComunicacaoRequests.WhatsAppBusinessForm form) {
        String base = normalizarBaseGenerica(form.evolutionBaseUrl(), "Evolution API");
        String instance = value(form.evolutionInstance());
        String apiKey = value(form.evolutionApiKey());
        if (instance.isBlank()) throw new RegraNegocioException("Informe o nome da instância da Evolution API.");
        if (apiKey.isBlank() && blank(cfg.getEvolutionApiKeyEncrypted())) {
            throw new RegraNegocioException("Informe a API Key da Evolution API.");
        }
        String encrypted = apiKey.isBlank() ? cfg.getEvolutionApiKeyEncrypted() : crypto.encrypt(apiKey);
        cfg.atualizarEvolution(base, encrypted, instance, form.ativa());
    }

    private void salvarWuzapi(ConfiguracaoWhatsAppBusiness cfg, ComunicacaoRequests.WhatsAppBusinessForm form) {
        String base = normalizarBaseGenerica(form.wuzapiBaseUrl(), "WuzAPI");
        String token = value(form.wuzapiToken());
        if (token.isBlank() && blank(cfg.getWuzapiTokenEncrypted())) {
            throw new RegraNegocioException("Informe o token da sessão do WuzAPI.");
        }
        String encrypted = token.isBlank() ? cfg.getWuzapiTokenEncrypted() : crypto.encrypt(token);
        String integrationKey = cfg.getWuzapiIntegrationKey();
        if (blank(integrationKey)) integrationKey = UUID.randomUUID().toString().replace("-", "");
        String hmacEncrypted = cfg.getWuzapiHmacSecretEncrypted();
        if (blank(hmacEncrypted)) hmacEncrypted = crypto.encrypt(UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", ""));
        cfg.atualizarWuzapi(base, encrypted, integrationKey, hmacEncrypted, form.ativa());
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
        return switch (cfg.getModoIntegracao()) {
            case META_CLOUD -> testarMeta(cfg);
            case EVOLUTION_API -> testarEvolution(cfg);
            case WUZAPI -> testarWuzapi(cfg);
            case N8N -> testarN8n(cfg);
        };
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

    private String testarEvolution(ConfiguracaoWhatsAppBusiness cfg) {
        validarEvolution(cfg);
        try {
            JsonNode resposta = evolutionClient(cfg).get()
                    .uri("/instance/connectionState/{instance}", cfg.getEvolutionInstance())
                    .header("apikey", crypto.decrypt(cfg.getEvolutionApiKeyEncrypted()))
                    .retrieve().body(JsonNode.class);
            String state = resposta == null ? null : resposta.path("instance").path("state").asText(null);
            if (blank(state)) state = resposta == null ? "desconhecido" : resposta.path("state").asText("desconhecido");
            return "Evolution API conectada. Estado da instância: " + state + ".";
        } catch (RestClientResponseException ex) {
            throw new RegraNegocioException("Evolution API recusou a conexão (HTTP " + ex.getStatusCode().value() + "). Verifique URL, API Key e nome da instância.");
        }
    }

    private String testarWuzapi(ConfiguracaoWhatsAppBusiness cfg) {
        validarWuzapi(cfg);
        try {
            JsonNode resposta = wuzapiClient(cfg).get()
                    .uri("/session/status")
                    .header("Token", crypto.decrypt(cfg.getWuzapiTokenEncrypted()))
                    .retrieve().body(JsonNode.class);
            JsonNode data = resposta == null ? null : resposta.path("data");
            boolean connected = data != null && data.path("Connected").asBoolean(false);
            boolean loggedIn = data != null && data.path("LoggedIn").asBoolean(false);
            return "WuzAPI respondendo. Conectado: " + (connected ? "sim" : "não") + "; sessão pronta: " + (loggedIn ? "sim" : "não") + ".";
        } catch (RestClientResponseException ex) {
            throw new RegraNegocioException("WuzAPI recusou a conexão (HTTP " + ex.getStatusCode().value() + "). Verifique URL e token da sessão.");
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
            String messageId = enviarPeloProvedorConfigurado(cfg, c);
            c.marcarEnviada(messageId);
            whatsappMensagemService.registrarSaida(cfg, c.getAgendamento().getCliente(), c.getAgendamento().getCliente().getTelefone(), c.getMensagem(), messageId);
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
                String messageId = enviarPeloProvedorConfigurado(cfg, c);
                c.marcarEnviada(messageId);
                whatsappMensagemService.registrarSaida(cfg, c.getAgendamento().getCliente(), c.getAgendamento().getCliente().getTelefone(), c.getMensagem(), messageId);
            } catch (Exception ex) {
                c.marcarErro(mensagemErro(ex));
            }
        }
    }

    @Transactional
    public String enviarMensagemAutomatica(Long empresaId, Cliente cliente, String telefone, String mensagem) {
        moduloAcessoService.exigir(ModuloSistema.COMUNICACAO);
        validarTenant(empresaId);
        if (mensagem == null || mensagem.isBlank()) throw new RegraNegocioException("Mensagem automática vazia.");
        ConfiguracaoWhatsAppBusiness cfg = configRepository.findByEmpresaId(empresaId)
                .orElseThrow(() -> new RegraNegocioException("WhatsApp Business não configurado."));
        if (!cfg.isAtiva()) throw new RegraNegocioException("A integração WhatsApp está desativada.");
        String numero = normalizarNumero(telefone);
        String providerId = switch (cfg.getModoIntegracao()) {
            case META_CLOUD -> enviarViaMeta(cfg, numero, mensagem);
            case EVOLUTION_API -> enviarViaEvolutionTexto(cfg, numero, mensagem);
            case WUZAPI -> enviarViaWuzapiTexto(cfg, numero, mensagem);
            case N8N -> throw new RegraNegocioException("A automação inteligente não está disponível para a integração n8n legada.");
        };
        if (blank(providerId)) providerId = "melvora:auto:" + UUID.randomUUID();
        whatsappMensagemService.registrarSaida(cfg, cliente, numero, mensagem, providerId);
        return providerId;
    }

    /** Envio usado por processos agendados do próprio sistema. Não depende de TenantContext HTTP. */
    @Transactional
    public String enviarMensagemAutomaticaInterna(Long empresaId, Cliente cliente, String telefone, String mensagem) {
        moduloAcessoService.exigir(empresaId, ModuloSistema.COMUNICACAO);
        if (cliente == null || cliente.getEmpresa() == null || !empresaId.equals(cliente.getEmpresa().getId()) || !cliente.isAtivo()) {
            throw new RegraNegocioException("Cliente inválida para comunicação automática.");
        }
        if (mensagem == null || mensagem.isBlank()) throw new RegraNegocioException("Mensagem automática vazia.");
        ConfiguracaoWhatsAppBusiness cfg = configRepository.findByEmpresaId(empresaId)
                .orElseThrow(() -> new RegraNegocioException("WhatsApp Business não configurado."));
        if (!cfg.isAtiva()) throw new RegraNegocioException("A integração WhatsApp está desativada.");
        String numero = normalizarNumero(telefone);
        String providerId = switch (cfg.getModoIntegracao()) {
            case META_CLOUD -> enviarViaMeta(cfg, numero, mensagem);
            case EVOLUTION_API -> enviarViaEvolutionTexto(cfg, numero, mensagem);
            case WUZAPI -> enviarViaWuzapiTexto(cfg, numero, mensagem);
            case N8N -> throw new RegraNegocioException("A automação automática não está disponível para a integração n8n legada.");
        };
        if (blank(providerId)) providerId = "melvora:retencao:" + UUID.randomUUID();
        whatsappMensagemService.registrarSaida(cfg, cliente, numero, mensagem, providerId);
        return providerId;
    }

    @Transactional
    public String enviarMensagemDireta(Long empresaId, Cliente cliente, String mensagem) {
        moduloAcessoService.exigir(ModuloSistema.COMUNICACAO);
        validarTenant(empresaId);
        if (cliente == null || cliente.getEmpresa() == null || !empresaId.equals(cliente.getEmpresa().getId()) || !cliente.isAtivo()) {
            throw new RegraNegocioException("Cliente inválida para comunicação.");
        }
        if (mensagem == null || mensagem.isBlank()) throw new RegraNegocioException("Digite uma mensagem antes de enviar.");
        if (mensagem.length() > 4000) throw new RegraNegocioException("A mensagem deve ter no máximo 4000 caracteres.");
        ConfiguracaoWhatsAppBusiness cfg = configRepository.findByEmpresaId(empresaId)
                .orElseThrow(() -> new RegraNegocioException("WhatsApp Business não configurado."));
        String telefone = normalizarNumero(cliente.getTelefone());
        String providerId = switch (cfg.getModoIntegracao()) {
            case META_CLOUD -> enviarViaMeta(cfg, telefone, mensagem);
            case EVOLUTION_API -> enviarViaEvolutionTexto(cfg, telefone, mensagem);
            case WUZAPI -> enviarViaWuzapiTexto(cfg, telefone, mensagem);
            case N8N -> throw new RegraNegocioException("A central de conversas não está disponível para a integração n8n legada.");
        };
        if (blank(providerId)) providerId = "melvora:" + UUID.randomUUID();
        whatsappMensagemService.registrarSaida(cfg, cliente, telefone, mensagem, providerId);
        return providerId;
    }

    private String enviarPeloProvedorConfigurado(ConfiguracaoWhatsAppBusiness cfg, ComunicacaoAgendada c) {
        return switch (cfg.getModoIntegracao()) {
            case META_CLOUD -> enviarViaMeta(cfg, c.getAgendamento().getCliente().getTelefone(), c.getMensagem());
            case N8N -> enviarViaN8n(cfg, c);
            case EVOLUTION_API -> enviarViaEvolution(cfg, c);
            case WUZAPI -> enviarViaWuzapi(cfg, c);
        };
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

    private String enviarViaEvolutionTexto(ConfiguracaoWhatsAppBusiness cfg, String numero, String mensagem) {
        validarEvolution(cfg);
        ObjectNode body = objectMapper.createObjectNode();
        body.put("number", numero);
        body.put("text", mensagem == null ? "" : mensagem);
        try {
            JsonNode resposta = evolutionClient(cfg).post()
                    .uri("/message/sendText/{instance}", cfg.getEvolutionInstance())
                    .header("apikey", crypto.decrypt(cfg.getEvolutionApiKeyEncrypted()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve().body(JsonNode.class);
            String id = resposta == null ? null : resposta.path("key").path("id").asText(null);
            if (blank(id) && resposta != null) id = resposta.path("messageId").asText(null);
            if (blank(id)) id = "evolution:" + UUID.randomUUID();
            return id;
        } catch (RestClientResponseException ex) {
            throw new RegraNegocioException("Evolution API falhou ao enviar a mensagem (HTTP " + ex.getStatusCode().value() + "): " + extrairErro(ex.getResponseBodyAsString()));
        }
    }

    private String enviarViaWuzapiTexto(ConfiguracaoWhatsAppBusiness cfg, String numero, String mensagem) {
        validarWuzapi(cfg);
        ObjectNode body = objectMapper.createObjectNode();
        body.put("Phone", numero);
        body.put("Body", mensagem == null ? "" : mensagem);
        try {
            JsonNode resposta = wuzapiClient(cfg).post()
                    .uri("/chat/send/text")
                    .header("Token", crypto.decrypt(cfg.getWuzapiTokenEncrypted()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve().body(JsonNode.class);
            String id = resposta == null ? null : resposta.path("data").path("Id").asText(null);
            if (blank(id) && resposta != null) id = resposta.path("Id").asText(null);
            if (blank(id)) id = "wuzapi:" + UUID.randomUUID();
            return id;
        } catch (RestClientResponseException ex) {
            throw new RegraNegocioException("WuzAPI falhou ao enviar a mensagem (HTTP " + ex.getStatusCode().value() + "): " + extrairErro(ex.getResponseBodyAsString()));
        }
    }

    private String enviarViaEvolution(ConfiguracaoWhatsAppBusiness cfg, ComunicacaoAgendada c) {
        return enviarViaEvolutionTexto(cfg, normalizarNumero(c.getAgendamento().getCliente().getTelefone()), c.getMensagem());
    }

    private String enviarViaWuzapi(ConfiguracaoWhatsAppBusiness cfg, ComunicacaoAgendada c) {
        return enviarViaWuzapiTexto(cfg, normalizarNumero(c.getAgendamento().getCliente().getTelefone()), c.getMensagem());
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
    public Map<String, Object> conectarWuzapi(Long empresaId) {
        moduloAcessoService.exigir(ModuloSistema.COMUNICACAO);
        validarTenant(empresaId);
        ConfiguracaoWhatsAppBusiness cfg = configRepository.findByEmpresaId(empresaId)
                .orElseThrow(() -> new RegraNegocioException("Configure o WuzAPI antes de conectar."));
        validarWuzapi(cfg);
        if (blank(cfg.getWuzapiIntegrationKey()) || blank(cfg.getWuzapiHmacSecretEncrypted())) {
            throw new RegraNegocioException("Salve novamente a integração WuzAPI para gerar a segurança do webhook.");
        }
        configurarWebhookWuzapi(cfg);
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.putArray("Subscribe").add("Message").add("ReadReceipt");
            body.put("Immediate", true);
            JsonNode resposta = wuzapiClient(cfg).post()
                    .uri("/session/connect")
                    .header("Token", crypto.decrypt(cfg.getWuzapiTokenEncrypted()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve().body(JsonNode.class);
            String jid = resposta == null ? null : resposta.path("data").path("jid").asText(null);
            if (!blank(jid)) cfg.definirWuzapiPhoneJid(jid);
            configRepository.save(cfg);
            JsonNode statusResponse = wuzapiClient(cfg).get().uri("/session/status")
                    .header("Token", crypto.decrypt(cfg.getWuzapiTokenEncrypted()))
                    .retrieve().body(JsonNode.class);
            return statusWuzapi(cfg, statusResponse == null ? null : statusResponse.path("data"));
        } catch (RestClientResponseException ex) {
            throw new RegraNegocioException("WuzAPI recusou a conexão (HTTP " + ex.getStatusCode().value() + "): " + extrairErro(ex.getResponseBodyAsString()));
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> statusWuzapi(Long empresaId) {
        moduloAcessoService.exigir(ModuloSistema.COMUNICACAO);
        validarTenant(empresaId);
        ConfiguracaoWhatsAppBusiness cfg = configRepository.findByEmpresaId(empresaId)
                .orElseThrow(() -> new RegraNegocioException("Configure o WuzAPI antes de consultar a sessão."));
        validarWuzapi(cfg);
        try {
            JsonNode resposta = wuzapiClient(cfg).get().uri("/session/status")
                    .header("Token", crypto.decrypt(cfg.getWuzapiTokenEncrypted()))
                    .retrieve().body(JsonNode.class);
            return statusWuzapi(cfg, resposta == null ? null : resposta.path("data"));
        } catch (RestClientResponseException ex) {
            throw new RegraNegocioException("Não foi possível consultar o WuzAPI (HTTP " + ex.getStatusCode().value() + ").");
        }
    }

    @Transactional(readOnly = true)
    public String qrWuzapi(Long empresaId) {
        moduloAcessoService.exigir(ModuloSistema.COMUNICACAO);
        validarTenant(empresaId);
        ConfiguracaoWhatsAppBusiness cfg = configRepository.findByEmpresaId(empresaId)
                .orElseThrow(() -> new RegraNegocioException("Configure o WuzAPI antes de solicitar o QR Code."));
        validarWuzapi(cfg);
        try {
            JsonNode resposta = wuzapiClient(cfg).get().uri("/session/qr")
                    .header("Token", crypto.decrypt(cfg.getWuzapiTokenEncrypted()))
                    .retrieve().body(JsonNode.class);
            String qr = resposta == null ? null : resposta.path("data").path("QRCode").asText(null);
            if (blank(qr)) throw new RegraNegocioException("O WuzAPI não disponibilizou um QR Code. Inicie a conexão e tente novamente.");
            return qr;
        } catch (RestClientResponseException ex) {
            throw new RegraNegocioException("WuzAPI não disponibilizou o QR Code (HTTP " + ex.getStatusCode().value() + ").");
        }
    }

    @Transactional
    public String desconectarWuzapi(Long empresaId, boolean logout) {
        moduloAcessoService.exigir(ModuloSistema.COMUNICACAO);
        validarTenant(empresaId);
        ConfiguracaoWhatsAppBusiness cfg = configRepository.findByEmpresaId(empresaId)
                .orElseThrow(() -> new RegraNegocioException("WuzAPI não configurado."));
        validarWuzapi(cfg);
        try {
            String endpoint = logout ? "/session/logout" : "/session/disconnect";
            wuzapiClient(cfg).post().uri(endpoint)
                    .header("Token", crypto.decrypt(cfg.getWuzapiTokenEncrypted()))
                    .retrieve().toBodilessEntity();
            if (logout) cfg.definirWuzapiPhoneJid(null);
            configRepository.save(cfg);
            return logout ? "Sessão WuzAPI encerrada. Será necessário escanear um novo QR Code." : "WuzAPI desconectado. A sessão foi preservada.";
        } catch (RestClientResponseException ex) {
            throw new RegraNegocioException("Não foi possível desconectar o WuzAPI (HTTP " + ex.getStatusCode().value() + ").");
        }
    }

    @Transactional
    public void processarWebhookWuzapi(String integrationKey, String signature, String rawBody, JsonNode payload) {
        ConfiguracaoWhatsAppBusiness cfg = configRepository.findByWuzapiIntegrationKey(integrationKey)
                .orElseThrow(() -> new RegraNegocioException("Integração WuzAPI não encontrada."));
        validarHmacWuzapi(cfg, signature, rawBody);
        String event = payload.path("event").asText(payload.path("type").asText(""));
        if ("Message".equalsIgnoreCase(event)) {
            JsonNode data = payload.path("data");
            boolean fromMe = data.path("fromMe").asBoolean(false);
            String providerId = data.path("id").asText(null);
            if (fromMe && providerId != null) {
                comunicacaoRepository.findByProviderMessageId(providerId).ifPresent(c -> c.marcarEnviada(providerId));
            } else {
                whatsappMensagemService.registrarEntrada(cfg, payload, rawBody);
            }
        } else if ("ReadReceipt".equalsIgnoreCase(event)) {
            processarRecibosWuzapi(cfg, payload.path("data"), true);
        } else if ("Receipt".equalsIgnoreCase(event)) {
            processarRecibosWuzapi(cfg, payload.path("data"), false);
        }
    }

    public String wuzapiWebhookPath(ConfiguracaoWhatsAppBusiness cfg) {
        return cfg.getWuzapiIntegrationKey() == null ? "" : "/webhooks/wuzapi/" + cfg.getWuzapiIntegrationKey();
    }

    private void configurarWebhookWuzapi(ConfiguracaoWhatsAppBusiness cfg) {
        String token = crypto.decrypt(cfg.getWuzapiTokenEncrypted());
        String callback = wuzapiWebhookPath(cfg);
        String webhookUrl = publicBaseUrl + callback;
        if (webhookUrl.isBlank()) throw new RegraNegocioException("Configure melvora.public-base-url para ativar o webhook seguro do WuzAPI.");

        ObjectNode body = objectMapper.createObjectNode();
        body.put("webhook", webhookUrl);
        body.putArray("events").add("Message").add("ReadReceipt");
        body.put("active", true);
        try {
            wuzapiClient(cfg).post().uri("/webhook")
                    .header("Token", token).contentType(MediaType.APPLICATION_JSON).body(body).retrieve().toBodilessEntity();
        } catch (RestClientResponseException ex) {
            ObjectNode legacy = objectMapper.createObjectNode();
            legacy.put("webhookURL", webhookUrl);
            legacy.putArray("events").add("Message").add("ReadReceipt");
            legacy.put("active", true);
            try {
                wuzapiClient(cfg).post().uri("/webhook")
                        .header("Token", token).contentType(MediaType.APPLICATION_JSON).body(legacy).retrieve().toBodilessEntity();
            } catch (RestClientResponseException retry) {
                throw new RegraNegocioException("Não foi possível configurar o webhook do WuzAPI (HTTP " + retry.getStatusCode().value() + ").");
            }
        }

        ObjectNode hmac = objectMapper.createObjectNode();
        hmac.put("hmac_key", crypto.decrypt(cfg.getWuzapiHmacSecretEncrypted()));
        try {
            wuzapiClient(cfg).post().uri("/session/hmac/config")
                    .header("Token", token).header("Authorization", token)
                    .contentType(MediaType.APPLICATION_JSON).body(hmac).retrieve().toBodilessEntity();
        } catch (RestClientResponseException ex) {
            throw new RegraNegocioException("Não foi possível configurar a assinatura HMAC do WuzAPI (HTTP " + ex.getStatusCode().value() + "). Verifique se sua versão do WuzAPI oferece HMAC para webhooks.");
        }
    }

    private Map<String, Object> statusWuzapi(ConfiguracaoWhatsAppBusiness cfg, JsonNode data) {
        Map<String, Object> result = new HashMap<>();
        result.put("connected", data != null && data.path("Connected").asBoolean(false));
        result.put("loggedIn", data != null && data.path("LoggedIn").asBoolean(false));
        result.put("jid", cfg.getWuzapiPhoneJid() == null ? "" : cfg.getWuzapiPhoneJid());
        result.put("webhookPath", wuzapiWebhookPath(cfg));
        return result;
    }

    private void processarRecibosWuzapi(ConfiguracaoWhatsAppBusiness cfg, JsonNode data, boolean leitura) {
        JsonNode ids = data == null ? null : data.path("ids");
        if (ids != null && ids.isArray()) {
            for (JsonNode id : ids) {
                String providerId = id.asText(null);
                if (providerId != null) {
                    comunicacaoRepository.findByProviderMessageId(providerId).ifPresent(c -> {
                        if (leitura) c.marcarLida(); else c.marcarEntregue();
                    });
                    if (leitura) whatsappMensagemService.marcarMensagensComoLidas(cfg.getEmpresaId(), java.util.List.of(providerId));
                }
            }
        }
    }

    private void validarHmacWuzapi(ConfiguracaoWhatsAppBusiness cfg, String signature, String rawBody) {
        if (blank(signature) || blank(cfg.getWuzapiHmacSecretEncrypted())) {
            throw new RegraNegocioException("Webhook WuzAPI sem assinatura HMAC válida.");
        }
        try {
            byte[] key = crypto.decrypt(cfg.getWuzapiHmacSecretEncrypted()).getBytes(StandardCharsets.UTF_8);
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec(key, "HmacSHA256"));

            String provided = signature.startsWith("sha256=") ? signature.substring(7) : signature;
            String rawHex = java.util.HexFormat.of().formatHex(mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8)));
            if (MessageDigest.isEqual(rawHex.getBytes(StandardCharsets.UTF_8), provided.getBytes(StandardCharsets.UTF_8))) return;

            // A implementação atual do WuzAPI serializa o objeto JSON antes de assinar.
            JsonNode parsed = objectMapper.readTree(rawBody);
            byte[] canonical = jsonOrdenado(parsed).getBytes(StandardCharsets.UTF_8);
            String canonicalHex = java.util.HexFormat.of().formatHex(mac.doFinal(canonical));
            if (MessageDigest.isEqual(canonicalHex.getBytes(StandardCharsets.UTF_8), provided.getBytes(StandardCharsets.UTF_8))) return;

            // Compatibilidade com implementações que usam Base64 para HMAC.
            String rawBase64 = Base64.getEncoder().encodeToString(mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8)));
            if (MessageDigest.isEqual(rawBase64.getBytes(StandardCharsets.UTF_8), provided.getBytes(StandardCharsets.UTF_8))) return;

            throw new RegraNegocioException("Assinatura HMAC do webhook WuzAPI inválida.");
        } catch (RegraNegocioException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("Não foi possível validar a assinatura HMAC do WuzAPI.", ex);
        }
    }

    /**
     * Serializa novamente o payload recebido para a tentativa de validação
     * compatível com implementações que assinam o JSON desserializado.
     *
     * Jackson 3 (tools.jackson.databind) não expõe mais o método fields()
     * utilizado pelas versões anteriores do Jackson. Não precisamos percorrer
     * manualmente o JsonNode aqui: sua representação textual já produz um JSON
     * válido e evita depender de APIs removidas.
     */
    private String jsonOrdenado(JsonNode node) {
        if (node == null || node.isNull()) return "null";
        return node.toString();
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

    private void validarEvolution(ConfiguracaoWhatsAppBusiness cfg) {
        if (cfg == null || blank(cfg.getEvolutionBaseUrl()) || blank(cfg.getEvolutionApiKeyEncrypted()) || blank(cfg.getEvolutionInstance())) {
            throw new RegraNegocioException("Configuração da Evolution API incompleta.");
        }
    }

    private void validarWuzapi(ConfiguracaoWhatsAppBusiness cfg) {
        if (cfg == null || blank(cfg.getWuzapiBaseUrl()) || blank(cfg.getWuzapiTokenEncrypted())) {
            throw new RegraNegocioException("Configuração do WuzAPI incompleta.");
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

    private RestClient evolutionClient(ConfiguracaoWhatsAppBusiness cfg) {
        return restClientBuilder.baseUrl(normalizarBaseGenerica(cfg.getEvolutionBaseUrl(), "Evolution API")).build();
    }

    private RestClient wuzapiClient(ConfiguracaoWhatsAppBusiness cfg) {
        return restClientBuilder.baseUrl(normalizarBaseGenerica(cfg.getWuzapiBaseUrl(), "WuzAPI")).build();
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

    private String normalizarBaseGenerica(String valor, String nome) {
        String v = value(valor);
        if (v.isBlank()) throw new RegraNegocioException("Informe a URL base da " + nome + ".");
        if (!v.startsWith("https://")) throw new RegraNegocioException("A URL da " + nome + " deve usar HTTPS.");
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
