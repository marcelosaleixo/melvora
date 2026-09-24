package com.marceloaleixo.melvora.service;

import com.marceloaleixo.melvora.dto.ServicoRequests;
import com.marceloaleixo.melvora.entity.Empresa;
import com.marceloaleixo.melvora.entity.Servico;
import com.marceloaleixo.melvora.entity.Usuario;
import com.marceloaleixo.melvora.entity.enums.ModuloSistema;
import com.marceloaleixo.melvora.entity.enums.Role;
import com.marceloaleixo.melvora.exception.RegraNegocioException;
import com.marceloaleixo.melvora.exception.ResourceNotFoundException;
import com.marceloaleixo.melvora.repository.EmpresaRepository;
import com.marceloaleixo.melvora.repository.ServicoRepository;
import com.marceloaleixo.melvora.repository.UsuarioRepository;
import com.marceloaleixo.melvora.tenant.TenantContext;
import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicoService {
    private final ServicoRepository repository;
    private final EmpresaRepository empresaRepository;
    private final UsuarioRepository usuarioRepository;
    private final ModuloAcessoService moduloAcessoService;

    public ServicoService(ServicoRepository repository, EmpresaRepository empresaRepository,
                          UsuarioRepository usuarioRepository, ModuloAcessoService moduloAcessoService) {
        this.repository = repository;
        this.empresaRepository = empresaRepository;
        this.usuarioRepository = usuarioRepository;
        this.moduloAcessoService = moduloAcessoService;
    }

    @Transactional(readOnly = true)
    public Page<Servico> listar(Pageable pageable) {
        moduloAcessoService.exigir(ModuloSistema.SERVICOS);
        return repository.findByEmpresaId(TenantContext.getRequired(), pageable);
    }

    @Transactional(readOnly = true)
    public List<Servico> listarAtivos() {
        moduloAcessoService.exigir(ModuloSistema.SERVICOS);
        return repository.findAtivosByEmpresaId(TenantContext.getRequired());
    }

    @Transactional(readOnly = true)
    public Servico buscar(Long id) {
        moduloAcessoService.exigir(ModuloSistema.SERVICOS);
        return repository.findByIdAndEmpresaId(id, TenantContext.getRequired())
                .orElseThrow(() -> new ResourceNotFoundException("Serviço não encontrado."));
    }

    @Transactional
    public Servico criar(ServicoRequests.WebForm form) {
        moduloAcessoService.exigir(ModuloSistema.SERVICOS);
        Long empresaId = TenantContext.getRequired();
        Empresa empresa = empresaRepository.findById(empresaId)
                .filter(Empresa::isAtiva)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa não encontrada ou inativa."));
        Servico servico = new Servico(empresa, normalizar(form.getNome()), normalizar(form.getDescricao()),
                normalizar(form.getCategoria()), form.getDuracaoMinutos(), dinheiro(form.getPreco()));
        servico.substituirProfissionais(carregarProfissionais(empresaId, form.getProfissionalIds()));
        return repository.save(servico);
    }

    @Transactional
    public Servico atualizar(Long id, ServicoRequests.WebForm form) {
        moduloAcessoService.exigir(ModuloSistema.SERVICOS);
        Long empresaId = TenantContext.getRequired();
        Servico servico = buscar(id);
        servico.atualizar(normalizar(form.getNome()), normalizar(form.getDescricao()), normalizar(form.getCategoria()),
                form.getDuracaoMinutos(), dinheiro(form.getPreco()));
        servico.substituirProfissionais(carregarProfissionais(empresaId, form.getProfissionalIds()));
        return servico;
    }

    @Transactional
    public void alternarStatus(Long id) {
        moduloAcessoService.exigir(ModuloSistema.SERVICOS);
        Servico servico = buscar(id);
        servico.definirAtivo(!servico.isAtivo());
    }

    @Transactional(readOnly = true)
    public Servico buscarParaAgendamento(Long id, Long profissionalId) {
        return buscarParaAgendamento(id, profissionalId, TenantContext.getRequired());
    }

    @Transactional(readOnly = true)
    public Servico buscarParaAgendamento(Long id, Long profissionalId, Long empresaId) {
        moduloAcessoService.exigir(empresaId, ModuloSistema.SERVICOS);
        Servico servico = repository.findByIdAndEmpresaId(id, empresaId)
                .orElseThrow(() -> new ResourceNotFoundException("Serviço não encontrado."));
        if (!servico.isAtivo()) throw new RegraNegocioException("O serviço selecionado está inativo.");
        if (!servico.podeSerExecutadoPor(profissionalId)) {
            throw new RegraNegocioException("O profissional selecionado não está habilitado para este serviço.");
        }
        return servico;
    }

    private Set<Usuario> carregarProfissionais(Long empresaId, List<Long> ids) {
        if (ids == null || ids.isEmpty()) return new LinkedHashSet<>();
        Set<Usuario> profissionais = new LinkedHashSet<>();
        for (Long id : ids.stream().distinct().toList()) {
            Usuario usuario = usuarioRepository.findByIdAndEmpresaId(id, empresaId)
                    .filter(u -> u.isAtivo() && (u.getRole() == Role.ADMIN || u.getRole() == Role.PROFISSIONAL))
                    .orElseThrow(() -> new RegraNegocioException("Um dos profissionais selecionados é inválido ou pertence a outra empresa."));
            profissionais.add(usuario);
        }
        return profissionais;
    }

    private String normalizar(String valor) {
        if (valor == null) return null;
        String v = valor.trim();
        return v.isBlank() ? null : v;
    }

    private BigDecimal dinheiro(BigDecimal valor) {
        return valor == null ? null : valor.setScale(2);
    }
}
