package org.mgroko.backend.repositorio;

import java.util.List;

import org.mgroko.backend.admin.dto.AdminDashboardProvinciaRow;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.enums.EstadoParticipacion;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProyectoRepository extends JpaRepository<Proyecto, Long>, JpaSpecificationExecutor<Proyecto> {

    Page<Proyecto> findByEstadoIn(List<EstadoProyecto> estados, Pageable pageable);

    Page<Proyecto> findByEstadoNot(EstadoProyecto estado, Pageable pageable);

    List<Proyecto> findByEstadoNotOrderByFechaInicioDesc(EstadoProyecto estado, Pageable pageable);

    long countByEstado(EstadoProyecto estado);

    long countByEstadoNot(EstadoProyecto estado);

    @Query("""
        SELECT new org.mgroko.backend.admin.dto.AdminDashboardProvinciaRow(
            prov.idProvincia, prov.nombre, COUNT(DISTINCT p.idProyecto))
        FROM Proyecto p
        JOIN p.ubicacion ub
        JOIN ub.ciudad ciudad
        JOIN ciudad.provincia prov
        WHERE p.estado <> org.mgroko.backend.modelo.enums.EstadoProyecto.BORRADOR
        GROUP BY prov.idProvincia, prov.nombre
        ORDER BY prov.nombre
    """)
    List<AdminDashboardProvinciaRow> contarProyectosNoBorradorPorProvincia();

    @Query("""
        SELECT COUNT(p) > 0
        FROM Proyecto p
        JOIN MiembroProyecto mp ON mp.proyecto = p
        WHERE LOWER(p.nombre) = LOWER(:nombre)
          AND mp.perfil.idPerfil = :idPerfil
          AND mp.rolProyecto.idRolProyecto = :idRolProyecto
          AND mp.estadoParticipacion = :estado
    """)
    boolean existeProyectoConNombreParaPerfil(
            @Param("nombre") String nombre,
            @Param("idPerfil") Long idPerfil,
            @Param("idRolProyecto") Long idRolProyecto,
            @Param("estado") EstadoParticipacion estado
    );
}
