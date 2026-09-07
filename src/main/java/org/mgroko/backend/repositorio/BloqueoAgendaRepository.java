package org.mgroko.backend.repositorio;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.mgroko.backend.modelo.BloqueoAgenda;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio de bloqueos manuales de la agenda. Un bloqueo manual es un
 * período "No disponible" definido por el usuario (compromisos, vacaciones,
 * etc.). Los bloqueos derivados de actividades son calculados y NO se
 * persisten en esta tabla.
 */
public interface BloqueoAgendaRepository extends JpaRepository<BloqueoAgenda, Long> {

    /**
     * Devuelve todos los bloqueos manuales de una agenda, ordenados por
     * inicio.
     *
     * @param idAgenda el ID de la agenda
     * @return bloqueos manuales de la agenda
     */
    List<BloqueoAgenda> findByAgenda_IdAgendaOrderByFechaHoraInicio(Long idAgenda);

    /**
     * Busca un bloqueo por su ID siempre que pertenezca a la agenda
     * indicada. Evita que un usuario acceda a bloqueos de otros.
     *
     * @param idBloqueo el ID del bloqueo
     * @param idAgenda  el ID de la agenda
     * @return el bloqueo si existe y pertenece a la agenda
     */
    Optional<BloqueoAgenda> findByIdBloqueoAndAgenda_IdAgenda(Long idBloqueo, Long idAgenda);

    /**
     * Indica si el rango [inicio, fin] se superpone con algún bloqueo manual
     * existente de la agenda. Dos intervalos solapan si
     * inicio_existente &lt; fin_nuevo Y fin_existente &gt; inicio_nuevo.
     *
     * @param idAgenda el ID de la agenda
     * @param fechaHoraFin  extremo fin del nuevo bloqueo
     * @param fechaHoraInicio extremo inicio del nuevo bloqueo
     * @return true si existe solapamiento con un bloqueo manual existente
     */
    boolean existsByAgenda_IdAgendaAndFechaHoraInicioLessThanAndFechaHoraFinGreaterThan(
            Long idAgenda, LocalDateTime fechaHoraFin, LocalDateTime fechaHoraInicio);
}