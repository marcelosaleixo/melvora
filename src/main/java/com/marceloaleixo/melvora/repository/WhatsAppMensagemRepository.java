package com.marceloaleixo.melvora.repository;

import com.marceloaleixo.melvora.entity.WhatsAppMensagem;
import com.marceloaleixo.melvora.entity.WhatsAppMensagem.Direcao;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WhatsAppMensagemRepository extends JpaRepository<WhatsAppMensagem, Long> {
    Optional<WhatsAppMensagem> findByEmpresaIdAndProviderMessageId(Long empresaId, String providerMessageId);

    @Query(value = """
        SELECT m.*
          FROM whatsapp_mensagens m
          JOIN (
              SELECT MAX(m2.id) AS id
                FROM whatsapp_mensagens m2
                LEFT JOIN clientes c2 ON c2.id = m2.cliente_id
               WHERE m2.empresa_id = :empresaId
                 AND (:busca = '' OR LOWER(COALESCE(c2.nome, '')) LIKE LOWER(CONCAT('%', :busca, '%'))
                      OR m2.telefone LIKE CONCAT('%', :busca, '%'))
               GROUP BY COALESCE(m2.cliente_id, 0), m2.telefone
          ) latest ON latest.id = m.id
         ORDER BY m.recebido_em DESC
        """,
        countQuery = """
        SELECT COUNT(*) FROM (
            SELECT COALESCE(m2.cliente_id, 0), m2.telefone
              FROM whatsapp_mensagens m2
              LEFT JOIN clientes c2 ON c2.id = m2.cliente_id
             WHERE m2.empresa_id = :empresaId
               AND (:busca = '' OR LOWER(COALESCE(c2.nome, '')) LIKE LOWER(CONCAT('%', :busca, '%'))
                    OR m2.telefone LIKE CONCAT('%', :busca, '%'))
             GROUP BY COALESCE(m2.cliente_id, 0), m2.telefone
        ) conversations
        """, nativeQuery = true)
    Page<WhatsAppMensagem> listarConversas(@Param("empresaId") Long empresaId, @Param("busca") String busca, Pageable pageable);

    Page<WhatsAppMensagem> findByEmpresaIdAndClienteIdOrderByRecebidoEmDesc(Long empresaId, Long clienteId, Pageable pageable);

    Page<WhatsAppMensagem> findByEmpresaIdAndTelefoneOrderByRecebidoEmDesc(Long empresaId, String telefone, Pageable pageable);


    @EntityGraph(attributePaths = {"empresa", "cliente"})
    @Query("select m from WhatsAppMensagem m where m.direcao = :direcao and m.automacaoProcessadaEm is null and m.mensagem is not null and length(trim(m.mensagem)) > 0 order by m.recebidoEm asc")
    Page<WhatsAppMensagem> listarPendentesAutomacao(@Param("direcao") Direcao direcao, Pageable pageable);

    long countByEmpresaIdAndClienteIdAndDirecaoAndLidaEmIsNull(Long empresaId, Long clienteId, Direcao direcao);

    long countByEmpresaIdAndTelefoneAndDirecaoAndLidaEmIsNull(Long empresaId, String telefone, Direcao direcao);

    @Modifying
    @Query("update WhatsAppMensagem m set m.lidaEm = :agora where m.empresa.id = :empresaId and m.cliente.id = :clienteId and m.direcao = :direcao and m.lidaEm is null")
    int marcarComoLidasPorCliente(@Param("empresaId") Long empresaId, @Param("clienteId") Long clienteId, @Param("direcao") Direcao direcao, @Param("agora") LocalDateTime agora);

    @Modifying
    @Query("update WhatsAppMensagem m set m.lidaEm = :agora where m.empresa.id = :empresaId and m.telefone = :telefone and m.direcao = :direcao and m.lidaEm is null")
    int marcarComoLidasPorTelefone(@Param("empresaId") Long empresaId, @Param("telefone") String telefone, @Param("direcao") Direcao direcao, @Param("agora") LocalDateTime agora);

    @Modifying
    @Query("update WhatsAppMensagem m set m.lidaEm = :agora where m.empresa.id = :empresaId and m.providerMessageId in :ids")
    int marcarComoLidasPorProviderIds(@Param("empresaId") Long empresaId, @Param("ids") java.util.Collection<String> ids, @Param("agora") LocalDateTime agora);
}
