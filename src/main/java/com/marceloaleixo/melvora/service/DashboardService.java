package com.marceloaleixo.melvora.service;

import com.marceloaleixo.melvora.dto.DashboardData;
import com.marceloaleixo.melvora.entity.enums.ModuloSistema;
import com.marceloaleixo.melvora.entity.enums.Role;
import com.marceloaleixo.melvora.exception.ResourceNotFoundException;
import com.marceloaleixo.melvora.repository.AplicacaoMegaHairRepository;
import com.marceloaleixo.melvora.repository.ClienteRepository;
import com.marceloaleixo.melvora.repository.EmpresaRepository;
import com.marceloaleixo.melvora.repository.LoteMegaHairRepository;
import com.marceloaleixo.melvora.repository.ManutencaoMegaHairRepository;
import com.marceloaleixo.melvora.repository.ProdutoRepository;
import com.marceloaleixo.melvora.repository.UsuarioRepository;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {
    private final UsuarioRepository usuarioRepository;
    private final EmpresaRepository empresaRepository;
    private final ClienteRepository clienteRepository;
    private final ProdutoRepository produtoRepository;
    private final LoteMegaHairRepository loteRepository;
    private final AplicacaoMegaHairRepository aplicacaoRepository;
    private final ManutencaoMegaHairRepository manutencaoRepository;
    private final ModuloAcessoService moduloAcessoService;

    public DashboardService(UsuarioRepository usuarioRepository, EmpresaRepository empresaRepository,
                            ClienteRepository clienteRepository, ProdutoRepository produtoRepository,
                            LoteMegaHairRepository loteRepository, AplicacaoMegaHairRepository aplicacaoRepository,
                            ManutencaoMegaHairRepository manutencaoRepository, ModuloAcessoService moduloAcessoService) {
        this.usuarioRepository = usuarioRepository;
        this.empresaRepository = empresaRepository;
        this.clienteRepository = clienteRepository;
        this.produtoRepository = produtoRepository;
        this.loteRepository = loteRepository;
        this.aplicacaoRepository = aplicacaoRepository;
        this.manutencaoRepository = manutencaoRepository;
        this.moduloAcessoService = moduloAcessoService;
    }

    @Transactional(readOnly = true)
    public DashboardData carregar(String email) {
        var usuario = usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário autenticado não encontrado."));

        if (usuario.getRole() == Role.SUPER_ADMIN) {
            return dashboardPlataforma(usuario.getNome(), usuario.getEmail());
        }

        if (usuario.getEmpresa() == null || !usuario.getEmpresa().isAtiva()) {
            throw new ResourceNotFoundException("Empresa do usuário não encontrada ou está inativa.");
        }

        long empresaId = usuario.getEmpresa().getId();
        EnumSet<ModuloSistema> ativos = EnumSet.noneOf(ModuloSistema.class);
        ativos.addAll(moduloAcessoService.modulosAtivosDaEmpresa(empresaId));

        long clientes = ativos.contains(ModuloSistema.CLIENTES)
                ? clienteRepository.countByEmpresaIdAndAtivoTrue(empresaId) : 0;
        long produtos = ativos.contains(ModuloSistema.PRODUTOS)
                ? produtoRepository.countByEmpresaIdAndAtivoTrue(empresaId) : 0;
        long lotes = ativos.contains(ModuloSistema.ESTOQUE)
                ? loteRepository.countByEmpresaId(empresaId) : 0;
        long aplicacoes = ativos.contains(ModuloSistema.MEGA_HAIR)
                ? aplicacaoRepository.countByEmpresaId(empresaId) : 0;
        long manutencoes = ativos.contains(ModuloSistema.MEGA_HAIR)
                ? manutencaoRepository.countByEmpresaId(empresaId) : 0;
        long usuarios = ativos.contains(ModuloSistema.EQUIPE)
                ? usuarioRepository.countByEmpresaIdAndAtivoTrue(empresaId) : 0;

        return new DashboardData(
                usuario.getNome(), usuario.getEmail(), usuario.getRole().name(), usuario.getEmpresa().getNomeFantasia(),
                false, 1, usuarios, clientes, produtos, lotes, aplicacoes, manutencoes,
                modulosEmpresa(usuario.getRole(), ativos));
    }

    private DashboardData dashboardPlataforma(String nome, String email) {
        return new DashboardData(nome, email, Role.SUPER_ADMIN.name(), "Plataforma Melvora", true,
                empresaRepository.countByAtivaTrue(), usuarioRepository.countByAtivo(true),
                clienteRepository.countByAtivo(true), produtoRepository.countByAtivo(true), loteRepository.count(),
                aplicacaoRepository.count(), manutencaoRepository.count(), List.of(
                        new DashboardData.Modulo("Empresas", "Gerencie os estabelecimentos da plataforma.", "🏢", "/empresas"),
                        new DashboardData.Modulo("Licenciamento", "Defina os módulos contratados por empresa.", "🔐", "/empresas"),
                        new DashboardData.Modulo("Usuários", "Acompanhe os acessos cadastrados.", "👥", "/super-admin/usuarios")));
    }

    private List<DashboardData.Modulo> modulosEmpresa(Role role, EnumSet<ModuloSistema> ativos) {
        var modulos = new ArrayList<DashboardData.Modulo>();
        adicionarSeAtivo(modulos, ativos, ModuloSistema.CLIENTES, "/clientes");
        if (ativos.contains(ModuloSistema.MEGA_HAIR) && (role == Role.ADMIN || role == Role.PROFISSIONAL)) {
            adicionarSeAtivo(modulos, ativos, ModuloSistema.MEGA_HAIR, "/mega-hair");
        }
        if (ativos.contains(ModuloSistema.ESTOQUE) && (role == Role.ADMIN || role == Role.PROFISSIONAL)) {
            adicionarSeAtivo(modulos, ativos, ModuloSistema.ESTOQUE, "/estoque");
        }
        if (ativos.contains(ModuloSistema.PRODUTOS) && role == Role.ADMIN) {
            adicionarSeAtivo(modulos, ativos, ModuloSistema.PRODUTOS, "/produtos");
        }
        if (ativos.contains(ModuloSistema.EQUIPE) && role == Role.ADMIN) {
            adicionarSeAtivo(modulos, ativos, ModuloSistema.EQUIPE, "/usuarios");
        }
        if (ativos.contains(ModuloSistema.HISTORICO) &&
                (role == Role.ADMIN || role == Role.PROFISSIONAL || role == Role.RECEPCIONISTA)) {
            adicionarSeAtivo(modulos, ativos, ModuloSistema.HISTORICO, "/clientes");
        }
        if (ativos.contains(ModuloSistema.AGENDA) &&
                (role == Role.ADMIN || role == Role.PROFISSIONAL || role == Role.RECEPCIONISTA)) {
            adicionarSeAtivo(modulos, ativos, ModuloSistema.AGENDA, "/agenda");
        }
        if (ativos.contains(ModuloSistema.FINANCEIRO) && (role == Role.ADMIN || role == Role.RECEPCIONISTA)) {
            adicionarSeAtivo(modulos, ativos, ModuloSistema.FINANCEIRO, "/financeiro");
        }
        if (ativos.contains(ModuloSistema.SERVICOS) && role == Role.ADMIN) {
            adicionarSeAtivo(modulos, ativos, ModuloSistema.SERVICOS, "/servicos");
        }
        if (ativos.contains(ModuloSistema.RELATORIOS) && role == Role.ADMIN) {
            adicionarSeAtivo(modulos, ativos, ModuloSistema.RELATORIOS, "/relatorios");
        }
        return modulos;
    }

    private void adicionarSeAtivo(List<DashboardData.Modulo> destino, EnumSet<ModuloSistema> ativos,
                                  ModuloSistema modulo, String href) {
        if (ativos.contains(modulo)) {
            destino.add(new DashboardData.Modulo(modulo.getNome(), modulo.getDescricao(), modulo.getIcone(), href));
        }
    }
}
