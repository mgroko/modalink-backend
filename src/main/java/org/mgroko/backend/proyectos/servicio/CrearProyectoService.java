package org.mgroko.backend.proyectos.servicio;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Objetivo;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Planificacion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RolProyecto;
import org.mgroko.backend.modelo.Ubicacion;
import org.mgroko.backend.modelo.enums.EstadoParticipacion;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.perfiles.exception.PerfilNoEncontradoException;
import org.mgroko.backend.proyectos.dto.CrearObjetivoRequest;
import org.mgroko.backend.proyectos.dto.CrearProyectoRequest;
import org.mgroko.backend.proyectos.dto.ProyectoResponse;
import org.mgroko.backend.proyectos.exception.NombreProyectoDuplicadoException;
import org.mgroko.backend.proyectos.exception.RangoFechasProyectoInvalidoException;
import org.mgroko.backend.proyectos.exception.RolProyectoNoEncontradoException;
import org.mgroko.backend.proyectos.mapper.ProyectoMapper;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
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

    public CrearProyectoService(
            ProyectoRepository proyectoRepository,
            PerfilRepository perfilRepository,
            RolProyectoRepository rolProyectoRepository,
            MiembroProyectoRepository miembroProyectoRepository,
            PlanificacionRepository planificacionRepository,
            ObjetivoRepository objetivoRepository,
            UbicacionService ubicacionService) {
        this.proyectoRepository = proyectoRepository;
        this.perfilRepository = perfilRepository;
        this.rolProyectoRepository = rolProyectoRepository;
        this.miembroProyectoRepository = miembroProyectoRepository;
        this.planificacionRepository = planificacionRepository;
        this.objetivoRepository = objetivoRepository;
        this.ubicacionService = ubicacionService;
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

        // 2. Validar que la fecha de entrega / fin no sea anterior a la fecha de inicio
        if (request.fechaFinEstipulada() != null && request.fechaFinEstipulada().isBefore(request.fechaInicio())) {
            throw new RangoFechasProyectoInvalidoException(
                    "La fecha de finalización o entrega no puede ser anterior a la fecha de inicio.");
        }

        // 3. Validar unicidad del nombre del proyecto para el perfil director
        boolean nombreExiste = proyectoRepository.existeProyectoConNombreParaPerfil(
                request.nombre(),
                idPerfilActivo,
                ROL_DIRECTOR,
                EstadoParticipacion.Activo
        );
        if (nombreExiste) {
            throw new NombreProyectoDuplicadoException(request.nombre());
        }

        // 4. Buscar rol Director
        RolProyecto rolDirector = rolProyectoRepository.findByNombre(ROL_DIRECTOR)
                .orElseThrow(() -> new RolProyectoNoEncontradoException(ROL_DIRECTOR));

        // 5. Resolver ubicación opcional
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
                .nombre(request.nombre().trim())
                .descripcion(request.descripcion().trim())
                .fechaInicio(request.fechaInicio())
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

        // 8. Inicializar la Planificación 1:1
        Planificacion planificacion = Planificacion.builder()
                .proyecto(proyectoGuardado)
                .fechaEntrega(request.fechaFinEstipulada())
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

        return ProyectoMapper.toResponse(
                proyectoGuardado,
                perfil,
                request.fechaFinEstipulada(),
                objetivosGuardados
        );
    }
}
