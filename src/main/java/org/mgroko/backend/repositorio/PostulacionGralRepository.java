package org.mgroko.backend.repositorio;

import java.util.List;
import java.util.Optional;

import org.mgroko.backend.modelo.PostulacionGral;
import org.mgroko.backend.modelo.enums.EstadoSolicitud;
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
