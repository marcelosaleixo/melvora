package com.marceloaleixo.melvora.controller;

import com.marceloaleixo.melvora.entity.enums.ModuloSistema;
import com.marceloaleixo.melvora.service.ClienteCrm360Service;
import com.marceloaleixo.melvora.service.ClienteService;
import com.marceloaleixo.melvora.service.ModuloAcessoService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class ClienteCrm360Controller {
    private final ClienteService clienteService;
    private final ClienteCrm360Service crmService;
    private final ModuloAcessoService moduloAcessoService;

    public ClienteCrm360Controller(ClienteService clienteService,
                                    ClienteCrm360Service crmService,
                                    ModuloAcessoService moduloAcessoService) {
        this.clienteService = clienteService;
        this.crmService = crmService;
        this.moduloAcessoService = moduloAcessoService;
    }

    @GetMapping("/clientes/{id}/crm")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE','PROFISSIONAL','RECEPCIONISTA')")
    public String crm(@PathVariable Long id, Model model) {
        moduloAcessoService.exigir(ModuloSistema.CLIENTES);
        model.addAttribute("cliente", clienteService.buscar(id));
        model.addAttribute("crm", crmService.gerar(id));
        model.addAttribute("agendaAtiva", moduloAcessoService.possui(ModuloSistema.AGENDA));
        model.addAttribute("financeiroAtivo", moduloAcessoService.possui(ModuloSistema.FINANCEIRO));
        model.addAttribute("comunicacaoAtiva", moduloAcessoService.possui(ModuloSistema.COMUNICACAO));
        model.addAttribute("historicoAtivo", moduloAcessoService.possui(ModuloSistema.HISTORICO));
        model.addAttribute("activePage", "clientes");
        return "pages/cliente-crm-360";
    }
}
