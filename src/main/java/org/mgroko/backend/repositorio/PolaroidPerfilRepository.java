package org.mgroko.backend.repositorio;

import java.util.List;
import java.util.Optional;

import org.mgroko.backend.modelo.PolaroidPerfil;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PolaroidPerfilRepository extends JpaRepository<PolaroidPerfil, Long> {

    @EntityGraph(attributePaths = {"imagen", "tipoPolaroid", "tipoPolaroid.grupo"})
    List<PolaroidPerfil> findByPerfilIdPerfilOrderByOrdenAsc(Long idPerfil);

    @EntityGraph(attributePaths = {"imagen", "tipoPolaroid", "tipoPolaroid.grupo"})
    Optional<PolaroidPerfil> findByIdPolaroidAndPerfilIdPerfil(Long idPolaroid, Long idPerfil);

    long countByPerfilIdPerfil(Long idPerfil);

    @Query("""
        SELECT COALESCE(MAX(p.orden), 0)
        FROM PolaroidPerfil p
        WHERE p.perfil.idPerfil = :idPerfil
    """)
    int obtenerMaximoOrden(@Param("idPerfil") Long idPerfil);
}
