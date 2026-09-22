package com.marceloaleixo.melvora.service;

import com.marceloaleixo.melvora.entity.Cliente;
import com.marceloaleixo.melvora.repository.ClienteRepository;
import com.marceloaleixo.melvora.dto.ClienteRequests;
import com.marceloaleixo.melvora.entity.Empresa;
import com.marceloaleixo.melvora.repository.EmpresaRepository;
import com.marceloaleixo.melvora.exception.RegraNegocioException;
import com.marceloaleixo.melvora.exception.ResourceNotFoundException;
import com.marceloaleixo.melvora.tenant.TenantContext;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClienteService {
    private final ClienteRepository clienteRepository;
    private final EmpresaRepository empresaRepository;
    private final ModuloAcessoService moduloAcessoService;

    public ClienteService(ClienteRepository clienteRepository, EmpresaRepository empresaRepository, ModuloAcessoService moduloAcessoService) {
        this.clienteRepository = clienteRepository;
        this.empresaRepository = empresaRepository;
        this.moduloAcessoService = moduloAcessoService;
    }

    @Transactional
    public Cliente criar(ClienteRequests.CriarClienteRequest request) {
        moduloAcessoService.exigir(com.marceloaleixo.melvora.entity.enums.ModuloSistema.CLIENTES);
        var empresa = empresaRepository.findById(TenantContext.getRequired())
                .orElseThrow(() -> new IllegalStateException("Empresa do usuário não encontrada."));
        if (!empresa.isAtiva()) throw new com.marceloaleixo.melvora.exception.RegraNegocioException("Empresa inativa.");
        return clienteRepository.save(new Cliente(empresa, request.nome().trim(), normalizar(request.telefone()),
                normalizar(request.email()), normalizar(request.observacoes())));
    }

    @Transactional(readOnly = true)
    public Page<Cliente> listar(Pageable pageable) {
        moduloAcessoService.exigir(com.marceloaleixo.melvora.entity.enums.ModuloSistema.CLIENTES);
        return clienteRepository.findByEmpresaIdAndAtivoTrue(TenantContext.getRequired(), pageable);
    }

    @Transactional(readOnly = true)
    public Cliente buscar(Long id) {
        moduloAcessoService.exigir(com.marceloaleixo.melvora.entity.enums.ModuloSistema.CLIENTES);
        return clienteRepository.findByIdAndEmpresaId(id, TenantContext.getRequired())
                .orElseThrow(() -> new com.marceloaleixo.melvora.exception.ResourceNotFoundException("Cliente não encontrado."));
    }

    private String normalizar(String value) { return value == null ? null : value.trim(); }
}
