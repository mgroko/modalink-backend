package org.mgroko.backend.repositorio;

import java.util.List;

import org.mgroko.backend.modelo.PostulacionGral;
import org.mgroko.backend.modelo.enums.EstadoSolicitud;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostulacionGralRepository extends JpaRepository<PostulacionGral, Long> {

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

    @Query("""
        SELECT p FROM PostulacionGral p
        JOIN FETCH p.perfil perfil
        JOIN FETCH perfil.usuario
        WHERE p.requerimientoGral.proyecto.idProyecto = :idProyecto
          AND p.estado = org.mgroko.backend.modelo.enums.EstadoSolicitud.PENDIENTE
    """)
    List<PostulacionGral> findPendientesDeProyectoConPerfil(@Param("idProyecto") Long idProyecto);
}
