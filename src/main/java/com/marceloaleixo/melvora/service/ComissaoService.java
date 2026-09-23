package com.marceloaleixo.melvora.service;

import com.marceloaleixo.melvora.dto.ComissaoRequests;
import com.marceloaleixo.melvora.entity.*;
import com.marceloaleixo.melvora.entity.enums.*;
import com.marceloaleixo.melvora.exception.*;
import com.marceloaleixo.melvora.repository.*;
import com.marceloaleixo.melvora.tenant.TenantContext;
import java.math.*;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ComissaoService {
    private final ComissaoRepository comissaoRepo;
    private final ConfiguracaoComissaoRepository configRepo;
    private final UsuarioRepository usuarioRepo;
    private final EmpresaRepository empresaRepo;
    private final ModuloAcessoService modulo;

    public ComissaoService(ComissaoRepository comissaoRepo, ConfiguracaoComissaoRepository configRepo,
                           UsuarioRepository usuarioRepo, EmpresaRepository empresaRepo, ModuloAcessoService modulo) {
        this.comissaoRepo = comissaoRepo; this.configRepo = configRepo; this.usuarioRepo = usuarioRepo;
        this.empresaRepo = empresaRepo; this.modulo = modulo;
    }

    private Long tenant() { return TenantContext.getRequired(); }
    private void exigir() { modulo.exigir(ModuloSistema.COMISSOES); }

    @Transactional(readOnly = true)
    public Page<Comissao> listar(Pageable pageable, Authentication auth) {
        exigir();
        Long empresaId = tenant();
        if (auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_PROFISSIONAL"))) {
            Usuario u = usuarioRepo.findByEmailIgnoreCase(auth.getName())
                    .filter(x -> x.getEmpresa() != null && empresaId.equals(x.getEmpresa().getId()))
                    .orElseThrow(() -> new ResourceNotFoundException("Profissional não encontrado."));
            return comissaoRepo.findByEmpresaIdAndProfissionalId(empresaId, u.getId(), pageable);
        }
        return comissaoRepo.findByEmpresaId(empresaId, pageable);
    }

    @Transactional(readOnly = true)
    public List<ConfiguracaoComissao> configuracoes() { exigir(); return configRepo.findByEmpresaIdOrderByProfissionalNomeAsc(tenant()); }

    @Transactional(readOnly = true)
    public List<Usuario> profissionais() {
        exigir();
        return usuarioRepo.findByEmpresaIdAndAtivoTrueAndRoleInOrderByNomeAsc(tenant(), List.of(Role.PROFISSIONAL));
    }

    @Transactional
    public void salvarConfiguracao(ComissaoRequests.ConfiguracaoForm form) {
        exigir();
        if (form.percentual() == null || form.percentual().compareTo(BigDecimal.ZERO) < 0 || form.percentual().compareTo(new BigDecimal("100.00")) > 0)
            throw new RegraNegocioException("O percentual deve estar entre 0% e 100%.");
        Long empresaId = tenant();
        Usuario profissional = usuarioRepo.findById(form.profissionalId())
                .filter(u -> u.getEmpresa() != null && empresaId.equals(u.getEmpresa().getId()))
                .filter(Usuario::isAtivo).filter(u -> u.getRole() == Role.PROFISSIONAL)
                .orElseThrow(() -> new RegraNegocioException("Profissional inválido ou inativo."));
        Empresa empresa = empresaRepo.findById(empresaId).orElseThrow(() -> new ResourceNotFoundException("Empresa não encontrada."));
        ConfiguracaoComissao config = configRepo.findByEmpresaIdAndProfissionalId(empresaId, profissional.getId()).orElse(null);
        if (config == null) configRepo.save(new ConfiguracaoComissao(empresa, profissional, form.percentual()));
        else config.atualizar(form.percentual());
    }

    @Transactional
    public void gerarParaReceitaPaga(LancamentoFinanceiro lancamento) {
        if (!modulo.possui(ModuloSistema.COMISSOES) || lancamento == null || lancamento.getId() == null) return;
        if (lancamento.getTipo() != TipoLancamentoFinanceiro.RECEITA || lancamento.getStatus() != StatusLancamentoFinanceiro.PAGO) return;
        Long empresaId = tenant();
        if (lancamento.getEmpresa() == null || !empresaId.equals(lancamento.getEmpresa().getId())) throw new RegraNegocioException("O lançamento não pertence à empresa atual.");
        if (comissaoRepo.existsByLancamentoIdAndEmpresaId(lancamento.getId(), empresaId)) return;
        Atendimento atendimento = lancamento.getAtendimento();
        if (atendimento == null || atendimento.getProfissional() == null) return;
        ConfiguracaoComissao config = configRepo.findByEmpresaIdAndProfissionalId(empresaId, atendimento.getProfissional().getId()).orElse(null);
        if (config == null || !config.isAtivo() || config.getPercentual().signum() == 0) return;
        BigDecimal base = lancamento.getValor();
        BigDecimal valor = base.multiply(config.getPercentual()).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        if (valor.signum() <= 0) return;
        comissaoRepo.save(new Comissao(lancamento.getEmpresa(), atendimento.getProfissional(), lancamento, base, config.getPercentual(), valor, lancamento.getDataMovimento()));
    }

    @Transactional
    public void pagar(Long id) { exigir(); Comissao c = buscar(id); c.pagar(); }
    @Transactional
    public void cancelar(Long id) { exigir(); Comissao c = buscar(id); c.cancelar(); }
    private Comissao buscar(Long id) { return comissaoRepo.findByIdAndEmpresaId(id, tenant()).orElseThrow(() -> new ResourceNotFoundException("Comissão não encontrada.")); }
    @Transactional(readOnly = true)
    public BigDecimal totalPendente() { exigir(); return comissaoRepo.total(tenant(), StatusComissao.PENDENTE, LocalDate.of(2000,1,1), LocalDate.now().plusYears(10)); }
}
