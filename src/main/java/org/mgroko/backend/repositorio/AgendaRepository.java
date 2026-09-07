package org.mgroko.backend.repositorio;

import java.util.Optional;

import org.mgroko.backend.modelo.Agenda;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgendaRepository extends JpaRepository<Agenda, Long> {

    /**
     * Busca la agenda del usuario. Cada usuario tiene exactamente una
     * agenda (UNIQUE en la base y creada por trigger al registrar).
     *
     * @param idUsuario el ID del usuario
     * @return la agenda del usuario si existe
     */
    Optional<Agenda> findByUsuario_IdUsuario(Long idUsuario);
}