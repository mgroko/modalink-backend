package org.mgroko.backend.perfiles.servicio;

import org.mgroko.backend.admin.exception.PerfilNoEncontradoException;
import org.mgroko.backend.admin.servicio.ConfiguracionSistemaService;
import org.mgroko.backend.auth.exception.UsuarioNoEncontradoException;
import org.mgroko.backend.conexiones.modelo.RelacionPerfil;
import org.mgroko.backend.conexiones.servicio.RelacionPerfilService;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.perfiles.dto.PerfilDetalleResponse;
import org.mgroko.backend.perfiles.mapper.PerfilMapper;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.UsuarioRepository;
import org.mgroko.backend.security.PerfilAccessPolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VerPerfilService {

    private final PerfilRepository perfilRepository;
    private final UsuarioRepository usuarioRepository;
    private final ConfiguracionSistemaService configuracionSistemaService;
    private final GestionTerminosCondicionesService gestionTerminosCondicionesService;
    private final RelacionPerfilService relacionPerfilService;

    public VerPerfilService(PerfilRepository perfilRepository, UsuarioRepository usuarioRepository,
            ConfiguracionSistemaService configuracionSistemaService,
            GestionTerminosCondicionesService gestionTerminosCondicionesService,
            RelacionPerfilService relacionPerfilService) {
        this.perfilRepository = perfilRepository;
        this.usuarioRepository = usuarioRepository;
        this.configuracionSistemaService = configuracionSistemaService;
        this.gestionTerminosCondicionesService = gestionTerminosCondicionesService;
        this.relacionPerfilService = relacionPerfilService;
    }

    @Transactional(readOnly = true)
    public PerfilDetalleResponse obtenerDetalle(Long idPerfil, Long idUsuarioAutenticado) {
        return obtenerDetalle(idPerfil, idUsuarioAutenticado, null);
    }

    @Transactional(readOnly = true)
    public PerfilDetalleResponse obtenerDetalle(
            Long idPerfil,
            Long idUsuarioAutenticado,
            Long idPerfilActivo) {

        PerfilVisible visible = obtenerPerfilVisible(idPerfil, idUsuarioAutenticado);
        int diasBaja = configuracionSistemaService.obtenerDiasBaja();

        RelacionPerfil relacion = visible.esPropietario()
                ? RelacionPerfil.ninguna()
                : relacionPerfilService.obtener(idPerfilActivo, visible.perfil().getIdPerfil());

        return PerfilMapper.toDetalleResponse(
                visible.perfil(),
                visible.esPropietario(),
                diasBaja,
                gestionTerminosCondicionesService.obtenerParaPerfil(visible.perfil()),
                relacion);
    }

    /**
     * Aplica las reglas de visibilidad de UC-14 y devuelve el perfil junto con
     * la marca de propiedad. Lo reutilizan otras consultas del área de perfil,
     * como el listado de reseñas.
     */
    @Transactional(readOnly = true)
    public PerfilVisible obtenerPerfilVisible(Long idPerfil, Long idUsuarioAutenticado) {
        Usuario usuarioAutenticado = usuarioRepository.findById(idUsuarioAutenticado)
                .orElseThrow(() -> new UsuarioNoEncontradoException("Usuario no encontrado."));

        if (!usuarioAutenticado.getEstado().permiteAcceso()) {
            throw new UsuarioNoEncontradoException("Usuario no encontrado.");
        }

        PerfilAccessPolicy.validarNoAdministrador(usuarioAutenticado);

        Perfil perfil = perfilRepository.findById(idPerfil)
                .orElseThrow(() -> new PerfilNoEncontradoException("Perfil no encontrado."));

        boolean esPropietario = perfil.getUsuario().getIdUsuario().equals(idUsuarioAutenticado);

        if (esPropietario) {
            // Si el perfil está en Baja definitiva, no está disponible.
            if (perfil.getEstado() == EstadoPerfil.Baja) {
                throw new PerfilNoEncontradoException("Perfil no encontrado.");
            }
            return new PerfilVisible(perfil, true);
        }

        // Si es un tercero viendo el perfil:
        // 1. El perfil debe estar Activo
        // 2. El usuario propietario debe estar Activo (no PendienteBaja, Deshabilitado ni Baja)
        if (perfil.getEstado() != EstadoPerfil.Activo) {
            throw new PerfilNoEncontradoException("Perfil no encontrado.");
        }

        if (perfil.getUsuario().getEstado() != EstadoUsuario.Activo) {
            throw new PerfilNoEncontradoException("Perfil no encontrado.");
        }

        return new PerfilVisible(perfil, false);
    }

    public record PerfilVisible(Perfil perfil, boolean esPropietario) {
    }
}
