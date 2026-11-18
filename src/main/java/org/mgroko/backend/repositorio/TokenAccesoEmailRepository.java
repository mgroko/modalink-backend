package org.mgroko.backend.repositorio;

import java.time.LocalDateTime;
import java.util.Optional;

import org.mgroko.backend.modelo.TokenAccesoEmail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface TokenAccesoEmailRepository extends JpaRepository<TokenAccesoEmail, Long> {

    Optional<TokenAccesoEmail> findByTokenHash(String tokenHash);

    Optional<TokenAccesoEmail> findByEntrega_IdEntrega(Long idEntrega);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM TokenAccesoEmail t WHERE t.tokenHash = :tokenHash")
    Optional<TokenAccesoEmail> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            DELETE FROM TokenAccesoEmail t
             WHERE t.expiraEn < :limite
                OR (t.usadoEn IS NOT NULL AND t.usadoEn < :limite)
            """)
    int eliminarAnteriores(@Param("limite") LocalDateTime limite);
}
