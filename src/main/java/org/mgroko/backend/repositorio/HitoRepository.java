package org.mgroko.backend.repositorio;

import java.util.Collection;
import java.util.List;

import org.mgroko.backend.modelo.Hito;
import org.mgroko.backend.repositorio.proyeccion.ProyectoDeEntidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HitoRepository extends JpaRepository<Hito, Long> {

    /**
     * Resuelve el proyecto de cada hito indicado. Se usa para enriquecer las
     * notificaciones sin persistir un dato derivable.
     */
    @Query("""
        SELECT h.idHito AS entidadId,
               h.proyecto.idProyecto AS idProyecto
        FROM Hito h
        WHERE h.idHito IN :ids
    """)
    List<ProyectoDeEntidad> findProyectoPorIds(@Param("ids") Collection<Long> ids);
}
