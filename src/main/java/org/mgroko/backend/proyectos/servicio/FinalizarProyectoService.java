package org.mgroko.backend.proyectos.servicio;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mgroko.backend.modelo.CalificacionPerfil;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.proyectos.dto.BloqueoFinalizacionResponse;
import org.mgroko.backend.proyectos.dto.CalificacionRequest;
import org.mgroko.backend.proyectos.dto.FinalizarProyectoRequest;
import org.mgroko.backend.proyectos.dto.FinalizarProyectoResponse;
import org.mgroko.backend.proyectos.dto.ProyectoResponse;
import org.mgroko.backend.proyectos.evento.ProyectoFinalizadoEvent;
import org.mgroko.backend.proyectos.exception.CalificacionIncompletaException;
import org.mgroko.backend.proyectos.exception.CalificacionInvalidaException;
import org.mgroko.backend.proyectos.exception.ProyectoNoFinalizableException;
import org.mgroko.backend.repositorio.CalificacionPerfilRepository;
import org.mgroko.backend.repositorio.ProyectoRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC-37: finaliza un proyecto en estado Confirmado.
 *
 * <p>La operación es atómica: valida el estado y la fecha del cronograma,
 * exige las calificaciones del director para todos los miembros activos,
 * persiste las calificaciones y cambia el proyecto a Finalizado con la fecha
 * actual del sistema.</p>
 */
@Service
public class FinalizarProyectoService {

    private final ContextoFinalizacionProyectoService contextoService;
    private final ProyectoRepository proyectoRepository;
    private final CalificacionPerfilRepository calificacionPerfilRepository;
    private final ObtenerProyectoService obtenerProyectoService;
    private final ApplicationEventPublisher eventPublisher;

    public FinalizarProyectoService(
            ContextoFinalizacionProyectoService contextoService,
            ProyectoRepository proyectoRepository,
            CalificacionPerfilRepository calificacionPerfilRepository,
            ObtenerProyectoService obtenerProyectoService,
            ApplicationEventPublisher eventPublisher) {
        this.contextoService = contextoService;
        this.proyectoRepository = proyectoRepository;
        this.calificacionPerfilRepository = calificacionPerfilRepository;
        this.obtenerProyectoService = obtenerProyectoService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public FinalizarProyectoResponse finalizar(
            Long idUsuario,
            Long idPerfilActivo,
            Long idProyecto,
            FinalizarProyectoRequest request) {

        ContextoFinalizacionProyectoService.Contexto contexto =
                contextoService.cargar(idUsuario, idPerfilActivo, idProyecto, true);

        if (!contexto.bloqueos().isEmpty()) {
            BloqueoFinalizacionResponse primero = contexto.bloqueos().get(0);
            throw new ProyectoNoFinalizableException(
                    primero.codigo(),
                    primero.mensaje(),
                    contexto.bloqueos());
        }

        Map<Long, MiembroProyecto> miembrosPorPerfil = indexarMiembros(
                contexto.miembrosActivos());

        Set<Long> perfilesYaCalificados = new LinkedHashSet<>();
        for (CalificacionPerfil calificacion : contexto.calificacionesExistentes()) {
            if (calificacion.getPerfilReceptor() != null
                    && calificacion.getPerfilReceptor().getIdPerfil() != null) {
                perfilesYaCalificados.add(
                        calificacion.getPerfilReceptor().getIdPerfil());
            }
        }

        Set<Long> objetivos = new LinkedHashSet<>();
        for (Long idPerfil : miembrosPorPerfil.keySet()) {
            if (!idPerfilActivo.equals(idPerfil)
                    && !perfilesYaCalificados.contains(idPerfil)) {
                objetivos.add(idPerfil);
            }
        }

        Map<Long, CalificacionRequest> calificacionesPorPerfil =
                validarCalificaciones(
                        request,
                        idPerfilActivo,
                        miembrosPorPerfil,
                        perfilesYaCalificados);

        Set<Long> faltantes = new LinkedHashSet<>(objetivos);
        faltantes.removeAll(calificacionesPorPerfil.keySet());

        if (!faltantes.isEmpty()) {
            throw new CalificacionIncompletaException(
                    List.copyOf(faltantes),
                    construirMensajeFaltantes(faltantes, miembrosPorPerfil));
        }

        LocalDateTime fechaFinalizacion = contexto.ahora();
        List<CalificacionPerfil> calificaciones = new ArrayList<>();

        for (CalificacionRequest solicitud : calificacionesPorPerfil.values()) {
            MiembroProyecto miembro = miembrosPorPerfil.get(solicitud.idPerfil());
            calificaciones.add(CalificacionPerfil.builder()
                    .proyecto(contexto.proyecto())
                    .perfilEmisor(contexto.perfilActor())
                    .perfilReceptor(miembro.getPerfil())
                    .valoracion(solicitud.valoracion())
                    .resenia(solicitud.resenia().trim())
                    .fechaRegistro(fechaFinalizacion)
                    .build());
        }

        calificacionPerfilRepository.saveAll(calificaciones);

        Proyecto proyecto = contexto.proyecto();
        proyecto.setEstado(EstadoProyecto.FINALIZADO);
        proyecto.setFechaFinalizacion(fechaFinalizacion);
        proyectoRepository.saveAndFlush(proyecto);

        eventPublisher.publishEvent(new ProyectoFinalizadoEvent(
                proyecto.getIdProyecto(),
                proyecto.getNombre(),
                fechaFinalizacion,
                idPerfilActivo));

        ProyectoResponse proyectoResponse = obtenerProyectoService.obtener(
                idUsuario, idPerfilActivo, idProyecto);

        return new FinalizarProyectoResponse(
                proyectoResponse,
                fechaFinalizacion,
                calificaciones.size());
    }

    private Map<Long, MiembroProyecto> indexarMiembros(
            List<MiembroProyecto> miembros) {

        Map<Long, MiembroProyecto> indexados = new LinkedHashMap<>();
        for (MiembroProyecto miembro : miembros) {
            if (miembro.getPerfil() == null
                    || miembro.getPerfil().getIdPerfil() == null) {
                continue;
            }
            indexados.putIfAbsent(
                    miembro.getPerfil().getIdPerfil(), miembro);
        }
        return indexados;
    }

    private Map<Long, CalificacionRequest> validarCalificaciones(
            FinalizarProyectoRequest request,
            Long idPerfilActivo,
            Map<Long, MiembroProyecto> miembrosPorPerfil,
            Set<Long> perfilesYaCalificados) {

        List<CalificacionRequest> solicitudes = request == null
                || request.calificaciones() == null
                ? List.of()
                : request.calificaciones();

        Map<Long, CalificacionRequest> porPerfil = new LinkedHashMap<>();

        for (CalificacionRequest solicitud : solicitudes) {
            if (solicitud == null || solicitud.idPerfil() == null) {
                throw new CalificacionInvalidaException(
                        "Cada calificación debe indicar el perfil del miembro.");
            }

            Long idPerfil = solicitud.idPerfil();

            if (idPerfilActivo.equals(idPerfil)) {
                throw new CalificacionInvalidaException(
                        "No podés calificarte a vos mismo.");
            }

            if (perfilesYaCalificados.contains(idPerfil)) {
                throw new CalificacionInvalidaException(
                        "El miembro ya tiene una calificación registrada para este proyecto.");
            }

            if (!miembrosPorPerfil.containsKey(idPerfil)) {
                throw new CalificacionInvalidaException(
                        "El perfil indicado no participa del proyecto.");
            }

            if (porPerfil.putIfAbsent(idPerfil, solicitud) != null) {
                throw new CalificacionInvalidaException(
                        "Hay calificaciones duplicadas para el mismo perfil.");
            }

            validarCalificacion(solicitud);
        }

        return porPerfil;
    }

    private void validarCalificacion(CalificacionRequest solicitud) {
        if (solicitud.valoracion() == null
                || solicitud.valoracion() < 1
                || solicitud.valoracion() > 5) {
            throw new CalificacionInvalidaException(
                    "La valoración debe estar entre 1 y 5.");
        }

        if (solicitud.resenia() == null || solicitud.resenia().isBlank()) {
            throw new CalificacionInvalidaException(
                    "La reseña es obligatoria.");
        }

        if (solicitud.resenia().trim().length() > 200) {
            throw new CalificacionInvalidaException(
                    "La reseña no puede superar los 200 caracteres.");
        }
    }

    private String construirMensajeFaltantes(
            Set<Long> faltantes,
            Map<Long, MiembroProyecto> miembrosPorPerfil) {

        List<String> nombres = faltantes.stream()
                .map(miembrosPorPerfil::get)
                .filter(miembro -> miembro != null && miembro.getPerfil() != null)
                .map(this::nombreMiembro)
                .toList();

        String detalle = nombres.isEmpty()
                ? faltantes.toString()
                : String.join(", ", nombres);

        return "Es obligatorio calificar a todos los miembros activos del proyecto. "
                + "Faltan: " + detalle + ".";
    }

    private String nombreMiembro(MiembroProyecto miembro) {
        Perfil perfil = miembro.getPerfil();
        if (perfil.getNombreArtistico() != null
                && !perfil.getNombreArtistico().isBlank()) {
            return perfil.getNombreArtistico();
        }
        if (perfil.getUsuario() != null) {
            String nombre = perfil.getUsuario().getNombre();
            String apellido = perfil.getUsuario().getApellido();
            String completo = ((nombre != null ? nombre : "")
                    + " " + (apellido != null ? apellido : "")).trim();
            if (!completo.isEmpty()) {
                return completo;
            }
        }
        return "perfil " + perfil.getIdPerfil();
    }
}
