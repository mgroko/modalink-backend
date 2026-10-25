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
