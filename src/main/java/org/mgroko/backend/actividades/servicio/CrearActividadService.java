package org.mgroko.backend.actividades.servicio;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.mgroko.backend.actividades.dto.ActividadResponse;
import org.mgroko.backend.actividades.dto.CrearActividadRequest;
import org.mgroko.backend.actividades.dto.CrearRequerimientoActividadRequest;
import org.mgroko.backend.actividades.dto.RequerimientoActividadResponse;
import org.mgroko.backend.actividades.exception.DuracionInvalidaException;
import org.mgroko.backend.actividades.exception.EstadoProyectoNoPermiteActividadesException;
import org.mgroko.backend.actividades.exception.NombreActividadDuplicadoException;
import org.mgroko.backend.actividades.exception.PredecesoraInvalidaException;
import org.mgroko.backend.actividades.mapper.ActividadMapper;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Planificacion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.Ubicacion;
import org.mgroko.backend.modelo.enums.EstadoCronograma;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.perfiles.exception.PerfilNoEncontradoException;
import org.mgroko.backend.proyectos.exception.ProyectoNoEncontradoException;
import org.mgroko.backend.proyectos.servicio.ProyectoSecurityService;
import org.mgroko.backend.repositorio.ActividadRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.PlanificacionRepository;
import org.mgroko.backend.ubicacion.servicio.UbicacionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CrearActividadService {

    private static final String PERMISO_CREAR_ACTIVIDAD = "CREAR_ACTIVIDAD";

    private final ActividadRepository actividadRepository;
    private final PlanificacionRepository planificacionRepository;
    private final PerfilRepository perfilRepository;
    private final ProyectoSecurityService proyectoSecurityService;
    private final UbicacionService ubicacionService;
    private final RequerimientoActividadService requerimientoActividadService;
    private final CronogramaService cronogramaService;

    public CrearActividadService(
            ActividadRepository actividadRepository,
            PlanificacionRepository planificacionRepository,
            PerfilRepository perfilRepository,
            ProyectoSecurityService proyectoSecurityService,
            UbicacionService ubicacionService,
            RequerimientoActividadService requerimientoActividadService,
            CronogramaService cronogramaService) {
        this.actividadRepository = actividadRepository;
        this.planificacionRepository = planificacionRepository;
        this.perfilRepository = perfilRepository;
        this.proyectoSecurityService = proyectoSecurityService;
        this.ubicacionService = ubicacionService;
        this.requerimientoActividadService = requerimientoActividadService;
        this.cronogramaService = cronogramaService;
    }

    @Transactional
    public ActividadResponse crear(
            Long idUsuario,
            Long idPerfilActivo,
            Long idProyecto,
            CrearActividadRequest request) {

        if (idPerfilActivo == null) {
            throw new PerfilActivoNoSeleccionadoException();
        }

        Perfil perfil = perfilRepository.findByIdPerfilAndUsuarioIdUsuario(idPerfilActivo, idUsuario)
                .orElseThrow(PerfilNoEncontradoException::new);

        if (perfil.getEstado() != EstadoPerfil.Activo) {
            throw new PerfilEnBajaException("El perfil no se encuentra activo.");
        }

        Planificacion planificacion = planificacionRepository
                .findByProyectoIdProyectoForUpdate(idProyecto)
                .orElseThrow(() -> new ProyectoNoEncontradoException(idProyecto));

        Proyecto proyecto = planificacion.getProyecto();
        validarEstadoProyecto(proyecto);
        proyectoSecurityService.validarPermiso(idProyecto, idPerfilActivo, PERMISO_CREAR_ACTIVIDAD);

        String nombre = request.nombre().trim();
        if (actividadRepository.existeNombreEnPlanificacion(
                planificacion.getIdPlanificacion(), nombre)) {
            throw new NombreActividadDuplicadoException(nombre);
        }

        int duracionMinutos = calcularDuracionMinutos(request);
        Ubicacion ubicacion = resolverUbicacion(request);
        Set<Actividad> predecesoras = resolverPredecesoras(
                request.predecesoras(),
                planificacion.getIdPlanificacion());

        Actividad actividad = Actividad.builder()
                .nombre(nombre)
                .descripcion(request.descripcion() != null ? request.descripcion().trim() : null)
                .duracionMinutos(duracionMinutos)
                .fechaHoraInicio(null)
                .horarioPreferenciaInicio(request.horarioPreferenciaInicio())
                .planificacion(planificacion)
                .ubicacion(ubicacion)
                .predecesoras(predecesoras)
                .build();

        Actividad guardada = actividadRepository.saveAndFlush(actividad);

        List<RequerimientoActividadResponse> requerimientos = persistirRequerimientos(
                guardada, request.requerimientos());

        var calculo = planificacion.getFechaEntrega() == null
                ? null
                : cronogramaService.calcularProyeccion(
                        planificacion,
                        actividadRepository.findConPredecesorasByPlanificacion(
                                planificacion.getIdPlanificacion()));
        EstadoCronograma estadoCronograma = calculo == null
                ? EstadoCronograma.SIN_FECHA_ENTREGA
                : cronogramaService.estadoDe(calculo);
        return ActividadMapper.toResponse(
                guardada,
                requerimientos,
                List.of(),
                estadoCronograma,
                calculo,
                calculo == null ? null : cronogramaService.inicioEfectivo(planificacion),
                calculo == null || cronogramaService.esTentativa(planificacion));
    }

    private void validarEstadoProyecto(Proyecto proyecto) {
        EstadoProyecto estado = proyecto.getEstado();
        if (estado != EstadoProyecto.BORRADOR
                && estado != EstadoProyecto.PUBLICADO) {
            throw new EstadoProyectoNoPermiteActividadesException(estado.name());
        }
    }

    private int calcularDuracionMinutos(CrearActividadRequest request) {
        if (request.duracion() == null || request.unidadDuracion() == null) {
            throw new DuracionInvalidaException("La duracion y su unidad son obligatorias.");
        }

        long minutos;
        try {
            minutos = request.unidadDuracion().aMinutos(request.duracion());
        } catch (ArithmeticException ex) {
            throw new DuracionInvalidaException("La duracion indicada es demasiado grande.");
        }

        if (minutos <= 0 || minutos > Integer.MAX_VALUE) {
            throw new DuracionInvalidaException(
                    "La duracion debe ser mayor que cero y expresarse en un rango valido.");
        }

        return (int) minutos;
    }

    private Ubicacion resolverUbicacion(CrearActividadRequest request) {
        if (request.ubicacion() == null || request.ubicacion().localidadId() == null
                || request.ubicacion().localidadId().isBlank()) {
            return null;
        }
        return ubicacionService.obtenerOCrear(
                request.ubicacion().localidadId(),
                request.ubicacion().provinciaId());
    }

    private Set<Actividad> resolverPredecesoras(
            List<Long> idsPredecesoras,
            Long idPlanificacion) {

        Set<Actividad> resultado = new LinkedHashSet<>();
        if (idsPredecesoras == null || idsPredecesoras.isEmpty()) {
            return resultado;
        }

        Set<Long> idsUnicos = new HashSet<>();
        for (Long idPredecesora : idsPredecesoras) {
            if (idPredecesora == null || !idsUnicos.add(idPredecesora)) {
                throw new PredecesoraInvalidaException(
                        "La lista de predecesoras contiene ids nulos o duplicados.");
            }
        }

        List<Actividad> encontradas = actividadRepository.findByIdsAndPlanificacion(
                idsUnicos, idPlanificacion);

        if (encontradas.size() != idsUnicos.size()) {
            throw new PredecesoraInvalidaException(
                    "Todas las predecesoras deben existir y pertenecer a la misma planificacion.");
        }

        resultado.addAll(encontradas);
        return resultado;
    }

    private List<RequerimientoActividadResponse> persistirRequerimientos(
            Actividad actividad,
            List<CrearRequerimientoActividadRequest> requerimientos) {

        List<RequerimientoActividadResponse> resultado = new ArrayList<>();
        if (requerimientos == null || requerimientos.isEmpty()) {
            return resultado;
        }

        for (CrearRequerimientoActividadRequest requerimiento : requerimientos) {
            resultado.add(requerimientoActividadService.persistir(actividad, requerimiento));
        }
        return resultado;
    }
}
