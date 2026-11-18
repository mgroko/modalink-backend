package org.mgroko.backend.repositorio;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.mgroko.backend.modelo.PostulacionGral;
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

public interface PostulacionGralRepository
        extends JpaRepository<PostulacionGral, Long>, JpaSpecificationExecutor<PostulacionGral> {

    @Override
    @EntityGraph(attributePaths = {
            "perfil",
            "perfil.usuario",
            "perfil.profesion",
            "requerimientoGral",
            "requerimientoGral.profesion"
    })
    List<PostulacionGral> findAll(Specification<PostulacionGral> spec);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT p FROM PostulacionGral p
        JOIN FETCH p.perfil perfil
        JOIN FETCH perfil.usuario
        JOIN FETCH perfil.profesion
        JOIN FETCH p.requerimientoGral requerimiento
        JOIN FETCH requerimiento.proyecto
        JOIN FETCH requerimiento.profesion
        WHERE p.idPostulacionGral = :idPostulacion
    """)
    Optional<PostulacionGral> findByIdParaGestion(
            @Param("idPostulacion") Long idPostulacion);

    /**
     * Carga la postulación general para darla de baja (UC-27) bloqueando la
     * fila, de modo que una aceptación o rechazo concurrente del director no
     * pueda pisar la operación.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT p FROM PostulacionGral p
        JOIN FETCH p.perfil perfil
        JOIN FETCH perfil.usuario
        JOIN FETCH p.requerimientoGral requerimiento
        JOIN FETCH requerimiento.proyecto
        JOIN FETCH requerimiento.profesion
        WHERE p.idPostulacionGral = :idPostulacion
    """)
    Optional<PostulacionGral> findByIdParaBaja(
            @Param("idPostulacion") Long idPostulacion);

    /** Postulaciones generales del perfil, para el listado "mis postulaciones". */
    @EntityGraph(attributePaths = {
            "requerimientoGral",
            "requerimientoGral.proyecto",
            "requerimientoGral.profesion"
    })
    List<PostulacionGral> findByPerfil_IdPerfil(Long idPerfil);

    boolean existsByRequerimientoGralIdRequerimientoGralAndEstado(
            Long idRequerimientoGral, EstadoSolicitud estado);

    boolean existsByRequerimientoGral_IdRequerimientoGral(Long idRequerimientoGral);

    boolean existsByRequerimientoGral_IdRequerimientoGralAndPerfil_IdPerfil(
            Long idRequerimientoGral, Long idPerfil);

    void deleteByRequerimientoGral_IdRequerimientoGral(Long idRequerimientoGral);

    /**
     * Devuelve los requerimientos generales del conjunto que tienen al menos
     * una postulación en el estado indicado. Se usa para calcular en bloque si
     * un requerimiento está protegido en UC-36.
     */
    @Query("""
        SELECT DISTINCT p.requerimientoGral.idRequerimientoGral
        FROM PostulacionGral p
        WHERE p.requerimientoGral.idRequerimientoGral IN :ids
          AND p.estado = :estado
    """)
    List<Long> findIdsRequerimientoGralPorEstado(
            @Param("ids") Collection<Long> ids,
            @Param("estado") EstadoSolicitud estado);

    /**
     * Indica si el perfil ya tiene una postulación vigente al requerimiento
     * general. Las postulaciones {@code RECHAZADA} no bloquean un nuevo
     * intento: si el cupo se libera, el perfil puede volver a postularse. La
     * base garantiza la unicidad únicamente para los estados vigentes con el
     * índice parcial {@code UQ_perfil_postulacion_gral}.
     *
     * @param idPerfil            perfil postulante
     * @param idRequerimientoGral requerimiento general
     * @param estados             estados que se consideran vigentes
     * @return true si ya existe una postulación vigente
     */
    boolean existsByPerfil_IdPerfilAndRequerimientoGral_IdRequerimientoGralAndEstadoIn(
            Long idPerfil, Long idRequerimientoGral, Collection<EstadoSolicitud> estados);

    /**
     * Estados de las postulaciones vigentes del perfil para los requerimientos
     * indicados, en una sola consulta. Se usa para marcar {@code yaPostulado}
     * en el buscador de oportunidades. Las rechazadas se omiten para permitir
     * volver a postularse cuando se libera el cupo.
     */
    @Query("""
        SELECT p.requerimientoGral.idRequerimientoGral AS entidadId,
               p.estado AS estado
        FROM PostulacionGral p
        WHERE p.perfil.idPerfil = :idPerfil
          AND p.requerimientoGral.idRequerimientoGral IN :ids
          AND p.estado <> org.mgroko.backend.modelo.enums.EstadoSolicitud.RECHAZADA
    """)
    List<EstadoPostulacionProyeccion> findEstadosPorPerfilYRequerimientos(
            @Param("idPerfil") Long idPerfil,
            @Param("ids") Collection<Long> ids);

    /**
     * Cuenta las postulaciones del requerimiento general en el estado
     * indicado, para detectar requerimientos ya satisfechos.
     *
     * @param idRequerimientoGral requerimiento general
     * @param estado              estado a contar
     * @return cantidad de postulaciones en ese estado
     */
    long countByRequerimientoGral_IdRequerimientoGralAndEstado(
            Long idRequerimientoGral, EstadoSolicitud estado);

    /**
     * Cuenta las postulaciones generales del proyecto en un estado. Se usa
     * para el badge de pendientes del encabezado del proyecto.
     */
    long countByRequerimientoGral_Proyecto_IdProyectoAndEstado(
            Long idProyecto, EstadoSolicitud estado);

    @Query("""
        SELECT p FROM PostulacionGral p
        JOIN FETCH p.perfil perfil
        JOIN FETCH perfil.usuario
        WHERE p.requerimientoGral.proyecto.idProyecto = :idProyecto
          AND p.estado = org.mgroko.backend.modelo.enums.EstadoSolicitud.PENDIENTE
    """)
    List<PostulacionGral> findPendientesDeProyectoConPerfil(@Param("idProyecto") Long idProyecto);

    /**
     * Postulaciones pendientes de un requerimiento general bloqueadas para el
     * rechazo automático al cubrirse el cupo. Se ordenan por id para que dos
     * transacciones concurrentes tomen los locks en el mismo orden.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT p FROM PostulacionGral p
        JOIN FETCH p.perfil perfil
        JOIN FETCH perfil.usuario
        JOIN FETCH p.requerimientoGral requerimiento
        JOIN FETCH requerimiento.proyecto
        WHERE requerimiento.idRequerimientoGral = :idRequerimientoGral
          AND p.estado = org.mgroko.backend.modelo.enums.EstadoSolicitud.PENDIENTE
        ORDER BY p.idPostulacionGral ASC
    """)
    List<PostulacionGral> findPendientesPorRequerimientoGralParaActualizar(
            @Param("idRequerimientoGral") Long idRequerimientoGral);

    @Query("""
        SELECT p FROM PostulacionGral p
        JOIN FETCH p.perfil perfil
        JOIN FETCH perfil.usuario
        JOIN FETCH p.requerimientoGral r
        JOIN FETCH r.proyecto proy
        WHERE proy.idProyecto = :idProyecto
          AND perfil.idPerfil = :idPerfil
          AND p.estado = org.mgroko.backend.modelo.enums.EstadoSolicitud.PENDIENTE
    """)
    List<PostulacionGral> findPendientesDeProyectoYPerfil(
            @Param("idProyecto") Long idProyecto,
            @Param("idPerfil") Long idPerfil);

    @Query("""
        SELECT p FROM PostulacionGral p
        JOIN FETCH p.perfil perfil
        JOIN FETCH p.requerimientoGral r
        WHERE perfil.idPerfil = :idPerfil
          AND r.idRequerimientoGral IN :idsRequerimiento
          AND p.estado <> org.mgroko.backend.modelo.enums.EstadoSolicitud.PENDIENTE
    """)
    List<PostulacionGral> findHistoricasPorPerfilYRequerimientos(
            @Param("idPerfil") Long idPerfil,
            @Param("idsRequerimiento") Collection<Long> idsRequerimiento);

    @Query("""
        SELECT p FROM PostulacionGral p
        JOIN FETCH p.perfil perfil
        WHERE p.requerimientoGral.idRequerimientoGral = :idRequerimientoGral
          AND p.estado <> org.mgroko.backend.modelo.enums.EstadoSolicitud.PENDIENTE
    """)
    List<PostulacionGral> findHistoricasPorRequerimientoGral(
            @Param("idRequerimientoGral") Long idRequerimientoGral);

    /**
     * Resuelve el proyecto de cada postulación general indicada. Se usa para
     * enriquecer las notificaciones sin persistir un dato derivable.
     */
    @Query("""
        SELECT p.idPostulacionGral AS entidadId,
               p.requerimientoGral.proyecto.idProyecto AS idProyecto
        FROM PostulacionGral p
        WHERE p.idPostulacionGral IN :ids
    """)
    List<ProyectoDeEntidad> findProyectoPorIds(@Param("ids") Collection<Long> ids);
}
