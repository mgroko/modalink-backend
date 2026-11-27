package org.mgroko.backend.proyectos.servicio;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.mgroko.backend.actividades.dto.ActividadCronogramaResponse;
import org.mgroko.backend.actividades.dto.CronogramaResponse;
import org.mgroko.backend.actividades.exception.FechaEntregaNoDefinidaException;
import org.mgroko.backend.actividades.servicio.ObtenerCronogramaService;
import org.mgroko.backend.admin.servicio.ConfiguracionSistemaService;
import org.mgroko.backend.modelo.CalificacionPerfil;
import org.mgroko.backend.modelo.Contrato;
import org.mgroko.backend.modelo.FirmaContrato;
import org.mgroko.backend.modelo.InvitacionActividad;
import org.mgroko.backend.modelo.InvitacionGral;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Moodboard;
import org.mgroko.backend.modelo.Objetivo;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Planificacion;
import org.mgroko.backend.modelo.PostulacionActividad;
import org.mgroko.backend.modelo.PostulacionGral;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RequerimientoGralProyecto;
import org.mgroko.backend.modelo.enums.EstadoContrato;
import org.mgroko.backend.modelo.enums.EstadoFirmaContrato;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.RolProyectoUsuario;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.perfiles.exception.PerfilNoEncontradoException;
import org.mgroko.backend.proyectos.dto.AlertaInforme;
import org.mgroko.backend.proyectos.dto.CupoRequerimiento;
import org.mgroko.backend.proyectos.dto.InformeProyectoResponse;
import org.mgroko.backend.proyectos.dto.InformeProyectoResponse.ActividadProximaInforme;
import org.mgroko.backend.proyectos.dto.InformeProyectoResponse.CalificacionesInforme;
import org.mgroko.backend.proyectos.dto.InformeProyectoResponse.ContratoInforme;
import org.mgroko.backend.proyectos.dto.InformeProyectoResponse.ContratosInforme;
import org.mgroko.backend.proyectos.dto.InformeProyectoResponse.CronogramaInforme;
import org.mgroko.backend.proyectos.dto.InformeProyectoResponse.EquipoInforme;
import org.mgroko.backend.proyectos.dto.InformeProyectoResponse.FirmasInforme;
import org.mgroko.backend.proyectos.dto.InformeProyectoResponse.GeneradoPorInforme;
import org.mgroko.backend.proyectos.dto.InformeProyectoResponse.HistorialInforme;
import org.mgroko.backend.proyectos.dto.InformeProyectoResponse.ImagenInforme;
import org.mgroko.backend.proyectos.dto.InformeProyectoResponse.MetricasInforme;
import org.mgroko.backend.proyectos.dto.InformeProyectoResponse.MiembroInforme;
import org.mgroko.backend.proyectos.dto.InformeProyectoResponse.MoodboardInforme;
import org.mgroko.backend.proyectos.dto.InformeProyectoResponse.ProyectoInforme;
import org.mgroko.backend.proyectos.dto.InformeProyectoResponse.RequerimientoCoberturaInforme;
import org.mgroko.backend.proyectos.dto.ObjetivoResponse;
import org.mgroko.backend.proyectos.exception.ProyectoNoEncontradoException;
import org.mgroko.backend.repositorio.ActividadRepository;
import org.mgroko.backend.repositorio.CalificacionPerfilRepository;
import org.mgroko.backend.repositorio.ContratoRepository;
import org.mgroko.backend.repositorio.FirmaContratoRepository;
import org.mgroko.backend.repositorio.InvitacionActividadRepository;
import org.mgroko.backend.repositorio.InvitacionGralRepository;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
import org.mgroko.backend.repositorio.MoodboardRepository;
import org.mgroko.backend.repositorio.ObjetivoRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.PlanificacionRepository;
import org.mgroko.backend.repositorio.PostulacionActividadRepository;
import org.mgroko.backend.repositorio.PostulacionGralRepository;
import org.mgroko.backend.repositorio.ProyectoRepository;
import org.mgroko.backend.repositorio.RequerimientoGralProyectoRepository;
import org.mgroko.backend.ubicacion.mapper.UbicacionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Arma el informe ejecutivo de estado y salud de un proyecto.
 *
 * <p>Es una consulta de solo lectura. La carga de datos y el mapeo a DTO se
 * hacen dentro de la transacción; el renderizado PDF queda fuera.</p>
 */
@Service
public class GenerarInformeProyectoService {

    private static final int MAX_PROXIMAS_ACTIVIDADES = 8;
    private static final int MAX_HISTORIAL_PDF = 20;
    private static final String VERSION_INFORME = "1.0";

    private final ProyectoRepository proyectoRepository;
    private final PerfilRepository perfilRepository;
    private final MiembroProyectoRepository miembroProyectoRepository;
    private final PlanificacionRepository planificacionRepository;
    private final ObjetivoRepository objetivoRepository;
    private final RequerimientoGralProyectoRepository requerimientoGralProyectoRepository;
    private final CupoRequerimientoService cupoRequerimientoService;
    private final MoodboardRepository moodboardRepository;
    private final ConfiguracionSistemaService configuracionSistemaService;
    private final ActividadRepository actividadRepository;
    private final ObtenerCronogramaService obtenerCronogramaService;
    private final ContratoRepository contratoRepository;
    private final FirmaContratoRepository firmaContratoRepository;
    private final PostulacionGralRepository postulacionGralRepository;
    private final PostulacionActividadRepository postulacionActividadRepository;
    private final InvitacionGralRepository invitacionGralRepository;
    private final InvitacionActividadRepository invitacionActividadRepository;
    private final CalificacionPerfilRepository calificacionPerfilRepository;
    private final ProyectoSecurityService proyectoSecurityService;
    private final DetectarAlertasProyectoService detectarAlertasProyectoService;
    private final CalcularEstadoAreasService calcularEstadoAreasService;
    private final Clock reloj;

    public GenerarInformeProyectoService(
            ProyectoRepository proyectoRepository,
            PerfilRepository perfilRepository,
            MiembroProyectoRepository miembroProyectoRepository,
            PlanificacionRepository planificacionRepository,
            ObjetivoRepository objetivoRepository,
            RequerimientoGralProyectoRepository requerimientoGralProyectoRepository,
            CupoRequerimientoService cupoRequerimientoService,
            MoodboardRepository moodboardRepository,
            ConfiguracionSistemaService configuracionSistemaService,
            ActividadRepository actividadRepository,
            ObtenerCronogramaService obtenerCronogramaService,
            ContratoRepository contratoRepository,
            FirmaContratoRepository firmaContratoRepository,
            PostulacionGralRepository postulacionGralRepository,
            PostulacionActividadRepository postulacionActividadRepository,
            InvitacionGralRepository invitacionGralRepository,
            InvitacionActividadRepository invitacionActividadRepository,
            CalificacionPerfilRepository calificacionPerfilRepository,
            ProyectoSecurityService proyectoSecurityService,
            DetectarAlertasProyectoService detectarAlertasProyectoService,
            CalcularEstadoAreasService calcularEstadoAreasService,
            Clock reloj) {
        this.proyectoRepository = proyectoRepository;
        this.perfilRepository = perfilRepository;
        this.miembroProyectoRepository = miembroProyectoRepository;
        this.planificacionRepository = planificacionRepository;
        this.objetivoRepository = objetivoRepository;
        this.requerimientoGralProyectoRepository = requerimientoGralProyectoRepository;
        this.cupoRequerimientoService = cupoRequerimientoService;
        this.moodboardRepository = moodboardRepository;
        this.configuracionSistemaService = configuracionSistemaService;
        this.actividadRepository = actividadRepository;
        this.obtenerCronogramaService = obtenerCronogramaService;
        this.contratoRepository = contratoRepository;
        this.firmaContratoRepository = firmaContratoRepository;
        this.postulacionGralRepository = postulacionGralRepository;
        this.postulacionActividadRepository = postulacionActividadRepository;
        this.invitacionGralRepository = invitacionGralRepository;
        this.invitacionActividadRepository = invitacionActividadRepository;
        this.calificacionPerfilRepository = calificacionPerfilRepository;
        this.proyectoSecurityService = proyectoSecurityService;
        this.detectarAlertasProyectoService = detectarAlertasProyectoService;
        this.calcularEstadoAreasService = calcularEstadoAreasService;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public InformeProyectoResponse generar(
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

        Proyecto proyecto = proyectoRepository.findByIdConUbicacion(idProyecto)
                .orElseThrow(() -> new ProyectoNoEncontradoException(idProyecto));

        proyectoSecurityService.validarDirector(idProyecto, idPerfilActivo);

        LocalDateTime ahora = LocalDateTime.now(reloj).withNano(0);

        Planificacion planificacion = planificacionRepository
                .findByProyectoIdProyecto(idProyecto)
                .orElse(null);

        List<Objetivo> objetivos =
                objetivoRepository.findByProyectoIdProyecto(idProyecto);

        List<RequerimientoGralProyecto> requerimientos =
                requerimientoGralProyectoRepository.findByProyectoIdProyecto(idProyecto);

        Map<Long, CupoRequerimiento> cupos =
                cupoRequerimientoService.calcularGenerales(requerimientos);

        Moodboard moodboard = moodboardRepository
                .findByProyectoIdProyecto(idProyecto)
                .orElse(null);

        List<MiembroProyecto> miembros = miembroProyectoRepository
                .buscarTodosDeProyectoConPerfilProfesionYRol(idProyecto);

        List<org.mgroko.backend.modelo.Actividad> actividades = planificacion == null
                ? List.of()
                : actividadRepository.findConPredecesorasByPlanificacion(
                        planificacion.getIdPlanificacion());

        CronogramaResponse cronograma = cargarCronograma(
                idUsuario,
                idPerfilActivo,
                idProyecto,
                planificacion);

        List<Contrato> contratos =
                contratoRepository.findByProyectoIdProyectoConTipo(idProyecto);

        Map<Long, List<FirmaContrato>> firmasPorContrato = contratos.isEmpty()
                ? Map.of()
                : firmaContratoRepository.findConPartesPorContratos(
                        contratos.stream().map(Contrato::getIdContrato).toList())
                        .stream()
                        .collect(Collectors.groupingBy(FirmaContrato::getIdContrato));

        List<PostulacionGral> postulacionesGral =
                postulacionGralRepository.findPendientesDeProyectoConPerfil(idProyecto);
        List<PostulacionActividad> postulacionesActividad =
                postulacionActividadRepository.findPendientesDeProyectoConPerfil(idProyecto);
        List<InvitacionGral> invitacionesGral =
                invitacionGralRepository.findPendientesDeProyectoConDestinatario(idProyecto);
        List<InvitacionActividad> invitacionesActividad =
                invitacionActividadRepository.findPendientesDeProyectoConDestinatario(idProyecto);

        List<CalificacionPerfil> calificaciones =
                calificacionPerfilRepository.findByProyectoIdProyecto(idProyecto);

        int limiteMoodboard = configuracionSistemaService.obtenerMaxImagenesMoodboard();

        ContextoInformeProyecto contexto = new ContextoInformeProyecto(
                proyecto,
                perfil,
                planificacion,
                objetivos,
                requerimientos,
                cupos,
                moodboard,
                limiteMoodboard,
                miembros,
                actividades,
                cronograma,
                contratos,
                firmasPorContrato,
                postulacionesGral,
                postulacionesActividad,
                invitacionesGral,
                invitacionesActividad,
                calificaciones,
                ahora);

        List<AlertaInforme> alertas = detectarAlertasProyectoService.detectar(contexto);
        var resumen = calcularEstadoAreasService.calcular(contexto, alertas);

        return new InformeProyectoResponse(
                proyecto(contexto),
                ahora,
                ahora,
                new GeneradoPorInforme(
                        perfil.getIdPerfil(),
                        perfil.getNombreArtistico(),
                        RolProyectoUsuario.DIRECTOR.name()),
                contexto.modo(),
                VERSION_INFORME,
                resumen,
                metricas(contexto),
                moodboard(contexto),
                equipo(contexto),
                cronograma(contexto),
                contratos(contexto),
                alertas,
                historial(contexto),
                calificaciones(contexto));
    }

    private CronogramaResponse cargarCronograma(
            Long idUsuario,
            Long idPerfilActivo,
            Long idProyecto,
            Planificacion planificacion) {

        if (planificacion == null) {
            return null;
        }
        try {
            return obtenerCronogramaService.obtener(idUsuario, idPerfilActivo, idProyecto);
        } catch (FechaEntregaNoDefinidaException ex) {
            return null;
        }
    }

    private ProyectoInforme proyecto(ContextoInformeProyecto contexto) {
        Proyecto proyecto = contexto.proyecto();
        return new ProyectoInforme(
                proyecto.getIdProyecto(),
                proyecto.getNombre(),
                proyecto.getDescripcion(),
                proyecto.getEstado() != null ? proyecto.getEstado().name() : null,
                proyecto.getPrivacidad() != null ? proyecto.getPrivacidad().name() : null,
                proyecto.getFechaInicio() != null ? proyecto.getFechaInicio().toLocalDate() : null,
                contexto.fechaEntrega(),
                proyecto.getFechaFinalizacion(),
                proyecto.getFechaCancelacion(),
                Boolean.TRUE.equals(proyecto.getFinalizacionPrematura()),
                UbicacionMapper.toResponse(proyecto.getUbicacion()),
                contexto.objetivos().stream()
                        .map(objetivo -> new ObjetivoResponse(
                                objetivo.getIdObjetivo(),
                                objetivo.getNombre(),
                                objetivo.getDescripcion()))
                        .toList());
    }

    private MetricasInforme metricas(ContextoInformeProyecto contexto) {
        return new MetricasInforme(
                contexto.avanceActividades(),
                contexto.avancePonderado(),
                contexto.margenDias(),
                contexto.margenMinutos(),
                contexto.coberturaCupos(),
                contexto.estabilidadEquipo(),
                contexto.preparacionContractual(),
                contexto.completitudBrief(),
                contexto.actividadesVencidas(),
                contexto.postulacionesPendientes(),
                contexto.invitacionesPendientes(),
                contexto.contratosPendientesFirma(),
                contexto.velocidadAproximada());
    }

    private MoodboardInforme moodboard(ContextoInformeProyecto contexto) {
        Moodboard moodboard = contexto.moodboard();
        if (moodboard == null) {
            return new MoodboardInforme(null, null, null, 0, contexto.limiteMoodboard(), List.of());
        }

        List<ImagenInforme> imagenes = moodboard.getImagenes() == null
                ? List.of()
                : moodboard.getImagenes().stream()
                        .sorted(Comparator.comparing(
                                org.mgroko.backend.modelo.Imagen::getIdImagen))
                        .map(imagen -> new ImagenInforme(imagen.getIdImagen(), imagen.getUrl()))
                        .toList();

        return new MoodboardInforme(
                moodboard.getIdMoodboard(),
                moodboard.getDescripcion(),
                moodboard.getFechaCreacion(),
                imagenes.size(),
                contexto.limiteMoodboard(),
                imagenes);
    }

    private EquipoInforme equipo(ContextoInformeProyecto contexto) {
        List<MiembroInforme> activos = contexto.miembrosActivos().stream()
                .sorted(Comparator.comparing(
                        miembro -> miembro.getPerfil().getNombreArtistico(),
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .map(this::miembro)
                .toList();

        List<MiembroInforme> historicos = contexto.miembrosHistoricos().stream()
                .sorted(Comparator.comparing(
                        MiembroProyecto::getFechaBaja,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::miembro)
                .toList();

        List<MiembroInforme> todos = new ArrayList<>(activos);
        todos.addAll(historicos);

        List<RequerimientoCoberturaInforme> requerimientos =
                contexto.requerimientosActivos().stream()
                        .map(requerimiento -> requerimiento(contexto, requerimiento))
                        .toList();

        return new EquipoInforme(
                contexto.totalActivos(),
                contexto.totalDirectores(),
                contexto.totalBajas(),
                contexto.estabilidadEquipo(),
                todos,
                historicos,
                requerimientos);
    }

    private MiembroInforme miembro(MiembroProyecto miembro) {
        Perfil perfil = miembro.getPerfil();
        return new MiembroInforme(
                miembro.getIdMiembro(),
                perfil.getIdPerfil(),
                perfil.getNombreArtistico(),
                perfil.getProfesion() != null ? perfil.getProfesion().getNombre() : null,
                miembro.getRolProyecto() != null
                        ? RolProyectoUsuario.desde(miembro.getRolProyecto()).name()
                        : null,
                miembro.getEstadoParticipacion() != null
                        ? miembro.getEstadoParticipacion().name()
                        : null,
                miembro.getFechaAlta(),
                miembro.getFechaBaja(),
                miembro.getMotivoBaja(),
                miembro.getIdTycAceptado() != null);
    }

    private RequerimientoCoberturaInforme requerimiento(
            ContextoInformeProyecto contexto,
            RequerimientoGralProyecto requerimiento) {

        CupoRequerimiento cupo = contexto.cupos()
                .getOrDefault(
                        requerimiento.getIdRequerimientoGral(),
                        new CupoRequerimiento(
                                requerimiento.getCantidad() == null ? 0 : requerimiento.getCantidad(),
                                0,
                                requerimiento.getCantidad() == null ? 0 : requerimiento.getCantidad(),
                                false));

        double porcentaje = cupo.cantidad() > 0
                ? (double) cupo.cubiertos() / cupo.cantidad()
                : 1.0;

        return new RequerimientoCoberturaInforme(
                requerimiento.getIdRequerimientoGral(),
                requerimiento.getProfesion() != null
                        ? requerimiento.getProfesion().getNombre()
                        : null,
                requerimiento.getDescripcion(),
                cupo.cantidad(),
                cupo.cubiertos(),
                cupo.faltantes(),
                porcentaje);
    }

    private CronogramaInforme cronograma(ContextoInformeProyecto contexto) {
        List<ActividadProximaInforme> proximas = contexto.proximasActividades(
                        MAX_PROXIMAS_ACTIVIDADES)
                .stream()
                .map(this::actividadProxima)
                .toList();

        return new CronogramaInforme(
                contexto.cronograma() != null && contexto.cronograma().resumen() != null
                        ? contexto.cronograma().resumen().fechaInicio()
                        : null,
                contexto.fechaFinProyectada(),
                contexto.fechaEntrega(),
                contexto.margenMinutos(),
                contexto.margenDias(),
                contexto.cronogramaViable(),
                contexto.estadoCronograma(),
                contexto.totalActividades(),
                contexto.actividadesCriticas(),
                contexto.actividadesVencidas(),
                contexto.conflictos(),
                contexto.actividadesPorEstado(),
                proximas);
    }

    private ActividadProximaInforme actividadProxima(ActividadCronogramaResponse actividad) {
        LocalDateTime inicio = actividad.inicioPersistido() != null
                ? actividad.inicioPersistido()
                : actividad.inicioProyectado();
        return new ActividadProximaInforme(
                actividad.idActividad(),
                actividad.nombre(),
                inicio,
                actividad.estado(),
                Boolean.TRUE.equals(actividad.enRutaCritica()));
    }

    private ContratosInforme contratos(ContextoInformeProyecto contexto) {
        Map<String, Integer> porEstado = new LinkedHashMap<>();
        for (EstadoContrato estado : EstadoContrato.values()) {
            porEstado.put(estado.name(), 0);
        }
        for (Contrato contrato : contexto.contratos()) {
            if (contrato.getEstado() != null) {
                porEstado.merge(contrato.getEstado().name(), 1, Integer::sum);
            }
        }

        List<ContratoInforme> contratos = contexto.contratos().stream()
                .map(contrato -> contrato(contrato,
                        contexto.firmasPorContrato()
                                .getOrDefault(contrato.getIdContrato(), List.of())))
                .toList();

        return new ContratosInforme(
                contexto.contratos().size(),
                porEstado,
                new FirmasInforme(
                        contexto.firmasFirmadas(),
                        contexto.firmasPendientes(),
                        contexto.firmasTotales()),
                contratos);
    }

    private ContratoInforme contrato(Contrato contrato, List<FirmaContrato> firmas) {
        int firmadas = (int) firmas.stream()
                .filter(firma -> firma.getEstadoFirma() == EstadoFirmaContrato.FIRMADO)
                .count();
        int totalPartes = (int) firmas.stream()
                .filter(firma -> firma.getEstadoFirma() == EstadoFirmaContrato.PENDIENTE
                        || firma.getEstadoFirma() == EstadoFirmaContrato.FIRMADO)
                .count();
        return new ContratoInforme(
                contrato.getIdContrato(),
                contrato.getNombre(),
                contrato.getVersionActual(),
                contrato.getEstado() != null ? contrato.getEstado().name() : null,
                firmadas,
                totalPartes);
    }

    private List<HistorialInforme> historial(ContextoInformeProyecto contexto) {
        return contexto.miembrosHistoricos().stream()
                .sorted(Comparator.comparing(
                        MiembroProyecto::getFechaBaja,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(MAX_HISTORIAL_PDF)
                .map(miembro -> new HistorialInforme(
                        miembro.getFechaBaja() != null
                                ? miembro.getFechaBaja()
                                : miembro.getFechaAlta(),
                        miembro.getEstadoParticipacion() != null
                                ? switch (miembro.getEstadoParticipacion()) {
                                    case BAJA_VOLUNTARIA -> "Baja voluntaria";
                                    case ELIMINADO -> "Miembro eliminado";
                                    default -> "Cambio de participación";
                                }
                                : "Cambio de participación",
                        miembro.getPerfil() != null
                                ? miembro.getPerfil().getNombreArtistico()
                                : null,
                        miembro.getMotivoBaja()))
                .toList();
    }

    private CalificacionesInforme calificaciones(ContextoInformeProyecto contexto) {
        if (contexto.proyecto().getEstado() != EstadoProyecto.FINALIZADO) {
            return new CalificacionesInforme(false, null, 0L, 0L);
        }
        return new CalificacionesInforme(
                true,
                contexto.calificacionesPromedio(),
                contexto.calificacionesCantidad(),
                contexto.calificacionesEsperadas());
    }
}
