package org.mgroko.backend.invitaciones.servicio;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.mgroko.backend.invitaciones.dto.CrearInvitacionActividadBatchRequest;
import org.mgroko.backend.invitaciones.dto.CrearInvitacionActividadRequest;
import org.mgroko.backend.invitaciones.dto.CrearInvitacionGralBatchRequest;
import org.mgroko.backend.invitaciones.dto.CrearInvitacionGralRequest;
import org.mgroko.backend.invitaciones.dto.InvitacionBatchError;
import org.mgroko.backend.invitaciones.dto.InvitacionBatchResponse;
import org.mgroko.backend.invitaciones.dto.InvitacionResponse;
import org.mgroko.backend.invitaciones.exception.InvitacionNoPermitidaException;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RequerimientoActividad;
import org.mgroko.backend.modelo.enums.EstadoActividad;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.perfiles.exception.PerfilNoEncontradoException;
import org.mgroko.backend.postulaciones.exception.RequerimientoNoEncontradoException;
import org.mgroko.backend.proyectos.exception.ProyectoNoEncontradoException;
import org.mgroko.backend.proyectos.servicio.ProyectoSecurityService;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.ProyectoRepository;
import org.mgroko.backend.repositorio.RequerimientoActividadRepository;
import org.springframework.stereotype.Service;

/**
 * UC-31: invitación a varios perfiles en una sola operación.
 *
 * <p>El procesamiento es de éxito parcial: cada perfil se intenta en una
 * transacción independiente mediante {@link CrearInvitacionService}. Los
 * errores de un perfil no revierten las invitaciones ya creadas.</p>
 */
@Service
public class CrearInvitacionBatchService {

    private final CrearInvitacionService crearInvitacionService;
    private final PerfilRepository perfilRepository;
    private final ProyectoRepository proyectoRepository;
    private final RequerimientoActividadRepository requerimientoActividadRepository;
    private final ProyectoSecurityService proyectoSecurityService;

    public CrearInvitacionBatchService(
            CrearInvitacionService crearInvitacionService,
            PerfilRepository perfilRepository,
            ProyectoRepository proyectoRepository,
            RequerimientoActividadRepository requerimientoActividadRepository,
            ProyectoSecurityService proyectoSecurityService) {
        this.crearInvitacionService = crearInvitacionService;
        this.perfilRepository = perfilRepository;
        this.proyectoRepository = proyectoRepository;
        this.requerimientoActividadRepository = requerimientoActividadRepository;
        this.proyectoSecurityService = proyectoSecurityService;
    }

    public InvitacionBatchResponse invitarGeneral(
            Long idUsuario,
            Long idPerfilActivo,
            Long idProyecto,
            CrearInvitacionGralBatchRequest request) {

        validarContextoGeneral(idUsuario, idPerfilActivo, idProyecto);

        List<InvitacionResponse> creadas = new ArrayList<>();
        List<InvitacionBatchError> errores = new ArrayList<>();

        for (Long idPerfilDestinatario : idsUnicos(request.idsPerfilDestinatario())) {
            try {
                creadas.add(crearInvitacionService.invitarAProyecto(
                        idUsuario,
                        idPerfilActivo,
                        idProyecto,
                        new CrearInvitacionGralRequest(
                                idPerfilDestinatario, request.mensaje())));
            } catch (InvitacionNoPermitidaException ex) {
                errores.add(new InvitacionBatchError(
                        idPerfilDestinatario, ex.getMessage(), ex.getCodigo()));
            } catch (PerfilEnBajaException | PerfilNoEncontradoException ex) {
                errores.add(new InvitacionBatchError(
                        idPerfilDestinatario, ex.getMessage(), null));
            }
        }

        return new InvitacionBatchResponse(creadas, errores);
    }

    public InvitacionBatchResponse invitarActividad(
            Long idUsuario,
            Long idPerfilActivo,
            Long idProyecto,
            CrearInvitacionActividadBatchRequest request) {

        RequerimientoActividad requerimiento = validarContextoActividad(
                idUsuario, idPerfilActivo, idProyecto, request.idRequerimientoActividad());

        List<InvitacionResponse> creadas = new ArrayList<>();
        List<InvitacionBatchError> errores = new ArrayList<>();

        for (Long idPerfilDestinatario : idsUnicos(request.idsPerfilDestinatario())) {
            try {
                creadas.add(crearInvitacionService.invitarAProyectoActividad(
                        idUsuario,
                        idPerfilActivo,
                        idProyecto,
                        new CrearInvitacionActividadRequest(
                                requerimiento.getIdRequerimiento(),
                                idPerfilDestinatario,
                                request.mensaje())));
            } catch (InvitacionNoPermitidaException ex) {
                errores.add(new InvitacionBatchError(
                        idPerfilDestinatario, ex.getMessage(), ex.getCodigo()));
            } catch (PerfilEnBajaException | PerfilNoEncontradoException ex) {
                errores.add(new InvitacionBatchError(
                        idPerfilDestinatario, ex.getMessage(), null));
            } catch (RequerimientoNoEncontradoException ex) {
                errores.add(new InvitacionBatchError(
                        idPerfilDestinatario, ex.getMessage(), null));
            }
        }

        return new InvitacionBatchResponse(creadas, errores);
    }

    private void validarContextoGeneral(
            Long idUsuario,
            Long idPerfilActivo,
            Long idProyecto) {

        validarPerfilActivo(idUsuario, idPerfilActivo);

        Proyecto proyecto = proyectoRepository.findById(idProyecto)
                .orElseThrow(() -> new ProyectoNoEncontradoException(idProyecto));

        proyectoSecurityService.validarPermiso(
                idProyecto, idPerfilActivo, ProyectoSecurityService.PERMISO_INVITAR_MIEMBRO);
        proyectoSecurityService.validarDirector(idProyecto, idPerfilActivo);

        if (proyecto.getEstado() != EstadoProyecto.PUBLICADO
                && proyecto.getEstado() != EstadoProyecto.CONFIRMADO) {
            throw new InvitacionNoPermitidaException(
                    "Sólo se puede invitar en proyectos Publicados o Confirmados.");
        }
    }

    private RequerimientoActividad validarContextoActividad(
            Long idUsuario,
            Long idPerfilActivo,
            Long idProyecto,
            Long idRequerimientoActividad) {

        validarContextoGeneral(idUsuario, idPerfilActivo, idProyecto);

        RequerimientoActividad requerimiento = requerimientoActividadRepository
                .findByIdConActividadYProyecto(idRequerimientoActividad)
                .orElseThrow(() -> new RequerimientoNoEncontradoException(
                        "El requerimiento de actividad "
                                + idRequerimientoActividad
                                + " no existe."));

        if (!requerimiento.getActividad().getPlanificacion().getProyecto()
                .getIdProyecto().equals(idProyecto)) {
            throw new RequerimientoNoEncontradoException(
                    "El requerimiento no pertenece al proyecto indicado.");
        }

        EstadoActividad estado = requerimiento.getActividad().getEstado();
        if (estado != EstadoActividad.PENDIENTE && estado != EstadoActividad.EN_CURSO) {
            throw new InvitacionNoPermitidaException(
                    "Sólo se puede invitar a actividades Pendientes o En curso.");
        }

        return requerimiento;
    }

    private void validarPerfilActivo(Long idUsuario, Long idPerfilActivo) {
        if (idPerfilActivo == null) {
            throw new PerfilActivoNoSeleccionadoException();
        }

        Perfil perfil = perfilRepository
                .findByIdPerfilAndUsuarioIdUsuario(idPerfilActivo, idUsuario)
                .orElseThrow(PerfilNoEncontradoException::new);

        if (perfil.getEstado() != EstadoPerfil.ACTIVO) {
            throw new PerfilEnBajaException("El perfil no se encuentra activo.");
        }

        if (perfil.getUsuario() == null
                || perfil.getUsuario().getEstado() != EstadoUsuario.ACTIVO) {
            throw new PerfilEnBajaException("El usuario del perfil no se encuentra activo.");
        }
    }

    private List<Long> idsUnicos(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        Set<Long> unicos = new LinkedHashSet<>();
        for (Long id : ids) {
            if (id != null) {
                unicos.add(id);
            }
        }
        return new ArrayList<>(unicos);
    }
}
