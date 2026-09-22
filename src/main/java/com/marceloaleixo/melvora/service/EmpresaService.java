package com.marceloaleixo.melvora.service;

import com.marceloaleixo.melvora.dto.EmpresaRequests;
import com.marceloaleixo.melvora.entity.Empresa;
import com.marceloaleixo.melvora.exception.RegraNegocioException;
import com.marceloaleixo.melvora.exception.ResourceNotFoundException;
import com.marceloaleixo.melvora.repository.EmpresaRepository;
import com.marceloaleixo.melvora.repository.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmpresaService {
    private final EmpresaRepository empresaRepository;
    private final UsuarioRepository usuarioRepository;
    private final ModuloAcessoService moduloAcessoService;

    public EmpresaService(EmpresaRepository empresaRepository, UsuarioRepository usuarioRepository, ModuloAcessoService moduloAcessoService) {
        this.empresaRepository = empresaRepository;
        this.usuarioRepository = usuarioRepository;
        this.moduloAcessoService = moduloAcessoService;
    }

    @Transactional(readOnly = true)
    public Page<Empresa> listar(Pageable pageable) {
        return empresaRepository.findAll(pageable);
    }

    @Transactional
    public Empresa criar(EmpresaRequests.Criar request) {
        String nome = normalizarNome(request.nomeFantasia());
        if (empresaRepository.existsByNomeFantasiaIgnoreCase(nome)) {
            throw new RegraNegocioException("Já existe uma empresa com este nome fantasia.");
        }
        Empresa empresa = empresaRepository.save(new Empresa(nome));
        moduloAcessoService.inicializarEmpresa(empresa.getId());
        return empresa;
    }

    @Transactional(readOnly = true)
    public Empresa buscar(Long id) {
        if (id == null) {
            throw new ResourceNotFoundException("Empresa não encontrada.");
        }
        return empresaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa não encontrada."));
    }

    @Transactional
    public Empresa alterarStatus(Long id, boolean ativa) {
        if (id == null) {
            throw new RegraNegocioException("Empresa inválida.");
        }

        Empresa empresa = empresaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa não encontrada."));

        if (!ativa && empresa.isAtiva()) {
            long administradoresAtivos = usuarioRepository.countByEmpresaIdAndAtivoTrueAndRole(
                    id, com.marceloaleixo.melvora.entity.enums.Role.ADMIN);
            if (administradoresAtivos == 0) {
                throw new RegraNegocioException("A empresa não pode ser desativada porque não possui um ADMIN ativo responsável.");
            }
        }

        empresa.setAtiva(ativa);
        return empresaRepository.save(empresa);
    }

    private String normalizarNome(String nome) {
        String normalizado = nome == null ? "" : nome.trim().replaceAll("\\s{2,}", " ");
        if (normalizado.isBlank()) {
            throw new RegraNegocioException("Nome fantasia é obrigatório.");
        }
        return normalizado;
    }
}
