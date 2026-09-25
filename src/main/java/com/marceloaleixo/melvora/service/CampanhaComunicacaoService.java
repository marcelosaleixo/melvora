package com.marceloaleixo.melvora.service;

import com.marceloaleixo.melvora.entity.CampanhaComunicacao;
import com.marceloaleixo.melvora.entity.CampanhaEnvio;
import com.marceloaleixo.melvora.entity.Cliente;
import com.marceloaleixo.melvora.entity.Empresa;
import com.marceloaleixo.melvora.entity.Servico;
import com.marceloaleixo.melvora.entity.enums.ModuloSistema;
import com.marceloaleixo.melvora.exception.RegraNegocioException;
import com.marceloaleixo.melvora.repository.*;
import com.marceloaleixo.melvora.tenant.TenantContext;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CampanhaComunicacaoService {
    private final ModuloAcessoService modulo;
    private final CampanhaComunicacaoRepository campanhaRepository;
    private final CampanhaEnvioRepository envioRepository;
    private final ClienteRepository clienteRepository;
    private final AtendimentoRepository atendimentoRepository;
    private final ServicoRepository servicoRepository;
    private final PreferenciaComunicacaoContatoRepository preferenciaRepository;
    private final EmpresaRepository empresaRepository;
    private final WhatsAppBusinessService whatsAppBusinessService;

    public CampanhaComunicacaoService(ModuloAcessoService modulo, CampanhaComunicacaoRepository campanhaRepository,
            CampanhaEnvioRepository envioRepository, ClienteRepository clienteRepository,
            AtendimentoRepository atendimentoRepository, ServicoRepository servicoRepository,
            PreferenciaComunicacaoContatoRepository preferenciaRepository, EmpresaRepository empresaRepository,
            WhatsAppBusinessService whatsAppBusinessService) {
        this.modulo=modulo; this.campanhaRepository=campanhaRepository; this.envioRepository=envioRepository;
        this.clienteRepository=clienteRepository; this.atendimentoRepository=atendimentoRepository;
        this.servicoRepository=servicoRepository; this.preferenciaRepository=preferenciaRepository;
        this.empresaRepository=empresaRepository; this.whatsAppBusinessService=whatsAppBusinessService;
    }

    @Transactional(readOnly=true)
    public List<CampanhaComunicacao> listar() {
        modulo.exigir(ModuloSistema.COMUNICACAO);
        return campanhaRepository.findByEmpresaIdOrderByCreatedAtDesc(TenantContext.getRequired());
    }

    @Transactional(readOnly=true)
    public long contarElegiveis(CampanhaComunicacao.Segmento segmento, Integer dias, Long servicoId) {
        modulo.exigir(ModuloSistema.COMUNICACAO);
        Long empresaId=TenantContext.getRequired();
        return candidatos(empresaId, segmento, dias, servicoId).stream().filter(id -> elegivelParaEnvio(empresaId,id,dias==null?30:dias)).count();
    }

    @Transactional
    public CampanhaComunicacao criar(String nome, CampanhaComunicacao.Segmento segmento, Integer dias, Long servicoId, String mensagem, Integer cooldown) {
        modulo.exigir(ModuloSistema.COMUNICACAO);
        validar(nome, segmento, dias, servicoId, mensagem, cooldown);
        Long empresaId=TenantContext.getRequired();
        Empresa empresa=empresaRepository.findById(empresaId).orElseThrow(() -> new RegraNegocioException("Empresa não encontrada."));
        Servico servico=null;
        if (segmento==CampanhaComunicacao.Segmento.SERVICO_REALIZADO) {
            servico=servicoRepository.findByIdAndEmpresaId(servicoId,empresaId).orElseThrow(() -> new RegraNegocioException("Serviço não encontrado."));
        }
        return campanhaRepository.save(new CampanhaComunicacao(empresa,nome.trim(),segmento,dias,servico,mensagem.trim(),cooldown));
    }

    @Transactional
    public ResultadoEnvio enviar(Long campanhaId) {
        modulo.exigir(ModuloSistema.COMUNICACAO);
        Long empresaId=TenantContext.getRequired();
        CampanhaComunicacao campanha=campanhaRepository.findByIdAndEmpresaId(campanhaId,empresaId).orElseThrow(() -> new RegraNegocioException("Campanha não encontrada."));
        if (campanha.getStatus()==CampanhaComunicacao.Status.ENVIANDO) throw new RegraNegocioException("A campanha já está sendo processada.");
        if (campanha.getStatus()==CampanhaComunicacao.Status.CONCLUIDA) throw new RegraNegocioException("A campanha já foi concluída.");
        campanha.iniciar();
        campanhaRepository.save(campanha);
        int enviados=0, bloqueados=0, erros=0;
        List<Long> ids=candidatos(empresaId,campanha.getSegmento(),campanha.getDiasSemRetorno(),campanha.getServico()==null?null:campanha.getServico().getId());
        for (Long clienteId : ids) {
            Cliente cliente=clienteRepository.findByIdAndEmpresaId(clienteId,empresaId).orElse(null);
            if (cliente==null) continue;
            if (envioRepository.existsByCampanhaIdAndClienteId(campanhaId,clienteId)) continue;
            String telefone=cliente.getTelefone();
            String mensagem=personalizar(campanha,cliente);
            CampanhaEnvio envio=new CampanhaEnvio(campanha.getEmpresa(),campanha,cliente,telefone==null?"":telefone,mensagem,CampanhaEnvio.Status.PENDENTE);
            if (!elegivelParaEnvio(empresaId,clienteId,campanha.getCooldownDias())) { envio.bloquear("Sem consentimento, sem telefone ou cooldown ativo."); envioRepository.save(envio); bloqueados++; continue; }
            try {
                String providerId=whatsAppBusinessService.enviarMensagemAutomaticaInterna(empresaId,cliente,telefone,mensagem);
                envio.marcarEnviada(providerId); enviados++;
            } catch (Exception ex) { envio.marcarErro(ex.getMessage()); erros++; }
            envioRepository.save(envio);
        }
        campanha.concluir(); campanhaRepository.save(campanha);
        return new ResultadoEnvio(enviados,bloqueados,erros);
    }

    private boolean elegivelParaEnvio(Long empresaId, Long clienteId, int cooldown) {
        Cliente cliente=clienteRepository.findByIdAndEmpresaId(clienteId,empresaId).orElse(null);
        if (cliente==null || !cliente.isAtivo() || cliente.getTelefone()==null || cliente.getTelefone().isBlank()) return false;
        var pref=preferenciaRepository.findByEmpresaIdAndClienteId(empresaId,clienteId).orElse(null);
        if (pref==null || !pref.isRetencaoOptIn() || pref.getRetencaoOptOutAt()!=null) return false;
        LocalDateTime ultima=envioRepository.ultimaEnviada(empresaId,clienteId,CampanhaEnvio.Status.ENVIADA);
        return ultima==null || !ultima.isAfter(LocalDateTime.now().minusDays(cooldown));
    }

    private List<Long> candidatos(Long empresaId, CampanhaComunicacao.Segmento segmento, Integer dias, Long servicoId) {
        return switch(segmento) {
            case INATIVAS -> atendimentoRepository.buscarOportunidadesRetencaoV2(empresaId, LocalDateTime.now().minusDays(dias==null?60:dias), org.springframework.data.domain.PageRequest.of(0,500)).stream().map(r -> (Long)r[0]).toList();
            case SEM_PROXIMO_AGENDAMENTO -> clienteRepository.idsSemProximoAgendamento(empresaId);
            case SERVICO_REALIZADO -> {
                Servico s=servicoRepository.findByIdAndEmpresaId(servicoId,empresaId).orElseThrow(() -> new RegraNegocioException("Serviço não encontrado."));
                yield atendimentoRepository.clientesQueRealizaramServico(empresaId,s.getNome());
            }
        };
    }

    private String personalizar(CampanhaComunicacao c, Cliente cliente) {
        String msg=c.getMensagem().replace("{cliente}",cliente.getNome()).replace("{empresa}",c.getEmpresa().getNomeFantasia());
        if (c.getServico()!=null) msg=msg.replace("{servico}",c.getServico().getNome());
        return msg;
    }

    private void validar(String nome, CampanhaComunicacao.Segmento segmento, Integer dias, Long servicoId, String mensagem, Integer cooldown) {
        if (nome==null || nome.isBlank() || nome.length()>120) throw new RegraNegocioException("Informe um nome válido para a campanha.");
        if (segmento==null) throw new RegraNegocioException("Selecione um segmento.");
        if (mensagem==null || mensagem.isBlank() || mensagem.length()>4000) throw new RegraNegocioException("A mensagem deve ter entre 1 e 4000 caracteres.");
        if (dias!=null && (dias<1 || dias>365)) throw new RegraNegocioException("Dias sem retorno deve estar entre 1 e 365.");
        if (cooldown==null || cooldown<1 || cooldown>365) throw new RegraNegocioException("Cooldown deve estar entre 1 e 365 dias.");
        if (segmento==CampanhaComunicacao.Segmento.SERVICO_REALIZADO && servicoId==null) throw new RegraNegocioException("Selecione o serviço do segmento.");
    }
    public record ResultadoEnvio(int enviados,int bloqueados,int erros) {}
}
