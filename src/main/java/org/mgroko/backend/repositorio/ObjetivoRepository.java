package org.mgroko.backend.repositorio;

import java.util.List;

import org.mgroko.backend.modelo.Objetivo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ObjetivoRepository extends JpaRepository<Objetivo, Long> {

    List<Objetivo> findByProyectoIdProyecto(Long idProyecto);
}
