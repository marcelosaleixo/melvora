package com.marceloaleixo.melvora.repository;

import com.marceloaleixo.melvora.entity.WhatsAppAgendamentoAcaoPendente;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WhatsAppAgendamentoAcaoPendenteRepository extends JpaRepository<WhatsAppAgendamentoAcaoPendente, Long> {
    @Query("select a from WhatsAppAgendamentoAcaoPendente a join fetch a.cliente where a.empresa.id=:empresaId and a.telefone=:telefone and a.status in :status and a.expiraEm > :agora order by a.createdAt desc")
    Optional<WhatsAppAgendamentoAcaoPendente> findAtiva(@Param("empresaId") Long empresaId,
                                                         @Param("telefone") String telefone,
                                                         @Param("status") Collection<WhatsAppAgendamentoAcaoPendente.Status> status,
                                                         @Param("agora") LocalDateTime agora);

    @Query("select a from WhatsAppAgendamentoAcaoPendente a where a.status in :status and a.expiraEm <= :agora")
    java.util.List<WhatsAppAgendamentoAcaoPendente> listarExpiradas(@Param("status") Collection<WhatsAppAgendamentoAcaoPendente.Status> status,
                                                                      @Param("agora") LocalDateTime agora);
}
