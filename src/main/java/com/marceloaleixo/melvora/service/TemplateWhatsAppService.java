package com.marceloaleixo.melvora.service;

import com.marceloaleixo.melvora.dto.TemplateWhatsAppRequests;
import com.marceloaleixo.melvora.entity.Empresa;
import com.marceloaleixo.melvora.entity.TemplateWhatsApp;
import com.marceloaleixo.melvora.entity.enums.ModuloSistema;
import com.marceloaleixo.melvora.exception.RegraNegocioException;
import com.marceloaleixo.melvora.repository.EmpresaRepository;
import com.marceloaleixo.melvora.repository.TemplateWhatsAppRepository;
import com.marceloaleixo.melvora.tenant.TenantContext;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TemplateWhatsAppService {
    private final TemplateWhatsAppRepository repository;
    private final EmpresaRepository empresaRepository;
    private final ModuloAcessoService moduloAcessoService;

    public TemplateWhatsAppService(TemplateWhatsAppRepository repository,
                                   EmpresaRepository empresaRepository,
                                   ModuloAcessoService moduloAcessoService) {
        this.repository = repository;
        this.empresaRepository = empresaRepository;
        this.moduloAcessoService = moduloAcessoService;
    }

    @Transactional(readOnly = true)
    public Page<TemplateWhatsApp> listar(Pageable pageable) {
        moduloAcessoService.exigir(ModuloSistema.COMUNICACAO);
        return repository.findByEmpresaId(TenantContext.getRequired(), pageable);
    }

    @Transactional(readOnly = true)
    public List<TemplateWhatsApp> ativos() {
        moduloAcessoService.exigir(ModuloSistema.COMUNICACAO);
        return repository.findByEmpresaIdAndAtivoTrueOrderByNomeAsc(TenantContext.getRequired());
    }

    @Transactional
    public void garantirPadroes() {
        moduloAcessoService.exigir(ModuloSistema.COMUNICACAO);
        Long empresaId = TenantContext.getRequired();
        Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new RegraNegocioException("Empresa não encontrada."));
        criarPadraoSeAusente(empresa, "Confirmação de agendamento",
                "Olá, {cliente}! 😊\n\nPassando para confirmar seu atendimento em {empresa}.\n📅 {data} às {hora}\n💇 {servico}\n👤 Profissional: {profissional}\n\nPodemos confirmar seu horário?");
        criarPadraoSeAusente(empresa, "Lembrete de atendimento",
                "Olá, {cliente}! 💖\n\nEste é um lembrete do seu atendimento em {empresa}.\n📅 {data} às {hora}\n💇 {servico}\n👤 Profissional: {profissional}\n\nEsperamos você!");
        criarPadraoSeAusente(empresa, "Pós-atendimento",
                "Olá, {cliente}! 💕\n\nFoi um prazer receber você na {empresa}. Esperamos que tenha gostado do seu atendimento de {servico}.\n\nQuando precisar, estaremos à disposição!");
    }

    private void criarPadraoSeAusente(Empresa empresa, String nome, String mensagem) {
        if (!repository.existsByEmpresaIdAndNomeIgnoreCase(empresa.getId(), nome)) {
            repository.save(new TemplateWhatsApp(empresa, nome, mensagem));
        }
    }

    @Transactional
    public void criar(TemplateWhatsAppRequests.WebForm form) {
        moduloAcessoService.exigir(ModuloSistema.COMUNICACAO);
        Long empresaId = TenantContext.getRequired();
        if (repository.existsByEmpresaIdAndNomeIgnoreCase(empresaId, form.nome().trim())) {
            throw new RegraNegocioException("Já existe um template com esse nome.");
        }
        Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new RegraNegocioException("Empresa não encontrada."));
        repository.save(new TemplateWhatsApp(empresa, form.nome().trim(), form.mensagem().trim()));
    }

    @Transactional
    public void editar(Long id, TemplateWhatsAppRequests.WebForm form) {
        moduloAcessoService.exigir(ModuloSistema.COMUNICACAO);
        Long empresaId = TenantContext.getRequired();
        TemplateWhatsApp template = repository.findByIdAndEmpresaId(id, empresaId)
                .orElseThrow(() -> new RegraNegocioException("Template não encontrado."));
        if (!template.getNome().equalsIgnoreCase(form.nome().trim())
                && repository.existsByEmpresaIdAndNomeIgnoreCase(empresaId, form.nome().trim())) {
            throw new RegraNegocioException("Já existe um template com esse nome.");
        }
        template.atualizar(form.nome().trim(), form.mensagem().trim());
    }

    @Transactional
    public boolean alternarStatus(Long id) {
        moduloAcessoService.exigir(ModuloSistema.COMUNICACAO);
        TemplateWhatsApp template = repository.findByIdAndEmpresaId(id, TenantContext.getRequired())
                .orElseThrow(() -> new RegraNegocioException("Template não encontrado."));
        template.definirAtivo(!template.isAtivo());
        return template.isAtivo();
    }

    @Transactional(readOnly = true)
    public TemplateWhatsApp buscar(Long id) {
        moduloAcessoService.exigir(ModuloSistema.COMUNICACAO);
        return repository.findByIdAndEmpresaId(id, TenantContext.getRequired())
                .orElseThrow(() -> new RegraNegocioException("Template não encontrado."));
    }
}
