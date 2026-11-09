package org.mgroko.backend.repositorio;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.mgroko.backend.modelo.PerfilTyc;
import org.mgroko.backend.modelo.PerfilTycId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Acceso a las versiones de términos y condiciones de cada perfil (UC-23).
 */
public interface PerfilTycRepository extends JpaRepository<PerfilTyc, PerfilTycId> {

    /**
     * Devuelve la versión vigente (sin fecha de fin) de un perfil.
     */
    Optional<PerfilTyc> findFirstByPerfilIdPerfilAndFechaFinIsNullOrderByFechaInicioDesc(Long idPerfil);

    /**
     * Devuelve todas las versiones de un perfil, de la más reciente a la más
     * antigua.
     */
    List<PerfilTyc> findByPerfilIdPerfilOrderByFechaInicioDesc(Long idPerfil);

    /**
     * Devuelve las versiones vigentes de varios perfiles en una sola consulta.
     * Se usa en la invitación masiva para evitar N+1.
     */
    @Query("""
        SELECT pt FROM PerfilTyc pt
        WHERE pt.perfil.idPerfil IN :idsPerfil
          AND pt.fechaFin IS NULL
    """)
    List<PerfilTyc> findVigentesByPerfilIds(@Param("idsPerfil") Collection<Long> idsPerfil);

    /**
     * Siguiente valor de la identidad de {@code perfil_tyc.id_tyc}. Se asigna
     * explícitamente para evitar las limitaciones de {@code @GeneratedValue}
     * dentro de una {@code @EmbeddedId} con PK compuesta.
     */
    @Query(value = "SELECT nextval(pg_get_serial_sequence('perfil_tyc', 'id_tyc'))",
            nativeQuery = true)
    Long nextIdTyc();
}
