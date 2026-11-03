package org.mgroko.backend.proyectos.servicio;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.mgroko.backend.modelo.AsignacionActividad;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Notificacion;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.notificaciones.servicio.NotificacionDestinoResolver;
import org.mgroko.backend.notificaciones.servicio.NotificacionService;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.perfiles.exception.PerfilNoEncontradoException;
import org.mgroko.backend.proyectos.dto.ConflictoFijoResponse;
import org.mgroko.backend.proyectos.dto.ReprogramacionFallidaResponse;
import org.mgroko.backend.proyectos.dto.ReprogramacionProyectoResponse;
import org.mgroko.backend.proyectos.dto.ResumenReprogramacionResponse;
import org.mgroko.backend.proyectos.exception.ProyectoNoConfirmableException;
import org.mgroko.backend.proyectos.exception.ProyectoNoEncontradoException;
import org.mgroko.backend.repositorio.AsignacionActividadRepository;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.ProyectoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Orquesta la reprogramación automática de los proyectos confirmados en los
 * que participa un usuario cuando cambia su jornada o margen.
 *
 * <p>Cada proyecto se reprograma en su propia transacción. Si uno falla, los
 * demás continúan y el proyecto fallido conserva su plan anterior. Las
 * notificaciones son posteriores a la transacción y best-effort: no revierten
 * una reprogramación ya confirmada.</p>
 */
@Service
public class JornadaReprogramacionService {

    private static final Logger LOG =
            LoggerFactory.getLogger(JornadaReprogramacionService.class);

    private final AsignacionActividadRepository asignacionActividadRepository;
    private final ReprogramarProyectoService reprogramarProyectoService;
    private final ProyectoRepository proyectoRepository;
    private final MiembroProyectoRepository miembroProyectoRepository;
    private final NotificacionService notificacionService;
    private final PerfilRepository perfilRepository;
    private final ProyectoSecurityService proyectoSecurityService;
    private final boolean automatica;

    public JornadaReprogramacionService(
            AsignacionActividadRepository asignacionActividadRepository,
            ReprogramarProyectoService reprogramarProyectoService,
            ProyectoRepository proyectoRepository,
            MiembroProyectoRepository miembroProyectoRepository,
            NotificacionService notificacionService,
            PerfilRepository perfilRepository,
            ProyectoSecurityService proyectoSecurityService,
            @Value("${agenda.reprogramacion-automatica:true}") boolean automatica) {
        this.asignacionActividadRepository = asignacionActividadRepository;
        this.reprogramarProyectoService = reprogramarProyectoService;
        this.proyectoRepository = proyectoRepository;
        this.miembroProyectoRepository = miembroProyectoRepository;
        this.notificacionService = notificacionService;
        this.perfilRepository = perfilRepository;
        this.proyectoSecurityService = proyectoSecurityService;
        this.automatica = automatica;
    }

    /**
     * Detecta y reprograma los proyectos confirmados afectados por el cambio
     * de jornada o margen del usuario.
     */
    public ResumenReprogramacionResponse reprogramarAfectados(Long idUsuario) {
        if (!automatica) {
            return ResumenReprogramacionResponse.vacio();
        }

        List<Long> idsProyectos = asignacionActividadRepository
                .findProyectosConfirmadosDeUsuario(idUsuario);
        if (idsProyectos.isEmpty()) {
            return ResumenReprogramacionResponse.vacio();
        }

        Map<Long, String> nombres = new HashMap<>();
        for (Proyecto proyecto : proyectoRepository.findAllById(idsProyectos)) {
            nombres.put(proyecto.getIdProyecto(), proyecto.getNombre());
        }

        List<ReprogramacionProyectoResponse> exitosos = new ArrayList<>();
        List<ReprogramacionFallidaResponse> fallidos = new ArrayList<>();

        for (Long idProyecto : idsProyectos.stream().sorted().toList()) {
            String nombre = nombres.getOrDefault(
                    idProyecto, "Proyecto " + idProyecto);
            try {
                ReprogramacionProyectoResponse resultado =
                        reprogramarProyectoService.reprogramar(
                                idProyecto,
                                MotivoReprogramacion.CAMBIO_JORNADA,
                                null);

                if (resultado.huboCambios()
                        || !resultado.conflictosFijos().isEmpty()) {
                    exitosos.add(resultado);
                    notificarExito(resultado);
                }
            } catch (ProyectoNoConfirmableException ex) {
                ReprogramacionFallidaResponse fallo =
                        new ReprogramacionFallidaResponse(
                                idProyecto,
                                nombre,
                                ex.getCodigo(),
                                ex.getMessage(),
                                ex.getBloqueos());
                fallidos.add(fallo);
                notificarFallo(fallo);
            } catch (RuntimeException ex) {
                LOG.error("No se pudo reprogramar el proyecto {} por cambio de jornada.",
                        idProyecto, ex);
                ReprogramacionFallidaResponse fallo =
                        new ReprogramacionFallidaResponse(
                                idProyecto,
                                nombre,
                                "ERROR_REPROGRAMACION",
                                ex.getMessage() != null
                                        ? ex.getMessage()
                                        : "Error inesperado al reprogramar el proyecto.",
                                List.of());
                fallidos.add(fallo);
                notificarFallo(fallo);
            }
        }

        return new ResumenReprogramacionResponse(
                exitosos.size() + fallidos.size(),
                exitosos,
                fallidos);
    }

    /**
     * Reintento manual de un proyecto que falló. Solo un director activo del
     * proyecto puede ejecutarlo.
     */
    public ReprogramacionProyectoResponse reintentar(
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

        proyectoRepository.findById(idProyecto)
                .orElseThrow(() -> new ProyectoNoEncontradoException(idProyecto));

        proyectoSecurityService.validarDirector(idProyecto, idPerfilActivo);

        ReprogramacionProyectoResponse resultado =
                reprogramarProyectoService.reprogramar(
                        idProyecto,
                        MotivoReprogramacion.REINTENTO,
                        null);
        notificarExito(resultado);
        return resultado;
    }

    private void notificarExito(ReprogramacionProyectoResponse resultado) {
        if (!resultado.huboCambios() && resultado.conflictosFijos().isEmpty()) {
            return;
        }

        try {
            Map<Long, Destinatario> destinatarios = new LinkedHashMap<>();

            for (MiembroProyecto director
                    : miembroProyectoRepository.buscarDirectoresDeProyectos(
                            List.of(resultado.idProyecto()))) {
                agregar(destinatarios, director.getPerfil());
            }

            List<Long> idsActividadesMovidas = resultado.actividades().stream()
                    .map(actividad -> actividad.idActividad())
                    .toList();
            if (!idsActividadesMovidas.isEmpty()) {
                for (AsignacionActividad asignacion
                        : asignacionActividadRepository.findActivasByActividadIds(
                                idsActividadesMovidas)) {
                    if (asignacion.getMiembro() != null) {
                        agregar(destinatarios, asignacion.getMiembro().getPerfil());
                    }
                }
            }

            for (ConflictoFijoResponse conflicto : resultado.conflictosFijos()) {
                for (Long idUsuario : conflicto.idsUsuarios()) {
                    perfilRepository
                            .findByUsuario_IdUsuarioAndEstado(
                                    idUsuario, EstadoPerfil.Activo)
                            .stream()
                            .findFirst()
                            .ifPresent(perfil -> agregar(destinatarios, perfil));
                }
            }

            String mensaje = "El cronograma del proyecto '"
                    + resultado.nombreProyecto()
                    + "' se reprogramó por "
                    + descripcionMotivo(resultado.motivo())
                    + ". Actividades movidas: "
                    + resultado.actividades().size()
                    + ".";

            List<Notificacion> notificaciones = destinatarios.values().stream()
                    .map(destino -> notificacionService.crear(
                            destino.usuario(),
                            destino.perfil(),
                            NotificacionService.TIPO_AJUSTE_HORARIO,
                            mensaje,
                            NotificacionDestinoResolver.ENTIDAD_PROYECTO,
                            resultado.idProyecto(),
                            resultado.idProyecto()))
                    .toList();
            notificacionService.registrarTodas(notificaciones);

            if (!resultado.conflictosFijos().isEmpty()) {
                notificarConflictosFijos(resultado);
            }
        } catch (RuntimeException ex) {
            LOG.warn("No se pudieron enviar las notificaciones de reprogramación del proyecto {}.",
                    resultado.idProyecto(), ex);
        }
    }

    private void notificarConflictosFijos(ReprogramacionProyectoResponse resultado) {
        String mensaje = "El proyecto '"
                + resultado.nombreProyecto()
                + "' tiene "
                + resultado.conflictosFijos().size()
                + " actividad(es) en curso o finalizada(s) que quedaron fuera de la nueva disponibilidad y no se movieron.";
        List<Notificacion> notificaciones = new ArrayList<>();
        for (MiembroProyecto director
                : miembroProyectoRepository.buscarDirectoresDeProyectos(
                        List.of(resultado.idProyecto()))) {
            if (director.getPerfil() == null
                    || director.getPerfil().getUsuario() == null) {
                continue;
            }
            notificaciones.add(notificacionService.crear(
                    director.getPerfil().getUsuario(),
                    director.getPerfil(),
                    NotificacionService.TIPO_SOBRECOMPROMISO,
                    mensaje,
                    NotificacionDestinoResolver.ENTIDAD_PROYECTO,
                    resultado.idProyecto(),
                    resultado.idProyecto()));
        }
        notificacionService.registrarTodas(notificaciones);
    }

    private void notificarFallo(ReprogramacionFallidaResponse fallo) {
        try {
            String mensaje = "El proyecto '"
                    + fallo.nombreProyecto()
                    + "' no pudo reprogramarse automáticamente por un cambio de disponibilidad. Motivo: "
                    + (fallo.mensaje() != null ? fallo.mensaje() : fallo.codigo())
                    + ". El cronograma anterior quedó intacto.";
            List<Notificacion> notificaciones = new ArrayList<>();
            for (MiembroProyecto director
                    : miembroProyectoRepository.buscarDirectoresDeProyectos(
                            List.of(fallo.idProyecto()))) {
                if (director.getPerfil() == null
                        || director.getPerfil().getUsuario() == null) {
                    continue;
                }
                notificaciones.add(notificacionService.crear(
                        director.getPerfil().getUsuario(),
                        director.getPerfil(),
                        NotificacionService.TIPO_SOBRECOMPROMISO,
                        mensaje,
                        NotificacionDestinoResolver.ENTIDAD_PROYECTO,
                        fallo.idProyecto(),
                        fallo.idProyecto()));
            }
            notificacionService.registrarTodas(notificaciones);
        } catch (RuntimeException ex) {
            LOG.warn("No se pudieron enviar las notificaciones de fallo del proyecto {}.",
                    fallo.idProyecto(), ex);
        }
    }

    private void agregar(Map<Long, Destinatario> destinatarios, Perfil perfil) {
        if (perfil == null
                || perfil.getUsuario() == null
                || perfil.getUsuario().getIdUsuario() == null) {
            return;
        }
        destinatarios.putIfAbsent(
                perfil.getUsuario().getIdUsuario(),
                new Destinatario(perfil.getUsuario(), perfil));
    }

    private String descripcionMotivo(String motivo) {
        if (MotivoReprogramacion.CAMBIO_ENTREGA.name().equals(motivo)) {
            return "un cambio de fecha de entrega";
        }
        if (MotivoReprogramacion.REINTENTO.name().equals(motivo)) {
            return "un reintento manual";
        }
        return "un cambio de jornada o margen";
    }

    private record Destinatario(Usuario usuario, Perfil perfil) {
    }
}
