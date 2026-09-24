package com.marceloaleixo.melvora.service;

import com.marceloaleixo.melvora.entity.Cliente;
import com.marceloaleixo.melvora.entity.ConfiguracaoWhatsAppBusiness;
import com.marceloaleixo.melvora.entity.Empresa;
import com.marceloaleixo.melvora.entity.WhatsAppMensagem;
import com.marceloaleixo.melvora.entity.WhatsAppMensagem.Direcao;
import com.marceloaleixo.melvora.repository.ClienteRepository;
import com.marceloaleixo.melvora.repository.WhatsAppMensagemRepository;
import com.marceloaleixo.melvora.repository.PreferenciaComunicacaoContatoRepository;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class WhatsAppMensagemService {
    private static final Pattern NON_DIGIT = Pattern.compile("\\D");

    private final WhatsAppMensagemRepository mensagemRepository;
    private final ClienteRepository clienteRepository;
    private final ObjectMapper objectMapper;
    private final PreferenciaComunicacaoContatoRepository preferenciaRepository;

    public WhatsAppMensagemService(WhatsAppMensagemRepository mensagemRepository, ClienteRepository clienteRepository, ObjectMapper objectMapper, PreferenciaComunicacaoContatoRepository preferenciaRepository) {
        this.mensagemRepository = mensagemRepository;
        this.clienteRepository = clienteRepository;
        this.objectMapper = objectMapper;
        this.preferenciaRepository = preferenciaRepository;
    }

    @Transactional
    public void registrarEntrada(ConfiguracaoWhatsAppBusiness cfg, JsonNode payload, String rawPayload) {
        if (cfg == null || cfg.getEmpresa() == null) return;
        JsonNode data = payload.path("data");
        String providerId = text(data, "id");
        if (providerId == null || providerId.isBlank()) return;
        if (mensagemRepository.findByEmpresaIdAndProviderMessageId(cfg.getEmpresaId(), providerId).isPresent()) return;

        String telefone = telefoneFromJid(firstNonBlank(text(data, "sender"), text(data, "chat"), text(data, "from")));
        if (telefone == null || telefone.isBlank()) return;
        String mensagem = extrairTexto(data.path("message"));
        if (mensagem == null) mensagem = extrairTexto(data);
        String pushName = text(data, "pushName");
        LocalDateTime recebidoEm = data.has("timestamp") && data.get("timestamp").canConvertToLong()
                ? LocalDateTime.ofInstant(Instant.ofEpochSecond(data.get("timestamp").asLong()), ZoneId.systemDefault())
                : LocalDateTime.now();

        Cliente cliente = localizarCliente(cfg.getEmpresaId(), telefone);
        Empresa empresa = cfg.getEmpresa();
        mensagemRepository.save(new WhatsAppMensagem(
                empresa, cliente, providerId, WhatsAppMensagem.Direcao.ENTRADA,
                telefone, pushName, mensagem, sanitizarPayload(payload, rawPayload), recebidoEm));
        processarOptOut(empresa, cliente, telefone, mensagem);
    }

    private void processarOptOut(Empresa empresa, Cliente cliente, String telefone, String mensagem) {
        if (mensagem == null || telefone == null) return;
        String comando = mensagem.trim().toUpperCase(java.util.Locale.ROOT).replaceAll("\\s+", " ");
        if (!java.util.Set.of("SAIR", "PARAR", "STOP", "REMOVER", "NÃO QUERO RECEBER", "NAO QUERO RECEBER").contains(comando)) return;
        var pref = preferenciaRepository.findByEmpresaIdAndTelefone(empresa.getId(), telefone)
                .orElseGet(() -> new com.marceloaleixo.melvora.entity.PreferenciaComunicacaoContato(empresa, cliente, telefone));
        if (cliente != null) pref.vincularCliente(cliente);
        pref.bloquearRetencao();
        preferenciaRepository.save(pref);
    }


    @Transactional
    public void registrarSaida(ConfiguracaoWhatsAppBusiness cfg, Cliente cliente, String telefone, String mensagem, String providerMessageId) {
        if (cfg == null || cfg.getEmpresa() == null || providerMessageId == null || providerMessageId.isBlank()) return;
        String canonical = canonical(telefone);
        if (canonical == null) return;
        if (mensagemRepository.findByEmpresaIdAndProviderMessageId(cfg.getEmpresaId(), providerMessageId).isPresent()) return;
        mensagemRepository.save(new WhatsAppMensagem(
                cfg.getEmpresa(), cliente, providerMessageId, Direcao.SAIDA, canonical,
                cfg.getEmpresa().getNomeFantasia(), mensagem, "{}", LocalDateTime.now()));
    }

    @Transactional
    public int marcarConversaComoLida(Long empresaId, Long clienteId, String telefone) {
        if (clienteId != null) {
            return mensagemRepository.marcarComoLidasPorCliente(empresaId, clienteId, Direcao.ENTRADA, LocalDateTime.now());
        }
        if (telefone == null || telefone.isBlank()) return 0;
        return mensagemRepository.marcarComoLidasPorTelefone(empresaId, canonical(telefone), Direcao.ENTRADA, LocalDateTime.now());
    }

    @Transactional
    public int marcarMensagensComoLidas(Long empresaId, Collection<String> providerIds) {
        if (providerIds == null || providerIds.isEmpty()) return 0;
        return mensagemRepository.marcarComoLidasPorProviderIds(empresaId, providerIds, LocalDateTime.now());
    }

    private Cliente localizarCliente(Long empresaId, String telefone) {
        String canonical = canonical(telefone);
        if (canonical == null) return null;
        return clienteRepository.findAtivaByEmpresaIdAndTelefone(empresaId, canonical).orElse(null);
    }

    private String sanitizarPayload(JsonNode payload, String fallback) {
        try {
            JsonNode copy = payload == null ? null : payload.deepCopy();
            if (copy != null && copy.isObject()) {
                ((tools.jackson.databind.node.ObjectNode) copy).remove("token");
                ((tools.jackson.databind.node.ObjectNode) copy).remove("authorization");
                ((tools.jackson.databind.node.ObjectNode) copy).remove("Authorization");
            }
            return copy == null ? fallback : objectMapper.writeValueAsString(copy);
        } catch (Exception ignored) {
            return "{}";
        }
    }

    private static String canonical(String value) {
        if (value == null) return null;
        String n = NON_DIGIT.matcher(value).replaceAll("");
        if (n.isBlank()) return null;
        if (n.startsWith("55") && (n.length() == 12 || n.length() == 13)) return n;
        if (n.length() == 10 || n.length() == 11) return "55" + n;
        return n;
    }

    private static String telefoneFromJid(String value) {
        if (value == null) return null;
        int at = value.indexOf('@');
        String n = at > 0 ? value.substring(0, at) : value;
        int colon = n.indexOf(':');
        if (colon > 0) n = n.substring(0, colon);
        return canonical(n);
    }

    private static String extrairTexto(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) return null;
        if (node.isTextual()) return node.asText();
        for (String key : new String[]{"conversation", "text", "Body", "body"}) {
            JsonNode value = node.path(key);
            if (value.isTextual() && !value.asText().isBlank()) return value.asText();
        }
        for (String key : new String[]{"extendedTextMessage", "imageMessage", "videoMessage", "documentMessage"}) {
            JsonNode child = node.path(key);
            if (child.isObject()) {
                String text = extrairTexto(child);
                if (text != null && !text.isBlank()) return text;
                JsonNode caption = child.path("caption");
                if (caption.isTextual()) return caption.asText();
            }
        }
        return null;
    }

    private static String text(JsonNode node, String key) {
        JsonNode value = node == null ? null : node.path(key);
        return value != null && value.isValueNode() && !value.isNull() ? value.asText(null) : null;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) if (value != null && !value.isBlank()) return value;
        return null;
    }
}
