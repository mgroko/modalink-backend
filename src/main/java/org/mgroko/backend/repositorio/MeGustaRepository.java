package org.mgroko.backend.repositorio;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.mgroko.backend.modelo.MeGusta;
import org.mgroko.backend.modelo.MeGustaId;
import org.mgroko.backend.repositorio.proyeccion.ConteoPorPublicacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MeGustaRepository extends JpaRepository<MeGusta, MeGustaId> {

    /**
     * Inserta el Me gusta solo si no existe. El {@code ON CONFLICT} sobre la PK
     * compuesta hace que la operación sea idempotente y segura ante requests
     * concurrentes, sin dejar la transacción en estado de rollback.
     *
     * @return 1 si insertó, 0 si ya existía
     */
    @Modifying(flushAutomatically = true)
    @Query(value = """
            INSERT INTO me_gusta (id_publicacion, id_perfil, fecha)
            VALUES (:idPublicacion, :idPerfil, :fecha)
            ON CONFLICT (id_publicacion, id_perfil) DO NOTHING
            """, nativeQuery = true)
    int insertarSiNoExiste(
            @Param("idPublicacion") Long idPublicacion,
            @Param("idPerfil") Long idPerfil,
            @Param("fecha") LocalDateTime fecha);

    @Modifying(flushAutomatically = true)
    @Query("""
            DELETE FROM MeGusta m
            WHERE m.id.idPublicacion = :idPublicacion
              AND m.id.idPerfil = :idPerfil
            """)
    int eliminar(
            @Param("idPublicacion") Long idPublicacion,
            @Param("idPerfil") Long idPerfil);

    @Query("""
            SELECT COUNT(m)
            FROM MeGusta m
            WHERE m.id.idPublicacion = :idPublicacion
            """)
    long contarPorPublicacion(@Param("idPublicacion") Long idPublicacion);

    @Query("""
            SELECT m.id.idPublicacion AS idPublicacion, COUNT(m) AS total
            FROM MeGusta m
            WHERE m.id.idPublicacion IN :idsPublicaciones
            GROUP BY m.id.idPublicacion
            """)
    List<ConteoPorPublicacion> contarPorPublicaciones(
            @Param("idsPublicaciones") Collection<Long> idsPublicaciones);

    @Query("""
            SELECT m.id.idPublicacion
            FROM MeGusta m
            WHERE m.id.idPublicacion IN :idsPublicaciones
              AND m.id.idPerfil = :idPerfil
            """)
    List<Long> findIdsPublicacionesConMeGusta(
            @Param("idsPublicaciones") Collection<Long> idsPublicaciones,
            @Param("idPerfil") Long idPerfil);
}
