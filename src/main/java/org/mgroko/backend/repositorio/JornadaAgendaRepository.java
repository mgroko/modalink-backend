package org.mgroko.backend.repositorio;

import java.util.List;

import org.mgroko.backend.modelo.JornadaAgenda;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JornadaAgendaRepository extends JpaRepository<JornadaAgenda, Long> {

    /**
     * Devuelve la jornada laboral de una agenda, ordenada por día de la
     * semana (1 = Lunes ... 7 = Domingo).
     *
     * @param idAgenda el ID de la agenda
     * @return días laborables de la agenda
     */
    List<JornadaAgenda> findByAgenda_IdAgendaOrderByDiaSemana(Long idAgenda);
}