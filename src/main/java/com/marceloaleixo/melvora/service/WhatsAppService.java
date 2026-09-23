package com.marceloaleixo.melvora.service;

import com.marceloaleixo.melvora.entity.Agendamento;
import com.marceloaleixo.melvora.entity.Cliente;
import com.marceloaleixo.melvora.entity.TemplateWhatsApp;
import com.marceloaleixo.melvora.entity.enums.ModuloSistema;
import com.marceloaleixo.melvora.exception.RegraNegocioException;
import com.marceloaleixo.melvora.tenant.TenantContext;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WhatsAppService {
    private static final Pattern NON_DIGIT = Pattern.compile("\\D");
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");

    private final ModuloAcessoService moduloAcessoService;

    public WhatsAppService(ModuloAcessoService moduloAcessoService) {
        this.moduloAcessoService = moduloAcessoService;
    }

    @Transactional(readOnly = true)
    public String linkParaCliente(Cliente cliente, String mensagem) {
        moduloAcessoService.exigir(ModuloSistema.COMUNICACAO);
        validarCliente(cliente);
        return montarLink(cliente.getTelefone(), mensagem);
    }

    @Transactional(readOnly = true)
    public String linkConfirmacao(Agendamento agendamento) {
        moduloAcessoService.exigir(ModuloSistema.COMUNICACAO);
        validarAgendamento(agendamento);
        String nome = agendamento.getCliente().getNome();
        String servico = agendamento.getServico() != null ? agendamento.getServico().getNome() : agendamento.getTipo().getNome();
        String mensagem = "Olá, " + nome + "! 😊\n\n"
                + "Passando para confirmar seu atendimento no Melvora.\n"
                + "📅 " + DATA_HORA.format(agendamento.getDataHoraInicio()) + "\n"
                + "💇 " + servico + "\n"
                + "👤 Profissional: " + agendamento.getProfissional().getNome() + "\n\n"
                + "Podemos confirmar seu horário?";
        return montarLink(agendamento.getCliente().getTelefone(), mensagem);
    }

    @Transactional(readOnly = true)
    public String linkComTemplate(Agendamento agendamento, TemplateWhatsApp template) {
        moduloAcessoService.exigir(ModuloSistema.COMUNICACAO);
        validarAgendamento(agendamento);
        if (template == null || !template.isAtivo() || !TenantContext.getRequired().equals(template.getEmpresa().getId())) {
            throw new RegraNegocioException("Template inválido para comunicação.");
        }
        String servico = agendamento.getServico() != null ? agendamento.getServico().getNome() : agendamento.getTipo().getNome();
        String mensagem = template.getMensagem()
                .replace("{cliente}", agendamento.getCliente().getNome())
                .replace("{empresa}", agendamento.getEmpresa().getNomeFantasia())
                .replace("{servico}", servico)
                .replace("{data}", agendamento.getDataHoraInicio().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                .replace("{hora}", agendamento.getDataHoraInicio().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")))
                .replace("{profissional}", agendamento.getProfissional().getNome());
        return montarLink(agendamento.getCliente().getTelefone(), mensagem);
    }

    @Transactional(readOnly = true)
    public String linkLembrete(Agendamento agendamento) {
        moduloAcessoService.exigir(ModuloSistema.COMUNICACAO);
        validarAgendamento(agendamento);
        String nome = agendamento.getCliente().getNome();
        String servico = agendamento.getServico() != null ? agendamento.getServico().getNome() : agendamento.getTipo().getNome();
        String mensagem = "Olá, " + nome + "! 💖\n\n"
                + "Este é um lembrete do seu atendimento no Melvora.\n"
                + "📅 " + DATA_HORA.format(agendamento.getDataHoraInicio()) + "\n"
                + "💇 " + servico + "\n"
                + "👤 Profissional: " + agendamento.getProfissional().getNome() + "\n\n"
                + "Esperamos você!";
        return montarLink(agendamento.getCliente().getTelefone(), mensagem);
    }

    private String montarLink(String telefone, String mensagem) {
        String numero = normalizarNumero(telefone);
        String encoded = URLEncoder.encode(mensagem == null ? "" : mensagem, StandardCharsets.UTF_8)
                .replace("+", "%20");
        return "https://wa.me/" + numero + "?text=" + encoded;
    }

    private String normalizarNumero(String telefone) {
        if (telefone == null || telefone.isBlank()) {
            throw new RegraNegocioException("A cliente não possui telefone cadastrado.");
        }
        String numero = NON_DIGIT.matcher(telefone).replaceAll("");
        if (numero.length() == 10 || numero.length() == 11) numero = "55" + numero;
        if (!numero.startsWith("55") || numero.length() < 12 || numero.length() > 13) {
            throw new RegraNegocioException("O telefone da cliente não possui um formato válido para WhatsApp.");
        }
        return numero;
    }

    private void validarCliente(Cliente cliente) {
        if (cliente == null || !cliente.isAtivo() || !TenantContext.getRequired().equals(cliente.getEmpresa().getId())) {
            throw new RegraNegocioException("Cliente inválida para comunicação.");
        }
    }

    private void validarAgendamento(Agendamento agendamento) {
        if (agendamento == null || !TenantContext.getRequired().equals(agendamento.getEmpresa().getId())) {
            throw new RegraNegocioException("Agendamento inválido para comunicação.");
        }
        validarCliente(agendamento.getCliente());
    }
}
