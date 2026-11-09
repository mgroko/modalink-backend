package org.mgroko.backend.proyectos.servicio;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mgroko.backend.modelo.CalificacionPerfil;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.RolProyectoUsuario;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.perfiles.exception.PerfilNoEncontradoException;
import org.mgroko.backend.proyectos.dto.CalificacionRequest;
import org.mgroko.backend.proyectos.dto.CalificacionResponse;
import org.mgroko.backend.proyectos.dto.CalificacionesProyectoResponse;
import org.mgroko.backend.proyectos.dto.CalificarEquipoRequest;
import org.mgroko.backend.proyectos.dto.MiembroCalificableResponse;
import org.mgroko.backend.proyectos.exception.AccesoDenegadoProyectoException;
import org.mgroko.backend.proyectos.exception.CalificacionIncompletaException;
import org.mgroko.backend.proyectos.exception.CalificacionInvalidaException;
import org.mgroko.backend.proyectos.exception.ProyectoNoCalificableException;
import org.mgroko.backend.proyectos.exception.ProyectoNoEncontradoException;
import org.mgroko.backend.repositorio.CalificacionPerfilRepository;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.ProyectoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.mgroko.backend.auditoria.servicio.AuditarOperacion;

/**
 * UC-37: una vez finalizado el proyecto, cada participante activo califica a
 * todos los demás integrantes activos.
 */
@Service
public class CalificarEquipoService {

    private final PerfilRepository perfilRepository;
    private final ProyectoRepository proyectoRepository;
    private final MiembroProyectoRepository miembroProyectoRepository;
    private final CalificacionPerfilRepository calificacionPerfilRepository;
    private final Clock reloj;

    public CalificarEquipoService(
            PerfilRepository perfilRepository,
            ProyectoRepository proyectoRepository,
            MiembroProyectoRepository miembroProyectoRepository,
            CalificacionPerfilRepository calificacionPerfilRepository,
            Clock reloj) {
        this.perfilRepository = perfilRepository;
        this.proyectoRepository = proyectoRepository;
        this.miembroProyectoRepository = miembroProyectoRepository;
        this.calificacionPerfilRepository = calificacionPerfilRepository;
        this.reloj = reloj;
    }

    @Transactional
    @AuditarOperacion
    public CalificacionesProyectoResponse calificar(
            Long idUsuario,
            Long idPerfilActivo,
            Long idProyecto,
            CalificarEquipoRequest request) {

        Contexto contexto = cargarContexto(idUsuario, idPerfilActivo, idProyecto);
        Map<Long, MiembroProyecto> miembros = indexarMiembros(
                contexto.miembrosActivos());
        Set<Long> yaCalificados = idsCalificados(
                contexto.calificacionesExistentes());

        Set<Long> pendientes = calcularPendientes(
                miembros, idPerfilActivo, yaCalificados);

        Map<Long, CalificacionRequest> solicitudes = validarCalificaciones(
                request, idPerfilActivo, miembros, yaCalificados);

        Set<Long> faltantes = new LinkedHashSet<>(pendientes);
        faltantes.removeAll(solicitudes.keySet());

        if (!faltantes.isEmpty()) {
            throw new CalificacionIncompletaException(
                    List.copyOf(faltantes),
                    construirMensajeFaltantes(faltantes, miembros));
        }

        LocalDateTime ahora = LocalDateTime.now(reloj).withNano(0);
        List<CalificacionPerfil> nuevas = new ArrayList<>();

        for (CalificacionRequest solicitud : solicitudes.values()) {
            MiembroProyecto miembro = miembros.get(solicitud.idPerfil());
            nuevas.add(CalificacionPerfil.builder()
                    .proyecto(contexto.proyecto())
                    .perfilEmisor(contexto.perfilActor())
                    .perfilReceptor(miembro.getPerfil())
                    .valoracion(solicitud.valoracion())
                    .resenia(solicitud.resenia().trim())
                    .fechaRegistro(ahora)
                    .build());
        }

        List<CalificacionPerfil> guardadas =
                calificacionPerfilRepository.saveAll(nuevas);

        Set<Long> todosCalificados = new HashSet<>(yaCalificados);
        todosCalificados.addAll(solicitudes.keySet());

        List<CalificacionPerfil> emitidas = new ArrayList<>(
                contexto.calificacionesExistentes());
        emitidas.addAll(guardadas);

        return construirRespuesta(
                idProyecto,
                contexto,
                idPerfilActivo,
                miembros,
                todosCalificados,
                emitidas);
    }

    @Transactional(readOnly = true)
    public CalificacionesProyectoResponse obtener(
            Long idUsuario,
            Long idPerfilActivo,
            Long idProyecto) {

        Contexto contexto = cargarContexto(idUsuario, idPerfilActivo, idProyecto);
        Map<Long, MiembroProyecto> miembros = indexarMiembros(
                contexto.miembrosActivos());
        Set<Long> yaCalificados = idsCalificados(
                contexto.calificacionesExistentes());

        return construirRespuesta(
                idProyecto,
                contexto,
                idPerfilActivo,
                miembros,
                yaCalificados,
                contexto.calificacionesExistentes());
    }

    private Contexto cargarContexto(
            Long idUsuario,
            Long idPerfilActivo,
            Long idProyecto) {

        if (idPerfilActivo == null) {
            throw new PerfilActivoNoSeleccionadoException();
        }

        Perfil perfil = perfilRepository
                .findByIdPerfilAndUsuarioIdUsuario(idPerfilActivo, idUsuario)
                .orElseThrow(PerfilNoEncontradoException::new);

        if (perfil.getEstado() != EstadoPerfil.Activo) {
            throw new PerfilEnBajaException("El perfil no se encuentra activo.");
        }

        Proyecto proyecto = proyectoRepository.findById(idProyecto)
                .orElseThrow(() -> new ProyectoNoEncontradoException(idProyecto));

        if (proyecto.getEstado() != EstadoProyecto.FINALIZADO) {
            throw new ProyectoNoCalificableException(
                    proyecto.getEstado().getNombre());
        }

        miembroProyectoRepository
                .findMiembroActivoConRol(idProyecto, idPerfilActivo)
                .orElseThrow(() -> new AccesoDenegadoProyectoException(
                        "Debés ser miembro activo del proyecto para calificar al equipo."));

        List<MiembroProyecto> miembros = miembroProyectoRepository
                .buscarActivosDeProyectoConPerfilProfesionYRol(idProyecto);

        List<CalificacionPerfil> existentes = calificacionPerfilRepository
                .findByProyectoIdProyectoAndPerfilEmisorIdPerfil(
                        idProyecto, idPerfilActivo);

        return new Contexto(proyecto, perfil, miembros, existentes);
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

    private Set<Long> idsCalificados(List<CalificacionPerfil> calificaciones) {
        Set<Long> ids = new LinkedHashSet<>();
        for (CalificacionPerfil calificacion : calificaciones) {
            if (calificacion.getPerfilReceptor() != null
                    && calificacion.getPerfilReceptor().getIdPerfil() != null) {
                ids.add(calificacion.getPerfilReceptor().getIdPerfil());
            }
        }
        return ids;
    }

    private Set<Long> calcularPendientes(
            Map<Long, MiembroProyecto> miembros,
            Long idPerfilActivo,
            Set<Long> yaCalificados) {

        Set<Long> pendientes = new LinkedHashSet<>();
        for (Long idPerfil : miembros.keySet()) {
            if (!idPerfilActivo.equals(idPerfil)
                    && !yaCalificados.contains(idPerfil)) {
                pendientes.add(idPerfil);
            }
        }
        return pendientes;
    }

    private Map<Long, CalificacionRequest> validarCalificaciones(
            CalificarEquipoRequest request,
            Long idPerfilActivo,
            Map<Long, MiembroProyecto> miembrosPorPerfil,
            Set<Long> yaCalificados) {

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

            if (yaCalificados.contains(idPerfil)) {
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

    private CalificacionesProyectoResponse construirRespuesta(
            Long idProyecto,
            Contexto contexto,
            Long idPerfilActivo,
            Map<Long, MiembroProyecto> miembros,
            Set<Long> calificados,
            List<CalificacionPerfil> calificaciones) {

        List<MiembroCalificableResponse> miembrosACalificar = new ArrayList<>();
        int requeridas = 0;

        for (MiembroProyecto miembro : miembros.values()) {
            Perfil perfil = miembro.getPerfil();
            if (perfil == null
                    || perfil.getIdPerfil() == null
                    || idPerfilActivo.equals(perfil.getIdPerfil())) {
                continue;
            }
            requeridas++;
            miembrosACalificar.add(toMiembroCalificable(
                    miembro, calificados.contains(perfil.getIdPerfil())));
        }

        int realizadas = (int) miembrosACalificar.stream()
                .filter(MiembroCalificableResponse::yaCalificado)
                .count();
        int pendientes = Math.max(0, requeridas - realizadas);

        List<CalificacionResponse> emitidas = calificaciones.stream()
                .map(this::toCalificacionResponse)
                .toList();

        return new CalificacionesProyectoResponse(
                idProyecto,
                pendientes == 0,
                requeridas,
                realizadas,
                pendientes,
                miembrosACalificar,
                emitidas);
    }

    private MiembroCalificableResponse toMiembroCalificable(
            MiembroProyecto miembro,
            boolean yaCalificado) {

        Perfil perfil = miembro.getPerfil();
        RolProyectoUsuario rol = RolProyectoUsuario.desde(
                miembro.getRolProyecto());

        return new MiembroCalificableResponse(
                perfil.getIdPerfil(),
                perfil.getNombreArtistico(),
                perfil.getUsuario() != null ? perfil.getUsuario().getNombre() : null,
                perfil.getUsuario() != null ? perfil.getUsuario().getApellido() : null,
                rol != null ? rol.name() : null,
                yaCalificado);
    }

    private CalificacionResponse toCalificacionResponse(
            CalificacionPerfil calificacion) {

        return new CalificacionResponse(
                calificacion.getIdCalificacion(),
                calificacion.getPerfilEmisor() != null
                        ? calificacion.getPerfilEmisor().getIdPerfil()
                        : null,
                calificacion.getPerfilReceptor() != null
                        ? calificacion.getPerfilReceptor().getIdPerfil()
                        : null,
                calificacion.getValoracion(),
                calificacion.getResenia(),
                calificacion.getFechaRegistro());
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

        return "Es obligatorio calificar a todos los integrantes activos pendientes. "
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

    private record Contexto(
            Proyecto proyecto,
            Perfil perfilActor,
            List<MiembroProyecto> miembrosActivos,
            List<CalificacionPerfil> calificacionesExistentes) {
    }
}
