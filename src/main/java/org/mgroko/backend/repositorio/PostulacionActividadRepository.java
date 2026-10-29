package org.mgroko.backend.repositorio;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.mgroko.backend.modelo.PostulacionActividad;
import org.mgroko.backend.modelo.enums.EstadoSolicitud;
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

    boolean existsByRequerimiento_IdRequerimiento(Long idRequerimiento);

    /**
     * Indica si el perfil ya se postuló al requerimiento, sin importar el
     * estado de la postulación. La base también lo garantiza con el índice
     * único sobre (id_perfil, id_requerimiento_act).
     *
     * @param idPerfil         perfil postulante
     * @param idRequerimiento  requerimiento de actividad
     * @return true si ya existe una postulación
     */
    boolean existsByPerfil_IdPerfilAndRequerimiento_IdRequerimiento(
            Long idPerfil, Long idRequerimiento);

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
