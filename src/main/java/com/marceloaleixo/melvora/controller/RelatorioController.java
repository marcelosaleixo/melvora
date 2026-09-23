package com.marceloaleixo.melvora.controller;

import com.marceloaleixo.melvora.entity.enums.ModuloSistema;
import com.marceloaleixo.melvora.service.RelatorioService;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class RelatorioController {
    private final RelatorioService service;
    public RelatorioController(RelatorioService service){this.service=service;}

    @GetMapping("/relatorios")
    @PreAuthorize("hasRole('ADMIN')")
    public String index(@RequestParam(required=false) LocalDate inicio, @RequestParam(required=false) LocalDate fim, Model model){
        LocalDate hoje=LocalDate.now();
        LocalDate primeiro=hoje.withDayOfMonth(1);
        inicio=inicio==null?primeiro:inicio; fim=fim==null?hoje:fim;
        try {
            model.addAttribute("relatorio",service.gerar(inicio,fim));
        } catch (RuntimeException e) {
            model.addAttribute("erro", e.getMessage()==null?"Não foi possível gerar o relatório.":e.getMessage());
        }
        model.addAttribute("inicio",inicio); model.addAttribute("fim",fim); model.addAttribute("modulo", ModuloSistema.RELATORIOS);
        return "pages/relatorios";
    }
}
