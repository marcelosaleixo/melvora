package com.marceloaleixo.melvora.controller;

import com.marceloaleixo.melvora.entity.CampanhaComunicacao;
import com.marceloaleixo.melvora.entity.enums.ModuloSistema;
import com.marceloaleixo.melvora.repository.ServicoRepository;
import com.marceloaleixo.melvora.service.CampanhaComunicacaoService;
import com.marceloaleixo.melvora.service.ModuloAcessoService;
import com.marceloaleixo.melvora.tenant.TenantContext;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/comunicacao/campanhas")
@PreAuthorize("hasRole('ADMIN')")
public class CampanhaComunicacaoController {
    private final ModuloAcessoService modulo;
    private final CampanhaComunicacaoService service;
    private final ServicoRepository servicoRepository;
    public CampanhaComunicacaoController(ModuloAcessoService modulo, CampanhaComunicacaoService service, ServicoRepository servicoRepository){this.modulo=modulo;this.service=service;this.servicoRepository=servicoRepository;}

    @GetMapping
    public String index(Model model){
        modulo.exigir(ModuloSistema.COMUNICACAO);
        Long empresaId=TenantContext.getRequired();
        model.addAttribute("campanhas",service.listar());
        model.addAttribute("servicos",servicoRepository.findAtivosByEmpresaId(empresaId));
        model.addAttribute("segmentos",CampanhaComunicacao.Segmento.values());
        model.addAttribute("activePage","campanhas");
        return "pages/campanhas-comunicacao";
    }

    @PostMapping
    public String criar(@RequestParam String nome,@RequestParam CampanhaComunicacao.Segmento segmento,
                        @RequestParam(required=false) Integer diasSemRetorno,@RequestParam(required=false) Long servicoId,
                        @RequestParam String mensagem,@RequestParam(defaultValue="30") Integer cooldownDias,
                        RedirectAttributes ra){
        try { service.criar(nome,segmento,diasSemRetorno,servicoId,mensagem,cooldownDias); ra.addFlashAttribute("sucesso","Campanha criada como rascunho. Revise e envie quando estiver pronta."); }
        catch(Exception ex){ra.addFlashAttribute("erro",ex.getMessage());}
        return "redirect:/comunicacao/campanhas";
    }

    @PostMapping("/{id}/enviar")
    public String enviar(@PathVariable Long id, RedirectAttributes ra){
        try { var r=service.enviar(id); ra.addFlashAttribute("sucesso","Campanha processada: "+r.enviados()+" enviada(s), "+r.bloqueados()+" bloqueada(s), "+r.erros()+" com erro."); }
        catch(Exception ex){ra.addFlashAttribute("erro",ex.getMessage());}
        return "redirect:/comunicacao/campanhas";
    }
}
