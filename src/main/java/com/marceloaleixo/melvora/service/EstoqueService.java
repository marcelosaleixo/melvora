package com.marceloaleixo.melvora.service;

import com.marceloaleixo.melvora.dto.LoteRequests;
import com.marceloaleixo.melvora.entity.AplicacaoMegaHair;
import com.marceloaleixo.melvora.entity.Empresa;
import com.marceloaleixo.melvora.entity.LoteMegaHair;
import com.marceloaleixo.melvora.entity.MovimentacaoEstoque;
import com.marceloaleixo.melvora.entity.Produto;
import com.marceloaleixo.melvora.entity.enums.TipoMovimentacaoEstoque;
import com.marceloaleixo.melvora.entity.enums.TipoProduto;
import com.marceloaleixo.melvora.exception.RegraNegocioException;
import com.marceloaleixo.melvora.exception.ResourceNotFoundException;
import com.marceloaleixo.melvora.repository.EmpresaRepository;
import com.marceloaleixo.melvora.repository.LoteMegaHairRepository;
import com.marceloaleixo.melvora.repository.MovimentacaoEstoqueRepository;
import com.marceloaleixo.melvora.repository.ProdutoRepository;
import com.marceloaleixo.melvora.tenant.TenantContext;

import java.util.Locale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EstoqueService {

    private final LoteMegaHairRepository loteRepository;
    private final MovimentacaoEstoqueRepository movimentacaoRepository;
    private final ProdutoRepository produtoRepository;
    private final EmpresaRepository empresaRepository;
    private final ModuloAcessoService moduloAcessoService;

    public EstoqueService(
            LoteMegaHairRepository loteRepository,
            MovimentacaoEstoqueRepository movimentacaoRepository,
            ProdutoRepository produtoRepository,
            EmpresaRepository empresaRepository,
            ModuloAcessoService moduloAcessoService) {
        this.loteRepository = loteRepository;
        this.movimentacaoRepository = movimentacaoRepository;
        this.produtoRepository = produtoRepository;
        this.empresaRepository = empresaRepository;
        this.moduloAcessoService = moduloAcessoService;
    }

    @Transactional
    public LoteMegaHair criarLote(
            LoteRequests.CriarLoteRequest request) {
        moduloAcessoService.exigir(com.marceloaleixo.melvora.entity.enums.ModuloSistema.ESTOQUE);

        if (request == null) {
            throw new RegraNegocioException(
                "Os dados do lote são obrigatórios."
            );
        }

        long empresaId = TenantContext.getRequired();

        Empresa empresa = empresaRepository.findById(empresaId)
            .filter(Empresa::isAtiva)
            .orElseThrow(() ->
                new ResourceNotFoundException(
                    "Empresa não encontrada ou inativa."
                )
            );

        Produto produto = produtoRepository
            .findByIdAndEmpresaId(
                request.produtoId(),
                empresaId
            )
            .orElseThrow(() ->
                new ResourceNotFoundException(
                    "Produto não encontrado."
                )
            );

        if (produto.getTipo() != TipoProduto.MEGA_HAIR) {
            throw new RegraNegocioException(
                "Somente produtos Mega Hair podem possuir lote rastreável."
            );
        }

        String codigo = request.codigo()
            .trim()
            .toUpperCase(Locale.ROOT);

        if (loteRepository.existsByEmpresaIdAndCodigo(
                empresaId,
                codigo)) {
            throw new RegraNegocioException(
                "Já existe um lote com este código."
            );
        }

        LoteMegaHair lote = loteRepository.save(
            new LoteMegaHair(
                empresa,
                produto,
                codigo,
                request.quantidade(),
                request.pesoPorUnidadeGramas(),
                request.comprimentoCm(),
                request.tipoFio(),
                request.cor().trim(),
                request.metodo()
            )
        );

        movimentacaoRepository.save(
            new MovimentacaoEstoque(
                empresa,
                lote,
                null,
                TipoMovimentacaoEstoque.ENTRADA,
                request.quantidade(),
                request.quantidade()
            )
        );

        return lote;
    }

    @Transactional(readOnly = true)
    public Page<LoteMegaHair> listar(Pageable pageable) {
        moduloAcessoService.exigir(com.marceloaleixo.melvora.entity.enums.ModuloSistema.ESTOQUE);
        return loteRepository.findByEmpresaId(
            TenantContext.getRequired(),
            pageable
        );
    }

    @Transactional(readOnly = true)
    public LoteMegaHair buscar(Long id) {
        moduloAcessoService.exigir(com.marceloaleixo.melvora.entity.enums.ModuloSistema.ESTOQUE);

        if (id == null || id <= 0) {
            throw new RegraNegocioException(
                "ID do lote inválido."
            );
        }

        return loteRepository
            .findByIdAndEmpresaId(
                id,
                TenantContext.getRequired()
            )
            .orElseThrow(() ->
                new ResourceNotFoundException(
                    "Lote não encontrado."
                )
            );
    }

    @Transactional
    public LoteMegaHair consumirParaAplicacao(
            Long loteId,
            int quantidade) {
        moduloAcessoService.exigir(com.marceloaleixo.melvora.entity.enums.ModuloSistema.ESTOQUE);

        if (quantidade <= 0) {
            throw new RegraNegocioException(
                "A quantidade aplicada deve ser maior que zero."
            );
        }

        LoteMegaHair lote = loteRepository
            .findByIdAndEmpresaIdForUpdate(
                loteId,
                TenantContext.getRequired()
            )
            .orElseThrow(() ->
                new ResourceNotFoundException(
                    "Lote não encontrado."
                )
            );

        if (quantidade > lote.getQuantidadeDisponivel()) {
            throw new RegraNegocioException(
                "Estoque insuficiente para o lote "
                + lote.getCodigo()
                + ". Disponível: "
                + lote.getQuantidadeDisponivel()
                + "."
            );
        }

        lote.retirar(quantidade);
        return lote;
    }

    @Transactional
    public MovimentacaoEstoque registrarSaida(
            LoteMegaHair lote,
            AplicacaoMegaHair aplicacao,
            int quantidade) {
        moduloAcessoService.exigir(com.marceloaleixo.melvora.entity.enums.ModuloSistema.ESTOQUE);

        if (quantidade <= 0) {
            throw new RegraNegocioException(
                "A quantidade da saída deve ser maior que zero."
            );
        }

        return movimentacaoRepository.save(
            new MovimentacaoEstoque(
                lote.getEmpresa(),
                lote,
                aplicacao,
                TipoMovimentacaoEstoque.SAIDA,
                quantidade,
                lote.getQuantidadeDisponivel()
            )
        );
    }
}
