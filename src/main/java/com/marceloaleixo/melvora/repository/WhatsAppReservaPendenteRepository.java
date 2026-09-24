package com.marceloaleixo.melvora.repository;

import com.marceloaleixo.melvora.entity.WhatsAppReservaPendente;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WhatsAppReservaPendenteRepository extends JpaRepository<WhatsAppReservaPendente, Long> {
    @Query("select r from WhatsAppReservaPendente r join fetch r.cliente c join fetch r.servico s where r.empresa.id=:empresaId and r.telefone=:telefone and r.status in :status and r.expiraEm > :agora order by r.createdAt desc")
    Optional<WhatsAppReservaPendente> findAtiva(@Param("empresaId") Long empresaId, @Param("telefone") String telefone,
                                                 @Param("status") java.util.Collection<WhatsAppReservaPendente.Status> status,
                                                 @Param("agora") LocalDateTime agora);
}
