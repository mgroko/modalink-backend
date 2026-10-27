package org.mgroko.backend.proyectos.servicio;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.mgroko.backend.modelo.CaracteristicaTecnica;
import org.mgroko.backend.modelo.Habilidad;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Moodboard;
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
import org.mgroko.backend.proyectos.dto.CrearObjetivoRequest;
import org.mgroko.backend.proyectos.dto.CupoRequerimiento;
import org.mgroko.backend.proyectos.dto.ModificarProyectoRequest;
import org.mgroko.backend.proyectos.dto.ModificarRequerimientoGralRequest;
import org.mgroko.backend.proyectos.dto.MoodboardResponse;
import org.mgroko.backend.proyectos.dto.ProyectoResponse;
import org.mgroko.backend.proyectos.dto.RequerimientoGralResponse;
import org.mgroko.backend.proyectos.exception.EstadoProyectoNoModificableException;
import org.mgroko.backend.proyectos.exception.FechaEntregaInvalidaException;
import org.mgroko.backend.proyectos.exception.ProyectoNoEncontradoException;
import org.mgroko.backend.proyectos.exception.RequerimientoInvalidoException;
import org.mgroko.backend.proyectos.exception.RequerimientoSatisfechoException;
import org.mgroko.backend.proyectos.exception.UbicacionObligatoriaException;
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
import org.mgroko.backend.ubicacion.servicio.UbicacionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final CupoRequerimientoService cupoRequerimientoService;
    private final UbicacionService ubicacionService;
    private final RequerimientoGralService requerimientoGralService;
    private final ProyectoSecurityService proyectoSecurityService;

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
            CupoRequerimientoService cupoRequerimientoService,
            UbicacionService ubicacionService,
            RequerimientoGralService requerimientoGralService,
            ProyectoSecurityService proyectoSecurityService) {
        this.perfilRepository = perfilRepository;
        this.proyectoRepository = proyectoRepository;
        this.miembroProyectoRepository = miembroProyectoRepository;
        this.planificacionRepository = planificacionRepository;
        this.objetivoRepository = objetivoRepository;
        this.moodboardRepository = moodboardRepository;
        this.requerimientoGralProyectoRepository = requerimientoGralProyectoRepository;
        this.requerimientoActividadRepository = requerimientoActividadRepository;
        this.requerimientoGralCaractValorRepository = requerimientoGralCaractValorRepository;
        this.cupoRequerimientoService = cupoRequerimientoService;
        this.ubicacionService = ubicacionService;
        this.requerimientoGralService = requerimientoGralService;
        this.proyectoSecurityService = proyectoSecurityService;
    }

    @Transactional
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

        actualizarDatosBasicos(proyecto, request);
        actualizarUbicacion(proyecto, request);
        actualizarFechaEntrega(idProyecto, request);
        actualizarObjetivos(idProyecto, proyecto, request.objetivos());
        actualizarRequerimientos(idProyecto, proyecto, request.requerimientosGral());
        actualizarMoodboard(idProyecto, proyecto, request);

        Proyecto guardado = proyectoRepository.save(proyecto);

        validarInformacionBasicaSiCorresponde(idProyecto, guardado);

        return armarResponse(guardado, perfil);
    }

    private void validarEstadoModificable(Proyecto proyecto) {
        EstadoProyecto estado = proyecto.getEstado();
        if (estado != EstadoProyecto.BORRADOR
                && estado != EstadoProyecto.PUBLICADO
                && estado != EstadoProyecto.CONFIRMADO) {
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

    private void actualizarRequerimientos(
            Long idProyecto, Proyecto proyecto, List<ModificarRequerimientoGralRequest> requerimientos) {

        if (requerimientos == null) {
            return;
        }

        List<RequerimientoGralProyecto> existentes =
                requerimientoGralProyectoRepository.findByProyectoIdProyecto(idProyecto);
        Map<Long, RequerimientoGralProyecto> porId = new HashMap<>();
        for (RequerimientoGralProyecto existente : existentes) {
            porId.put(existente.getIdRequerimientoGral(), existente);
        }

        for (ModificarRequerimientoGralRequest requerimientoRequest : requerimientos) {
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
            if (esSatisfecho(existente)) {
                throw new RequerimientoSatisfechoException(idRequerimiento);
            }
        }

        for (RequerimientoGralProyecto existente : existentes) {
            if (!esSatisfecho(existente)) {
                requerimientoGralService.eliminar(existente);
            }
        }

        for (ModificarRequerimientoGralRequest requerimientoRequest : requerimientos) {
            requerimientoGralService.persistir(
                    proyecto,
                    requerimientoRequest.idProfesion(),
                    requerimientoRequest.cantidad(),
                    requerimientoRequest.descripcion(),
                    requerimientoRequest.caracteristicas(),
                    requerimientoRequest.habilidades());
        }
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
            moodboardRepository.save(Moodboard.builder()
                    .proyecto(proyecto)
                    .descripcion(descripcion)
                    .build());
            return;
        }

        moodboard.setDescripcion(descripcion);
        moodboardRepository.save(moodboard);
    }

    private void validarInformacionBasicaSiCorresponde(Long idProyecto, Proyecto proyecto) {
        if (proyecto.getEstado() == EstadoProyecto.BORRADOR) {
            return;
        }

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

    private boolean esSatisfecho(RequerimientoGralProyecto requerimiento) {
        if (requerimiento == null || requerimiento.getIdRequerimientoGral() == null) {
            return false;
        }
        return cupoRequerimientoService.calcularGeneral(requerimiento).satisfecho();
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
                cupo.faltantes());
    }
}
