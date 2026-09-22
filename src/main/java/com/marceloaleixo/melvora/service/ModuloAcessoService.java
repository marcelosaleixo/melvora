package com.marceloaleixo.melvora.service;

import com.marceloaleixo.melvora.entity.EmpresaModulo;
import com.marceloaleixo.melvora.entity.enums.ModuloSistema;
import com.marceloaleixo.melvora.exception.RegraNegocioException;
import com.marceloaleixo.melvora.exception.ModuloNaoContratadoException;
import com.marceloaleixo.melvora.repository.EmpresaModuloRepository;
import com.marceloaleixo.melvora.repository.EmpresaRepository;
import com.marceloaleixo.melvora.tenant.TenantContext;
import java.util.EnumSet;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ModuloAcessoService {
    private final EmpresaModuloRepository repository;
    private final EmpresaRepository empresaRepository;

    public ModuloAcessoService(EmpresaModuloRepository repository, EmpresaRepository empresaRepository) {
        this.repository = repository;
        this.empresaRepository = empresaRepository;
    }

    @Transactional(readOnly = true)
    public boolean possui(ModuloSistema modulo) {
        Long empresaId = TenantContext.getRequired();
        return repository.findByEmpresaIdAndModulo(empresaId, modulo)
                .map(EmpresaModulo::isAtivo)
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public void exigir(ModuloSistema modulo) {
        if (!possui(modulo)) {
            throw new ModuloNaoContratadoException("O módulo " + modulo.getNome() + " não está contratado para esta empresa.");
        }
    }

    @Transactional(readOnly = true)
    public List<ModuloSistema> modulosAtivosDaEmpresa(Long empresaId) {
        return repository.findModulosAtivos(empresaId);
    }

    @Transactional
    public void configurar(Long empresaId, List<ModuloSistema> solicitados) {
        var empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new RegraNegocioException("Empresa não encontrada."));
        if (!empresa.isAtiva()) {
            throw new RegraNegocioException("Não é possível alterar módulos de uma empresa inativa.");
        }

        EnumSet<ModuloSistema> desejados = ModuloSistema.comDependencias(solicitados == null ? List.of() : solicitados);
        for (ModuloSistema modulo : ModuloSistema.values()) {
            EmpresaModulo registro = repository.findByEmpresaIdAndModulo(empresaId, modulo)
                    .orElseGet(() -> repository.save(new EmpresaModulo(empresa, modulo, false)));
            registro.setAtivo(desejados.contains(modulo));
        }
        repository.flush();
    }

    @Transactional
    public void inicializarEmpresa(Long empresaId) {
        if (empresaId == null) {
            throw new RegraNegocioException("Empresa inválida para inicialização dos módulos.");
        }

        // Todo tenant começa com o núcleo mínimo. Módulos adicionais ficam sob licenciamento do SUPER_ADMIN.
        configurar(empresaId, List.of(ModuloSistema.CLIENTES, ModuloSistema.EQUIPE));
    }
}
