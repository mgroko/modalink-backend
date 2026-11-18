package org.mgroko.backend.repositorio;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.mgroko.backend.modelo.PostulacionActividad;
import org.mgroko.backend.modelo.enums.EstadoSolicitud;
import org.mgroko.backend.repositorio.proyeccion.EstadoPostulacionProyeccion;
import org.mgroko.backend.repositorio.proyeccion.ProyectoDeEntidad;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface PostulacionActividadRepository
        extends JpaRepository<PostulacionActividad, Long>, JpaSpecificationExecutor<PostulacionActividad> {

    @Override
    @EntityGraph(attributePaths = {
            "perfil",
            "perfil.usuario",
            "perfil.profesion",
            "requerimiento",
            "requerimiento.profesion",
            "requerimiento.actividad",
            "requerimiento.actividad.planificacion",
            "requerimiento.actividad.planificacion.proyecto"
    })
    List<PostulacionActividad> findAll(Specification<PostulacionActividad> spec);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT p FROM PostulacionActividad p
        JOIN FETCH p.perfil perfil
        JOIN FETCH perfil.usuario
        JOIN FETCH perfil.profesion
        JOIN FETCH p.requerimiento requerimiento
        JOIN FETCH requerimiento.profesion
        JOIN FETCH requerimiento.actividad actividad
        JOIN FETCH actividad.planificacion planificacion
        JOIN FETCH planificacion.proyecto
        WHERE p.idPostulacion = :idPostulacion
    """)
    Optional<PostulacionActividad> findByIdParaGestion(
            @Param("idPostulacion") Long idPostulacion);

    /**
     * Carga la postulación de actividad para darla de baja (UC-27) bloqueando
     * la fila, de modo que una aceptación o rechazo concurrente del director
     * no pueda pisar la operación.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT p FROM PostulacionActividad p
        JOIN FETCH p.perfil perfil
        JOIN FETCH perfil.usuario
        JOIN FETCH p.requerimiento requerimiento
        JOIN FETCH requerimiento.profesion
        JOIN FETCH requerimiento.actividad actividad
        JOIN FETCH actividad.planificacion planificacion
        JOIN FETCH planificacion.proyecto
        WHERE p.idPostulacion = :idPostulacion
    """)
    Optional<PostulacionActividad> findByIdParaBaja(
            @Param("idPostulacion") Long idPostulacion);

    /** Postulaciones de actividad del perfil, para el listado "mis postulaciones". */
    @EntityGraph(attributePaths = {
            "requerimiento",
            "requerimiento.profesion",
            "requerimiento.actividad",
            "requerimiento.actividad.planificacion",
            "requerimiento.actividad.planificacion.proyecto"
    })
    List<PostulacionActividad> findByPerfil_IdPerfil(Long idPerfil);

    boolean existsByRequerimiento_IdRequerimientoAndEstado(
            Long idRequerimiento, EstadoSolicitud estado);

    boolean existsByRequerimiento_IdRequerimientoAndPerfil_IdPerfil(
            Long idRequerimiento, Long idPerfil);

    boolean existsByRequerimiento_IdRequerimiento(Long idRequerimiento);

    void deleteByRequerimiento_IdRequerimiento(Long idRequerimiento);

    /**
     * Indica si el perfil ya tiene una postulación vigente al requerimiento.
     * Las postulaciones {@code RECHAZADA} no bloquean un nuevo intento: si el
     * cupo se libera, el perfil puede volver a postularse. La base garantiza la
     * unicidad únicamente para los estados vigentes con el índice parcial
     * {@code UQ_perfil_postulacion_act}.
     *
     * @param idPerfil         perfil postulante
     * @param idRequerimiento  requerimiento de actividad
     * @param estados          estados que se consideran vigentes
     * @return true si ya existe una postulación vigente
     */
    boolean existsByPerfil_IdPerfilAndRequerimiento_IdRequerimientoAndEstadoIn(
            Long idPerfil, Long idRequerimiento, Collection<EstadoSolicitud> estados);

    /**
     * Estados de las postulaciones vigentes del perfil para los requerimientos
     * de actividad indicados, en una sola consulta. Las rechazadas se omiten
     * para que el perfil pueda volver a postularse tras liberarse el cupo.
     */
    @Query("""
        SELECT p.requerimiento.idRequerimiento AS entidadId,
               p.estado AS estado
        FROM PostulacionActividad p
        WHERE p.perfil.idPerfil = :idPerfil
          AND p.requerimiento.idRequerimiento IN :ids
          AND p.estado <> org.mgroko.backend.modelo.enums.EstadoSolicitud.RECHAZADA
    """)
    List<EstadoPostulacionProyeccion> findEstadosPorPerfilYRequerimientos(
            @Param("idPerfil") Long idPerfil,
            @Param("ids") Collection<Long> ids);

    /**
     * Cuenta las postulaciones del requerimiento en el estado indicado. Se usa
     * para detectar requerimientos ya satisfechos antes de aceptar una nueva
     * postulación.
     *
     * @param idRequerimiento requerimiento de actividad
     * @param estado          estado a contar
     * @return cantidad de postulaciones en ese estado
     */
    long countByRequerimiento_IdRequerimientoAndEstado(
            Long idRequerimiento, EstadoSolicitud estado);

    /**
     * Cuenta las postulaciones de actividad del proyecto en un estado. Se usa
     * para el badge de pendientes del encabezado del proyecto.
     */
    long countByRequerimiento_Actividad_Planificacion_Proyecto_IdProyectoAndEstado(
            Long idProyecto, EstadoSolicitud estado);

    @Query("""
        SELECT p FROM PostulacionActividad p
        JOIN FETCH p.perfil perfil
        JOIN FETCH perfil.usuario
        WHERE p.requerimiento.actividad.planificacion.proyecto.idProyecto = :idProyecto
          AND p.estado = org.mgroko.backend.modelo.enums.EstadoSolicitud.PENDIENTE
    """)
    List<PostulacionActividad> findPendientesDeProyectoConPerfil(@Param("idProyecto") Long idProyecto);

    /**
     * Postulaciones pendientes de un requerimiento bloqueadas para el rechazo
     * automático al cubrirse el cupo. Se ordenan por id para que dos
     * transacciones concurrentes tomen los locks en el mismo orden.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT p FROM PostulacionActividad p
        JOIN FETCH p.perfil perfil
        JOIN FETCH perfil.usuario
        JOIN FETCH p.requerimiento requerimiento
        JOIN FETCH requerimiento.actividad actividad
        JOIN FETCH actividad.planificacion planificacion
        JOIN FETCH planificacion.proyecto
        WHERE requerimiento.idRequerimiento = :idRequerimiento
          AND p.estado = org.mgroko.backend.modelo.enums.EstadoSolicitud.PENDIENTE
        ORDER BY p.idPostulacion ASC
    """)
    List<PostulacionActividad> findPendientesPorRequerimientoParaActualizar(
            @Param("idRequerimiento") Long idRequerimiento);

    @Query("""
        SELECT p FROM PostulacionActividad p
        JOIN FETCH p.perfil perfil
        JOIN FETCH perfil.usuario
        JOIN FETCH p.requerimiento r
        JOIN FETCH r.actividad a
        JOIN FETCH a.planificacion pl
        JOIN FETCH pl.proyecto proy
        WHERE proy.idProyecto = :idProyecto
          AND perfil.idPerfil = :idPerfil
          AND p.estado = org.mgroko.backend.modelo.enums.EstadoSolicitud.PENDIENTE
    """)
    List<PostulacionActividad> findPendientesDeProyectoYPerfil(
            @Param("idProyecto") Long idProyecto,
            @Param("idPerfil") Long idPerfil);

    @Query("""
        SELECT p FROM PostulacionActividad p
        JOIN FETCH p.perfil perfil
        JOIN FETCH p.requerimiento r
        WHERE perfil.idPerfil = :idPerfil
          AND r.idRequerimiento IN :idsRequerimiento
          AND p.estado <> org.mgroko.backend.modelo.enums.EstadoSolicitud.PENDIENTE
    """)
    List<PostulacionActividad> findHistoricasPorPerfilYRequerimientos(
            @Param("idPerfil") Long idPerfil,
            @Param("idsRequerimiento") Collection<Long> idsRequerimiento);

    @Query("""
        SELECT p FROM PostulacionActividad p
        JOIN FETCH p.perfil perfil
        WHERE p.requerimiento.idRequerimiento = :idRequerimiento
          AND p.estado <> org.mgroko.backend.modelo.enums.EstadoSolicitud.PENDIENTE
    """)
    List<PostulacionActividad> findHistoricasPorRequerimiento(
            @Param("idRequerimiento") Long idRequerimiento);

    @Query("""
        SELECT p FROM PostulacionActividad p
        JOIN FETCH p.perfil perfil
        JOIN FETCH perfil.usuario
        WHERE p.requerimiento.actividad.idActividad = :idActividad
    """)
    List<PostulacionActividad> findByActividadIdConPerfil(
            @Param("idActividad") Long idActividad);

    /**
     * Resuelve el proyecto de cada postulación de actividad indicada. Se usa
     * para enriquecer las notificaciones sin persistir un dato derivable.
     */
    @Query("""
        SELECT p.idPostulacion AS entidadId,
               p.requerimiento.actividad.planificacion.proyecto.idProyecto AS idProyecto
        FROM PostulacionActividad p
        WHERE p.idPostulacion IN :ids
    """)
    List<ProyectoDeEntidad> findProyectoPorIds(@Param("ids") Collection<Long> ids);
}
