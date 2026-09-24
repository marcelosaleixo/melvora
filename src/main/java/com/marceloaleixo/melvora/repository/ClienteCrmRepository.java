package com.marceloaleixo.melvora.repository;

import com.marceloaleixo.melvora.entity.Atendimento;
import com.marceloaleixo.melvora.entity.WhatsAppMensagem;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Consultas agregadas usadas exclusivamente pelo painel CRM 360. */
public interface ClienteCrmRepository extends JpaRepository<Atendimento, Long> {

    @EntityGraph(attributePaths = {"cliente", "profissional", "agendamento"})
    Page<Atendimento> findByEmpresaIdAndClienteIdOrderByDataHoraInicioDesc(
            Long empresaId, Long clienteId, Pageable pageable);

    @Query("""
        select coalesce(sum(l.valor), 0)
        from LancamentoFinanceiro l
        where l.empresa.id = :empresa
          and l.cliente.id = :cliente
          and l.tipo = com.marceloaleixo.melvora.entity.enums.TipoLancamentoFinanceiro.RECEITA
          and l.status = com.marceloaleixo.melvora.entity.enums.StatusLancamentoFinanceiro.PAGO
        """)
    BigDecimal totalReceitasPagas(@Param("empresa") Long empresa, @Param("cliente") Long cliente);

    @Query("""
        select count(l)
        from LancamentoFinanceiro l
        where l.empresa.id = :empresa
          and l.cliente.id = :cliente
          and l.tipo = com.marceloaleixo.melvora.entity.enums.TipoLancamentoFinanceiro.RECEITA
          and l.status = com.marceloaleixo.melvora.entity.enums.StatusLancamentoFinanceiro.PAGO
        """)
    long quantidadeReceitasPagas(@Param("empresa") Long empresa, @Param("cliente") Long cliente);

    @Query("""
        select coalesce(avg(a.nota), 0)
        from AvaliacaoAtendimento a
        where a.empresa.id = :empresa
          and a.cliente.id = :cliente
          and a.nota is not null
        """)
    Double mediaAvaliacao(@Param("empresa") Long empresa, @Param("cliente") Long cliente);

    @Query("""
        select count(a)
        from AvaliacaoAtendimento a
        where a.empresa.id = :empresa
          and a.cliente.id = :cliente
          and a.nota is not null
        """)
    long quantidadeAvaliacoes(@Param("empresa") Long empresa, @Param("cliente") Long cliente);

    @Query("""
        select count(m)
        from WhatsAppMensagem m
        where m.empresa.id = :empresa
          and m.cliente.id = :cliente
          and m.direcao = :direcao
          and m.lidaEm is null
        """)
    long mensagensNaoLidas(@Param("empresa") Long empresa,
                           @Param("cliente") Long cliente,
                           @Param("direcao") WhatsAppMensagem.Direcao direcao);

    @Query("""
        select max(m.recebidoEm)
        from WhatsAppMensagem m
        where m.empresa.id = :empresa
          and m.cliente.id = :cliente
        """)
    LocalDateTime ultimaMensagem(@Param("empresa") Long empresa, @Param("cliente") Long cliente);

    @Query(value = """
        select m.mensagem
        from whatsapp_mensagens m
        where m.empresa_id = :empresa
          and m.cliente_id = :cliente
          and m.mensagem is not null
        order by m.recebido_em desc, m.id desc
        limit 1
        """, nativeQuery = true)
    String ultimaMensagemTexto(@Param("empresa") Long empresa, @Param("cliente") Long cliente);
}
