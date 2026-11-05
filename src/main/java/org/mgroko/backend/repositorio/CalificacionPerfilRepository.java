package org.mgroko.backend.repositorio;

import java.util.List;

import org.mgroko.backend.modelo.CalificacionPerfil;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CalificacionPerfilRepository extends JpaRepository<CalificacionPerfil, Long> {

    List<CalificacionPerfil> findByProyectoIdProyecto(Long idProyecto);

    List<CalificacionPerfil> findByProyectoIdProyectoAndPerfilEmisorIdPerfil(
            Long idProyecto,
            Long idPerfilEmisor);

    List<CalificacionPerfil> findByProyectoIdProyectoAndPerfilReceptorIdPerfil(
            Long idProyecto,
            Long idPerfilReceptor);

    boolean existsByProyectoIdProyectoAndPerfilEmisorIdPerfilAndPerfilReceptorIdPerfil(
            Long idProyecto,
            Long idPerfilEmisor,
            Long idPerfilReceptor);

    long countByProyectoIdProyectoAndPerfilEmisorIdPerfil(
            Long idProyecto,
            Long idPerfilEmisor);
}
