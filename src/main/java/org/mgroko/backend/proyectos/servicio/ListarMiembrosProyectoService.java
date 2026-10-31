package org.mgroko.backend.proyectos.servicio;

import java.util.List;
import java.util.Locale;

import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.RolProyectoUsuario;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.perfiles.exception.PerfilNoEncontradoException;
import org.mgroko.backend.proyectos.dto.MiembroProyectoResponse;
import org.mgroko.backend.proyectos.exception.AccesoDenegadoProyectoException;
import org.mgroko.backend.proyectos.exception.ProyectoNoEncontradoException;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.ProyectoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC-70: lista los miembros activos del proyecto para que el director elija
 * el destinatario de una asignación.
 */
@Service
public class ListarMiembrosProyectoService {

    public static final String MOTIVO_SIN_PERMISO = "SIN_PERMISO";
    public static final String MOTIVO_PROYECTO_NO_PERMITE_BAJA = "PROYECTO_NO_PERMITE_BAJA";
    public static final String MOTIVO_AUTOELIMINACION = "AUTOELIMINACION";
    public static final String MOTIVO_ULTIMO_DIRECTOR = "ULTIMO_DIRECTOR";

    private final ProyectoRepository proyectoRepository;
    private final PerfilRepository perfilRepository;
    private final MiembroProyectoRepository miembroProyectoRepository;
    private final ProyectoSecurityService proyectoSecurityService;

    public ListarMiembrosProyectoService(
            ProyectoRepository proyectoRepository,
            PerfilRepository perfilRepository,
            MiembroProyectoRepository miembroProyectoRepository,
            ProyectoSecurityService proyectoSecurityService) {
        this.proyectoRepository = proyectoRepository;
        this.perfilRepository = perfilRepository;
        this.miembroProyectoRepository = miembroProyectoRepository;
        this.proyectoSecurityService = proyectoSecurityService;
    }

    @Transactional(readOnly = true)
    public PaginaResponse<MiembroProyectoResponse> listar(
            Long idUsuario,
            Long idPerfilActivo,
            Long idProyecto,
            String nombre,
            String rol,
            Long idProfesion,
            int page,
            int size,
            boolean todos) {

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

        proyectoSecurityService.validarPermiso(
                idProyecto,
                idPerfilActivo,
                ProyectoSecurityService.PERMISO_VER_PROYECTO);

        MiembroProyecto actor = miembroProyectoRepository
                .findMiembroActivoConPermisos(idProyecto, idPerfilActivo)
                .orElseThrow(() -> new AccesoDenegadoProyectoException(
                        "No tienes el permiso 'VER_PROYECTO' en este proyecto o no eres un miembro activo."));

        boolean actorEsDirector = RolProyectoUsuario.desde(actor.getRolProyecto())
                == RolProyectoUsuario.DIRECTOR;
        boolean actorPuedeEliminar = tienePermiso(
                actor,
                ProyectoSecurityService.PERMISO_ELIMINAR_INTEGRANTE);
        boolean proyectoPermiteEliminar = proyecto.getEstado() == EstadoProyecto.PUBLICADO
                || proyecto.getEstado() == EstadoProyecto.CONFIRMADO;

        List<MiembroProyecto> miembros = miembroProyectoRepository
                .buscarActivosDeProyectoConPerfilProfesionYRol(idProyecto);

        long directoresActivos = miembros.stream()
                .filter(miembro -> RolProyectoUsuario.desde(miembro.getRolProyecto())
                        == RolProyectoUsuario.DIRECTOR)
                .count();

        String nombreNormalizado = normalizar(nombre);
        String rolNormalizado = normalizar(rol);

        List<MiembroProyectoResponse> filtrados = miembros
                .stream()
                .filter(miembro -> coincideNombre(miembro, nombreNormalizado))
                .filter(miembro -> coincideRol(miembro, rolNormalizado))
                .filter(miembro -> coincideProfesion(miembro, idProfesion))
                .map(miembro -> toResponse(
                        miembro,
                        idPerfilActivo,
                        actorEsDirector,
                        actorPuedeEliminar,
                        proyectoPermiteEliminar,
                        directoresActivos))
                .toList();

        Pageable pageable = construirPageable(page, size, todos);
        List<MiembroProyectoResponse> contenido = pageable.isPaged()
                ? paginar(filtrados, pageable)
                : filtrados;

        Page<MiembroProyectoResponse> resultado =
                new PageImpl<>(contenido, pageable, filtrados.size());

        return PaginaResponse.fromPage(resultado);
    }

    private boolean coincideNombre(
            MiembroProyecto miembro,
            String nombreNormalizado) {

        if (nombreNormalizado == null) {
            return true;
        }

        String nombreArtistico = normalizar(miembro.getPerfil().getNombreArtistico());
        String nombreUsuario = normalizar(miembro.getPerfil().getUsuario().getNombre());
        String apellidoUsuario = normalizar(miembro.getPerfil().getUsuario().getApellido());

        return (nombreArtistico != null && nombreArtistico.contains(nombreNormalizado))
                || (nombreUsuario != null && nombreUsuario.contains(nombreNormalizado))
                || (apellidoUsuario != null && apellidoUsuario.contains(nombreNormalizado));
    }

    private boolean coincideRol(
            MiembroProyecto miembro,
            String rolNormalizado) {

        if (rolNormalizado == null) {
            return true;
        }

        RolProyectoUsuario rol = RolProyectoUsuario.desde(miembro.getRolProyecto());
        return rol != null && rol.name().toLowerCase(Locale.ROOT).equals(rolNormalizado);
    }

    private boolean coincideProfesion(
            MiembroProyecto miembro,
            Long idProfesion) {

        return idProfesion == null
                || (miembro.getPerfil().getProfesion() != null
                        && idProfesion.equals(
                                miembro.getPerfil().getProfesion().getIdProfesion()));
    }

    private MiembroProyectoResponse toResponse(
            MiembroProyecto miembro,
            Long idPerfilActivo,
            boolean actorEsDirector,
            boolean actorPuedeEliminar,
            boolean proyectoPermiteEliminar,
            long directoresActivos) {

        Perfil perfil = miembro.getPerfil();
        RolProyectoUsuario rol = RolProyectoUsuario.desde(miembro.getRolProyecto());

        String motivoNoEliminable = motivoNoEliminable(
                miembro,
                idPerfilActivo,
                actorEsDirector,
                actorPuedeEliminar,
                proyectoPermiteEliminar,
                directoresActivos);

        return new MiembroProyectoResponse(
                miembro.getIdMiembro(),
                perfil.getIdPerfil(),
                perfil.getUsuario().getIdUsuario(),
                perfil.getNombreArtistico(),
                perfil.getUsuario().getNombre(),
                perfil.getUsuario().getApellido(),
                perfil.getProfesion() != null
                        ? perfil.getProfesion().getIdProfesion()
                        : null,
                perfil.getProfesion() != null
                        ? perfil.getProfesion().getNombre()
                        : null,
                rol != null ? rol.name() : null,
                miembro.getEstadoParticipacion() != null
                        ? miembro.getEstadoParticipacion().name()
                        : null,
                motivoNoEliminable == null,
                motivoNoEliminable);
    }

    private String motivoNoEliminable(
            MiembroProyecto miembro,
            Long idPerfilActivo,
            boolean actorEsDirector,
            boolean actorPuedeEliminar,
            boolean proyectoPermiteEliminar,
            long directoresActivos) {

        if (!actorEsDirector || !actorPuedeEliminar) {
            return MOTIVO_SIN_PERMISO;
        }
        if (!proyectoPermiteEliminar) {
            return MOTIVO_PROYECTO_NO_PERMITE_BAJA;
        }
        if (miembro.getPerfil() != null
                && idPerfilActivo != null
                && idPerfilActivo.equals(miembro.getPerfil().getIdPerfil())) {
            return MOTIVO_AUTOELIMINACION;
        }
        if (RolProyectoUsuario.desde(miembro.getRolProyecto())
                == RolProyectoUsuario.DIRECTOR
                && directoresActivos <= 1) {
            return MOTIVO_ULTIMO_DIRECTOR;
        }
        return null;
    }

    private boolean tienePermiso(MiembroProyecto miembro, String permiso) {
        if (miembro == null
                || miembro.getRolProyecto() == null
                || miembro.getRolProyecto().getPermisos() == null) {
            return false;
        }
        return miembro.getRolProyecto().getPermisos().stream()
                .anyMatch(permisoProyecto -> permisoProyecto != null
                        && permiso.equalsIgnoreCase(permisoProyecto.getNombre()));
    }

    private Pageable construirPageable(int page, int size, boolean todos) {
        if (todos || size <= 0) {
            return Pageable.unpaged();
        }
        return PageRequest.of(Math.max(0, page), size);
    }

    private List<MiembroProyectoResponse> paginar(
            List<MiembroProyectoResponse> miembros,
            Pageable pageable) {

        int desde = (int) pageable.getOffset();
        if (desde >= miembros.size()) {
            return List.of();
        }
        int hasta = Math.min(desde + pageable.getPageSize(), miembros.size());
        return miembros.subList(desde, hasta);
    }

    private String normalizar(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.trim().toLowerCase(Locale.ROOT);
    }
}
