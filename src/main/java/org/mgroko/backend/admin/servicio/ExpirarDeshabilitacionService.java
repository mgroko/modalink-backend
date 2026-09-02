package org.mgroko.backend.admin.servicio;

import java.time.LocalDateTime;
import java.util.List;

import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.repositorio.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reactiva automáticamente las cuentas cuya deshabilitación con duración
 * (UC-04) ha vencido: vuelve su estado a {@code Activo} y limpia el motivo
 * y la fecha hasta la deshabilitación.
 */
@Service
public class ExpirarDeshabilitacionService {

    private final UsuarioRepository usuarioRepository;

    public ExpirarDeshabilitacionService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public int reactivarVencidos() {
        List<Usuario> vencidos = usuarioRepository
                .findByEstadoAndFechaHastaDeshabilitacionBefore(EstadoUsuario.Deshabilitado, LocalDateTime.now());

        for (Usuario usuario : vencidos) {
            usuario.setEstado(EstadoUsuario.Activo);
            usuario.setMotivoDeshabilitacion(null);
            usuario.setFechaHastaDeshabilitacion(null);
        }

        if (!vencidos.isEmpty()) {
            usuarioRepository.saveAll(vencidos);
        }
        return vencidos.size();
    }
}