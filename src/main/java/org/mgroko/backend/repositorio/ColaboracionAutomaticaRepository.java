package org.mgroko.backend.repositorio;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.mgroko.backend.modelo.ColaboracionAutomatica;
import org.mgroko.backend.modelo.enums.EstadoColaboracionAutomatica;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface ColaboracionAutomaticaRepository
        extends JpaRepository<ColaboracionAutomatica, Long>,
                JpaSpecificationExecutor<ColaboracionAutomatica> {

    @EntityGraph(attributePaths = {
            "imagen",
            "publicacionA",
            "publicacionA.perfil",
            "publicacionA.perfil.profesion",
            "publicacionA.perfil.imagen",
            "publicacionB",
            "publicacionB.perfil",
            "publicacionB.perfil.profesion",
            "publicacionB.perfil.imagen",
            "perfilA",
            "perfilA.profesion",
            "perfilA.imagen",
            "perfilB",
            "perfilB.profesion",
            "perfilB.imagen"
    })
    @Query("SELECT c FROM ColaboracionAutomatica c WHERE c.idColaboracionAutomatica = :id")
    Optional<ColaboracionAutomatica> findByIdConDetalle(
            @Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT c
            FROM ColaboracionAutomatica c
            WHERE c.idColaboracionAutomatica = :id
            """)
    Optional<ColaboracionAutomatica> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            SELECT COUNT(c) > 0
            FROM ColaboracionAutomatica c
            WHERE c.publicacionA.idPublicacion = :idPublicacionA
              AND c.publicacionB.idPublicacion = :idPublicacionB
              AND c.estado IN :estados
            """)
    boolean existsByParYEstados(
            @Param("idPublicacionA") Long idPublicacionA,
            @Param("idPublicacionB") Long idPublicacionB,
            @Param("estados") Collection<EstadoColaboracionAutomatica> estados);

    @Query("""
            SELECT c
            FROM ColaboracionAutomatica c
            WHERE c.estado = :estado
              AND (c.publicacionA.idPublicacion = :idPublicacion
                OR c.publicacionB.idPublicacion = :idPublicacion)
            """)
    List<ColaboracionAutomatica> findByPublicacionYEstado(
            @Param("idPublicacion") Long idPublicacion,
            @Param("estado") EstadoColaboracionAutomatica estado);

    @Query("""
            SELECT c
            FROM ColaboracionAutomatica c
            WHERE c.estado = :estado
              AND c.imagen.idImagen = :idImagen
            """)
    List<ColaboracionAutomatica> findByImagenYEstado(
            @Param("idImagen") Long idImagen,
            @Param("estado") EstadoColaboracionAutomatica estado);

    @Query("""
            SELECT c
            FROM ColaboracionAutomatica c
            WHERE c.estado = :estado
              AND (
                (c.perfilA.idPerfil = :idPerfilA AND c.perfilB.idPerfil = :idPerfilB)
                OR
                (c.perfilA.idPerfil = :idPerfilB AND c.perfilB.idPerfil = :idPerfilA)
              )
            """)
    List<ColaboracionAutomatica> findEntrePerfilesYEstado(
            @Param("idPerfilA") Long idPerfilA,
            @Param("idPerfilB") Long idPerfilB,
            @Param("estado") EstadoColaboracionAutomatica estado);

    @Query("""
            SELECT c
            FROM ColaboracionAutomatica c
            WHERE c.estado = :estado
              AND (
                (c.publicacionA.idPublicacion = :idPublicacion
                  AND c.perfilB.idPerfil = :idPerfil)
                OR
                (c.publicacionB.idPublicacion = :idPublicacion
                  AND c.perfilA.idPerfil = :idPerfil)
              )
            """)
    List<ColaboracionAutomatica> findConfirmadaPorPublicacionYPerfil(
            @Param("idPublicacion") Long idPublicacion,
            @Param("idPerfil") Long idPerfil,
            @Param("estado") EstadoColaboracionAutomatica estado);

    @Override
    @EntityGraph(attributePaths = {
            "imagen",
            "publicacionA",
            "publicacionA.perfil",
            "publicacionA.perfil.profesion",
            "publicacionA.perfil.imagen",
            "publicacionB",
            "publicacionB.perfil",
            "publicacionB.perfil.profesion",
            "publicacionB.perfil.imagen",
            "perfilA",
            "perfilA.profesion",
            "perfilA.imagen",
            "perfilB",
            "perfilB.profesion",
            "perfilB.imagen"
    })
    Page<ColaboracionAutomatica> findAll(Specification<ColaboracionAutomatica> spec, Pageable pageable);
}
