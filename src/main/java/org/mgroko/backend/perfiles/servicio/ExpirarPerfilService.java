package org.mgroko.backend.perfiles.servicio;

import java.time.LocalDateTime;
import java.util.List;

import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Expira los perfiles cuyo plazo de baja (UC-12) ha vencido:
 * pasa su estado a {@code Baja}, lo que los vuelve inaccesibles para la comunidad.
 * <p>
 * El plazo en días lo resuelve el llamador (scheduler o ejecución manual
 * desde el panel de administración).
 */
@Service
public class ExpirarPerfilService {

    private final PerfilRepository perfilRepository;

    public ExpirarPerfilService(PerfilRepository perfilRepository) {
        this.perfilRepository = perfilRepository;
    }

    @Transactional
    public int expirarVencidos(int diasBaja) {
        LocalDateTime fechaLimite = LocalDateTime.now().minusDays(diasBaja);
        List<Perfil> vencidos = perfilRepository
                .findByEstadoAndFechaSolicitudBajaBefore(EstadoPerfil.PendienteBaja, fechaLimite);

        for (Perfil perfil : vencidos) {
            perfil.setEstado(EstadoPerfil.Baja);
            // TODO: Eliminar publicaciones independientes del perfil.
        }
        if (!vencidos.isEmpty()) {
            perfilRepository.saveAll(vencidos);
        }
        return vencidos.size();
    }
}