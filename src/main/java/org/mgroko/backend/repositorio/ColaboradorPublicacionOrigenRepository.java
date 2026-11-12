package org.mgroko.backend.repositorio;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.mgroko.backend.modelo.ColaboradorPublicacionOrigen;
import org.mgroko.backend.modelo.ColaboradorPublicacionOrigenId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ColaboradorPublicacionOrigenRepository
        extends JpaRepository<ColaboradorPublicacionOrigen, ColaboradorPublicacionOrigenId> {

    Optional<ColaboradorPublicacionOrigen> findByIdPublicacionAndIdPerfil(
            Long idPublicacion, Long idPerfil);

    @Query("""
            SELECT o
            FROM ColaboradorPublicacionOrigen o
            WHERE o.idPublicacion IN :idsPublicacion
            """)
    List<ColaboradorPublicacionOrigen> findByIdPublicacionIn(
            @Param("idsPublicacion") Collection<Long> idsPublicacion);
}
