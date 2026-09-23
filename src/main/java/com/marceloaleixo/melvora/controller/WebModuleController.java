package com.marceloaleixo.melvora.controller;

import com.marceloaleixo.melvora.dto.ClienteRequests;
import com.marceloaleixo.melvora.dto.LoteRequests;
import com.marceloaleixo.melvora.dto.MegaHairRequests;
import com.marceloaleixo.melvora.dto.ProdutoRequests;
import com.marceloaleixo.melvora.entity.enums.MetodoMegaHair;
import com.marceloaleixo.melvora.entity.enums.ModuloSistema;
import com.marceloaleixo.melvora.entity.enums.TipoFio;
import com.marceloaleixo.melvora.entity.enums.TipoProduto;
import com.marceloaleixo.melvora.exception.RegraNegocioException;
import com.marceloaleixo.melvora.repository.ClienteRepository;
import com.marceloaleixo.melvora.repository.EmpresaRepository;
import com.marceloaleixo.melvora.repository.UsuarioRepository;
import com.marceloaleixo.melvora.service.ClienteService;
import com.marceloaleixo.melvora.service.AgendaService;
import com.marceloaleixo.melvora.service.EstoqueService;
import com.marceloaleixo.melvora.service.MegaHairService;
import com.marceloaleixo.melvora.service.ModuloAcessoService;
import com.marceloaleixo.melvora.service.ProdutoService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class WebModuleController {
    private final ClienteService clienteService;
    private final ProdutoService produtoService;
    private final EstoqueService estoqueService;
    private final MegaHairService megaHairService;
    private final EmpresaRepository empresaRepository;
    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final ModuloAcessoService moduloAcessoService;
    private final AgendaService agendaService;

    public WebModuleController(ClienteService clienteService, ProdutoService produtoService, EstoqueService estoqueService,
                                MegaHairService megaHairService, EmpresaRepository empresaRepository,
                                ClienteRepository clienteRepository, UsuarioRepository usuarioRepository, ModuloAcessoService moduloAcessoService, AgendaService agendaService) {
        this.clienteService = clienteService; this.produtoService = produtoService; this.estoqueService = estoqueService;
        this.megaHairService = megaHairService; this.empresaRepository = empresaRepository; this.clienteRepository = clienteRepository;
        this.usuarioRepository = usuarioRepository; this.moduloAcessoService = moduloAcessoService; this.agendaService = agendaService;
    }

    @GetMapping("/clientes")
    @PreAuthorize("hasAnyRole('ADMIN','PROFISSIONAL','RECEPCIONISTA')")
    public String clientes(@RequestParam(defaultValue="0") int page, Model model) {
        moduloAcessoService.exigir(ModuloSistema.CLIENTES);
        model.addAttribute("pagina", clienteService.listar(PageRequest.of(Math.max(page,0), 15, Sort.by("nome").ascending())));
        model.addAttribute("clienteForm", new ClienteRequests.CriarClienteRequest("", "", "", ""));
        return "pages/clientes";
    }

    @PostMapping("/clientes")
    @PreAuthorize("hasAnyRole('ADMIN','PROFISSIONAL','RECEPCIONISTA')")
    public String criarCliente(@Valid @ModelAttribute("clienteForm") ClienteRequests.CriarClienteRequest form,
                               BindingResult bindingResult, Model model, RedirectAttributes ra) {
        moduloAcessoService.exigir(ModuloSistema.CLIENTES);
        if (bindingResult.hasErrors()) {
            model.addAttribute("pagina", clienteService.listar(PageRequest.of(0, 15, Sort.by("nome").ascending())));
            return "pages/clientes";
        }
        try {
            clienteService.criar(form);
            ra.addFlashAttribute("sucesso", "Cliente cadastrada com sucesso.");
        } catch (RuntimeException ex) {
            model.addAttribute("erro", mensagemSegura(ex));
            model.addAttribute("pagina", clienteService.listar(PageRequest.of(0, 15, Sort.by("nome").ascending())));
            return "pages/clientes";
        }
        return "redirect:/clientes";
    }

    @GetMapping("/clientes/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','PROFISSIONAL','RECEPCIONISTA')")
    public String clienteDetalhe(@PathVariable Long id, Model model) {
        moduloAcessoService.exigir(ModuloSistema.CLIENTES);
        var cliente = clienteService.buscar(id);
        model.addAttribute("cliente", cliente);
        model.addAttribute("agendaAtiva", moduloAcessoService.possui(ModuloSistema.AGENDA));
        if (moduloAcessoService.possui(ModuloSistema.AGENDA)) {
            model.addAttribute("painelCliente", clienteService.painelOperacional(id));
        }
        model.addAttribute("historicoAtivo", moduloAcessoService.possui(ModuloSistema.HISTORICO));
        if (moduloAcessoService.possui(ModuloSistema.HISTORICO)) {
            model.addAttribute("historico", megaHairService.historico(id));
        }
        return "pages/cliente-detalhe";
    }

    @GetMapping("/clientes/{id}/editar")
    @PreAuthorize("hasAnyRole('ADMIN','PROFISSIONAL','RECEPCIONISTA')")
    public String editarClienteForm(@PathVariable Long id, Model model) {
        moduloAcessoService.exigir(ModuloSistema.CLIENTES);
        var cliente = clienteService.buscar(id);
        model.addAttribute("clienteId", id);
        model.addAttribute("clienteForm", new ClienteRequests.EditarClienteRequest(
                cliente.getNome(), cliente.getTelefone(), cliente.getEmail(), cliente.getObservacoes()));
        return "pages/cliente-editar";
    }

    @PostMapping("/clientes/{id}/editar")
    @PreAuthorize("hasAnyRole('ADMIN','PROFISSIONAL','RECEPCIONISTA')")
    public String editarCliente(@PathVariable Long id,
                                 @Valid @ModelAttribute("clienteForm") ClienteRequests.EditarClienteRequest form,
                                 BindingResult bindingResult, Model model, RedirectAttributes ra) {
        moduloAcessoService.exigir(ModuloSistema.CLIENTES);
        if (bindingResult.hasErrors()) {
            model.addAttribute("clienteId", id);
            return "pages/cliente-editar";
        }
        try {
            clienteService.editar(id, form);
            ra.addFlashAttribute("sucesso", "Cliente atualizada com sucesso.");
            return "redirect:/clientes/" + id;
        } catch (RuntimeException ex) {
            model.addAttribute("clienteId", id);
            model.addAttribute("erro", mensagemSegura(ex));
            return "pages/cliente-editar";
        }
    }

    @PostMapping("/clientes/{id}/alternar-status")
    @PreAuthorize("hasRole('ADMIN')")
    public String alternarStatusCliente(@PathVariable Long id, RedirectAttributes ra) {
        moduloAcessoService.exigir(ModuloSistema.CLIENTES);
        try {
            boolean ativo = clienteService.alternarAtivo(id);
            ra.addFlashAttribute("sucesso", ativo ? "Cliente ativada com sucesso." : "Cliente desativada com sucesso.");
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("erro", mensagemSegura(ex));
        }
        return "redirect:/clientes";
    }

    @GetMapping("/produtos")
    @PreAuthorize("hasRole('ADMIN')")
    public String produtos(@RequestParam(defaultValue="0") int page, Model model) {
        moduloAcessoService.exigir(ModuloSistema.PRODUTOS);
        model.addAttribute("pagina", produtoService.listar(PageRequest.of(Math.max(page,0), 15, Sort.by("nome").ascending())));
        model.addAttribute("tipos", TipoProduto.values());
        model.addAttribute("fios", TipoFio.values());
        model.addAttribute("metodos", MetodoMegaHair.values());
        if (!model.containsAttribute("produtoForm")) {
            model.addAttribute("produtoForm", new ProdutoRequests.WebForm());
        }
        return "pages/produtos";
    }

    @PostMapping("/produtos")
    @PreAuthorize("hasRole('ADMIN')")
    public String criarProduto(
            @Valid @ModelAttribute("produtoForm") ProdutoRequests.WebForm form,
            BindingResult bindingResult,
            @RequestParam(defaultValue="0") int page,
            Model model,
            RedirectAttributes ra) {
        moduloAcessoService.exigir(ModuloSistema.PRODUTOS);
        if (bindingResult.hasErrors()) {
            prepararCatalogoProdutos(model, page);
            return "pages/produtos";
        }
        try {
            produtoService.criar(form.toCriar(), empresaAtual());
            ra.addFlashAttribute("sucesso", "Produto cadastrado com sucesso.");
            return "redirect:/produtos";
        } catch (RuntimeException ex) {
            prepararCatalogoProdutos(model, page);
            model.addAttribute("erro", mensagemSegura(ex));
            return "pages/produtos";
        }
    }

    @GetMapping("/produtos/{id}/editar")
    @PreAuthorize("hasRole('ADMIN')")
    public String editarProdutoForm(@PathVariable Long id, Model model) {
        moduloAcessoService.exigir(ModuloSistema.PRODUTOS);
        var produto = produtoService.buscar(id);
        boolean megaHairSemModulo = produto.getTipo() == TipoProduto.MEGA_HAIR
                && !moduloAcessoService.possui(ModuloSistema.MEGA_HAIR);

        model.addAttribute("produtoId", id);
        model.addAttribute("produtoForm", produtoService.formulario(id));
        model.addAttribute("produtoMegaHairSemModulo", megaHairSemModulo);
        model.addAttribute("tipos", TipoProduto.values());
        model.addAttribute("fios", TipoFio.values());
        model.addAttribute("metodos", MetodoMegaHair.values());
        return "pages/produto-editar";
    }

    @PostMapping("/produtos/{id}/editar")
    @PreAuthorize("hasRole('ADMIN')")
    public String editarProduto(
            @PathVariable Long id,
            @Valid @ModelAttribute("produtoForm") ProdutoRequests.WebForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes ra) {
        moduloAcessoService.exigir(ModuloSistema.PRODUTOS);
        if (bindingResult.hasErrors()) {
            prepararDadosProduto(model, id);
            return "pages/produto-editar";
        }
        try {
            produtoService.editar(id, form.toEditar());
            ra.addFlashAttribute("sucesso", "Produto atualizado com sucesso.");
            return "redirect:/produtos";
        } catch (RuntimeException ex) {
            prepararDadosProduto(model, id);
            var produto = produtoService.buscar(id);
            model.addAttribute("produtoMegaHairSemModulo", produto.getTipo() == TipoProduto.MEGA_HAIR
                    && !moduloAcessoService.possui(ModuloSistema.MEGA_HAIR));
            model.addAttribute("erro", mensagemSegura(ex));
            return "pages/produto-editar";
        }
    }

    @PostMapping("/produtos/{id}/alternar-status")
    @PreAuthorize("hasRole('ADMIN')")
    public String alternarStatusProduto(@PathVariable Long id, RedirectAttributes ra) {
        moduloAcessoService.exigir(ModuloSistema.PRODUTOS);
        try {
            boolean ativo = produtoService.alternarAtivo(id);
            ra.addFlashAttribute("sucesso", ativo ? "Produto ativado com sucesso." : "Produto desativado com sucesso.");
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("erro", mensagemSegura(ex));
        }
        return "redirect:/produtos";
    }

    @GetMapping("/estoque")
    @PreAuthorize("hasAnyRole('ADMIN','PROFISSIONAL')")
    public String estoque(@RequestParam(defaultValue="0") int page, Model model) {
        moduloAcessoService.exigir(ModuloSistema.ESTOQUE);
        model.addAttribute("pagina", estoqueService.listar(PageRequest.of(Math.max(page,0), 15, Sort.by("codigo").ascending())));
        model.addAttribute("produtos", produtoService.listarAtivos().stream().filter(p -> p.getTipo() == TipoProduto.MEGA_HAIR).toList());
        model.addAttribute("fios", TipoFio.values()); model.addAttribute("metodos", MetodoMegaHair.values());
        return "pages/estoque";
    }

    @PostMapping("/estoque")
    @PreAuthorize("hasAnyRole('ADMIN','PROFISSIONAL')")
    public String criarLote(@RequestParam Long produtoId, @RequestParam String codigo, @RequestParam Integer quantidade,
                            @RequestParam BigDecimal pesoPorUnidadeGramas, @RequestParam Integer comprimentoCm,
                            @RequestParam TipoFio tipoFio, @RequestParam String cor, @RequestParam MetodoMegaHair metodo,
                            RedirectAttributes ra) {
        moduloAcessoService.exigir(ModuloSistema.ESTOQUE);
        try {
            estoqueService.criarLote(new LoteRequests.CriarLoteRequest(produtoId, codigo, quantidade, pesoPorUnidadeGramas, comprimentoCm, tipoFio, cor, metodo));
            ra.addFlashAttribute("sucesso", "Lote criado e entrada registrada no estoque.");
        } catch (RuntimeException ex) { ra.addFlashAttribute("erro", ex.getMessage()); }
        return "redirect:/estoque";
    }

    @GetMapping("/mega-hair")
    @PreAuthorize("hasAnyRole('ADMIN','PROFISSIONAL')")
    public String megaHair(@RequestParam(required = false) Long agendamentoId, Model model) {
        moduloAcessoService.exigir(ModuloSistema.MEGA_HAIR);
        model.addAttribute("clientes", clienteService.listar(PageRequest.of(0, 1000, Sort.by("nome").ascending())).getContent());
        model.addAttribute("profissionais", usuarioRepository.findProfissionaisAtivos(empresaAtual().getId()));
        model.addAttribute("lotes", estoqueService.listar(PageRequest.of(0, 1000, Sort.by("codigo").ascending())).getContent().stream()
                .filter(l -> l.getQuantidadeDisponivel() > 0)
                .toList());
        if (agendamentoId != null) {
            var agendamento = agendaService.buscar(agendamentoId);
            if (agendamento.getTipo() != com.marceloaleixo.melvora.entity.enums.TipoAgendamento.APLICACAO_MEGA_HAIR) throw new RegraNegocioException("O agendamento selecionado não é de aplicação de Mega Hair.");
            model.addAttribute("agendamentoContexto", agendamento);
        }
        return "pages/mega-hair";
    }

    @PostMapping("/mega-hair/aplicacoes")
    @PreAuthorize("hasAnyRole('ADMIN','PROFISSIONAL')")
    public String aplicar(@RequestParam Long clienteId, @RequestParam Long profissionalId, @RequestParam(required=false) Long agendamentoId,
                          @RequestParam List<Long> loteIds, @RequestParam List<Integer> quantidades,
                          @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso=org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate dataAplicacao,
                          @RequestParam(required=false) String observacoes, RedirectAttributes ra) {
        moduloAcessoService.exigir(ModuloSistema.MEGA_HAIR);
        try {
            if (loteIds == null || quantidades == null || loteIds.isEmpty() || loteIds.size() != quantidades.size()) {
                throw new RegraNegocioException("Selecione pelo menos um lote e informe a quantidade de cada lote.");
            }
            if (loteIds.size() > 20) {
                throw new RegraNegocioException("Uma aplicação pode utilizar no máximo 20 lotes.");
            }

            List<MegaHairRequests.ItemLoteRequest> itens = new ArrayList<>(loteIds.size());
            for (int i = 0; i < loteIds.size(); i++) {
                Long loteId = loteIds.get(i);
                Integer quantidade = quantidades.get(i);
                if (loteId == null || loteId <= 0 || quantidade == null || quantidade <= 0) {
                    throw new RegraNegocioException("Revise os lotes e as quantidades informadas.");
                }
                itens.add(new MegaHairRequests.ItemLoteRequest(loteId, quantidade));
            }

            megaHairService.aplicar(new MegaHairRequests.AplicarRequest(clienteId, profissionalId, agendamentoId, dataAplicacao, observacoes, itens));
            ra.addFlashAttribute("sucesso", "Aplicação registrada e estoque atualizado.");
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("erro", mensagemSegura(ex));
        }
        return "redirect:/mega-hair";
    }

    @GetMapping("/mega-hair/historico/{clienteId}")
    @PreAuthorize("hasAnyRole('ADMIN','PROFISSIONAL','RECEPCIONISTA')")
    public String historico(@PathVariable Long clienteId, Model model) {
        moduloAcessoService.exigir(ModuloSistema.HISTORICO);
        var cliente = clienteService.buscar(clienteId);
        model.addAttribute("cliente", cliente);
        model.addAttribute("historico", megaHairService.historico(clienteId));
        return "pages/historico-mega-hair";
    }

    @GetMapping("/mega-hair/manutencao/novo")
    @PreAuthorize("hasAnyRole('ADMIN','PROFISSIONAL')")
    public String novaManutencaoPorAgendamento(@RequestParam Long agendamentoId) {
        moduloAcessoService.exigir(ModuloSistema.MEGA_HAIR);
        var agendamento = agendaService.buscar(agendamentoId);
        if (agendamento.getTipo() != com.marceloaleixo.melvora.entity.enums.TipoAgendamento.MANUTENCAO_MEGA_HAIR) throw new RegraNegocioException("O agendamento selecionado não é de manutenção de Mega Hair.");
        Long aplicacaoId = megaHairService.ultimaAplicacaoIdDaCliente(agendamento.getCliente().getId());
        return "redirect:/mega-hair/manutencao/" + aplicacaoId + "?agendamentoId=" + agendamentoId;
    }

    @GetMapping("/mega-hair/manutencao/{aplicacaoId}")
    @PreAuthorize("hasAnyRole('ADMIN','PROFISSIONAL')")
    public String manutencaoForm(@PathVariable Long aplicacaoId, @RequestParam(required=false) Long agendamentoId, Model model) {
        moduloAcessoService.exigir(ModuloSistema.MEGA_HAIR);
        model.addAttribute("aplicacaoId", aplicacaoId);
        model.addAttribute("profissionais", usuarioRepository.findByEmpresaIdAndAtivoTrueOrderByNomeAsc(empresaAtual().getId()));
        model.addAttribute("tiposManutencao", new String[]{"MANUTENCAO","REAPLICACAO","REMOCAO"});
        model.addAttribute("agendamentoId", agendamentoId);
        if (agendamentoId != null) model.addAttribute("agendamentoContexto", agendaService.buscar(agendamentoId));
        return "pages/manutencao-mega-hair";
    }

    @PostMapping("/mega-hair/manutencao/{aplicacaoId}")
    @PreAuthorize("hasAnyRole('ADMIN','PROFISSIONAL')")
    public String registrarManutencao(@PathVariable Long aplicacaoId, @RequestParam Long profissionalId, @RequestParam(required=false) Long agendamentoId, @RequestParam String tipo,
                                      @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso=org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate dataManutencao,
                                      @RequestParam(required=false) String observacoes, RedirectAttributes ra) {
        moduloAcessoService.exigir(ModuloSistema.MEGA_HAIR);
        try { megaHairService.registrarManutencao(aplicacaoId, profissionalId, new MegaHairRequests.ManutencaoRequest(agendamentoId, dataManutencao, tipo, observacoes)); ra.addFlashAttribute("sucesso", "Manutenção registrada."); }
        catch (RuntimeException ex) { ra.addFlashAttribute("erro", ex.getMessage()); }
        return "redirect:/dashboard";
    }

    private void prepararCatalogoProdutos(Model model, int page) {
        model.addAttribute("pagina", produtoService.listar(PageRequest.of(Math.max(page,0), 15, Sort.by("nome").ascending())));
        model.addAttribute("tipos", TipoProduto.values());
        model.addAttribute("fios", TipoFio.values());
        model.addAttribute("metodos", MetodoMegaHair.values());
    }

    private void prepararDadosProduto(Model model, Long id) {
        model.addAttribute("produtoId", id);
        model.addAttribute("tipos", TipoProduto.values());
        model.addAttribute("fios", TipoFio.values());
        model.addAttribute("metodos", MetodoMegaHair.values());

        var produto = produtoService.buscar(id);
        model.addAttribute("produtoMegaHairSemModulo", produto.getTipo() == TipoProduto.MEGA_HAIR
                && !moduloAcessoService.possui(ModuloSistema.MEGA_HAIR));
    }

    private String mensagemSegura(RuntimeException ex) {
        String mensagem = ex.getMessage();
        return (mensagem == null || mensagem.isBlank())
            ? "Não foi possível concluir a operação."
            : mensagem;
    }

    private com.marceloaleixo.melvora.entity.Empresa empresaAtual() {
        Long id = com.marceloaleixo.melvora.tenant.TenantContext.getRequired();
        return empresaRepository.findById(id).filter(com.marceloaleixo.melvora.entity.Empresa::isAtiva)
                .orElseThrow(() -> new RegraNegocioException("Empresa atual não encontrada ou inativa."));
    }
}
