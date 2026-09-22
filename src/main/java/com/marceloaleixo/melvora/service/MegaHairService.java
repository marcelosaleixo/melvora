package com.marceloaleixo.melvora.service;

import com.marceloaleixo.melvora.dto.MegaHairRequests;
import com.marceloaleixo.melvora.entity.AplicacaoLote;
import com.marceloaleixo.melvora.entity.AplicacaoMegaHair;
import com.marceloaleixo.melvora.entity.ManutencaoMegaHair;
import com.marceloaleixo.melvora.entity.enums.Role;
import com.marceloaleixo.melvora.exception.RegraNegocioException;
import com.marceloaleixo.melvora.exception.ResourceNotFoundException;
import com.marceloaleixo.melvora.repository.AplicacaoMegaHairRepository;
import com.marceloaleixo.melvora.repository.ClienteRepository;
import com.marceloaleixo.melvora.repository.ManutencaoMegaHairRepository;
import com.marceloaleixo.melvora.repository.UsuarioRepository;
import com.marceloaleixo.melvora.tenant.TenantContext;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MegaHairService {

    private final AplicacaoMegaHairRepository aplicacaoRepository;
    private final ManutencaoMegaHairRepository manutencaoRepository;
    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final EstoqueService estoqueService;
    private final ModuloAcessoService moduloAcessoService;

    public MegaHairService(
            AplicacaoMegaHairRepository aplicacaoRepository,
            ManutencaoMegaHairRepository manutencaoRepository,
            ClienteRepository clienteRepository,
            UsuarioRepository usuarioRepository,
            EstoqueService estoqueService,
            ModuloAcessoService moduloAcessoService) {
        this.aplicacaoRepository = aplicacaoRepository;
        this.manutencaoRepository = manutencaoRepository;
        this.clienteRepository = clienteRepository;
        this.usuarioRepository = usuarioRepository;
        this.estoqueService = estoqueService;
        this.moduloAcessoService = moduloAcessoService;
    }

    @Transactional
    public AplicacaoMegaHair aplicar(
            MegaHairRequests.AplicarRequest request) {
        moduloAcessoService.exigir(com.marceloaleixo.melvora.entity.enums.ModuloSistema.MEGA_HAIR);

        if (request == null) {
            throw new RegraNegocioException(
                "Os dados da aplicação são obrigatórios."
            );
        }

        long empresaId = TenantContext.getRequired();

        var cliente = clienteRepository
            .findByIdAndEmpresaId(
                request.clienteId(),
                empresaId
            )
            .filter(c -> c.isAtivo())
            .orElseThrow(() ->
                new ResourceNotFoundException(
                    "Cliente não encontrado ou inativo."
                )
            );

        var profissional = usuarioRepository
            .findByIdAndEmpresaId(
                request.profissionalId(),
                empresaId
            )
            .filter(u ->
                u.isAtivo()
                && (u.getRole() == Role.PROFISSIONAL
                    || u.getRole() == Role.ADMIN)
            )
            .orElseThrow(() ->
                new ResourceNotFoundException(
                    "Profissional não encontrado ou sem permissão."
                )
            );

        if (request.dataAplicacao().isAfter(LocalDate.now())) {
            throw new RegraNegocioException(
                "A data da aplicação não pode estar no futuro."
            );
        }

        Set<Long> lotesInformados = new HashSet<>();

        for (var item : request.lotes()) {
            if (!lotesInformados.add(item.loteId())) {
                throw new RegraNegocioException(
                    "O mesmo lote não pode ser informado mais de uma vez na aplicação."
                );
            }
        }

        AplicacaoMegaHair aplicacao =
            new AplicacaoMegaHair(
                cliente.getEmpresa(),
                cliente,
                profissional,
                request.dataAplicacao(),
                normalizar(request.observacoes())
            );

        aplicacaoRepository.save(aplicacao);

        for (var item : request.lotes()) {

            var lote = estoqueService.consumirParaAplicacao(
                item.loteId(),
                item.quantidade()
            );

            if (!lote.getEmpresa().getId().equals(empresaId)) {
                throw new ResourceNotFoundException(
                    "Lote não pertence à empresa atual."
                );
            }

            var itemAplicacao =
                new AplicacaoLote(
                    lote,
                    item.quantidade()
                );

            aplicacao.adicionarLote(itemAplicacao);

            estoqueService.registrarSaida(
                lote,
                aplicacao,
                item.quantidade()
            );
        }

        return aplicacaoRepository.save(aplicacao);
    }

    @Transactional
    public ManutencaoMegaHair registrarManutencao(
            Long aplicacaoId,
            Long profissionalId,
            MegaHairRequests.ManutencaoRequest request) {
        moduloAcessoService.exigir(com.marceloaleixo.melvora.entity.enums.ModuloSistema.MEGA_HAIR);

        long empresaId = TenantContext.getRequired();

        var aplicacao = aplicacaoRepository
            .findByIdAndEmpresaId(
                aplicacaoId,
                empresaId
            )
            .orElseThrow(() ->
                new ResourceNotFoundException(
                    "Aplicação não encontrada."
                )
            );

        if (request.dataManutencao()
                .isBefore(aplicacao.getDataAplicacao())) {
            throw new RegraNegocioException(
                "A manutenção não pode ser anterior à aplicação."
            );
        }

        var profissional = usuarioRepository
            .findByIdAndEmpresaId(
                profissionalId,
                empresaId
            )
            .filter(u ->
                u.isAtivo()
                && (u.getRole() == Role.PROFISSIONAL
                    || u.getRole() == Role.ADMIN)
            )
            .orElseThrow(() ->
                new ResourceNotFoundException(
                    "Profissional não encontrado ou sem permissão."
                )
            );

        return manutencaoRepository.save(
            new ManutencaoMegaHair(
                aplicacao.getEmpresa(),
                aplicacao.getCliente(),
                aplicacao,
                profissional,
                request.dataManutencao(),
                request.tipo(),
                normalizar(request.observacoes())
            )
        );
    }

    @Transactional(readOnly = true)
    public MegaHairRequests.HistoricoResponse historico(
            Long clienteId) {
        moduloAcessoService.exigir(com.marceloaleixo.melvora.entity.enums.ModuloSistema.HISTORICO);

        long empresaId = TenantContext.getRequired();

        clienteRepository
            .findByIdAndEmpresaId(clienteId, empresaId)
            .orElseThrow(() ->
                new ResourceNotFoundException(
                    "Cliente não encontrado."
                )
            );

        var aplicacoes = aplicacaoRepository
            .findByEmpresaIdAndClienteIdOrderByDataAplicacaoDesc(
                empresaId,
                clienteId
            )
            .stream()
            .map(MegaHairRequests.AplicacaoResponse::from)
            .toList();

        var manutencoes = manutencaoRepository
            .findByEmpresaIdAndClienteIdOrderByDataManutencaoDesc(
                empresaId,
                clienteId
            )
            .stream()
            .map(MegaHairRequests.ManutencaoResponse::from)
            .toList();

        return new MegaHairRequests.HistoricoResponse(
            aplicacoes,
            manutencoes
        );
    }

    private String normalizar(String value) {
        return value == null ? null : value.trim();
    }
}
