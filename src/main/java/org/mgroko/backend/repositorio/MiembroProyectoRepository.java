package org.mgroko.backend.repositorio;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.enums.EstadoParticipacion;
import org.mgroko.backend.repositorio.proyeccion.CubiertosPorRequerimiento;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

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
        JOIN FETCH mp.perfil p
        JOIN FETCH p.usuario
        JOIN FETCH mp.rolProyecto
        WHERE mp.idMiembro = :idMiembro
          AND mp.proyecto.idProyecto = :idProyecto
    """)
    Optional<MiembroProyecto> findByIdMiembroAndProyecto(
            @Param("idMiembro") Long idMiembro,
            @Param("idProyecto") Long idProyecto);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT mp FROM MiembroProyecto mp
        JOIN FETCH mp.perfil p
        JOIN FETCH p.usuario
        JOIN FETCH mp.rolProyecto
        WHERE mp.idMiembro = :idMiembro
          AND mp.proyecto.idProyecto = :idProyecto
    """)
    Optional<MiembroProyecto> findByIdMiembroAndProyectoForUpdate(
            @Param("idMiembro") Long idMiembro,
            @Param("idProyecto") Long idProyecto);

    /**
     * Membresía activa del perfil en el proyecto, bloqueada para una baja
     * voluntaria (UC-40). El estado ACTIVO está garantizado por el índice
     * único parcial {@code UQ_miembro_proyecto_activo}.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT mp FROM MiembroProyecto mp
        JOIN FETCH mp.perfil p
        JOIN FETCH p.usuario
        JOIN FETCH mp.rolProyecto
        WHERE mp.proyecto.idProyecto = :idProyecto
          AND mp.perfil.idPerfil = :idPerfil
          AND mp.estadoParticipacion = org.mgroko.backend.modelo.enums.EstadoParticipacion.ACTIVO
    """)
    Optional<MiembroProyecto> findMiembroActivoDeProyectoParaActualizar(
            @Param("idProyecto") Long idProyecto,
            @Param("idPerfil") Long idPerfil);

    /**
     * Cuenta los Directores activos del proyecto distintos del miembro
     * indicado. Se usa para impedir que una baja deje al proyecto sin
     * Director.
     */
    @Query("""
        SELECT COUNT(mp) FROM MiembroProyecto mp
        JOIN mp.rolProyecto r
        WHERE mp.proyecto.idProyecto = :idProyecto
          AND mp.idMiembro <> :idMiembro
          AND mp.estadoParticipacion = org.mgroko.backend.modelo.enums.EstadoParticipacion.ACTIVO
          AND (LOWER(r.codigo) = 'director' OR LOWER(r.nombre) LIKE 'director%')
    """)
    long countOtrosDirectoresActivos(
            @Param("idProyecto") Long idProyecto,
            @Param("idMiembro") Long idMiembro);

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

    /**
     * Indica si existe cualquier fila de miembro vinculada al requerimiento,
     * sin importar su estado de participación. Se usa para detectar historial
     * que debe preservarse al editar o eliminar un requerimiento.
     */
    boolean existsByRequerimientoGral_IdRequerimientoGral(Long idRequerimientoGral);

    List<MiembroProyecto> findByRequerimientoGral_IdRequerimientoGral(
            Long idRequerimientoGral);

    boolean existsByRequerimientoGral_IdRequerimientoGralAndEstadoParticipacion(
            Long idRequerimientoGral,
            EstadoParticipacion estadoParticipacion);

    @Query("""
        SELECT DISTINCT mp.requerimientoGral.idRequerimientoGral
        FROM MiembroProyecto mp
        WHERE mp.requerimientoGral.idRequerimientoGral IN :ids
    """)
    List<Long> findIdsRequerimientoGralConMiembros(
            @Param("ids") Collection<Long> ids);

    @Query("""
        SELECT DISTINCT mp.requerimientoGral.idRequerimientoGral
        FROM MiembroProyecto mp
        WHERE mp.requerimientoGral.idRequerimientoGral IN :ids
          AND mp.estadoParticipacion = org.mgroko.backend.modelo.enums.EstadoParticipacion.ACTIVO
    """)
    List<Long> findIdsRequerimientoGralConMiembrosActivos(
            @Param("ids") Collection<Long> ids);

    /**
     * Membresías históricas del proyecto para la auditoría de bajas.
     */
    @Query("""
        SELECT mp FROM MiembroProyecto mp
        JOIN FETCH mp.perfil p
        JOIN FETCH p.usuario u
        JOIN FETCH p.profesion
        JOIN FETCH mp.rolProyecto r
        LEFT JOIN FETCH mp.perfilAutorBaja autor
        LEFT JOIN FETCH autor.usuario
        WHERE mp.proyecto.idProyecto = :idProyecto
          AND mp.estadoParticipacion <> org.mgroko.backend.modelo.enums.EstadoParticipacion.ACTIVO
        ORDER BY COALESCE(mp.fechaBaja, mp.fechaAlta) DESC, mp.idMiembro DESC
    """)
    List<MiembroProyecto> findHistoricosDeProyectoParaAuditoria(
            @Param("idProyecto") Long idProyecto);
}
