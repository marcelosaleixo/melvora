package com.marceloaleixo.melvora.service;

import com.marceloaleixo.melvora.entity.Cliente;
import com.marceloaleixo.melvora.entity.Agendamento;
import com.marceloaleixo.melvora.entity.enums.StatusAgendamento;
import com.marceloaleixo.melvora.dto.ClientePainelData;
import com.marceloaleixo.melvora.repository.AgendamentoRepository;
import com.marceloaleixo.melvora.repository.ClienteRepository;
import com.marceloaleixo.melvora.dto.ClienteRequests;
import com.marceloaleixo.melvora.entity.Empresa;
import com.marceloaleixo.melvora.repository.EmpresaRepository;
import com.marceloaleixo.melvora.exception.RegraNegocioException;
import com.marceloaleixo.melvora.exception.ResourceNotFoundException;
import com.marceloaleixo.melvora.tenant.TenantContext;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import com.marceloaleixo.melvora.entity.enums.ModuloSistema;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClienteService {
    private final ClienteRepository clienteRepository;
    private final EmpresaRepository empresaRepository;
    private final ModuloAcessoService moduloAcessoService;
    private final AgendamentoRepository agendamentoRepository;

    public ClienteService(ClienteRepository clienteRepository, EmpresaRepository empresaRepository, ModuloAcessoService moduloAcessoService, AgendamentoRepository agendamentoRepository) {
        this.clienteRepository = clienteRepository;
        this.empresaRepository = empresaRepository;
        this.moduloAcessoService = moduloAcessoService;
        this.agendamentoRepository = agendamentoRepository;
    }

    @Transactional
    public Cliente criar(ClienteRequests.CriarClienteRequest request) {
        moduloAcessoService.exigir(ModuloSistema.CLIENTES);
        var empresa = empresaRepository.findById(TenantContext.getRequired())
                .orElseThrow(() -> new IllegalStateException("Empresa do usuário não encontrada."));
        if (!empresa.isAtiva()) throw new com.marceloaleixo.melvora.exception.RegraNegocioException("Empresa inativa.");
        return clienteRepository.save(new Cliente(empresa, request.nome().trim(), normalizar(request.telefone()),
                normalizar(request.email()), normalizar(request.observacoes())));
    }

    @Transactional(readOnly = true)
    public Page<Cliente> listar(Pageable pageable) {
        moduloAcessoService.exigir(ModuloSistema.CLIENTES);
        return clienteRepository.findByEmpresaIdAndAtivoTrue(TenantContext.getRequired(), pageable);
    }

    @Transactional(readOnly = true)
    public Page<Cliente> listarTodos(Pageable pageable) {
        moduloAcessoService.exigir(ModuloSistema.CLIENTES);
        return clienteRepository.findByEmpresaId(TenantContext.getRequired(), pageable);
    }

    @Transactional(readOnly = true)
    public ClientePainelData painelOperacional(Long id) {
        moduloAcessoService.exigir(ModuloSistema.CLIENTES);
        long empresaId = TenantContext.getRequired();
        clienteRepository.findByIdAndEmpresaId(id, empresaId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrada."));

        List<StatusAgendamento> ocupamAgenda = List.of(
                StatusAgendamento.AGENDADO,
                StatusAgendamento.CONFIRMADO,
                StatusAgendamento.EM_ATENDIMENTO);

        var proximos = agendamentoRepository.listarProximosDaCliente(
                empresaId, id, LocalDateTime.now(), ocupamAgenda, PageRequest.of(0, 1));
        var recentes = agendamentoRepository.listarRecentesDaCliente(
                empresaId, id, PageRequest.of(0, 6));

        Agendamento proximo = proximos.isEmpty() ? null : proximos.get(0);
        Agendamento ultimo = recentes.stream()
                .filter(a -> !a.getDataHoraInicio().isAfter(LocalDateTime.now()))
                .findFirst()
                .orElse(null);

        return new ClientePainelData(
                proximo,
                ultimo,
                List.copyOf(recentes),
                agendamentoRepository.countByEmpresaIdAndClienteId(empresaId, id),
                agendamentoRepository.countByEmpresaIdAndClienteIdAndStatus(empresaId, id, StatusAgendamento.CONCLUIDO),
                agendamentoRepository.countByEmpresaIdAndClienteIdAndStatus(empresaId, id, StatusAgendamento.CANCELADO));
    }

    @Transactional(readOnly = true)
    public Cliente buscar(Long id) {
        moduloAcessoService.exigir(ModuloSistema.CLIENTES);
        return clienteRepository.findByIdAndEmpresaId(id, TenantContext.getRequired())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado."));
    }

    @Transactional
    public Cliente editar(Long id, ClienteRequests.EditarClienteRequest request) {
        moduloAcessoService.exigir(ModuloSistema.CLIENTES);
        Cliente cliente = buscar(id);
        cliente.atualizar(normalizarObrigatorio(request.nome()), normalizar(request.telefone()),
                normalizar(request.email()), normalizar(request.observacoes()));
        return clienteRepository.save(cliente);
    }

    @Transactional
    public boolean alternarAtivo(Long id) {
        moduloAcessoService.exigir(ModuloSistema.CLIENTES);
        Cliente cliente = buscar(id);
        cliente.definirAtivo(!cliente.isAtivo());
        clienteRepository.save(cliente);
        return cliente.isAtivo();
    }

    private String normalizar(String value) { return value == null ? null : value.trim(); }
    private String normalizarObrigatorio(String value) {
        String v = normalizar(value);
        if (v == null || v.isBlank()) throw new RegraNegocioException("O nome da cliente é obrigatório.");
        return v;
    }
}
