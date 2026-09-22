package com.marceloaleixo.melvora.controller;

import com.marceloaleixo.melvora.entity.enums.ModuloSistema;
import com.marceloaleixo.melvora.service.EmpresaService;
import com.marceloaleixo.melvora.service.ModuloAcessoService;
import java.util.Arrays;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class LicenciamentoController {
    private final EmpresaService empresaService;
    private final ModuloAcessoService moduloAcessoService;

    public LicenciamentoController(EmpresaService empresaService, ModuloAcessoService moduloAcessoService) {
        this.empresaService = empresaService;
        this.moduloAcessoService = moduloAcessoService;
    }

    @GetMapping("/empresas/{empresaId}/modulos")
    public String modulos(@PathVariable Long empresaId, Model model) {
        var empresa = empresaService.buscar(empresaId);
        var ativos = moduloAcessoService.modulosAtivosDaEmpresa(empresaId);
        model.addAttribute("empresa", empresa);
        model.addAttribute("modulos", Arrays.stream(ModuloSistema.values()).toList());
        model.addAttribute("ativos", ativos);
        return "pages/empresa-modulos";
    }

    @PostMapping("/empresas/{empresaId}/modulos")
    public String salvar(@PathVariable Long empresaId,
                         @RequestParam(name = "modulos", required = false) List<ModuloSistema> modulos,
                         RedirectAttributes ra) {
        try {
            moduloAcessoService.configurar(empresaId, modulos == null ? List.of() : modulos);
            ra.addFlashAttribute("sucesso", "Licenciamento atualizado com sucesso. Dependências necessárias foram liberadas automaticamente.");
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("erro", mensagemSegura(ex));
        }
        return "redirect:/empresas/{empresaId}/modulos";
    }

    private String mensagemSegura(RuntimeException ex) {
        return ex.getMessage() == null || ex.getMessage().isBlank()
                ? "Não foi possível atualizar o licenciamento."
                : ex.getMessage();
    }
}
