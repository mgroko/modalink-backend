package org.mgroko.backend.repositorio;

import java.util.List;

import org.mgroko.backend.modelo.PostulacionActividad;
import org.mgroko.backend.modelo.enums.EstadoSolicitud;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostulacionActividadRepository extends JpaRepository<PostulacionActividad, Long> {

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
}
