package org.mgroko.backend.proyectos.servicio;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.mgroko.backend.modelo.CaracteristicaTecnica;
import org.mgroko.backend.modelo.Habilidad;
import org.mgroko.backend.modelo.Imagen;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Moodboard;
import org.mgroko.backend.modelo.Notificacion;
import org.mgroko.backend.modelo.Objetivo;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.PermisoProyecto;
import org.mgroko.backend.modelo.Planificacion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RequerimientoGralCaract;
import org.mgroko.backend.modelo.RequerimientoGralCaractValor;
import org.mgroko.backend.modelo.RequerimientoGralProyecto;
import org.mgroko.backend.modelo.RolProyecto;
import org.mgroko.backend.modelo.enums.EstadoActividad;
import org.mgroko.backend.modelo.enums.EstadoParticipacion;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.RolProyectoUsuario;
import org.mgroko.backend.notificaciones.servicio.NotificacionService;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.perfiles.exception.PerfilNoEncontradoException;
import org.mgroko.backend.proyectos.dto.CaracteristicaRequerimientoResponse;
import org.mgroko.backend.proyectos.dto.ContextoConsultaProyecto;
import org.mgroko.backend.proyectos.dto.CrearObjetivoRequest;
import org.mgroko.backend.proyectos.dto.CrearRequerimientoCaractRequest;
import org.mgroko.backend.proyectos.dto.CupoRequerimiento;
import org.mgroko.backend.proyectos.dto.ModificarProyectoRequest;
import org.mgroko.backend.proyectos.dto.ModificarRequerimientoGralRequest;
import org.mgroko.backend.proyectos.dto.MoodboardResponse;
import org.mgroko.backend.proyectos.dto.ProyectoResponse;
import org.mgroko.backend.proyectos.dto.RequerimientoGralResponse;
import org.mgroko.backend.proyectos.exception.EstadoProyectoNoModificableException;
import org.mgroko.backend.proyectos.exception.FechaEntregaInvalidaException;
import org.mgroko.backend.proyectos.exception.ImagenMoodboardNoEncontradaException;
import org.mgroko.backend.proyectos.exception.ProyectoNoConfirmableException;
import org.mgroko.backend.proyectos.exception.ProyectoNoEncontradoException;
import org.mgroko.backend.proyectos.exception.RequerimientoGralProtegidoException;
import org.mgroko.backend.proyectos.exception.RequerimientoInvalidoException;
import org.mgroko.backend.proyectos.exception.UbicacionObligatoriaException;
import org.mgroko.backend.proyectos.mapper.ProyectoMapper;
import org.mgroko.backend.repositorio.ImagenRepository;
import org.mgroko.backend.repositorio.InvitacionGralRepository;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
import org.mgroko.backend.repositorio.MoodboardRepository;
import org.mgroko.backend.repositorio.ObjetivoRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.PlanificacionRepository;
import org.mgroko.backend.repositorio.PostulacionGralRepository;
import org.mgroko.backend.repositorio.ProyectoRepository;
import org.mgroko.backend.repositorio.RequerimientoActividadRepository;
import org.mgroko.backend.repositorio.RequerimientoGralCaractValorRepository;
import org.mgroko.backend.repositorio.RequerimientoGralProyectoRepository;
import org.mgroko.backend.storage.servicio.BorradoArchivosDiferido;
import org.mgroko.backend.ubicacion.servicio.UbicacionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.mgroko.backend.auditoria.servicio.AuditarOperacion;

/**
 * UC-36: Modificar proyecto. Permite editar la información básica de un proyecto
 * en estado Borrador, Publicado o Confirmado.
 *
 * Reglas acordadas:
 * - El nombre del proyecto es inmutable (no forma parte de la solicitud).
 * - Los requerimientos de personal con postulaciones aceptadas (satisfechos) no
 *   se pueden modificar ni eliminar: si viajan en la solicitud se rechaza, y si
 *   se omiten se conservan tal cual.
 * - En estado Publicado o Confirmado no se puede quitar la ubicación y el
 *   proyecto debe seguir contando con toda la información básica.
 */
@Service
public class ModificarProyectoService {

    public static final String PERMISO_MODIFICAR_PROYECTO = "MODIFICAR_PROYECTO";

    private final PerfilRepository perfilRepository;
    private final ProyectoRepository proyectoRepository;
    private final MiembroProyectoRepository miembroProyectoRepository;
    private final PlanificacionRepository planificacionRepository;
    private final ObjetivoRepository objetivoRepository;
    private final MoodboardRepository moodboardRepository;
    private final RequerimientoGralProyectoRepository requerimientoGralProyectoRepository;
    private final RequerimientoActividadRepository requerimientoActividadRepository;
    private final RequerimientoGralCaractValorRepository requerimientoGralCaractValorRepository;
    private final PostulacionGralRepository postulacionGralRepository;
    private final InvitacionGralRepository invitacionGralRepository;
    private final RequerimientoGralProteccionService requerimientoGralProteccionService;
    private final CupoRequerimientoService cupoRequerimientoService;
    private final UbicacionService ubicacionService;
    private final RequerimientoGralService requerimientoGralService;
    private final ProyectoSecurityService proyectoSecurityService;
    private final ReprogramarProyectoService reprogramarProyectoService;
    private final NotificacionService notificacionService;
    private final BajaVoluntariaProyectoPolicy bajaVoluntariaProyectoPolicy;
    private final RequerimientoSnapshotService requerimientoSnapshotService;
    private final ImagenRepository imagenRepository;
    private final BorradoArchivosDiferido borradoArchivosDiferido;
    private final Clock reloj;

    public ModificarProyectoService(
            PerfilRepository perfilRepository,
            ProyectoRepository proyectoRepository,
            MiembroProyectoRepository miembroProyectoRepository,
            PlanificacionRepository planificacionRepository,
            ObjetivoRepository objetivoRepository,
            MoodboardRepository moodboardRepository,
            RequerimientoGralProyectoRepository requerimientoGralProyectoRepository,
            RequerimientoActividadRepository requerimientoActividadRepository,
            RequerimientoGralCaractValorRepository requerimientoGralCaractValorRepository,
            PostulacionGralRepository postulacionGralRepository,
            InvitacionGralRepository invitacionGralRepository,
            RequerimientoGralProteccionService requerimientoGralProteccionService,
            CupoRequerimientoService cupoRequerimientoService,
            UbicacionService ubicacionService,
            RequerimientoGralService requerimientoGralService,
            ProyectoSecurityService proyectoSecurityService,
            ReprogramarProyectoService reprogramarProyectoService,
            NotificacionService notificacionService,
            BajaVoluntariaProyectoPolicy bajaVoluntariaProyectoPolicy,
            RequerimientoSnapshotService requerimientoSnapshotService,
            ImagenRepository imagenRepository,
            BorradoArchivosDiferido borradoArchivosDiferido,
            Clock reloj) {
        this.perfilRepository = perfilRepository;
        this.proyectoRepository = proyectoRepository;
        this.miembroProyectoRepository = miembroProyectoRepository;
        this.planificacionRepository = planificacionRepository;
        this.objetivoRepository = objetivoRepository;
        this.moodboardRepository = moodboardRepository;
        this.requerimientoGralProyectoRepository = requerimientoGralProyectoRepository;
        this.requerimientoActividadRepository = requerimientoActividadRepository;
        this.requerimientoGralCaractValorRepository = requerimientoGralCaractValorRepository;
        this.postulacionGralRepository = postulacionGralRepository;
        this.invitacionGralRepository = invitacionGralRepository;
        this.requerimientoGralProteccionService = requerimientoGralProteccionService;
        this.cupoRequerimientoService = cupoRequerimientoService;
        this.ubicacionService = ubicacionService;
        this.requerimientoGralService = requerimientoGralService;
        this.proyectoSecurityService = proyectoSecurityService;
        this.reprogramarProyectoService = reprogramarProyectoService;
        this.notificacionService = notificacionService;
        this.bajaVoluntariaProyectoPolicy = bajaVoluntariaProyectoPolicy;
        this.requerimientoSnapshotService = requerimientoSnapshotService;
        this.imagenRepository = imagenRepository;
        this.borradoArchivosDiferido = borradoArchivosDiferido;
        this.reloj = reloj;
    }

    @Transactional
    @AuditarOperacion
    public ProyectoResponse modificar(
            Long idUsuario, Long idPerfilActivo, Long idProyecto, ModificarProyectoRequest request) {

        if (idPerfilActivo == null) {
            throw new PerfilActivoNoSeleccionadoException();
        }

        Perfil perfil = perfilRepository.findByIdPerfilAndUsuarioIdUsuario(idPerfilActivo, idUsuario)
                .orElseThrow(PerfilNoEncontradoException::new);

        if (perfil.getEstado() != EstadoPerfil.Activo) {
            throw new PerfilEnBajaException("El perfil no se encuentra activo.");
        }

        Proyecto proyecto = proyectoRepository.findById(idProyecto)
                .orElseThrow(() -> new ProyectoNoEncontradoException(idProyecto));

        proyectoSecurityService.validarPermiso(idProyecto, idPerfilActivo, PERMISO_MODIFICAR_PROYECTO);

        validarEstadoModificable(proyecto);

        LocalDate fechaEntregaAnterior = null;

        if (proyecto.getEstado() == EstadoProyecto.CONFIRMADO) {
            // Bloquea el proyecto mientras se recalcula y persiste el cronograma.
            proyecto = proyectoRepository.findByIdParaGestion(idProyecto)
                    .orElseThrow(() -> new ProyectoNoEncontradoException(idProyecto));
            fechaEntregaAnterior = planificacionRepository
                    .findByProyectoIdProyectoForUpdate(idProyecto)
                    .map(Planificacion::getFechaEntrega)
                    .orElse(null);
        }

        actualizarDatosBasicos(proyecto, request);
        actualizarUbicacion(proyecto, request);
        reprogramarConfirmadoSiCambiaEntrega(idProyecto, proyecto, request);
        actualizarFechaEntrega(idProyecto, request);
        actualizarObjetivos(idProyecto, proyecto, request.objetivos());
        actualizarRequerimientos(idProyecto, proyecto, request.requerimientosGral());
        actualizarMoodboard(idProyecto, proyecto, request);

        Proyecto guardado = proyectoRepository.save(proyecto);

        validarInformacionBasicaSiCorresponde(idProyecto, guardado);
        notificarCambioFechaEntregaSiCorresponde(
                idProyecto, fechaEntregaAnterior, request);

        return armarResponse(guardado, perfil);
    }

    /**
     * Si el proyecto está confirmado y la fecha cambia, recalcula el CPM y la
     * disponibilidad de todas las actividades y persiste el nuevo plan.
     *
     * <p>Las actividades {@code PENDIENTE} se recalculan siempre. Las
     * {@code EN_CURSO} y {@code FINALIZADA} conservan su inicio real.</p>
     */
    private void reprogramarConfirmadoSiCambiaEntrega(
            Long idProyecto,
            Proyecto proyecto,
            ModificarProyectoRequest request) {

        if (proyecto.getEstado() != EstadoProyecto.CONFIRMADO
                || request.fechaEntregaEstipulada() == null) {
            return;
        }

        LocalDate nuevaFecha = request.fechaEntregaEstipulada();
        Planificacion planificacion = planificacionRepository
                .findByProyectoIdProyectoForUpdate(idProyecto)
                .orElseThrow(() -> new ProyectoNoEncontradoException(idProyecto));

        if (Objects.equals(nuevaFecha, planificacion.getFechaEntrega())) {
            return;
        }

        if (nuevaFecha.isBefore(LocalDate.now())) {
            throw new FechaEntregaInvalidaException(
                    "La fecha de entrega estipulada no puede ser anterior a la fecha actual.");
        }

        LocalDateTime fechaInicio = proyecto.getFechaInicio();
        if (fechaInicio == null) {
            throw new ProyectoNoConfirmableException(
                    "DATOS_INCONSISTENTES",
                    "El proyecto confirmado no tiene fecha de inicio.");
        }
        if (nuevaFecha.isBefore(fechaInicio.toLocalDate())) {
            throw new FechaEntregaInvalidaException(
                    "La fecha de entrega no puede ser anterior al inicio del proyecto.");
        }

        reprogramarProyectoService.reprogramar(
                idProyecto,
                MotivoReprogramacion.CAMBIO_ENTREGA,
                nuevaFecha);
    }

    private void notificarCambioFechaEntregaSiCorresponde(
            Long idProyecto,
            LocalDate fechaAnterior,
            ModificarProyectoRequest request) {

        if (fechaAnterior == null
                || request.fechaEntregaEstipulada() == null
                || Objects.equals(fechaAnterior, request.fechaEntregaEstipulada())) {
            return;
        }

        String motivo = request.motivoCambioFechaEntrega() == null
                ? null
                : request.motivoCambioFechaEntrega().trim();
        String mensaje = "La fecha de entrega del proyecto cambió de "
                + fechaAnterior + " a " + request.fechaEntregaEstipulada() + "."
                + (motivo == null || motivo.isBlank() ? "" : " Motivo: " + motivo);

        List<Notificacion> notificaciones = new ArrayList<>();
        for (MiembroProyecto miembro
                : miembroProyectoRepository.buscarMiembrosActivosDeProyecto(idProyecto)) {
            if (miembro.getPerfil() == null
                    || miembro.getPerfil().getUsuario() == null) {
                continue;
            }
            notificaciones.add(notificacionService.crear(
                    miembro.getPerfil().getUsuario(),
                    miembro.getPerfil(),
                    NotificacionService.TIPO_PROYECTO_FECHA_ENTREGA_MODIFICADA,
                    mensaje,
                    "PROYECTO",
                    idProyecto,
                    idProyecto));
        }
        notificacionService.registrarTodas(notificaciones);
    }

    private void validarEstadoModificable(Proyecto proyecto) {
        EstadoProyecto estado = proyecto.getEstado();
        if (estado == null || !estado.permiteModificacion()) {
            throw new EstadoProyectoNoModificableException(
                    estado != null ? estado.getNombre() : null);
        }
    }

    private void actualizarDatosBasicos(Proyecto proyecto, ModificarProyectoRequest request) {
        proyecto.setDescripcion(request.descripcion().trim());
        proyecto.setPrivacidad(request.privacidad());
        if (request.aceptaPostulacionGral() != null) {
            proyecto.setAceptaPostulacionGral(request.aceptaPostulacionGral());
        }
    }

    private void actualizarUbicacion(Proyecto proyecto, ModificarProyectoRequest request) {
        boolean traeUbicacion = request.ubicacion() != null
                && request.ubicacion().localidadId() != null
                && !request.ubicacion().localidadId().isBlank();

        if (traeUbicacion) {
            proyecto.setUbicacion(ubicacionService.obtenerOCrear(
                    request.ubicacion().localidadId(),
                    request.ubicacion().provinciaId()));
            return;
        }

        if (proyecto.getEstado() != EstadoProyecto.BORRADOR) {
            throw new UbicacionObligatoriaException();
        }

        proyecto.setUbicacion(null);
    }

    private void actualizarFechaEntrega(Long idProyecto, ModificarProyectoRequest request) {
        if (request.fechaEntregaEstipulada() == null) {
            return;
        }

        if (request.fechaEntregaEstipulada().isBefore(LocalDate.now())) {
            throw new FechaEntregaInvalidaException(
                    "La fecha de entrega estipulada no puede ser anterior a la fecha actual.");
        }

        Planificacion planificacion = planificacionRepository.findByProyectoIdProyecto(idProyecto)
                .orElse(null);
        if (planificacion != null) {
            planificacion.setFechaEntrega(request.fechaEntregaEstipulada());
            planificacionRepository.save(planificacion);
        }
    }

    private void actualizarObjetivos(Long idProyecto, Proyecto proyecto, List<CrearObjetivoRequest> objetivos) {
        if (objetivos == null) {
            return;
        }

        List<Objetivo> existentes = objetivoRepository.findByProyectoIdProyecto(idProyecto);
        if (!existentes.isEmpty()) {
            objetivoRepository.deleteAll(existentes);
        }

        for (CrearObjetivoRequest objetivoRequest : objetivos) {
            objetivoRepository.save(Objetivo.builder()
                    .proyecto(proyecto)
                    .nombre(objetivoRequest.nombre().trim())
                    .descripcion(objetivoRequest.descripcion() != null
                            ? objetivoRequest.descripcion().trim()
                            : null)
                    .build());
        }
    }

    /**
     * Aplica el payload de requerimientos generales con semántica de diff:
     * los protegidos se conservan, los no protegidos omitidos se eliminan y
     * los enviados se crean o actualizan en el lugar.
     */
    private void actualizarRequerimientos(
            Long idProyecto,
            Proyecto proyecto,
            List<ModificarRequerimientoGralRequest> requerimientos) {

        if (requerimientos == null) {
            return;
        }

        List<RequerimientoGralProyecto> existentes =
                requerimientoGralProyectoRepository.findByProyectoIdProyectoForUpdate(idProyecto);
        Map<Long, RequerimientoGralProyecto> porId = new HashMap<>();
        for (RequerimientoGralProyecto existente : existentes) {
            if (existente.getIdRequerimientoGral() != null) {
                porId.put(existente.getIdRequerimientoGral(), existente);
            }
        }
        Map<Long, RequerimientoSnapshot> snapshotsActuales =
                snapshotsExistentes(existentes);

        Set<Long> idsSolicitados = new HashSet<>();
        Set<Long> idsSinCambios = new HashSet<>();

        for (ModificarRequerimientoGralRequest requerimientoRequest : requerimientos) {
            if (requerimientoRequest == null) {
                throw new RequerimientoInvalidoException(
                        "Cada requerimiento debe incluir cantidad y profesión.");
            }

            Long idRequerimiento = requerimientoRequest.idRequerimientoGral();
            if (idRequerimiento == null) {
                continue;
            }

            RequerimientoGralProyecto existente = porId.get(idRequerimiento);
            if (existente == null) {
                throw new RequerimientoInvalidoException(
                        "El requerimiento de personal " + idRequerimiento
                                + " no pertenece a este proyecto.");
            }
            if (!idsSolicitados.add(idRequerimiento)) {
                throw new RequerimientoInvalidoException(
                        "El requerimiento de personal " + idRequerimiento
                                + " está duplicado en la solicitud.");
            }

            RequerimientoSnapshot snapshot = snapshotDesdeRequest(requerimientoRequest);
            if (snapshot.equals(snapshotsActuales.get(idRequerimiento))) {
                idsSinCambios.add(idRequerimiento);
                continue;
            }

            if (requerimientoGralProteccionService.estaProtegido(existente)) {
                throw new RequerimientoGralProtegidoException(idRequerimiento);
            }
        }

        LocalDateTime fechaHistorial = LocalDateTime.now(reloj);
        for (RequerimientoGralProyecto existente : existentes) {
            if (idsSolicitados.contains(existente.getIdRequerimientoGral())) {
                continue;
            }
            if (requerimientoGralProteccionService.estaProtegido(existente)) {
                // Política acordada: el requerimiento protegido omitido se conserva.
                continue;
            }
            requerimientoSnapshotService.asegurarHistorialGeneral(
                    existente, fechaHistorial);
            requerimientoGralService.eliminar(existente);
        }

        for (ModificarRequerimientoGralRequest requerimientoRequest : requerimientos) {
            if (requerimientoRequest.idRequerimientoGral() == null) {
                requerimientoGralService.persistir(
                        proyecto,
                        requerimientoRequest.idProfesion(),
                        requerimientoRequest.cantidad(),
                        requerimientoRequest.descripcion(),
                        requerimientoRequest.caracteristicas(),
                        requerimientoRequest.habilidades());
                continue;
            }

            if (idsSinCambios.contains(requerimientoRequest.idRequerimientoGral())) {
                continue;
            }

            RequerimientoGralProyecto existente =
                    porId.get(requerimientoRequest.idRequerimientoGral());
            requerimientoSnapshotService.asegurarHistorialGeneral(
                    existente, fechaHistorial);
            requerimientoGralService.actualizar(
                    existente,
                    requerimientoRequest.idProfesion(),
                    requerimientoRequest.cantidad(),
                    requerimientoRequest.descripcion(),
                    requerimientoRequest.caracteristicas(),
                    requerimientoRequest.habilidades());
        }
    }

    private Map<Long, RequerimientoSnapshot> snapshotsExistentes(
            List<RequerimientoGralProyecto> requerimientos) {

        List<Long> idsCaracteristicas = requerimientos.stream()
                .flatMap(requerimiento -> requerimiento.getCaracteristicas().stream())
                .map(RequerimientoGralCaract::getIdReqGralCaract)
                .filter(Objects::nonNull)
                .toList();

        Map<Long, Set<Long>> valoresPorCaracteristica = new HashMap<>();
        if (!idsCaracteristicas.isEmpty()) {
            for (RequerimientoGralCaractValor valor
                    : requerimientoGralCaractValorRepository
                            .findByIdReqGralCaractIn(idsCaracteristicas)) {
                valoresPorCaracteristica
                        .computeIfAbsent(valor.getIdReqGralCaract(), id -> new HashSet<>())
                        .add(valor.getIdValor());
            }
        }

        Map<Long, RequerimientoSnapshot> resultado = new HashMap<>();
        for (RequerimientoGralProyecto requerimiento : requerimientos) {
            Map<Long, CaracteristicaSnapshot> caracteristicas = new HashMap<>();
            for (RequerimientoGralCaract caracteristica
                    : requerimiento.getCaracteristicas()) {
                Long idCaracteristica = caracteristica.getCaracteristica()
                        .getIdCaracteristica();
                Set<Long> valores = new HashSet<>(valoresPorCaracteristica
                        .getOrDefault(caracteristica.getIdReqGralCaract(), Set.of()));
                caracteristicas.put(idCaracteristica, new CaracteristicaSnapshot(
                        idCaracteristica,
                        normalizar(caracteristica.getValorMin()),
                        normalizar(caracteristica.getValorMax()),
                        valores));
            }

            Set<Long> habilidades = new HashSet<>();
            if (requerimiento.getHabilidades() != null) {
                requerimiento.getHabilidades()
                        .forEach(habilidad -> habilidades.add(habilidad.getIdHabilidad()));
            }

            resultado.put(requerimiento.getIdRequerimientoGral(), new RequerimientoSnapshot(
                    requerimiento.getCantidad(),
                    requerimiento.getProfesion() != null
                            ? requerimiento.getProfesion().getIdProfesion()
                            : null,
                    normalizarTexto(requerimiento.getDescripcion()),
                    caracteristicas,
                    habilidades));
        }
        return resultado;
    }

    private RequerimientoSnapshot snapshotDesdeRequest(
            ModificarRequerimientoGralRequest requerimiento) {

        Map<Long, CaracteristicaSnapshot> caracteristicas = new HashMap<>();
        if (requerimiento.caracteristicas() != null) {
            for (CrearRequerimientoCaractRequest caracteristica
                    : requerimiento.caracteristicas()) {
                Set<Long> valores = caracteristica.valores() == null
                        ? Set.of()
                        : new HashSet<>(caracteristica.valores());
                if (caracteristicas.put(
                        caracteristica.idCaracteristica(),
                        new CaracteristicaSnapshot(
                                caracteristica.idCaracteristica(),
                                normalizar(caracteristica.valorMin()),
                                normalizar(caracteristica.valorMax()),
                                valores)) != null) {
                    throw new RequerimientoInvalidoException(
                            "La característica técnica "
                                    + caracteristica.idCaracteristica()
                                    + " está duplicada en el requerimiento.");
                }
            }
        }

        Set<Long> habilidades = requerimiento.habilidades() == null
                ? Set.of()
                : new HashSet<>(requerimiento.habilidades());

        return new RequerimientoSnapshot(
                requerimiento.cantidad(),
                requerimiento.idProfesion(),
                normalizarTexto(requerimiento.descripcion()),
                caracteristicas,
                habilidades);
    }

    private BigDecimal normalizar(BigDecimal valor) {
        return valor == null ? null : valor.stripTrailingZeros();
    }

    private String normalizarTexto(String texto) {
        if (texto == null) {
            return null;
        }
        String normalizado = texto.trim();
        return normalizado.isEmpty() ? null : normalizado;
    }

    private record CaracteristicaSnapshot(
            Long idCaracteristica,
            BigDecimal valorMin,
            BigDecimal valorMax,
            Set<Long> valores) {
    }

    private record RequerimientoSnapshot(
            Integer cantidad,
            Long idProfesion,
            String descripcion,
            Map<Long, CaracteristicaSnapshot> caracteristicas,
            Set<Long> habilidades) {
    }

    private void actualizarMoodboard(Long idProyecto, Proyecto proyecto, ModificarProyectoRequest request) {
        if (request.moodboard() == null) {
            return;
        }

        String descripcion = request.moodboard().descripcion() != null
                ? request.moodboard().descripcion().trim()
                : null;

        Moodboard moodboard = moodboardRepository.findByProyectoIdProyecto(idProyecto)
                .orElse(null);

        if (moodboard == null) {
            List<Long> conservadas = request.moodboard().imagenesConservadas();
            if (conservadas != null && !conservadas.isEmpty()) {
                throw new ImagenMoodboardNoEncontradaException(conservadas.get(0));
            }
            moodboardRepository.save(Moodboard.builder()
                    .proyecto(proyecto)
                    .descripcion(descripcion)
                    .build());
            return;
        }

        moodboard.setDescripcion(descripcion);

        if (request.moodboard().imagenesConservadas() != null) {
            reconciliarImagenes(moodboard, request.moodboard().imagenesConservadas());
        }

        moodboardRepository.save(moodboard);
    }

    /**
     * UC-36: deja en el moodboard únicamente las imágenes indicadas. Las
     * restantes se desasocian y se borran físicamente (fila + archivo). El
     * archivo se elimina después del commit para no dejar rastros si la
     * transacción falla.
     */
    private void reconciliarImagenes(Moodboard moodboard, List<Long> idsConservadas) {
        if (moodboard.getImagenes() == null) {
            moodboard.setImagenes(new HashSet<>());
        }

        Set<Long> conservadas = new HashSet<>();
        for (Long id : idsConservadas) {
            if (id != null) {
                conservadas.add(id);
            }
        }

        for (Long id : conservadas) {
            boolean pertenece = moodboard.getImagenes().stream()
                    .anyMatch(imagen -> id.equals(imagen.getIdImagen()));
            if (!pertenece) {
                throw new ImagenMoodboardNoEncontradaException(id);
            }
        }

        List<Imagen> aEliminar = moodboard.getImagenes().stream()
                .filter(imagen -> !conservadas.contains(imagen.getIdImagen()))
                .toList();

        if (aEliminar.isEmpty()) {
            return;
        }

        // Primero se quitan las filas de imagen_moodboard; recién después se
        // puede borrar la fila de imagen sin violar la FK.
        moodboard.getImagenes().removeAll(aEliminar);
        moodboardRepository.saveAndFlush(moodboard);

        for (Imagen imagen : aEliminar) {
            borradoArchivosDiferido.registrarBorrados(null, imagen.getNombreArchivo());
            imagenRepository.delete(imagen);
        }
    }

    private void validarInformacionBasicaSiCorresponde(Long idProyecto, Proyecto proyecto) {
        if (proyecto.getEstado() == EstadoProyecto.BORRADOR) {
            return;
        }

        // Fuerza a que las altas, bajas y modificaciones de requerimientos se
        // vean en las consultas de validación de esta misma transacción.
        requerimientoGralProyectoRepository.flush();

        LocalDate fechaEntrega = planificacionRepository.findByProyectoIdProyecto(idProyecto)
                .map(Planificacion::getFechaEntrega)
                .orElse(null);

        boolean tieneRequerimientoGral = !requerimientoGralProyectoRepository
                .findByProyectoIdProyecto(idProyecto).isEmpty();
        boolean tieneRequerimientoActividad = requerimientoActividadRepository
                .existsByProyectoExcluyendoEstado(idProyecto, EstadoActividad.CANCELADA);

        ProyectoInformacionBasicaValidator.validar(
                proyecto.getUbicacion(),
                fechaEntrega,
                moodboardRepository.findByProyectoIdProyecto(idProyecto).isPresent(),
                tieneRequerimientoGral || tieneRequerimientoActividad);
    }

    private ProyectoResponse armarResponse(Proyecto proyecto, Perfil perfilConsultante) {
        Perfil director = buscarDirector(proyecto.getIdProyecto());

        LocalDate fechaEntrega = planificacionRepository.findByProyectoIdProyecto(proyecto.getIdProyecto())
                .map(Planificacion::getFechaEntrega)
                .orElse(null);

        List<Objetivo> objetivos = objetivoRepository.findByProyectoIdProyecto(proyecto.getIdProyecto());

        List<RequerimientoGralProyecto> requerimientosEntidad =
                requerimientoGralProyectoRepository.findByProyectoIdProyecto(proyecto.getIdProyecto());
        Map<Long, Boolean> protegidos = requerimientoGralProteccionService
                .calcularProtegidos(requerimientosEntidad);

        List<RequerimientoGralResponse> requerimientos = new ArrayList<>();
        for (RequerimientoGralProyecto rg : requerimientosEntidad) {
            requerimientos.add(armarRequerimientoResponse(
                    rg,
                    protegidos.getOrDefault(rg.getIdRequerimientoGral(), Boolean.FALSE)));
        }

        MoodboardResponse moodboardResponse = moodboardRepository.findByProyectoIdProyecto(proyecto.getIdProyecto())
                .map(ProyectoMapper::toMoodboardResponse)
                .orElse(null);

        ContextoConsultaProyecto contexto = armarContexto(
                proyecto, perfilConsultante);

        return ProyectoMapper.toResponse(proyecto, director, fechaEntrega, objetivos,
                requerimientos, moodboardResponse, contexto);
    }

    private ContextoConsultaProyecto armarContexto(
            Proyecto proyecto,
            Perfil perfilConsultante) {
        Optional<MiembroProyecto> miembroActivo = miembroProyectoRepository
                .findMiembroActivoConPermisos(
                        proyecto.getIdProyecto(), perfilConsultante.getIdPerfil());

        if (miembroActivo.isEmpty()) {
            return new ContextoConsultaProyecto(
                    null, Boolean.FALSE, List.of(), Boolean.FALSE, Boolean.FALSE);
        }

        MiembroProyecto miembro = miembroActivo.get();
        RolProyecto rol = miembro.getRolProyecto();
        List<String> permisos = rol != null && rol.getPermisos() != null
                ? rol.getPermisos().stream()
                        .map(PermisoProyecto::getNombre)
                        .filter(Objects::nonNull)
                        .sorted()
                        .toList()
                : List.of();

        return new ContextoConsultaProyecto(
                RolProyectoUsuario.desde(rol),
                Boolean.TRUE,
                permisos,
                Boolean.FALSE,
                bajaVoluntariaProyectoPolicy.puedeDarseDeBaja(proyecto, miembro));
    }

    private Perfil buscarDirector(Long idProyecto) {
        return miembroProyectoRepository.findByProyectoIdProyecto(idProyecto).stream()
                .filter(m -> m.getEstadoParticipacion() == EstadoParticipacion.ACTIVO)
                .filter(m -> m.getRolProyecto() != null
                        && RolProyectoUsuario.desde(m.getRolProyecto()) == RolProyectoUsuario.DIRECTOR)
                .map(MiembroProyecto::getPerfil)
                .findFirst()
                .orElse(null);
    }

    private RequerimientoGralResponse armarRequerimientoResponse(
            RequerimientoGralProyecto rg,
            Boolean protegido) {
        List<CaracteristicaRequerimientoResponse> caracts = new ArrayList<>();
        for (RequerimientoGralCaract caract : rg.getCaracteristicas()) {
            CaracteristicaTecnica ct = caract.getCaracteristica();
            List<Long> valores = requerimientoGralCaractValorRepository
                    .findByIdReqGralCaract(caract.getIdReqGralCaract()).stream()
                    .map(RequerimientoGralCaractValor::getIdValor)
                    .sorted()
                    .toList();
            caracts.add(new CaracteristicaRequerimientoResponse(
                    ct.getIdCaracteristica(),
                    ct.getCodigo(),
                    ct.getTipoDato(),
                    caract.getValorMin(),
                    caract.getValorMax(),
                    valores));
        }

        List<Long> habilidades = rg.getHabilidades().stream()
                .map(Habilidad::getIdHabilidad)
                .sorted()
                .toList();

        CupoRequerimiento cupo = cupoRequerimientoService.calcularGeneral(rg);

        return new RequerimientoGralResponse(
                rg.getIdRequerimientoGral(),
                rg.getCantidad(),
                rg.getDescripcion(),
                rg.getProfesion() != null ? rg.getProfesion().getIdProfesion() : null,
                rg.getProfesion() != null ? rg.getProfesion().getNombre() : null,
                caracts,
                habilidades,
                cupo.satisfecho(),
                cupo.cubiertos(),
                cupo.faltantes(),
                Boolean.FALSE,
                protegido);
    }
}
