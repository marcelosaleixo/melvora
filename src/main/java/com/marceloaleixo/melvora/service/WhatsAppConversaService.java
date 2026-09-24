package com.marceloaleixo.melvora.service;

import com.marceloaleixo.melvora.dto.WhatsAppConversaResumo;
import com.marceloaleixo.melvora.entity.Cliente;
import com.marceloaleixo.melvora.entity.WhatsAppMensagem;
import com.marceloaleixo.melvora.entity.enums.ModuloSistema;
import com.marceloaleixo.melvora.exception.RegraNegocioException;
import com.marceloaleixo.melvora.repository.ClienteRepository;
import com.marceloaleixo.melvora.repository.WhatsAppMensagemRepository;
import com.marceloaleixo.melvora.tenant.TenantContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WhatsAppConversaService {
    private final WhatsAppMensagemRepository mensagemRepository;
    private final ClienteRepository clienteRepository;
    private final ModuloAcessoService moduloAcessoService;
    private final WhatsAppBusinessService whatsAppBusinessService;
    private final WhatsAppMensagemService whatsAppMensagemService;

    public WhatsAppConversaService(WhatsAppMensagemRepository mensagemRepository,
                                   ClienteRepository clienteRepository,
                                   ModuloAcessoService moduloAcessoService,
                                   WhatsAppBusinessService whatsAppBusinessService,
                                   WhatsAppMensagemService whatsAppMensagemService) {
        this.mensagemRepository = mensagemRepository;
        this.clienteRepository = clienteRepository;
        this.moduloAcessoService = moduloAcessoService;
        this.whatsAppBusinessService = whatsAppBusinessService;
        this.whatsAppMensagemService = whatsAppMensagemService;
    }

    @Transactional(readOnly = true)
    public Page<WhatsAppConversaResumo> listar(String busca, Pageable pageable) {
        moduloAcessoService.exigir(ModuloSistema.COMUNICACAO);
        Long empresaId = TenantContext.getRequired();
        String termo = busca == null ? "" : busca.trim();
        Page<WhatsAppMensagem> pagina = mensagemRepository.listarConversas(empresaId, termo, pageable);
        List<WhatsAppConversaResumo> itens = pagina.getContent().stream().map(this::resumo).toList();
        return new PageImpl<>(itens, pageable, pagina.getTotalElements());
    }

    @Transactional(readOnly = true)
    public ConversaDetalhe detalheCliente(Long clienteId, Pageable pageable) {
        moduloAcessoService.exigir(ModuloSistema.COMUNICACAO);
        Long empresaId = TenantContext.getRequired();
        Cliente cliente = clienteRepository.findByIdAndEmpresaId(clienteId, empresaId)
                .orElseThrow(() -> new RegraNegocioException("Cliente não encontrada."));
        Page<WhatsAppMensagem> paginaDesc = mensagemRepository.findByEmpresaIdAndClienteIdOrderByRecebidoEmDesc(empresaId, clienteId, pageable);
        return montarDetalhe(cliente, null, paginaDesc);
    }

    @Transactional(readOnly = true)
    public ConversaDetalhe detalheTelefone(String telefone, Pageable pageable) {
        moduloAcessoService.exigir(ModuloSistema.COMUNICACAO);
        Long empresaId = TenantContext.getRequired();
        String numero = normalizarTelefone(telefone);
        Cliente cliente = clienteRepository.findAtivaByEmpresaIdAndTelefone(empresaId, numero).orElse(null);
        Page<WhatsAppMensagem> paginaDesc = mensagemRepository.findByEmpresaIdAndTelefoneOrderByRecebidoEmDesc(empresaId, numero, pageable);
        if (paginaDesc.isEmpty()) throw new RegraNegocioException("Nenhuma conversa encontrada para este número.");
        return montarDetalhe(cliente, numero, paginaDesc);
    }

    @Transactional
    public int marcarComoLida(Long clienteId, String telefone) {
        moduloAcessoService.exigir(ModuloSistema.COMUNICACAO);
        Long empresaId = TenantContext.getRequired();
        if (clienteId != null) {
            clienteRepository.findByIdAndEmpresaId(clienteId, empresaId)
                    .orElseThrow(() -> new RegraNegocioException("Cliente não encontrada."));
        }
        return whatsAppMensagemService.marcarConversaComoLida(empresaId, clienteId, telefone);
    }

    @Transactional
    public void enviarParaCliente(Long clienteId, String mensagem) {
        moduloAcessoService.exigir(ModuloSistema.COMUNICACAO);
        Long empresaId = TenantContext.getRequired();
        Cliente cliente = clienteRepository.findByIdAndEmpresaId(clienteId, empresaId)
                .orElseThrow(() -> new RegraNegocioException("Cliente não encontrada."));
        whatsAppBusinessService.enviarMensagemDireta(empresaId, cliente, mensagem);
    }

    private WhatsAppConversaResumo resumo(WhatsAppMensagem mensagem) {
        Long clienteId = mensagem.getCliente() == null ? null : mensagem.getCliente().getId();
        String nome = mensagem.getCliente() == null ? null : mensagem.getCliente().getNome();
        long naoLidas = clienteId != null
                ? mensagemRepository.countByEmpresaIdAndClienteIdAndDirecaoAndLidaEmIsNull(
                        mensagem.getEmpresa().getId(), clienteId, WhatsAppMensagem.Direcao.ENTRADA)
                : mensagemRepository.countByEmpresaIdAndTelefoneAndDirecaoAndLidaEmIsNull(
                        mensagem.getEmpresa().getId(), mensagem.getTelefone(), WhatsAppMensagem.Direcao.ENTRADA);
        return new WhatsAppConversaResumo(clienteId, nome, mensagem.getTelefone(), mensagem.getMensagem(),
                mensagem.getDirecao(), mensagem.getRecebidoEm(), naoLidas);
    }

    private ConversaDetalhe montarDetalhe(Cliente cliente, String telefone, Page<WhatsAppMensagem> paginaDesc) {
        List<WhatsAppMensagem> mensagens = new ArrayList<>(paginaDesc.getContent());
        Collections.reverse(mensagens);
        String numero = telefone != null ? telefone : cliente.getTelefone();
        return new ConversaDetalhe(cliente, numero, new PageImpl<>(mensagens, paginaDesc.getPageable(), paginaDesc.getTotalElements()));
    }

    private String normalizarTelefone(String telefone) {
        if (telefone == null || telefone.isBlank()) throw new RegraNegocioException("Telefone inválido.");
        String numero = telefone.replaceAll("\\D", "");
        if (numero.length() == 10 || numero.length() == 11) numero = "55" + numero;
        if (!numero.startsWith("55") || numero.length() < 12 || numero.length() > 13) {
            throw new RegraNegocioException("Telefone inválido para WhatsApp.");
        }
        return numero;
    }

    public record ConversaDetalhe(Cliente cliente, String telefone, Page<WhatsAppMensagem> mensagens) {}
}
