package org.mgroko.backend.usuario.servicio;

import java.time.LocalDateTime;
import java.util.List;

import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.perfiles.servicio.EliminarImagenesPerfilService;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Expira las cuentas cuyo plazo de baja (UC-07) ha vencido:
 * pasa su estado a {@code Baja}, lo que da de baja la cuenta definitivamente
 * y oculta todos los perfiles asociados a la comunidad.
 * <p>
 * El plazo en días lo resuelve el llamador (scheduler o ejecución manual
 * desde el panel de administración).
 */
@Service
public class ExpirarCuentaService {

    private final UsuarioRepository usuarioRepository;
    private final PerfilRepository perfilRepository;
    private final EliminarImagenesPerfilService eliminarImagenesPerfilService;

    public ExpirarCuentaService(UsuarioRepository usuarioRepository, PerfilRepository perfilRepository,
            EliminarImagenesPerfilService eliminarImagenesPerfilService) {
        this.usuarioRepository = usuarioRepository;
        this.perfilRepository = perfilRepository;
        this.eliminarImagenesPerfilService = eliminarImagenesPerfilService;
    }

    @Transactional
    public int expirarVencidos(int diasBaja) {
        LocalDateTime fechaLimite = LocalDateTime.now().minusDays(diasBaja);
        List<Usuario> vencidos = usuarioRepository
                .findByEstadoAndFechaSolicitudBajaBefore(EstadoUsuario.PendienteBaja, fechaLimite);

        for (Usuario usuario : vencidos) {
            usuario.setEstado(EstadoUsuario.Baja);

            List<Perfil> perfiles = perfilRepository.findByUsuarioIdUsuario(usuario.getIdUsuario());
            for (Perfil perfil : perfiles) {
                if (perfil.getEstado() != EstadoPerfil.Baja) {
                    perfil.setEstado(EstadoPerfil.Baja);
                    // La imagen del perfil se elimina en el metodo dedicado: ahi
                    // se sumaran las publicaciones independientes cuando existan.
                    eliminarImagenesPerfilService.eliminarImagenesDePerfil(perfil);
                }
                // TODO: Eliminar publicaciones independientes del perfil (tabla publicacion aún no implementada).
            }
            if (!perfiles.isEmpty()) {
                perfilRepository.saveAll(perfiles);
            }
        }

        if (!vencidos.isEmpty()) {
            usuarioRepository.saveAll(vencidos);
        }
        return vencidos.size();
    }
}