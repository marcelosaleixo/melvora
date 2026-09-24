package com.marceloaleixo.melvora.controller;

import com.marceloaleixo.melvora.entity.enums.ModuloSistema;
import com.marceloaleixo.melvora.exception.RegraNegocioException;
import com.marceloaleixo.melvora.service.ModuloAcessoService;
import com.marceloaleixo.melvora.service.WhatsAppConversaService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@Validated
@RequestMapping("/comunicacao/conversas")
public class WhatsAppConversaController {
    private final WhatsAppConversaService service;
    private final ModuloAcessoService moduloAcessoService;

    public WhatsAppConversaController(WhatsAppConversaService service, ModuloAcessoService moduloAcessoService) {
        this.service = service;
        this.moduloAcessoService = moduloAcessoService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','PROFISSIONAL','RECEPCIONISTA')")
    public String listar(@RequestParam(defaultValue = "") @Size(max = 100) String busca,
                         @RequestParam(defaultValue = "0") @Min(0) int page,
                         Model model) {
        moduloAcessoService.exigir(ModuloSistema.COMUNICACAO);
        var pagina = service.listar(busca, PageRequest.of(Math.max(page, 0), 20));
        model.addAttribute("pagina", pagina);
        model.addAttribute("busca", busca == null ? "" : busca);
        model.addAttribute("activePage", "conversas");
        return "pages/whatsapp-conversas";
    }

    @GetMapping("/{clienteId}")
    @PreAuthorize("hasAnyRole('ADMIN','PROFISSIONAL','RECEPCIONISTA')")
    public String detalhe(@PathVariable Long clienteId,
                          @RequestParam(defaultValue = "0") @Min(0) int page,
                          Model model) {
        moduloAcessoService.exigir(ModuloSistema.COMUNICACAO);
        var detalhe = service.detalheCliente(clienteId, PageRequest.of(Math.max(page, 0), 40));
        model.addAttribute("detalhe", detalhe);
        model.addAttribute("cliente", detalhe.cliente());
        model.addAttribute("pagina", detalhe.mensagens());
        model.addAttribute("activePage", "conversas");
        return "pages/whatsapp-conversa";
    }

    @GetMapping("/numero/{telefone}")
    @PreAuthorize("hasAnyRole('ADMIN','PROFISSIONAL','RECEPCIONISTA')")
    public String detalheTelefone(@PathVariable String telefone,
                                  @RequestParam(defaultValue = "0") @Min(0) int page,
                                  Model model) {
        moduloAcessoService.exigir(ModuloSistema.COMUNICACAO);
        var detalhe = service.detalheTelefone(telefone, PageRequest.of(Math.max(page, 0), 40));
        model.addAttribute("detalhe", detalhe);
        model.addAttribute("cliente", detalhe.cliente());
        model.addAttribute("pagina", detalhe.mensagens());
        model.addAttribute("activePage", "conversas");
        return "pages/whatsapp-conversa";
    }

    @PostMapping("/{clienteId}/enviar")
    @PreAuthorize("hasAnyRole('ADMIN','PROFISSIONAL','RECEPCIONISTA')")
    public String enviar(@PathVariable Long clienteId,
                         @RequestParam @Size(min = 1, max = 4000) String mensagem,
                         RedirectAttributes ra) {
        moduloAcessoService.exigir(ModuloSistema.COMUNICACAO);
        try {
            service.enviarParaCliente(clienteId, mensagem);
            ra.addFlashAttribute("sucesso", "Mensagem enviada pelo provedor WhatsApp configurado.");
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("erro", mensagemSegura(ex));
        }
        return "redirect:/comunicacao/conversas/" + clienteId;
    }

    @PostMapping("/{clienteId}/marcar-lida")
    @PreAuthorize("hasAnyRole('ADMIN','PROFISSIONAL','RECEPCIONISTA')")
    public String marcarLida(@PathVariable Long clienteId, RedirectAttributes ra) {
        moduloAcessoService.exigir(ModuloSistema.COMUNICACAO);
        try {
            service.marcarComoLida(clienteId, null);
            ra.addFlashAttribute("sucesso", "Conversa marcada como lida.");
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("erro", mensagemSegura(ex));
        }
        return "redirect:/comunicacao/conversas/" + clienteId;
    }

    private String mensagemSegura(RuntimeException ex) {
        if (ex instanceof RegraNegocioException && ex.getMessage() != null) return ex.getMessage();
        return "Não foi possível concluir a operação.";
    }
}
