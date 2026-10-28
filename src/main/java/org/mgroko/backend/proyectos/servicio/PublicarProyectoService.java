package org.mgroko.backend.proyectos.servicio;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.mgroko.backend.modelo.CaracteristicaTecnica;
import org.mgroko.backend.modelo.Habilidad;
import org.mgroko.backend.modelo.MiembroProyecto;
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
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.perfiles.exception.PerfilNoEncontradoException;
import org.mgroko.backend.proyectos.dto.CaracteristicaRequerimientoResponse;
import org.mgroko.backend.proyectos.dto.ContextoConsultaProyecto;
import org.mgroko.backend.proyectos.dto.CupoRequerimiento;
import org.mgroko.backend.proyectos.dto.MoodboardResponse;
import org.mgroko.backend.proyectos.dto.ProyectoResponse;
import org.mgroko.backend.proyectos.dto.RequerimientoGralResponse;
import org.mgroko.backend.proyectos.exception.EstadoProyectoInvalidoException;
import org.mgroko.backend.proyectos.exception.ProyectoNoEncontradoException;
import org.mgroko.backend.proyectos.mapper.ProyectoMapper;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
import org.mgroko.backend.repositorio.MoodboardRepository;
import org.mgroko.backend.repositorio.ObjetivoRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.PlanificacionRepository;
import org.mgroko.backend.repositorio.ProyectoRepository;
import org.mgroko.backend.repositorio.RequerimientoActividadRepository;
import org.mgroko.backend.repositorio.RequerimientoGralCaractValorRepository;
import org.mgroko.backend.repositorio.RequerimientoGralProyectoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC-25: Publicar proyecto. Cambia el estado de "Borrador" a "Publicado",
 * haciendo el proyecto visible según su privacidad y habilitando las
 * postulaciones (la elegibilidad real depende además de aceptaPostulacionGral
 * y de la profesión del postulante, ya resueltas en ObtenerProyectoService).
 *
 * Información básica obligatoria para publicar (definida con producto):
 * ubicación, fecha de entrega (planificación), moodboard y al menos un
 * requerimiento general o de actividad no cancelado.
 */
@Service
public class PublicarProyectoService {

    public static final String PERMISO_PUBLICAR_PROYECTO = "PUBLICAR_PROYECTO";

    private final PerfilRepository perfilRepository;
    private final ProyectoRepository proyectoRepository;
    private final MiembroProyectoRepository miembroProyectoRepository;
    private final PlanificacionRepository planificacionRepository;
    private final MoodboardRepository moodboardRepository;
    private final RequerimientoGralProyectoRepository requerimientoGralProyectoRepository;
    private final RequerimientoActividadRepository requerimientoActividadRepository;
    private final RequerimientoGralCaractValorRepository requerimientoGralCaractValorRepository;
    private final ObjetivoRepository objetivoRepository;
    private final CupoRequerimientoService cupoRequerimientoService;
    private final ProyectoSecurityService proyectoSecurityService;

    public PublicarProyectoService(
            PerfilRepository perfilRepository,
            ProyectoRepository proyectoRepository,
            MiembroProyectoRepository miembroProyectoRepository,
            PlanificacionRepository planificacionRepository,
            MoodboardRepository moodboardRepository,
            RequerimientoGralProyectoRepository requerimientoGralProyectoRepository,
            RequerimientoActividadRepository requerimientoActividadRepository,
            RequerimientoGralCaractValorRepository requerimientoGralCaractValorRepository,
            ObjetivoRepository objetivoRepository,
            CupoRequerimientoService cupoRequerimientoService,
            ProyectoSecurityService proyectoSecurityService) {
        this.perfilRepository = perfilRepository;
        this.proyectoRepository = proyectoRepository;
        this.miembroProyectoRepository = miembroProyectoRepository;
        this.planificacionRepository = planificacionRepository;
        this.moodboardRepository = moodboardRepository;
        this.requerimientoGralProyectoRepository = requerimientoGralProyectoRepository;
        this.requerimientoActividadRepository = requerimientoActividadRepository;
        this.requerimientoGralCaractValorRepository = requerimientoGralCaractValorRepository;
        this.objetivoRepository = objetivoRepository;
        this.cupoRequerimientoService = cupoRequerimientoService;
        this.proyectoSecurityService = proyectoSecurityService;
    }

    @Transactional
    public ProyectoResponse publicar(Long idUsuario, Long idPerfilActivo, Long idProyecto) {
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

        proyectoSecurityService.validarPermiso(idProyecto, idPerfilActivo, PERMISO_PUBLICAR_PROYECTO);

        if (proyecto.getEstado() != EstadoProyecto.BORRADOR) {
            throw new EstadoProyectoInvalidoException(proyecto.getEstado().getNombre());
        }

        validarInformacionBasicaCompleta(idProyecto, proyecto);

        proyecto.setEstado(EstadoProyecto.PUBLICADO);
        Proyecto publicado = proyectoRepository.save(proyecto);

        return armarResponse(publicado, perfil);
    }

    private void validarInformacionBasicaCompleta(Long idProyecto, Proyecto proyecto) {
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

        List<RequerimientoGralResponse> requerimientos = new ArrayList<>();
        for (RequerimientoGralProyecto rg : requerimientoGralProyectoRepository.findByProyectoIdProyecto(proyecto.getIdProyecto())) {
            requerimientos.add(armarRequerimientoResponse(rg));
        }

        MoodboardResponse moodboardResponse = moodboardRepository.findByProyectoIdProyecto(proyecto.getIdProyecto())
                .map(ProyectoMapper::toMoodboardResponse)
                .orElse(null);

        ContextoConsultaProyecto contexto = armarContexto(proyecto.getIdProyecto(), perfilConsultante);

        return ProyectoMapper.toResponse(proyecto, director, fechaEntrega, objetivos,
                requerimientos, moodboardResponse, contexto);
    }

    /**
     * Contexto del perfil consultante tras publicar: siempre miembro activo con
     * el permiso PUBLICAR_PROYECTO (ya validado), por lo que se reutiliza la
     * membresía activa para exponer rol y permisos completos.
     */
    private ContextoConsultaProyecto armarContexto(Long idProyecto, Perfil perfilConsultante) {
        Optional<MiembroProyecto> miembroActivo = miembroProyectoRepository
                .findMiembroActivoConPermisos(idProyecto, perfilConsultante.getIdPerfil());

        if (miembroActivo.isEmpty()) {
            return new ContextoConsultaProyecto(null, Boolean.FALSE, List.of(), Boolean.FALSE);
        }

        RolProyecto rol = miembroActivo.get().getRolProyecto();
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
                Boolean.FALSE);
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

    private RequerimientoGralResponse armarRequerimientoResponse(RequerimientoGralProyecto rg) {
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
                Boolean.FALSE);
    }
}
