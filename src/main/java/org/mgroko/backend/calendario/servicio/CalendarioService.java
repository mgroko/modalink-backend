package org.mgroko.backend.calendario.servicio;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.mgroko.backend.auth.exception.UsuarioNoEncontradoException;
import org.mgroko.backend.calendario.dto.BloqueoResponse;
import org.mgroko.backend.calendario.dto.CalendarioResponse;
import org.mgroko.backend.calendario.dto.ConfigJornadaRequest;
import org.mgroko.backend.calendario.dto.ConfigJornadaResponse;
import org.mgroko.backend.calendario.dto.JornadaDiaRequest;
import org.mgroko.backend.calendario.dto.MarcarNoDisponibleRequest;
import org.mgroko.backend.calendario.exception.AgendaNoEncontradaException;
import org.mgroko.backend.calendario.exception.BloqueoNoEncontradoException;
import org.mgroko.backend.calendario.exception.BloqueoSolapadoException;
import org.mgroko.backend.calendario.exception.HorarioComprometidoException;
import org.mgroko.backend.calendario.exception.JornadaInvalidaException;
import org.mgroko.backend.calendario.exception.RangoInvalidoException;
import org.mgroko.backend.calendario.mapper.CalendarioMapper;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.Agenda;
import org.mgroko.backend.modelo.BloqueoAgenda;
import org.mgroko.backend.modelo.JornadaAgenda;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.repositorio.ActividadRepository;
import org.mgroko.backend.repositorio.AgendaRepository;
import org.mgroko.backend.repositorio.BloqueoAgendaRepository;
import org.mgroko.backend.repositorio.JornadaAgendaRepository;
import org.mgroko.backend.repositorio.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gestión del calendario del usuario. La disponibilidad se calcula como la
 * unión de los bloqueos manuales (persistidos en bloqueo_agenda) y los
 * bloqueos calculados por actividades de proyectos activos (derivados, no
 * persistidos, extendidos con el margen configurado en la agenda).
 */
@Service
public class CalendarioService {

    private static final List<EstadoProyecto> ESTADOS_ACTIVOS =
            List.of(EstadoProyecto.Publicado, EstadoProyecto.Confirmado);

    private final UsuarioRepository usuarioRepository;
    private final AgendaRepository agendaRepository;
    private final JornadaAgendaRepository jornadaAgendaRepository;
    private final BloqueoAgendaRepository bloqueoAgendaRepository;
    private final ActividadRepository actividadRepository;

    public CalendarioService(
            UsuarioRepository usuarioRepository,
            AgendaRepository agendaRepository,
            JornadaAgendaRepository jornadaAgendaRepository,
            BloqueoAgendaRepository bloqueoAgendaRepository,
            ActividadRepository actividadRepository) {
        this.usuarioRepository = usuarioRepository;
        this.agendaRepository = agendaRepository;
        this.jornadaAgendaRepository = jornadaAgendaRepository;
        this.bloqueoAgendaRepository = bloqueoAgendaRepository;
        this.actividadRepository = actividadRepository;
    }

    /**
     * Devuelve la agenda completa del usuario (jornada, bloqueos manuales
     * y bloqueos calculados por actividad).
     */
    @Transactional(readOnly = true)
    public CalendarioResponse obtener(Long idUsuario) {
        usuarioActivo(idUsuario);
        Agenda agenda = agendaDe(idUsuario);
        List<JornadaAgenda> dias =
                jornadaAgendaRepository.findByAgenda_IdAgendaOrderByDiaSemana(agenda.getIdAgenda());
        List<BloqueoAgenda> bloqueos =
                bloqueoAgendaRepository.findByAgenda_IdAgendaOrderByFechaHoraInicio(agenda.getIdAgenda());
        List<Actividad> actividades = actividadesDe(idUsuario);
        int margen = agenda.getMargenActividadMinutos() != null
        ? agenda.getMargenActividadMinutos()
        : Agenda.MARGEN_ACTIVIDAD_MINUTOS_DEFECTO;

        return new CalendarioResponse(
                CalendarioMapper.toConfigJornadaResponse(agenda, dias),
                bloqueos.stream().map(CalendarioMapper::toBloqueoResponse).toList(),
                actividades.stream()
                        .map(a -> CalendarioMapper.toBloqueoActividadResponse(a, margen))
                        .toList());
    }

    /**
     * Reemplaza la jornada laboral completa del usuario (días con sus
     * horarios) y el margen por actividad. La sincronización es por diff:
     * se insertan solo los días nuevos, se actualizan los que cambian de
     * horario y se eliminan los que ya no se envían; las filas sin cambios
     * quedan intactas (mismo id). Evita la violación de la unique
     * (id_agenda, dia_semana) que ocurría al borrar y reinsertar los mismos
     * días dentro de una sola transacción.
     */
    @Transactional
    public ConfigJornadaResponse configurarJornada(Long idUsuario, ConfigJornadaRequest request) {
        usuarioActivo(idUsuario);
        validarJornada(request);
        Agenda agenda = agendaDe(idUsuario);

        List<JornadaAgenda> actuales =
                jornadaAgendaRepository.findByAgenda_IdAgendaOrderByDiaSemana(agenda.getIdAgenda());
        Map<Integer, JornadaAgenda> porDia = actuales.stream()
                .collect(Collectors.toMap(JornadaAgenda::getDiaSemana, Function.identity()));

        for (JornadaDiaRequest dia : request.dias()) {
            JornadaAgenda existente = porDia.remove(dia.diaSemana());
            if (existente == null) {
                jornadaAgendaRepository.save(JornadaAgenda.builder()
                        .agenda(agenda)
                        .diaSemana(dia.diaSemana())
                        .horaInicio(dia.horaInicio())
                        .horaFin(dia.horaFin())
                        .build());
            } else if (!existente.getHoraInicio().equals(dia.horaInicio())
                    || !existente.getHoraFin().equals(dia.horaFin())) {
                existente.setHoraInicio(dia.horaInicio());
                existente.setHoraFin(dia.horaFin());
            }
        }
        jornadaAgendaRepository.deleteAll(porDia.values());

        agenda.setMargenActividadMinutos(request.margenActividadMinutos());
        agendaRepository.save(agenda);

        List<JornadaAgenda> dias =
                jornadaAgendaRepository.findByAgenda_IdAgendaOrderByDiaSemana(agenda.getIdAgenda());
        return CalendarioMapper.toConfigJornadaResponse(agenda, dias);
    }

    /**
     * Marca un bloque de tiempo como "No disponible" (UC-18). El motivo es
     * opcional. No permite superponerse con otro bloqueo manual ni con un
     * horario comprometido por una actividad de un proyecto activo.
     */
    @Transactional
    public BloqueoResponse marcarNoDisponible(Long idUsuario, MarcarNoDisponibleRequest request) {
        usuarioActivo(idUsuario);
        validarRango(request.fechaHoraInicio(), request.fechaHoraFin());
        Agenda agenda = agendaDe(idUsuario);

        if (solapaActividad(idUsuario, request.fechaHoraInicio(), request.fechaHoraFin(), agenda.getMargenActividadMinutos())) {
            throw new HorarioComprometidoException(
                    "El periodo ya se encuentra bloqueado automáticamente por una actividad de un proyecto activo.");
        }
        if (bloqueoAgendaRepository.existsByAgenda_IdAgendaAndFechaHoraInicioLessThanAndFechaHoraFinGreaterThan(
                agenda.getIdAgenda(), request.fechaHoraFin(), request.fechaHoraInicio())) {
            throw new BloqueoSolapadoException("El periodo se superpone con un bloqueo existente de tu calendario.");
        }

        BloqueoAgenda bloqueo = BloqueoAgenda.builder()
                .agenda(agenda)
                .fechaHoraInicio(request.fechaHoraInicio())
                .fechaHoraFin(request.fechaHoraFin())
                .motivo(request.motivo())
                .build();
        bloqueoAgendaRepository.save(bloqueo);
        return CalendarioMapper.toBloqueoResponse(bloqueo);
    }

    /**
     * Marca un bloque de tiempo como "Disponible" (UC-17) eliminando el
     * bloqueo manual. No se permite liberar un horario comprometido por una
     * actividad de un proyecto activo.
     */
    @Transactional
    public void marcarDisponible(Long idUsuario, Long idBloqueo) {
        usuarioActivo(idUsuario);
        Agenda agenda = agendaDe(idUsuario);

        BloqueoAgenda bloqueo = bloqueoAgendaRepository
                .findByIdBloqueoAndAgenda_IdAgenda(idBloqueo, agenda.getIdAgenda())
                .orElseThrow(() -> new BloqueoNoEncontradoException(
                        "El bloqueo no existe o no pertenece a tu calendario."));

        if (solapaActividad(idUsuario, bloqueo.getFechaHoraInicio(), bloqueo.getFechaHoraFin(), agenda.getMargenActividadMinutos())) {
            throw new HorarioComprometidoException(
                    "No se puede marcar como disponible un horario comprometido con una actividad de un proyecto activo.");
        }

        bloqueoAgendaRepository.delete(bloqueo);
    }

    /**
     * Indica si el rango [inicio, fin] solapa algún bloqueo calculado por
     * actividad de un proyecto activo (considerando el margen).
     */
    private boolean solapaActividad(Long idUsuario, LocalDateTime inicio, LocalDateTime fin, int margen) {
        return actividadesDe(idUsuario).stream()
                .anyMatch(a -> a.getFechaHoraInicio().minusMinutes(margen).isBefore(fin)
                        && a.getFechaHoraFin().plusMinutes(margen).isAfter(inicio));
    }

    private List<Actividad> actividadesDe(Long idUsuario) {
        return actividadRepository.findActividadesDeUsuario(idUsuario, ESTADOS_ACTIVOS);
    }

    private void validarJornada(ConfigJornadaRequest request) {
        Set<Integer> vistos = new HashSet<>();
        for (JornadaDiaRequest dia : request.dias()) {
            if (dia.diaSemana() < 1 || dia.diaSemana() > 7) {
                throw new JornadaInvalidaException("El día de la semana debe estar entre 1 (Lunes) y 7 (Domingo).");
            }
            if (!dia.horaFin().isAfter(dia.horaInicio())) {
                throw new JornadaInvalidaException("El horario de fin debe ser posterior al horario de inicio.");
            }
            if (!vistos.add(dia.diaSemana())) {
                throw new JornadaInvalidaException("No se puede repetir el mismo día de la semana en la jornada.");
            }
        }
    }

    private void validarRango(LocalDateTime inicio, LocalDateTime fin) {
        if (fin == null || inicio == null || !fin.isAfter(inicio)) {
            throw new RangoInvalidoException("La fecha y hora de fin debe ser posterior a la de inicio.");
        }
    }

    private Agenda agendaDe(Long idUsuario) {
        return agendaRepository.findByUsuario_IdUsuario(idUsuario)
                .orElseThrow(() -> new AgendaNoEncontradaException(
                        "No se encontró la agenda del usuario."));
    }

    private Usuario usuarioActivo(Long idUsuario) {
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new UsuarioNoEncontradoException("Usuario no encontrado."));
        if (usuario.getEstado() != EstadoUsuario.Activo) {
            throw new UsuarioNoEncontradoException("Usuario no encontrado.");
        }
        return usuario;
    }
}