package org.mgroko.backend.calendario.servicio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.mgroko.backend.actividades.dto.CalculoCpm;
import org.mgroko.backend.actividades.dto.RangoFactibleAbsoluto;
import org.mgroko.backend.actividades.servicio.CronogramaService;
import org.mgroko.backend.auth.exception.UsuarioNoEncontradoException;
import org.mgroko.backend.admin.servicio.ConfiguracionSistemaService;
import org.mgroko.backend.calendario.dto.BloqueoResponse;
import org.mgroko.backend.calendario.dto.CalendarioResponse;
import org.mgroko.backend.calendario.dto.ConfigJornadaRequest;
import org.mgroko.backend.calendario.dto.ConfigJornadaResponse;
import org.mgroko.backend.calendario.dto.JornadaDiaRequest;
import org.mgroko.backend.calendario.dto.MarcarNoDisponibleRequest;
import org.mgroko.backend.calendario.dto.ResultadoDisponibilidad;
import org.mgroko.backend.calendario.dto.DetalleMiembroDisponibilidad;
import org.mgroko.backend.calendario.exception.AgendaNoEncontradaException;
import org.mgroko.backend.calendario.exception.BloqueoNoEncontradoException;
import org.mgroko.backend.calendario.exception.BloqueoSolapadoException;
import org.mgroko.backend.calendario.exception.HorarioComprometidoException;
import org.mgroko.backend.calendario.exception.JornadaInvalidaException;
import org.mgroko.backend.calendario.exception.RangoInvalidoException;
import org.mgroko.backend.calendario.mapper.CalendarioMapper;
import org.mgroko.backend.calendario.servicio.DisponibilidadCalculator.Intervalo;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.Agenda;
import org.mgroko.backend.modelo.BloqueoAgenda;
import org.mgroko.backend.modelo.JornadaAgenda;
import org.mgroko.backend.modelo.Planificacion;
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
    private final ConfiguracionSistemaService configuracionSistemaService;
    private final CronogramaService cronogramaService;
    private final DisponibilidadCalculator disponibilidadCalculator = new DisponibilidadCalculator();

    public CalendarioService(
            UsuarioRepository usuarioRepository,
            AgendaRepository agendaRepository,
            JornadaAgendaRepository jornadaAgendaRepository,
            BloqueoAgendaRepository bloqueoAgendaRepository,
            ActividadRepository actividadRepository,
            ConfiguracionSistemaService configuracionSistemaService,
            CronogramaService cronogramaService) {
        this.usuarioRepository = usuarioRepository;
        this.agendaRepository = agendaRepository;
        this.jornadaAgendaRepository = jornadaAgendaRepository;
        this.bloqueoAgendaRepository = bloqueoAgendaRepository;
        this.actividadRepository = actividadRepository;
        this.configuracionSistemaService = configuracionSistemaService;
        this.cronogramaService = cronogramaService;
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
        int margen = margenDe(agenda);

        return new CalendarioResponse(
                CalendarioMapper.toConfigJornadaResponse(agenda, dias),
                bloqueos.stream().map(CalendarioMapper::toBloqueoResponse).toList(),
                actividades.stream()
                        .map(a -> CalendarioMapper.toBloqueoActividadResponse(a, margen))
                        .toList());
    }

    /**
     * Devuelve la agenda pública de un usuario (para visualización de disponibilidad
     * en su perfil). Incluye la jornada laboral, los bloqueos por actividades y los
     * bloqueos manuales con el motivo siempre visible: el motivo del bloqueo es
     * público para cualquier usuario autenticado que consulta el perfil.
     */
    @Transactional(readOnly = true)
    public CalendarioResponse obtenerPublico(Long idUsuario) {
        usuarioActivo(idUsuario);
        Agenda agenda = agendaDe(idUsuario);
        List<JornadaAgenda> dias =
                jornadaAgendaRepository.findByAgenda_IdAgendaOrderByDiaSemana(agenda.getIdAgenda());
        List<BloqueoAgenda> bloqueos =
                bloqueoAgendaRepository.findByAgenda_IdAgendaOrderByFechaHoraInicio(agenda.getIdAgenda());
        List<Actividad> actividades = actividadesDe(idUsuario);
        int margen = margenDe(agenda);

        return new CalendarioResponse(
                CalendarioMapper.toConfigJornadaResponse(agenda, dias),
                bloqueos.stream().map(CalendarioMapper::toBloqueoResponse).toList(),
                actividades.stream()
                        .map(a -> CalendarioMapper.toBloqueoActividadResponse(a, margen))
                        .toList());
    }

    /**
     * Verifica si el usuario tiene disponibilidad frente al rango factible de
     * una actividad (UC-26 y UC-70).
     *
     * <p>El rango se obtiene del CPM relativo de la planificación, proyectado
     * con el {@code t0} del proyecto, y puede ser tentativo mientras el
     * proyecto no tenga fecha de inicio confirmada. Mientras lo sea, el rango
     * evaluado es {@code [ahora, fecha de entrega]} (la actividad todavía no
     * está agendada) y la verificación es provisional; al confirmar el
     * proyecto se usa el rango estricto {@code [ES, LF]}.</p>
     *
     * <p>La regla no exige que la ventana completa esté libre: alcanza con que
     * exista un hueco continuo de al menos la duración de la actividad después
     * de descontar la jornada, los bloqueos manuales y las actividades ya
     * asignadas al usuario.</p>
     *
     * <p><strong>La verificación es provisional</strong> si
     * {@link ResultadoDisponibilidad#provisional()} es {@code true}: debe
     * repetirse al confirmar el proyecto (UC-60) y al asignar la actividad
     * (UC-70).</p>
     *
     * @param idUsuario usuario cuya agenda se evalúa
     * @param actividad actividad a la que se quiere postular o asignar
     * @return resultado con la disponibilidad y el rango evaluado
     */
    @Transactional(readOnly = true)
    public ResultadoDisponibilidad verificarDisponibilidad(Long idUsuario, Actividad actividad) {
        return verificarDisponibilidadMiembros(List.of(idUsuario), actividad);
    }

    /**
     * Evalúa una actividad contra el calendario de todos los miembros
     * indicados. La actividad solo está disponible cuando existe un hueco
     * común, aplicando el margen configurado a los bloqueos existentes y a la
     * actividad nueva.
     */
    @Transactional(readOnly = true)
    public ResultadoDisponibilidad verificarDisponibilidadMiembros(
            Collection<Long> idsUsuarios,
            Actividad actividad) {

        if (idsUsuarios == null || idsUsuarios.isEmpty()) {
            return new ResultadoDisponibilidad(
                    false, false, null, null, null, null, "SIN_MIEMBROS", List.of());
        }
        idsUsuarios.forEach(this::usuarioActivo);

        Planificacion planificacion = actividad.getPlanificacion();
        if (planificacion == null || planificacion.getIdPlanificacion() == null) {
            throw new IllegalArgumentException(
                    "La actividad no tiene una planificación asociada.");
        }

        List<Actividad> actividades = actividadRepository
                .findConPredecesorasByPlanificacion(planificacion.getIdPlanificacion());
        CalculoCpm calculo = cronogramaService.calcular(planificacion, actividades);
        RangoFactibleAbsoluto rango = cronogramaService.rangoFactibleAbsoluto(
                planificacion, calculo, actividad.getIdActividad());
        List<DetalleMiembroDisponibilidad> detalles = new ArrayList<>();
        List<List<Intervalo>> huecosPorMiembro = new ArrayList<>();
        int margenActividad = 0;

        for (Long idUsuario : idsUsuarios) {
            Agenda agenda = agendaDe(idUsuario);
            int margen = margenDe(agenda);
            margenActividad = Math.max(margenActividad, margen);
            List<Intervalo> ventanas = ventanasDe(agenda, rango.inicio(), rango.fin());
            List<Intervalo> bloqueos = bloqueosDe(idUsuario, agenda, rango.inicio(), rango.fin());
            List<Intervalo> huecos = disponibilidadCalculator.huecosLibres(ventanas, bloqueos);
            Optional<Intervalo> primerHueco = disponibilidadCalculator.primerHueco(
                    ventanas, bloqueos, actividad.getDuracionMinutos(), margen);
            String motivo = motivoIndividual(ventanas, huecos, primerHueco, actividad);
            detalles.add(new DetalleMiembroDisponibilidad(
                    idUsuario,
                    rango.inicio(),
                    rango.fin(),
                    huecos,
                    primerHueco.isPresent(),
                    motivo));
            huecosPorMiembro.add(huecos);
        }

        List<Intervalo> huecosComunes = disponibilidadCalculator.intersecarHuecos(
                huecosPorMiembro);
        Optional<Intervalo> seleccionado = disponibilidadCalculator.primerHuecoEnHuecos(
                huecosComunes,
                actividad.getDuracionMinutos(),
                margenActividad);
        boolean disponible = seleccionado.isPresent();
        String motivo = disponible
                ? null
                : idsUsuarios.size() > 1 ? "SIN_HUECO_COMUN"
                        : detalles.get(0).motivo();

        return new ResultadoDisponibilidad(
                disponible,
                rango.tentativa(),
                rango.inicio(),
                rango.fin(),
                disponible ? seleccionado.get().inicio() : null,
                disponible ? seleccionado.get().fin() : null,
                motivo,
                detalles);
    }

    private String motivoIndividual(
            List<Intervalo> ventanas,
            List<Intervalo> huecos,
            Optional<Intervalo> primerHueco,
            Actividad actividad) {
        if (ventanas.isEmpty()) {
            return "SIN_JORNADA";
        }
        if (primerHueco.isPresent()) {
            return null;
        }
        boolean hayVentanaSuficiente = ventanas.stream()
                .anyMatch(v -> v.minutos() >= actividad.getDuracionMinutos());
        return hayVentanaSuficiente && !huecos.isEmpty()
                ? "SIN_HUECO"
                : "DURACION_EXCEDE_VENTANA";
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
                        .horaInicioManana(dia.horaInicioManana())
                        .horaFinManana(dia.horaFinManana())
                        .horaInicioTarde(dia.horaInicioTarde())
                        .horaFinTarde(dia.horaFinTarde())
                        .build());
            } else if (cambioHorario(existente, dia)) {
                existente.setHoraInicioManana(dia.horaInicioManana());
                existente.setHoraFinManana(dia.horaFinManana());
                existente.setHoraInicioTarde(dia.horaInicioTarde());
                existente.setHoraFinTarde(dia.horaFinTarde());
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
     * obligatorio (NOT NULL en BD). No permite superponerse con otro bloqueo manual ni con un
     * horario comprometido por una actividad de un proyecto activo.
     */
    @Transactional
    public BloqueoResponse marcarNoDisponible(Long idUsuario, MarcarNoDisponibleRequest request) {
        usuarioActivo(idUsuario);
        validarRango(request.fechaHoraInicio(), request.fechaHoraFin());
        Agenda agenda = agendaDe(idUsuario);

        if (solapaActividad(idUsuario, request.fechaHoraInicio(), request.fechaHoraFin(), margenDe(agenda))) {
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

        if (solapaActividad(idUsuario, bloqueo.getFechaHoraInicio(), bloqueo.getFechaHoraFin(), margenDe(agenda))) {
            throw new HorarioComprometidoException(
                    "No se puede marcar como disponible un horario comprometido con una actividad de un proyecto activo.");
        }

        bloqueoAgendaRepository.delete(bloqueo);
    }

    /**
     * Indica si el rango [inicio, fin] solapa algún bloqueo calculado por
     * actividad de un proyecto activo (considerando el margen). Las
     * actividades sin inicio o fin se ignoran (no deberían existir: son
     * NOT NULL en BD).
     */
    private boolean solapaActividad(Long idUsuario, LocalDateTime inicio, LocalDateTime fin, int margen) {
        return actividadesDe(idUsuario).stream()
                .filter(a -> a.getFechaHoraInicio() != null && a.getFechaHoraFin() != null)
                .anyMatch(a -> a.getFechaHoraInicio().minusMinutes(margen).isBefore(fin)
                        && a.getFechaHoraFin().plusMinutes(margen).isAfter(inicio));
    }

    private List<Actividad> actividadesDe(Long idUsuario) {
        return actividadRepository.findActividadesDeUsuario(idUsuario, ESTADOS_ACTIVOS);
    }

    /**
     * Construye las ventanas de jornada del usuario recortadas al rango
     * factible. Un día sin fila en {@code jornada_agenda} no es laborable.
     */
    private List<Intervalo> ventanasDe(
            Agenda agenda,
            LocalDateTime desde,
            LocalDateTime hasta) {

        List<JornadaAgenda> dias = jornadaAgendaRepository
                .findByAgenda_IdAgendaOrderByDiaSemana(agenda.getIdAgenda());
        Map<Integer, JornadaAgenda> porDia = dias.stream()
                .collect(Collectors.toMap(JornadaAgenda::getDiaSemana, Function.identity()));

        List<Intervalo> ventanas = new ArrayList<>();
        LocalDate fecha = desde.toLocalDate();
        LocalDate ultima = hasta.toLocalDate();

        while (!fecha.isAfter(ultima)) {
            JornadaAgenda dia = porDia.get(fecha.getDayOfWeek().getValue());
            if (dia != null) {
                if (dia.esPartida()) {
                    agregarVentana(ventanas, fecha, dia.getHoraInicioManana(),
                            dia.getHoraFinManana(), desde, hasta);
                    agregarVentana(ventanas, fecha, dia.getHoraInicioTarde(),
                            dia.getHoraFinTarde(), desde, hasta);
                } else {
                    agregarVentana(ventanas, fecha, dia.getHoraInicioManana(),
                            dia.getHoraFinTarde(), desde, hasta);
                }
            }
            fecha = fecha.plusDays(1);
        }

        return ventanas;
    }

    private void agregarVentana(
            List<Intervalo> ventanas,
            LocalDate fecha,
            LocalTime inicio,
            LocalTime fin,
            LocalDateTime desde,
            LocalDateTime hasta) {

        if (inicio == null || fin == null) {
            return;
        }

        LocalDateTime inicioAbsoluto = fecha.atTime(inicio);
        LocalDateTime finAbsoluto = fecha.atTime(fin);
        LocalDateTime inicioRecortado = inicioAbsoluto.isAfter(desde) ? inicioAbsoluto : desde;
        LocalDateTime finRecortado = finAbsoluto.isBefore(hasta) ? finAbsoluto : hasta;

        if (finRecortado.isAfter(inicioRecortado)) {
            ventanas.add(new Intervalo(inicioRecortado, finRecortado));
        }
    }

    /**
     * Bloqueos que se descuentan de la jornada: los manuales del usuario y las
     * actividades ya asignadas, extendidas por el margen configurado.
     */
    private List<Intervalo> bloqueosDe(
            Long idUsuario,
            Agenda agenda,
            LocalDateTime desde,
            LocalDateTime hasta) {

        List<Intervalo> bloqueos = new ArrayList<>();

        for (BloqueoAgenda bloqueo : bloqueoAgendaRepository
                .findSolapados(agenda.getIdAgenda(), desde, hasta)) {
            if (bloqueo.getFechaHoraFin().isAfter(bloqueo.getFechaHoraInicio())) {
                bloqueos.add(new Intervalo(
                        bloqueo.getFechaHoraInicio(), bloqueo.getFechaHoraFin()));
            }
        }

        int margen = margenDe(agenda);
        for (Actividad actividad : actividadesDe(idUsuario)) {
            LocalDateTime inicio = actividad.getFechaHoraInicio();
            LocalDateTime fin = actividad.getFechaHoraFin();
            if (inicio == null || fin == null || !fin.isAfter(inicio)) {
                continue;
            }

            LocalDateTime inicioBloqueo = inicio.minusMinutes(margen);
            LocalDateTime finBloqueo = fin.plusMinutes(margen);

            if (inicioBloqueo.isBefore(hasta) && finBloqueo.isAfter(desde)) {
                bloqueos.add(new Intervalo(inicioBloqueo, finBloqueo));
            }
        }

        return bloqueos;
    }

    private void validarJornada(ConfigJornadaRequest request) {
        Set<Integer> vistos = new HashSet<>();
        for (JornadaDiaRequest dia : request.dias()) {
            if (dia.diaSemana() < 1 || dia.diaSemana() > 7) {
                throw new JornadaInvalidaException("El día de la semana debe estar entre 1 (Lunes) y 7 (Domingo).");
            }
            validarHorarios(dia);
            if (!vistos.add(dia.diaSemana())) {
                throw new JornadaInvalidaException("No se puede repetir el mismo día de la semana en la jornada.");
            }
        }
    }

    /**
     * Valida la consistencia del horario del día: jornada de corrido (par
     * del mediodía ausente) o partida (bloques de mañana y tarde en orden
     * estricto). No se admite informar solo una de las dos horas del
     * mediodía. Refleja los checks chk_jornada_* de la migración V19.
     */
    private void validarHorarios(JornadaDiaRequest dia) {
        boolean tieneFinManiana = dia.horaFinManana() != null;
        boolean tieneInicioTarde = dia.horaInicioTarde() != null;

        if (tieneFinManiana != tieneInicioTarde) {
            throw new JornadaInvalidaException(
                    "Para una jornada partida se deben informar el fin del bloque de la mañana y el inicio del bloque de la tarde; para una jornada de corrido, ninguno de los dos.");
        }
        if (!dia.horaFinTarde().isAfter(dia.horaInicioManana())) {
            throw new JornadaInvalidaException("El horario de fin debe ser posterior al horario de inicio.");
        }
        if (tieneFinManiana) {
            if (!dia.horaFinManana().isAfter(dia.horaInicioManana())) {
                throw new JornadaInvalidaException("El fin del bloque de la mañana debe ser posterior a su inicio.");
            }
            if (!dia.horaInicioTarde().isAfter(dia.horaFinManana())) {
                throw new JornadaInvalidaException("El bloque de la tarde debe comenzar después del fin del bloque de la mañana.");
            }
            if (!dia.horaFinTarde().isAfter(dia.horaInicioTarde())) {
                throw new JornadaInvalidaException("El fin del bloque de la tarde debe ser posterior a su inicio.");
            }
        }
    }

    /** Indica si el horario persistido del día difiere del enviado. */
    private boolean cambioHorario(JornadaAgenda existente, JornadaDiaRequest dia) {
        return !Objects.equals(existente.getHoraInicioManana(), dia.horaInicioManana())
                || !Objects.equals(existente.getHoraFinManana(), dia.horaFinManana())
                || !Objects.equals(existente.getHoraInicioTarde(), dia.horaInicioTarde())
                || !Objects.equals(existente.getHoraFinTarde(), dia.horaFinTarde());
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

    /**
     * Margen efectivo de la agenda en minutos: el valor propio si está
     * informado, o el default configurable de
     * {@code configuracion_sistema} (clave {@code AGENDA_MARGEN_ACTIVIDAD_MIN}).
     * Nunca retorna null (resuelve también el unboxing de H-07).
     */
    private int margenDe(Agenda agenda) {
        if (agenda.getMargenActividadMinutos() != null) {
            return agenda.getMargenActividadMinutos();
        }
        return configuracionSistemaService.obtenerMargenActividadDefecto();
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