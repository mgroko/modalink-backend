package org.mgroko.backend.repositorio;

import java.util.List;
import java.util.Optional;

import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.enums.EstadoParticipacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MiembroProyectoRepository extends JpaRepository<MiembroProyecto, Long> {

    List<MiembroProyecto> findByProyectoIdProyecto(Long idProyecto);

    Optional<MiembroProyecto> findByProyectoIdProyectoAndPerfilIdPerfil(Long idProyecto, Long idPerfil);

    @Query("""
        SELECT mp FROM MiembroProyecto mp
        JOIN FETCH mp.perfil p
        LEFT JOIN FETCH p.usuario
        WHERE mp.proyecto.idProyecto IN :idsProyectos
          AND mp.estadoParticipacion = org.mgroko.backend.modelo.enums.EstadoParticipacion.ACTIVO
          AND (LOWER(mp.rolProyecto.codigo) = 'director'
               OR LOWER(mp.rolProyecto.nombre) LIKE 'director%')
    """)
    List<MiembroProyecto> buscarDirectoresDeProyectos(@Param("idsProyectos") List<Long> idsProyectos);

    boolean existsByProyectoIdProyectoAndPerfilIdPerfilAndEstadoParticipacion(
            Long idProyecto, Long idPerfil, EstadoParticipacion estadoParticipacion);

    @Query("""
        SELECT mp FROM MiembroProyecto mp
        JOIN FETCH mp.rolProyecto rp
        LEFT JOIN FETCH rp.permisos
        WHERE mp.proyecto.idProyecto = :idProyecto
          AND mp.perfil.idPerfil = :idPerfil
          AND mp.estadoParticipacion = org.mgroko.backend.modelo.enums.EstadoParticipacion.ACTIVO
    """)
    Optional<MiembroProyecto> findMiembroActivoConPermisos(
            @Param("idProyecto") Long idProyecto,
            @Param("idPerfil") Long idPerfil);
}
