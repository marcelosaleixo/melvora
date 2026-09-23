package com.marceloaleixo.melvora.controller;

import com.marceloaleixo.melvora.dto.ServicoRequests;
import com.marceloaleixo.melvora.entity.enums.ModuloSistema;
import com.marceloaleixo.melvora.exception.RegraNegocioException;
import com.marceloaleixo.melvora.service.ModuloAcessoService;
import com.marceloaleixo.melvora.service.ServicoService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/servicos")
@PreAuthorize("hasRole('ADMIN')")
public class ServicoController {
    private final ServicoService service;
    private final com.marceloaleixo.melvora.repository.UsuarioRepository usuarioRepository;
    private final ModuloAcessoService moduloAcessoService;

    public ServicoController(ServicoService service, com.marceloaleixo.melvora.repository.UsuarioRepository usuarioRepository,
                             ModuloAcessoService moduloAcessoService) {
        this.service = service;
        this.usuarioRepository = usuarioRepository;
        this.moduloAcessoService = moduloAcessoService;
    }

    @GetMapping
    public String index(@RequestParam(defaultValue = "0") int page, Model model) {
        moduloAcessoService.exigir(ModuloSistema.SERVICOS);
        int pagina = Math.max(page, 0);
        model.addAttribute("pagina", service.listar(PageRequest.of(pagina, 10, Sort.by("nome").ascending())));
        model.addAttribute("servicoForm", new ServicoRequests.WebForm());
        model.addAttribute("profissionais", usuarioRepository.findProfissionaisAtivos(com.marceloaleixo.melvora.tenant.TenantContext.getRequired()));
        model.addAttribute("activePage", "servicos");
        return "pages/servicos";
    }

    @PostMapping
    public String criar(@Valid @ModelAttribute("servicoForm") ServicoRequests.WebForm form,
                        BindingResult bindingResult, Model model, RedirectAttributes ra) {
        moduloAcessoService.exigir(ModuloSistema.SERVICOS);
        if (bindingResult.hasErrors()) {
            carregarLista(model, form, 0);
            return "pages/servicos";
        }
        try {
            service.criar(form);
            ra.addFlashAttribute("sucesso", "Serviço cadastrado com sucesso.");
            return "redirect:/servicos";
        } catch (RuntimeException ex) {
            carregarLista(model, form, 0);
            model.addAttribute("erro", mensagem(ex));
            return "pages/servicos";
        }
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        moduloAcessoService.exigir(ModuloSistema.SERVICOS);
        try {
            var servico = service.buscar(id);
            model.addAttribute("servicoId", id);
            model.addAttribute("servico", servico);
            model.addAttribute("servicoForm", new ServicoRequests.WebForm(servico));
            model.addAttribute("profissionais", usuarioRepository.findProfissionaisAtivos(com.marceloaleixo.melvora.tenant.TenantContext.getRequired()));
            model.addAttribute("activePage", "servicos");
            return "pages/servico-editar";
        } catch (RuntimeException ex) {
            model.addAttribute("erro", mensagem(ex));
            return "redirect:/servicos";
        }
    }

    @PostMapping("/{id}/editar")
    public String salvarEdicao(@PathVariable Long id, @Valid @ModelAttribute("servicoForm") ServicoRequests.WebForm form,
                               BindingResult bindingResult, Model model, RedirectAttributes ra) {
        moduloAcessoService.exigir(ModuloSistema.SERVICOS);
        if (bindingResult.hasErrors()) {
            model.addAttribute("servicoId", id);
            model.addAttribute("servico", service.buscar(id));
            model.addAttribute("profissionais", usuarioRepository.findProfissionaisAtivos(com.marceloaleixo.melvora.tenant.TenantContext.getRequired()));
            model.addAttribute("activePage", "servicos");
            return "pages/servico-editar";
        }
        try {
            service.atualizar(id, form);
            ra.addFlashAttribute("sucesso", "Serviço atualizado com sucesso.");
            return "redirect:/servicos";
        } catch (RuntimeException ex) {
            model.addAttribute("servicoId", id);
            model.addAttribute("servico", service.buscar(id));
            model.addAttribute("profissionais", usuarioRepository.findProfissionaisAtivos(com.marceloaleixo.melvora.tenant.TenantContext.getRequired()));
            model.addAttribute("activePage", "servicos");
            model.addAttribute("erro", mensagem(ex));
            return "pages/servico-editar";
        }
    }

    @PostMapping("/{id}/alternar-status")
    public String alternarStatus(@PathVariable Long id, RedirectAttributes ra) {
        moduloAcessoService.exigir(ModuloSistema.SERVICOS);
        try {
            service.alternarStatus(id);
            ra.addFlashAttribute("sucesso", "Status do serviço atualizado.");
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("erro", mensagem(ex));
        }
        return "redirect:/servicos";
    }

    private void carregarLista(Model model, ServicoRequests.WebForm form, int page) {
        model.addAttribute("pagina", service.listar(PageRequest.of(page, 10, Sort.by("nome").ascending())));
        model.addAttribute("servicoForm", form);
        model.addAttribute("profissionais", usuarioRepository.findProfissionaisAtivos(com.marceloaleixo.melvora.tenant.TenantContext.getRequired()));
        model.addAttribute("activePage", "servicos");
    }

    private String mensagem(RuntimeException ex) {
        return ex.getMessage() == null || ex.getMessage().isBlank() ? "Não foi possível concluir a operação." : ex.getMessage();
    }
}
