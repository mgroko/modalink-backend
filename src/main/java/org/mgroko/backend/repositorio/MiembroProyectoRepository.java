package org.mgroko.backend.repositorio;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.enums.EstadoParticipacion;
import org.mgroko.backend.repositorio.proyeccion.CubiertosPorRequerimiento;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MiembroProyectoRepository extends JpaRepository<MiembroProyecto, Long> {

    @EntityGraph(attributePaths = {"perfil", "rolProyecto"})
    List<MiembroProyecto> findByProyectoIdProyecto(Long idProyecto);

    boolean existsByProyectoIdProyectoAndPerfilIdPerfil(Long idProyecto, Long idPerfil);

    Optional<MiembroProyecto> findFirstByProyectoIdProyectoAndPerfilIdPerfilOrderByFechaAltaDesc(
            Long idProyecto, Long idPerfil);

    List<MiembroProyecto> findByProyectoIdProyectoAndPerfilIdPerfilOrderByFechaAltaAsc(
            Long idProyecto, Long idPerfil);

    @Query("""
        SELECT mp FROM MiembroProyecto mp
        JOIN FETCH mp.rolProyecto
        WHERE mp.proyecto.idProyecto IN :idsProyectos
          AND mp.perfil.idPerfil = :idPerfil
          AND mp.estadoParticipacion = org.mgroko.backend.modelo.enums.EstadoParticipacion.ACTIVO
    """)
    List<MiembroProyecto> buscarMiembrosActivosDeProyectos(
            @Param("idsProyectos") List<Long> idsProyectos,
            @Param("idPerfil") Long idPerfil);

    @Query("""
        SELECT mp FROM MiembroProyecto mp
        JOIN FETCH mp.rolProyecto
        WHERE mp.proyecto.idProyecto IN :idsProyectos
          AND mp.perfil.idPerfil = :idPerfil
    """)
    List<MiembroProyecto> buscarMembresiasDeProyectos(
            @Param("idsProyectos") List<Long> idsProyectos,
            @Param("idPerfil") Long idPerfil);

    @Query("""
        SELECT mp FROM MiembroProyecto mp
        JOIN FETCH mp.perfil p
        JOIN FETCH p.usuario
        WHERE mp.proyecto.idProyecto = :idProyecto
          AND mp.estadoParticipacion = org.mgroko.backend.modelo.enums.EstadoParticipacion.ACTIVO
    """)
    List<MiembroProyecto> buscarMiembrosActivosDeProyecto(@Param("idProyecto") Long idProyecto);

    /**
     * Miembros activos de un proyecto con perfil, usuario, profesión y rol
     * inicializados. Se usa para que el director pueda elegir a quién asignar
     * una actividad (UC-70).
     */
    @Query("""
        SELECT mp FROM MiembroProyecto mp
        JOIN FETCH mp.perfil p
        JOIN FETCH p.usuario u
        JOIN FETCH p.profesion pr
        JOIN FETCH mp.rolProyecto r
        WHERE mp.proyecto.idProyecto = :idProyecto
          AND mp.estadoParticipacion = org.mgroko.backend.modelo.enums.EstadoParticipacion.ACTIVO
        ORDER BY LOWER(p.nombreArtistico) ASC, mp.idMiembro ASC
    """)
    List<MiembroProyecto> buscarActivosDeProyectoConPerfilProfesionYRol(
            @Param("idProyecto") Long idProyecto);

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

    @Query("""
        SELECT mp FROM MiembroProyecto mp
        JOIN FETCH mp.rolProyecto
        WHERE mp.proyecto.idProyecto = :idProyecto
          AND mp.perfil.idPerfil = :idPerfil
          AND mp.estadoParticipacion = org.mgroko.backend.modelo.enums.EstadoParticipacion.ACTIVO
    """)
    Optional<MiembroProyecto> findMiembroActivoConRol(
            @Param("idProyecto") Long idProyecto,
            @Param("idPerfil") Long idPerfil);

    @Query("""
        SELECT COUNT(mp) FROM MiembroProyecto mp
        WHERE mp.requerimientoGral.idRequerimientoGral = :idRequerimientoGral
          AND mp.estadoParticipacion = org.mgroko.backend.modelo.enums.EstadoParticipacion.ACTIVO
    """)
    long countActivosPorRequerimientoGral(
            @Param("idRequerimientoGral") Long idRequerimientoGral);

    /**
     * Cuenta los miembros activos vinculados a cada requerimiento general en
     * una sola consulta. Los requerimientos sin miembros no aparecen; el
     * consumidor debe asumir cero.
     */
    @Query("""
        SELECT mp.requerimientoGral.idRequerimientoGral AS idRequerimiento,
               COUNT(mp) AS cubiertos
        FROM MiembroProyecto mp
        WHERE mp.requerimientoGral.idRequerimientoGral IN :ids
          AND mp.estadoParticipacion = org.mgroko.backend.modelo.enums.EstadoParticipacion.ACTIVO
        GROUP BY mp.requerimientoGral.idRequerimientoGral
    """)
    List<CubiertosPorRequerimiento> countActivosPorRequerimientoGralIds(
            @Param("ids") Collection<Long> ids);
}
