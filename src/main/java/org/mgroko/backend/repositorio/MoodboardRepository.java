package org.mgroko.backend.repositorio;

import java.util.Optional;

import org.mgroko.backend.modelo.Moodboard;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MoodboardRepository extends JpaRepository<Moodboard, Long> {

    Optional<Moodboard> findByProyectoIdProyecto(Long idProyecto);
}
