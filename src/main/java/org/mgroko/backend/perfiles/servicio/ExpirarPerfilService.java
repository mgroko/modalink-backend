package org.mgroko.backend.perfiles.servicio;

import java.time.LocalDateTime;
import java.util.List;

import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Expira los perfiles cuyo plazo de 30 días de baja (UC-12) ha vencido:
 * pasa su estado a {@code Baja}, lo que los vuelve inaccesibles para la comunidad.
 */
@Service
public class ExpirarPerfilService {

    private static final int DIAS_PARA_ELIMINACION = 30;

    private final PerfilRepository perfilRepository;

    public ExpirarPerfilService(PerfilRepository perfilRepository) {
        this.perfilRepository = perfilRepository;
    }

    @Transactional
    public int expirarVencidos() {
        LocalDateTime fechaLimite = LocalDateTime.now().minusDays(DIAS_PARA_ELIMINACION);
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