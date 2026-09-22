package com.marceloaleixo.melvora.controller;

import com.marceloaleixo.melvora.dto.UsuarioRequests;
import com.marceloaleixo.melvora.entity.enums.Role;
import com.marceloaleixo.melvora.entity.enums.ModuloSistema;
import com.marceloaleixo.melvora.service.ModuloAcessoService;
import com.marceloaleixo.melvora.repository.EmpresaRepository;
import com.marceloaleixo.melvora.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.validation.BindingResult;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class UsuarioController {
    private final UsuarioService service;
    private final EmpresaRepository empresaRepository;
    private final ModuloAcessoService moduloAcessoService;

    public UsuarioController(UsuarioService service, EmpresaRepository empresaRepository, ModuloAcessoService moduloAcessoService) {
        this.service = service;
        this.empresaRepository = empresaRepository;
        this.moduloAcessoService = moduloAcessoService;
    }

    @GetMapping("/usuarios")
    @PreAuthorize("hasRole('ADMIN')")
    public String usuarios(@RequestParam(defaultValue = "0") int page, Model model) {
        moduloAcessoService.exigir(ModuloSistema.EQUIPE);
        prepararModeloEmpresa(model, page);
        return "pages/usuarios";
    }

    @PostMapping("/usuarios")
    @PreAuthorize("hasRole('ADMIN')")
    public String criar(
            @Valid @ModelAttribute("usuarioForm") UsuarioRequests.Criar form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes ra) {

        moduloAcessoService.exigir(ModuloSistema.EQUIPE);
        if (bindingResult.hasErrors()) {
            prepararModeloEmpresa(model, 0);
            return "pages/usuarios";
        }

        try {
            service.criarParaEmpresa(form);
            ra.addFlashAttribute("sucesso", "Usuário cadastrado com sucesso.");
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("erro", mensagemSegura(ex));
        }
        return "redirect:/usuarios";
    }

    @PostMapping("/usuarios/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public String status(@PathVariable Long id, @RequestParam boolean ativo, RedirectAttributes ra) {
        moduloAcessoService.exigir(ModuloSistema.EQUIPE);
        try {
            service.alterarStatus(id, ativo, false);
            ra.addFlashAttribute("sucesso", ativo ? "Usuário ativado." : "Usuário desativado.");
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("erro", mensagemSegura(ex));
        }
        return "redirect:/usuarios";
    }

    @GetMapping("/super-admin/usuarios")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public String plataforma(@RequestParam(defaultValue = "0") int page, Model model) {
        prepararModeloPlataforma(model, page);
        return "pages/usuarios";
    }

    @PostMapping("/super-admin/usuarios")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public String criarPlataforma(
            @Valid @ModelAttribute("usuarioForm") UsuarioRequests.Criar form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes ra) {

        if (bindingResult.hasErrors()) {
            prepararModeloPlataforma(model, 0);
            return "pages/usuarios";
        }

        try {
            service.criarPelaPlataforma(form);
            ra.addFlashAttribute("sucesso", "Usuário cadastrado com sucesso.");
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("erro", mensagemSegura(ex));
        }
        return "redirect:/super-admin/usuarios";
    }

    @PostMapping("/super-admin/usuarios/{id}/status")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public String statusPlataforma(@PathVariable Long id, @RequestParam boolean ativo, RedirectAttributes ra) {
        try {
            service.alterarStatus(id, ativo, true);
            ra.addFlashAttribute("sucesso", ativo ? "Usuário ativado." : "Usuário desativado.");
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("erro", mensagemSegura(ex));
        }
        return "redirect:/super-admin/usuarios";
    }

    private void prepararModeloEmpresa(Model model, int page) {
        model.addAttribute("pagina", service.listarEmpresa(PageRequest.of(Math.max(0, page), 15, Sort.by("nome").ascending())));
        model.addAttribute("roles", new Role[]{Role.PROFISSIONAL, Role.RECEPCIONISTA});
        model.addAttribute("plataforma", false);
        if (!model.containsAttribute("usuarioForm")) {
            model.addAttribute("usuarioForm", new UsuarioRequests.Criar("", "", "", null, null));
        }
    }

    private void prepararModeloPlataforma(Model model, int page) {
        model.addAttribute("pagina", service.listarPlataforma(PageRequest.of(Math.max(0, page), 15, Sort.by("nome").ascending())));
        model.addAttribute("roles", new Role[]{Role.ADMIN, Role.PROFISSIONAL, Role.RECEPCIONISTA});
        model.addAttribute("empresas", empresaRepository.findAll(Sort.by("nomeFantasia").ascending()));
        model.addAttribute("plataforma", true);
        if (!model.containsAttribute("usuarioForm")) {
            model.addAttribute("usuarioForm", new UsuarioRequests.Criar("", "", "", null, null));
        }
    }

    private String mensagemSegura(RuntimeException ex) {
        return ex.getMessage() == null || ex.getMessage().isBlank() ? "Não foi possível concluir a operação." : ex.getMessage();
    }
}
