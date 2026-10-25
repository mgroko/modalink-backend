package org.mgroko.backend.proyectos.servicio;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.mgroko.backend.common.util.LikePatrones;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Moodboard;
import org.mgroko.backend.modelo.Objetivo;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.PermisoProyecto;
import org.mgroko.backend.modelo.Planificacion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RolProyecto;
import org.mgroko.backend.modelo.Ubicacion;
import org.mgroko.backend.modelo.enums.EstadoParticipacion;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.RolProyectoUsuario;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.perfiles.exception.PerfilNoEncontradoException;
import org.mgroko.backend.proyectos.dto.CrearObjetivoRequest;
import org.mgroko.backend.proyectos.dto.CrearProyectoRequest;
import org.mgroko.backend.proyectos.dto.CrearRequerimientoGralRequest;
import org.mgroko.backend.proyectos.dto.ContextoConsultaProyecto;
import org.mgroko.backend.proyectos.dto.MoodboardResponse;
import org.mgroko.backend.proyectos.dto.ProyectoResponse;
import org.mgroko.backend.proyectos.dto.RequerimientoGralResponse;
import org.mgroko.backend.proyectos.exception.FechaEntregaInvalidaException;
import org.mgroko.backend.proyectos.exception.NombreProyectoDuplicadoException;
import org.mgroko.backend.proyectos.exception.RolProyectoNoEncontradoException;
import org.mgroko.backend.proyectos.mapper.ProyectoMapper;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
import org.mgroko.backend.repositorio.MoodboardRepository;
import org.mgroko.backend.repositorio.ObjetivoRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.PlanificacionRepository;
import org.mgroko.backend.repositorio.ProyectoRepository;
import org.mgroko.backend.repositorio.RolProyectoRepository;
import org.mgroko.backend.ubicacion.servicio.UbicacionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CrearProyectoService {

    public static final String ROL_DIRECTOR = "Director";

    private final ProyectoRepository proyectoRepository;
    private final PerfilRepository perfilRepository;
    private final RolProyectoRepository rolProyectoRepository;
    private final MiembroProyectoRepository miembroProyectoRepository;
    private final PlanificacionRepository planificacionRepository;
    private final ObjetivoRepository objetivoRepository;
    private final UbicacionService ubicacionService;
    private final RequerimientoGralService requerimientoGralService;
    private final MoodboardRepository moodboardRepository;

    public CrearProyectoService(
            ProyectoRepository proyectoRepository,
            PerfilRepository perfilRepository,
            RolProyectoRepository rolProyectoRepository,
            MiembroProyectoRepository miembroProyectoRepository,
            PlanificacionRepository planificacionRepository,
            ObjetivoRepository objetivoRepository,
            UbicacionService ubicacionService,
            RequerimientoGralService requerimientoGralService,
            MoodboardRepository moodboardRepository) {
        this.proyectoRepository = proyectoRepository;
        this.perfilRepository = perfilRepository;
        this.rolProyectoRepository = rolProyectoRepository;
        this.miembroProyectoRepository = miembroProyectoRepository;
        this.planificacionRepository = planificacionRepository;
        this.objetivoRepository = objetivoRepository;
        this.ubicacionService = ubicacionService;
        this.requerimientoGralService = requerimientoGralService;
        this.moodboardRepository = moodboardRepository;
    }

    @Transactional
    public ProyectoResponse crear(Long idUsuario, Long idPerfilActivo, CrearProyectoRequest request) {
        if (idPerfilActivo == null) {
            throw new PerfilActivoNoSeleccionadoException();
        }

        // 1. Validar que el perfil exista y pertenezca al usuario autenticado
        Perfil perfil = perfilRepository.findByIdPerfilAndUsuarioIdUsuario(idPerfilActivo, idUsuario)
                .orElseThrow(PerfilNoEncontradoException::new);

        if (perfil.getEstado() != EstadoPerfil.Activo) {
            throw new PerfilEnBajaException("El perfil no se encuentra activo.");
        }

        // 2. Validar que la fecha de entrega estipulada no sea anterior a la fecha actual
        if (request.fechaEntregaEstipulada() != null && request.fechaEntregaEstipulada().isBefore(LocalDate.now())) {
            throw new FechaEntregaInvalidaException(
                    "La fecha de entrega estipulada no puede ser anterior a la fecha actual.");
        }

        // 3. Buscar rol Director (se resuelve antes de validar unicidad para
        // comparar por el rol exacto que se va a asignar, no por su nombre)
        RolProyecto rolDirector = rolProyectoRepository
                .findByNombre(LikePatrones.escapar(ROL_DIRECTOR), ROL_DIRECTOR)
                .orElseThrow(() -> new RolProyectoNoEncontradoException(ROL_DIRECTOR));

        // 4. Validar unicidad del nombre del proyecto para el perfil director
        String nombreProyecto = request.nombre().trim();
        boolean nombreExiste = proyectoRepository.existeProyectoConNombreParaPerfil(
                nombreProyecto,
                idPerfilActivo,
                rolDirector.getIdRolProyecto(),
                EstadoParticipacion.Activo
        );
        if (nombreExiste) {
            throw new NombreProyectoDuplicadoException(nombreProyecto);
        }

        // 5. Resolver ubicaciÃ³n opcional
        Ubicacion ubicacion = null;
        if (request.ubicacion() != null && request.ubicacion().localidadId() != null
                && !request.ubicacion().localidadId().isBlank()) {
            ubicacion = ubicacionService.obtenerOCrear(
                    request.ubicacion().localidadId(),
                    request.ubicacion().provinciaId()
            );
        }

        // 6. Crear entidad Proyecto en estado Borrador
        Proyecto proyecto = Proyecto.builder()
                .nombre(nombreProyecto)
                .descripcion(request.descripcion().trim())
                .estado(EstadoProyecto.Borrador)
                .privacidad(request.privacidad())
                .aceptaPostulacionGral(Boolean.TRUE.equals(request.aceptaPostulacionGral()))
                .ubicacion(ubicacion)
                .build();

        Proyecto proyectoGuardado = proyectoRepository.save(proyecto);

        // 7. Asociar al perfil activo como Director (MiembroProyecto)
        MiembroProyecto miembroDirector = MiembroProyecto.builder()
                .proyecto(proyectoGuardado)
                .perfil(perfil)
                .rolProyecto(rolDirector)
                .estadoParticipacion(EstadoParticipacion.Activo)
                .build();

        miembroProyectoRepository.save(miembroDirector);

        // 8. Inicializar la PlanificaciÃ³n 1:1
        Planificacion planificacion = Planificacion.builder()
                .proyecto(proyectoGuardado)
                .fechaEntrega(request.fechaEntregaEstipulada())
                .build();

        planificacionRepository.save(planificacion);

        // 9. Guardar objetivos si se recibieron
        List<Objetivo> objetivosGuardados = new ArrayList<>();
        if (request.objetivos() != null && !request.objetivos().isEmpty()) {
            for (CrearObjetivoRequest objReq : request.objetivos()) {
                Objetivo obj = Objetivo.builder()
                        .proyecto(proyectoGuardado)
                        .nombre(objReq.nombre().trim())
                        .descripcion(objReq.descripcion() != null ? objReq.descripcion().trim() : null)
                        .build();
                objetivosGuardados.add(objetivoRepository.save(obj));
            }
        }

        // 10. Guardar requerimientos de personal si se recibieron
        List<RequerimientoGralResponse> requerimientosResponse = new ArrayList<>();
        if (request.requerimientosGral() != null && !request.requerimientosGral().isEmpty()) {
            for (CrearRequerimientoGralRequest reqGral : request.requerimientosGral()) {
                requerimientosResponse.add(requerimientoGralService.persistir(
                        proyectoGuardado,
                        reqGral.idProfesion(),
                        reqGral.cantidad(),
                        reqGral.descripcion(),
                        reqGral.caracteristicas(),
                        reqGral.habilidades()));
            }
        }

        // 11. Guardar moodboard si se recibiÃ³
        MoodboardResponse moodboardResponse = null;
        if (request.moodboard() != null) {
            Moodboard moodboard = Moodboard.builder()
                    .proyecto(proyectoGuardado)
                    .descripcion(request.moodboard().descripcion() != null
                            ? request.moodboard().descripcion().trim()
                            : null)
                    .build();
            Moodboard guardado = moodboardRepository.save(moodboard);
            moodboardResponse = ProyectoMapper.toMoodboardResponse(guardado);
        }

        // El creador queda como Director: contexto de rol/permisos para la
        // respuesta (mismo contrato que GET /proyectos/{idProyecto})
        List<String> permisosDirector = rolDirector.getPermisos() != null
                ? rolDirector.getPermisos().stream()
                        .map(PermisoProyecto::getNombre)
                        .filter(Objects::nonNull)
                        .sorted()
                        .toList()
                : List.of();

        return ProyectoMapper.toResponse(
                proyectoGuardado,
                perfil,
                request.fechaEntregaEstipulada(),
                objetivosGuardados,
                requerimientosResponse,
                moodboardResponse,
                new ContextoConsultaProyecto(
                        RolProyectoUsuario.desde(rolDirector),
                        Boolean.TRUE,
                        permisosDirector,
                        Boolean.FALSE)
        );
    }
}
