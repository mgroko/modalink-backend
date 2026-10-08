package org.mgroko.backend.repositorio;

import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.enums.EstadoParticipacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProyectoRepository extends JpaRepository<Proyecto, Long> {

    @Query("""
        SELECT COUNT(p) > 0
        FROM Proyecto p
        JOIN MiembroProyecto mp ON mp.proyecto = p
        WHERE LOWER(p.nombre) = LOWER(:nombre)
          AND mp.perfil.idPerfil = :idPerfil
          AND mp.rolProyecto.nombre = :nombreRol
          AND mp.estadoParticipacion = :estado
    """)
    boolean existeProyectoConNombreParaPerfil(
            @Param("nombre") String nombre,
            @Param("idPerfil") Long idPerfil,
            @Param("nombreRol") String nombreRol,
            @Param("estado") EstadoParticipacion estado
    );
}
