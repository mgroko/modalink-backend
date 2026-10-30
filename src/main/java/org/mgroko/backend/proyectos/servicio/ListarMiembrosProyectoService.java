package org.mgroko.backend.proyectos.servicio;

import java.util.List;
import java.util.Locale;

import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.RolProyectoUsuario;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.perfiles.exception.PerfilNoEncontradoException;
import org.mgroko.backend.proyectos.dto.MiembroProyectoResponse;
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

        proyectoRepository.findById(idProyecto)
                .orElseThrow(() -> new ProyectoNoEncontradoException(idProyecto));

        proyectoSecurityService.validarPermiso(
                idProyecto,
                idPerfilActivo,
                ProyectoSecurityService.PERMISO_VER_PROYECTO);

        String nombreNormalizado = normalizar(nombre);
        String rolNormalizado = normalizar(rol);

        List<MiembroProyectoResponse> filtrados = miembroProyectoRepository
                .buscarActivosDeProyectoConPerfilProfesionYRol(idProyecto)
                .stream()
                .filter(miembro -> coincideNombre(miembro, nombreNormalizado))
                .filter(miembro -> coincideRol(miembro, rolNormalizado))
                .filter(miembro -> coincideProfesion(miembro, idProfesion))
                .map(this::toResponse)
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

    private MiembroProyectoResponse toResponse(MiembroProyecto miembro) {
        Perfil perfil = miembro.getPerfil();
        RolProyectoUsuario rol = RolProyectoUsuario.desde(miembro.getRolProyecto());

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
                        : null);
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
