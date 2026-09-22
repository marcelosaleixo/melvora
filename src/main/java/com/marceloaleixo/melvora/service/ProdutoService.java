package com.marceloaleixo.melvora.service;

import com.marceloaleixo.melvora.dto.ProdutoRequests;
import com.marceloaleixo.melvora.entity.Empresa;
import com.marceloaleixo.melvora.entity.Produto;
import com.marceloaleixo.melvora.entity.ProdutoMegaHair;
import com.marceloaleixo.melvora.entity.enums.ModuloSistema;
import com.marceloaleixo.melvora.entity.enums.TipoProduto;
import com.marceloaleixo.melvora.exception.RegraNegocioException;
import com.marceloaleixo.melvora.exception.ResourceNotFoundException;
import com.marceloaleixo.melvora.repository.ProdutoMegaHairRepository;
import com.marceloaleixo.melvora.repository.ProdutoRepository;
import com.marceloaleixo.melvora.repository.LoteMegaHairRepository;
import com.marceloaleixo.melvora.tenant.TenantContext;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProdutoService {
    private final ProdutoRepository produtoRepository;
    private final ProdutoMegaHairRepository megaHairRepository;
    private final ModuloAcessoService moduloAcessoService;
    private final LoteMegaHairRepository loteRepository;

    public ProdutoService(ProdutoRepository produtoRepository,
                          ProdutoMegaHairRepository megaHairRepository,
                          ModuloAcessoService moduloAcessoService,
                          LoteMegaHairRepository loteRepository) {
        this.produtoRepository = produtoRepository;
        this.megaHairRepository = megaHairRepository;
        this.moduloAcessoService = moduloAcessoService;
        this.loteRepository = loteRepository;
    }

    @Transactional
    public Produto criar(ProdutoRequests.Criar request, Empresa empresa) {
        moduloAcessoService.exigir(ModuloSistema.PRODUTOS);
        validarEmpresa(empresa);
        validar(request);

        Long empresaId = empresa.getId();
        String nome = normalizarNome(request.nome());
        if (produtoRepository.existsByEmpresaIdAndNomeIgnoreCase(empresaId, nome)) {
            throw new RegraNegocioException("Já existe um produto com este nome nesta empresa.");
        }

        Produto produto = produtoRepository.save(
            new Produto(empresa, nome, request.tipo(), request.precoVenda(), request.precoCusto())
        );
        sincronizarMegaHair(produto, request.megaHair());
        return produto;
    }

    @Transactional(readOnly = true)
    public Page<Produto> listar(Pageable pageable) {
        moduloAcessoService.exigir(ModuloSistema.PRODUTOS);
        return produtoRepository.findByEmpresaId(TenantContext.getRequired(), pageable);
    }

    @Transactional(readOnly = true)
    public List<Produto> listarAtivos() {
        moduloAcessoService.exigir(ModuloSistema.PRODUTOS);
        return produtoRepository.findByEmpresaIdAndAtivoTrueOrderByNomeAsc(TenantContext.getRequired());
    }

    @Transactional(readOnly = true)
    public Produto buscar(Long id) {
        moduloAcessoService.exigir(ModuloSistema.PRODUTOS);
        validarId(id);
        return produtoRepository.findByIdAndEmpresaId(id, TenantContext.getRequired())
            .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado."));
    }

    @Transactional(readOnly = true)
    public ProdutoRequests.WebForm formulario(Long id) {
        Produto produto = buscar(id);
        var mh = megaHairRepository.findByProdutoId(id)
            .map(x -> new ProdutoRequests.ProdutoMegaHairData(
                x.getComprimentoCm(), x.getPesoGramas(), x.getTipoFio(),
                x.getCor(), x.getMetodo(), x.getOrigem()))
            .orElse(null);
        return new ProdutoRequests.WebForm(produto, mh);
    }

    @Transactional
    public Produto editar(Long id, ProdutoRequests.Editar request) {
        moduloAcessoService.exigir(ModuloSistema.PRODUTOS);
        validarId(id);
        validar(request);

        Produto produto = buscar(id);
        String nome = normalizarNome(request.nome());
        if (produtoRepository.existsByEmpresaIdAndNomeIgnoreCaseAndIdNot(
                produto.getEmpresa().getId(), nome, id)) {
            throw new RegraNegocioException("Já existe outro produto com este nome nesta empresa.");
        }

        if (request.tipo() == TipoProduto.MEGA_HAIR) {
            moduloAcessoService.exigir(ModuloSistema.MEGA_HAIR);
        }

        if (produto.getTipo() == TipoProduto.MEGA_HAIR
                && request.tipo() == TipoProduto.COMUM
                && loteRepository.existsByEmpresaIdAndProdutoId(produto.getEmpresa().getId(), produto.getId())) {
            throw new RegraNegocioException(
                "Não é possível transformar este produto em comum porque já existem lotes de Mega Hair vinculados. " +
                "Preserve o produto para manter o histórico do estoque."
            );
        }

        produto.atualizar(nome, request.tipo(), request.precoVenda(), request.precoCusto());
        sincronizarMegaHair(produto, request.megaHair());
        return produto;
    }

    @Transactional
    public boolean alternarAtivo(Long id) {
        moduloAcessoService.exigir(ModuloSistema.PRODUTOS);
        validarId(id);
        Produto produto = buscar(id);
        produto.definirAtivo(!produto.isAtivo());
        return produto.isAtivo();
    }

    private void sincronizarMegaHair(Produto produto, ProdutoRequests.MegaHair dados) {
        if (produto.getTipo() != TipoProduto.MEGA_HAIR) {
            megaHairRepository.findByProdutoId(produto.getId()).ifPresent(megaHairRepository::delete);
            return;
        }

        moduloAcessoService.exigir(ModuloSistema.MEGA_HAIR);
        if (dados == null) {
            throw new RegraNegocioException("Produto Mega Hair exige seus dados técnicos.");
        }

        String cor = dados.cor() == null ? null : dados.cor().trim();
        if (cor == null || cor.isBlank()) {
            throw new RegraNegocioException("A cor é obrigatória para produto Mega Hair.");
        }

        var existente = megaHairRepository.findByProdutoId(produto.getId());
        if (existente.isPresent()) {
            existente.get().atualizar(dados.comprimentoCm(), dados.pesoGramas(), dados.tipoFio(),
                cor, dados.metodo(), normalizarOpcional(dados.origem()));
        } else {
            megaHairRepository.save(new ProdutoMegaHair(produto, dados.comprimentoCm(), dados.pesoGramas(),
                dados.tipoFio(), cor, dados.metodo(), normalizarOpcional(dados.origem())));
        }
    }

    private void validar(ProdutoRequests.Criar request) {
        if (request == null) throw new RegraNegocioException("Os dados do produto são obrigatórios.");
        validarComum(request.nome(), request.tipo(), request.precoVenda(), request.precoCusto());
        if (request.tipo() == TipoProduto.MEGA_HAIR && request.megaHair() == null) {
            throw new RegraNegocioException("Produto Mega Hair exige seus dados técnicos.");
        }
        if (request.tipo() == TipoProduto.COMUM && request.megaHair() != null) {
            throw new RegraNegocioException("Produto comum não pode possuir dados específicos de Mega Hair.");
        }
    }

    private void validar(ProdutoRequests.Editar request) {
        if (request == null) throw new RegraNegocioException("Os dados do produto são obrigatórios.");
        validarComum(request.nome(), request.tipo(), request.precoVenda(), request.precoCusto());
        if (request.tipo() == TipoProduto.MEGA_HAIR && request.megaHair() == null) {
            throw new RegraNegocioException("Produto Mega Hair exige seus dados técnicos.");
        }
        if (request.tipo() == TipoProduto.COMUM && request.megaHair() != null) {
            throw new RegraNegocioException("Produto comum não pode possuir dados específicos de Mega Hair.");
        }
    }

    private void validarComum(String nome, TipoProduto tipo, java.math.BigDecimal venda, java.math.BigDecimal custo) {
        if (nome == null || nome.isBlank()) throw new RegraNegocioException("O nome do produto é obrigatório.");
        if (nome.trim().length() > 150) throw new RegraNegocioException("O nome do produto deve ter no máximo 150 caracteres.");
        if (tipo == null) throw new RegraNegocioException("O tipo do produto é obrigatório.");
        if (venda == null || venda.signum() < 0) throw new RegraNegocioException("O preço de venda é inválido.");
        if (custo == null || custo.signum() < 0) throw new RegraNegocioException("O preço de custo é inválido.");
    }

    private void validarEmpresa(Empresa empresa) {
        if (empresa == null || empresa.getId() == null || !empresa.isAtiva()) {
            throw new RegraNegocioException("A empresa não está disponível para esta operação.");
        }
    }

    private void validarId(Long id) {
        if (id == null || id <= 0) throw new RegraNegocioException("ID do produto inválido.");
    }

    private String normalizarNome(String nome) {
        return nome.trim().replaceAll("\\s+", " ");
    }

    private String normalizarOpcional(String valor) {
        if (valor == null) return null;
        String normalizado = valor.trim().replaceAll("\\s+", " ");
        return normalizado.isBlank() ? null : normalizado;
    }
}
