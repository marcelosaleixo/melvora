package com.marceloaleixo.melvora.service;

import com.marceloaleixo.melvora.entity.*;
import com.marceloaleixo.melvora.entity.enums.ModuloSistema;
import com.marceloaleixo.melvora.entity.enums.Role;
import com.marceloaleixo.melvora.entity.enums.StatusAgendamento;
import com.marceloaleixo.melvora.exception.RegraNegocioException;
import com.marceloaleixo.melvora.repository.*;
import com.marceloaleixo.melvora.tenant.TenantContext;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AgendaDisponibilidadeService {
    private static final List<StatusAgendamento> OCUPADOS = List.of(StatusAgendamento.AGENDADO, StatusAgendamento.CONFIRMADO, StatusAgendamento.EM_ATENDIMENTO);
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private final HorarioFuncionamentoAgendaRepository horarioRepository;
    private final EmpresaRepository empresaRepository;
    private final ServicoRepository servicoRepository;
    private final UsuarioRepository usuarioRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final ModuloAcessoService moduloAcessoService;

    public AgendaDisponibilidadeService(HorarioFuncionamentoAgendaRepository horarioRepository, EmpresaRepository empresaRepository,
                                        ServicoRepository servicoRepository, UsuarioRepository usuarioRepository,
                                        AgendamentoRepository agendamentoRepository, ModuloAcessoService moduloAcessoService) {
        this.horarioRepository=horarioRepository; this.empresaRepository=empresaRepository; this.servicoRepository=servicoRepository;
        this.usuarioRepository=usuarioRepository; this.agendamentoRepository=agendamentoRepository; this.moduloAcessoService=moduloAcessoService;
    }

    @Transactional(readOnly=true)
    public List<HorarioFuncionamentoAgenda> listarHorarios(){
        moduloAcessoService.exigir(ModuloSistema.AGENDA);
        return horarioRepository.findByEmpresaIdOrderByDiaSemanaAsc(TenantContext.getRequired());
    }

    @Transactional
    public void salvarHorario(int dia, LocalTime inicio, LocalTime fim, int intervalo, boolean ativo){
        moduloAcessoService.exigir(ModuloSistema.AGENDA);
        if(dia<1||dia>7) throw new RegraNegocioException("Dia da semana inválido.");
        if(inicio==null||fim==null||!fim.isAfter(inicio)) throw new RegraNegocioException("O horário de funcionamento é inválido.");
        if(intervalo<5||intervalo>240) throw new RegraNegocioException("O intervalo deve estar entre 5 e 240 minutos.");
        Long empresaId=TenantContext.getRequired();
        Empresa empresa=empresaRepository.findById(empresaId).orElseThrow(()->new RegraNegocioException("Empresa não encontrada."));
        HorarioFuncionamentoAgenda h=horarioRepository.findByEmpresaIdAndDiaSemana(empresaId,dia).orElseGet(()->new HorarioFuncionamentoAgenda(empresa,DayOfWeek.of(dia),inicio,fim,intervalo,ativo));
        h.atualizar(inicio,fim,intervalo,ativo); horarioRepository.save(h);
    }

    @Transactional(readOnly=true)
    public List<Slot> consultar(LocalDate data, Long servicoId, Long profissionalId, int limite){
        return consultar(TenantContext.getRequired(), data, servicoId, profissionalId, limite);
    }

    @Transactional(readOnly=true)
    public List<Slot> consultar(Long empresaId, LocalDate data, Long servicoId, Long profissionalId, int limite){
        moduloAcessoService.exigir(empresaId, ModuloSistema.AGENDA);
        if(data==null) throw new RegraNegocioException("Informe a data desejada.");
        if(data.isBefore(LocalDate.now())) throw new RegraNegocioException("A data informada já passou.");
        HorarioFuncionamentoAgenda horario=horarioRepository.findByEmpresaIdAndDiaSemana(empresaId,data.getDayOfWeek().getValue()).orElse(null);
        if(horario==null||!horario.isAtivo()) return List.of();
        Servico servico=null;
        if(servicoId!=null) servico=servicoRepository.findByIdAndEmpresaId(servicoId,empresaId).filter(Servico::isAtivo).orElseThrow(()->new RegraNegocioException("Serviço não encontrado ou inativo."));
        int duracao=servico==null?30:servico.getDuracaoMinutos();
        List<Usuario> profissionais=profissionalId!=null
                ? usuarioRepository.findByIdAndEmpresaId(profissionalId,empresaId).filter(u->u.isAtivo()&&(u.getRole()==Role.ADMIN||u.getRole()==Role.PROFISSIONAL)).map(List::of).orElse(List.of())
                : usuarioRepository.findByEmpresaIdAndAtivoTrueAndRoleInOrderByNomeAsc(empresaId,List.of(Role.ADMIN,Role.PROFISSIONAL));
        List<Slot> slots=new ArrayList<>();
        LocalDateTime cursor=LocalDateTime.of(data,horario.getHoraInicio());
        LocalDateTime limiteDia=LocalDateTime.of(data,horario.getHoraFim());
        LocalDateTime agora=LocalDateTime.now();
        while(!cursor.plusMinutes(duracao).isAfter(limiteDia) && slots.size()<Math.max(1,limite)){
            LocalDateTime fim=cursor.plusMinutes(duracao);
            for(Usuario p:profissionais){
                if(servico!=null && !servico.podeSerExecutadoPor(p.getId())) continue;
                if(cursor.isBefore(agora)) continue;
                if(!agendamentoRepository.existeConflitoProfissional(empresaId,p.getId(),cursor,fim,OCUPADOS)){
                    slots.add(new Slot(cursor,fim,p.getId(),p.getNome()));
                    break;
                }
            }
            cursor=cursor.plusMinutes(horario.getIntervaloMinutos());
        }
        return slots;
    }

    public record Slot(LocalDateTime inicio, LocalDateTime fim, Long profissionalId, String profissional){
        public String hora(){return inicio.format(HORA);}
        public String texto(){return hora()+" — "+profissional;}
    }
}
