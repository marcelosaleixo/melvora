package com.marceloaleixo.melvora.repository;

import com.marceloaleixo.melvora.entity.WhatsAppConfirmacaoPendente;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WhatsAppConfirmacaoPendenteRepository extends JpaRepository<WhatsAppConfirmacaoPendente, Long> {
    @Query("select c from WhatsAppConfirmacaoPendente c join fetch c.cliente where c.empresa.id=:empresaId and c.telefone=:telefone and c.status in :status and c.expiraEm > :agora order by c.createdAt desc")
    Optional<WhatsAppConfirmacaoPendente> findAtiva(@Param("empresaId") Long empresaId,
                                                     @Param("telefone") String telefone,
                                                     @Param("status") Collection<WhatsAppConfirmacaoPendente.Status> status,
                                                     @Param("agora") LocalDateTime agora);

    @Query("select c from WhatsAppConfirmacaoPendente c where c.status = :status and c.expiraEm <= :agora")
    List<WhatsAppConfirmacaoPendente> listarExpiradas(@Param("status") WhatsAppConfirmacaoPendente.Status status,
                                                       @Param("agora") LocalDateTime agora);
}
