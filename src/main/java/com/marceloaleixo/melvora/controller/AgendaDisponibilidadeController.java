package com.marceloaleixo.melvora.controller;

import com.marceloaleixo.melvora.entity.enums.ModuloSistema;
import com.marceloaleixo.melvora.exception.RegraNegocioException;
import com.marceloaleixo.melvora.service.AgendaDisponibilidadeService;
import com.marceloaleixo.melvora.tenant.TenantContext;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/agenda/disponibilidade")
public class AgendaDisponibilidadeController {
    private final AgendaDisponibilidadeService service;
    private final com.marceloaleixo.melvora.service.ModuloAcessoService modulo;
    public AgendaDisponibilidadeController(AgendaDisponibilidadeService service, com.marceloaleixo.melvora.service.ModuloAcessoService modulo){this.service=service;this.modulo=modulo;}

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public String tela(Model model){
        modulo.exigir(ModuloSistema.AGENDA);
        var existentes=service.listarHorarios();
        List<DiaView> dias=new ArrayList<>();
        for(int i=1;i<=7;i++){
            final int diaSemana = i;
            var h=existentes.stream().filter(x->x.getDiaSemana()==diaSemana).findFirst().orElse(null);
            dias.add(new DiaView(diaSemana, DayOfWeek.of(diaSemana).getDisplayName(java.time.format.TextStyle.FULL, new java.util.Locale("pt","BR")),
                    h==null?"08:00":h.getHoraInicio().toString(), h==null?"18:00":h.getHoraFim().toString(), h==null?15:h.getIntervaloMinutos(), h!=null&&h.isAtivo()));
        }
        model.addAttribute("dias",dias); model.addAttribute("activePage","agenda");
        return "pages/agenda-disponibilidade";
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public String salvar(@RequestParam int diaSemana,@RequestParam String horaInicio,@RequestParam String horaFim,
                         @RequestParam int intervaloMinutos,@RequestParam(defaultValue="false") boolean ativo,RedirectAttributes ra){
        modulo.exigir(ModuloSistema.AGENDA);
        try{service.salvarHorario(diaSemana,LocalTime.parse(horaInicio),LocalTime.parse(horaFim),intervaloMinutos,ativo);ra.addFlashAttribute("sucesso","Horário de funcionamento salvo.");}
        catch(RuntimeException ex){ra.addFlashAttribute("erro", ex instanceof RegraNegocioException && ex.getMessage()!=null?ex.getMessage():"Não foi possível salvar o horário.");}
        return "redirect:/agenda/disponibilidade";
    }
    public record DiaView(int dia,String nome,String inicio,String fim,int intervalo,boolean ativo){}
}
