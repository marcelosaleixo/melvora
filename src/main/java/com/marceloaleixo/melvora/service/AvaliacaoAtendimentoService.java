package com.marceloaleixo.melvora.service;

import com.marceloaleixo.melvora.dto.AvaliacaoRequests;
import com.marceloaleixo.melvora.entity.Agendamento;
import com.marceloaleixo.melvora.entity.Atendimento;
import com.marceloaleixo.melvora.entity.AvaliacaoAtendimento;
import com.marceloaleixo.melvora.entity.enums.ModuloSistema;
import com.marceloaleixo.melvora.entity.enums.StatusAgendamento;
import com.marceloaleixo.melvora.exception.RegraNegocioException;
import com.marceloaleixo.melvora.repository.AtendimentoRepository;
import com.marceloaleixo.melvora.repository.AvaliacaoAtendimentoRepository;
import com.marceloaleixo.melvora.tenant.TenantContext;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AvaliacaoAtendimentoService {
    private final AvaliacaoAtendimentoRepository repository;
    private final AtendimentoRepository atendimentoRepository;
    private final ModuloAcessoService moduloAcessoService;
    private final String publicBaseUrl;

    public AvaliacaoAtendimentoService(AvaliacaoAtendimentoRepository repository,
                                       AtendimentoRepository atendimentoRepository,
                                       ModuloAcessoService moduloAcessoService,
                                       @Value("${melvora.public-base-url:}") String publicBaseUrl) {
        this.repository = repository;
        this.atendimentoRepository = atendimentoRepository;
        this.moduloAcessoService = moduloAcessoService;
        this.publicBaseUrl = publicBaseUrl == null ? "" : publicBaseUrl.replaceAll("/+$", "");
    }

    @Transactional
    public AvaliacaoAtendimento garantirParaAtendimento(Atendimento atendimento) {
        if (atendimento == null || atendimento.getId() == null) return null;
        Long empresaId = atendimento.getEmpresa().getId();
        return repository.findByAtendimentoIdAndEmpresaId(atendimento.getId(), empresaId)
                .orElseGet(() -> repository.save(new AvaliacaoAtendimento(atendimento.getEmpresa(), atendimento)));
    }

    @Transactional(readOnly = true)
    public AvaliacaoAtendimento buscarPublica(String token) {
        if (token == null || token.isBlank()) throw new RegraNegocioException("Link de avaliação inválido.");
        return repository.findByPublicToken(token.trim())
                .orElseThrow(() -> new RegraNegocioException("Link de avaliação inválido ou expirado."));
    }

    @Transactional
    public void responderPublica(String token, AvaliacaoRequests.WebForm form) {
        AvaliacaoAtendimento avaliacao = buscarPublica(token);
        if (avaliacao.respondida()) throw new RegraNegocioException("Esta avaliação já foi respondida. Obrigado pelo feedback! 💜");
        Atendimento atendimento = avaliacao.getAtendimento();
        if (atendimento == null || atendimento.getAgendamento().getStatus() != StatusAgendamento.CONCLUIDO
                || atendimento.getDataHoraInicio() == null || atendimento.getDataHoraInicio().isAfter(LocalDateTime.now())) {
            throw new RegraNegocioException("A avaliação só pode ser enviada após um atendimento concluído.");
        }
        if (form.nota() == null || form.nota() < 1 || form.nota() > 5) throw new RegraNegocioException("Escolha uma nota de 1 a 5.");
        avaliacao.responder(form.nota(), form.comentario());
    }

    @Transactional(readOnly = true)
    public Page<AvaliacaoAtendimento> listar(Pageable pageable) {
        moduloAcessoService.exigir(ModuloSistema.COMUNICACAO);
        return repository.findByEmpresaIdOrderByCreatedAtDesc(TenantContext.getRequired(), pageable);
    }

    @Transactional(readOnly = true)
    public Resumo resumo() {
        moduloAcessoService.exigir(ModuloSistema.COMUNICACAO);
        Long empresaId = TenantContext.getRequired();
        Double media = repository.mediaNota(empresaId);
        return new Resumo(media == null ? 0.0 : media, repository.countRespondidas(empresaId));
    }

    public String linkPublico(AvaliacaoAtendimento avaliacao) {
        if (avaliacao == null) return null;
        String base = publicBaseUrl.isBlank() ? "http://localhost:8080" : publicBaseUrl;
        return base + "/avaliacao/" + avaliacao.getPublicToken();
    }

    public record Resumo(double media, long respondidas) {
        public String mediaFormatada() { return String.format(Locale.forLanguageTag("pt-BR"), "%.1f", media); }
    }
}
