package org.mgroko.backend.repositorio;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.mgroko.backend.modelo.PostulacionGral;
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

    /**
     * Indica si el perfil ya se postuló al requerimiento general, sin importar
     * el estado de la postulación. La base también lo garantiza con el índice
     * único sobre (id_perfil, id_requerimiento_gral).
     *
     * @param idPerfil           perfil postulante
     * @param idRequerimientoGral requerimiento general
     * @return true si ya existe una postulación
     */
    boolean existsByPerfil_IdPerfilAndRequerimientoGral_IdRequerimientoGral(
            Long idPerfil, Long idRequerimientoGral);

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
