package com.marceloaleixo.melvora.service;

import com.marceloaleixo.melvora.dto.UsuarioRequests;
import com.marceloaleixo.melvora.entity.Empresa;
import com.marceloaleixo.melvora.entity.Usuario;
import com.marceloaleixo.melvora.entity.enums.Role;
import com.marceloaleixo.melvora.exception.RegraNegocioException;
import com.marceloaleixo.melvora.exception.ResourceNotFoundException;
import com.marceloaleixo.melvora.repository.EmpresaRepository;
import com.marceloaleixo.melvora.repository.UsuarioRepository;
import com.marceloaleixo.melvora.tenant.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {
    private final UsuarioRepository usuarioRepository;
    private final EmpresaRepository empresaRepository;
    private final PasswordEncoder passwordEncoder;
    private final ModuloAcessoService moduloAcessoService;

    public UsuarioService(UsuarioRepository usuarioRepository, EmpresaRepository empresaRepository, PasswordEncoder passwordEncoder, ModuloAcessoService moduloAcessoService) {
        this.usuarioRepository = usuarioRepository;
        this.empresaRepository = empresaRepository;
        this.passwordEncoder = passwordEncoder;
        this.moduloAcessoService = moduloAcessoService;
    }

    @Transactional(readOnly = true)
    public Page<Usuario> listarEmpresa(Pageable pageable) {
        moduloAcessoService.exigir(com.marceloaleixo.melvora.entity.enums.ModuloSistema.EQUIPE);
        return usuarioRepository.findByEmpresaId(TenantContext.getRequired(), pageable);
    }

    @Transactional(readOnly = true)
    public Page<Usuario> listarPlataforma(Pageable pageable) {
        return usuarioRepository.findAll(pageable);
    }

    @Transactional
    public Usuario criarParaEmpresa(UsuarioRequests.Criar request) {
        moduloAcessoService.exigir(com.marceloaleixo.melvora.entity.enums.ModuloSistema.EQUIPE);
        Long empresaId = TenantContext.getRequired();
        validarPerfilOperacional(request.role());
        Empresa empresa = empresaRepository.findById(empresaId)
            .filter(Empresa::isAtiva)
            .orElseThrow(() -> new ResourceNotFoundException("Empresa ativa não encontrada."));
        return salvar(request, request.role(), empresa);
    }

    @Transactional
    public Usuario criarPelaPlataforma(UsuarioRequests.Criar request) {
        if (request.role() == Role.SUPER_ADMIN) {
            throw new RegraNegocioException("SUPER_ADMIN não pode ser criado por esta tela.");
        }
        if (request.empresaId() == null) {
            throw new RegraNegocioException("Selecione a empresa do usuário.");
        }
        Empresa empresa = empresaRepository.findById(request.empresaId())
            .filter(Empresa::isAtiva)
            .orElseThrow(() -> new ResourceNotFoundException("Empresa ativa não encontrada."));
        return salvar(request, request.role(), empresa);
    }

    @Transactional
    public Usuario alterarStatus(Long id, boolean ativo, boolean plataforma) {
        Usuario usuario = plataforma
            ? usuarioRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."))
            : usuarioRepository.findByIdAndEmpresaId(id, TenantContext.getRequired()).orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));
        if (!ativo && (usuario.getRole() == Role.SUPER_ADMIN || usuario.getRole() == Role.ADMIN)) {
            throw new RegraNegocioException(
                usuario.getRole() == Role.SUPER_ADMIN
                    ? "O SUPER_ADMIN não pode ser desativado."
                    : "O ADMIN da empresa não pode ser desativado."
            );
        }
        usuario.setAtivo(ativo);
        return usuarioRepository.save(usuario);
    }

    private Usuario salvar(UsuarioRequests.Criar request, Role role, Empresa empresa) {
        String email = request.email().trim().toLowerCase();
        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new RegraNegocioException("Já existe um usuário com este e-mail.");
        }
        return usuarioRepository.save(new Usuario(
            request.nome().trim(), email, passwordEncoder.encode(request.senha()), role, empresa
        ));
    }

    private void validarPerfilOperacional(Role role) {
        if (role != Role.PROFISSIONAL && role != Role.RECEPCIONISTA) {
            throw new RegraNegocioException("O administrador da empresa só pode criar os perfis Profissional e Recepcionista.");
        }
    }
}
