package org.mgroko.backend.conexiones.servicio;

import java.util.Optional;

import org.mgroko.backend.conexiones.modelo.EstadoRelacionPerfil;
import org.mgroko.backend.conexiones.modelo.RelacionPerfil;
import org.mgroko.backend.modelo.Conexion;
import org.mgroko.backend.modelo.SolicitudConexion;
import org.mgroko.backend.repositorio.ConexionRepository;
import org.mgroko.backend.repositorio.SolicitudConexionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Calcula qué acción puede ejecutar el perfil activo sobre otro perfil:
 * conectarse, ver una solicitud enviada/recibida o abrir una conexión
 * existente.
 */
@Service
public class RelacionPerfilService {

    private final SolicitudConexionRepository solicitudConexionRepository;
    private final ConexionRepository conexionRepository;

    public RelacionPerfilService(
            SolicitudConexionRepository solicitudConexionRepository,
            ConexionRepository conexionRepository) {
        this.solicitudConexionRepository = solicitudConexionRepository;
        this.conexionRepository = conexionRepository;
    }

    @Transactional(readOnly = true)
    public RelacionPerfil obtener(Long idPerfilActivo, Long idPerfilObjetivo) {
        if (idPerfilActivo == null || idPerfilObjetivo == null
                || idPerfilActivo.equals(idPerfilObjetivo)) {
            return RelacionPerfil.ninguna();
        }

        Optional<Conexion> conexion = conexionRepository
                .findActivaEntre(idPerfilActivo, idPerfilObjetivo);

        if (conexion.isPresent()) {
            return new RelacionPerfil(
                    EstadoRelacionPerfil.CONECTADO,
                    null,
                    conexion.get().getIdConexion());
        }

        Optional<SolicitudConexion> pendiente = solicitudConexionRepository
                .findPendienteEntre(idPerfilActivo, idPerfilObjetivo);

        if (pendiente.isPresent()) {
            boolean enviada = pendiente.get().getPerfilRemitente()
                    .getIdPerfil()
                    .equals(idPerfilActivo);

            return new RelacionPerfil(
                    enviada
                            ? EstadoRelacionPerfil.SOLICITUD_ENVIADA
                            : EstadoRelacionPerfil.SOLICITUD_RECIBIDA,
                    pendiente.get().getIdSolicitudConexion(),
                    null);
        }

        return RelacionPerfil.ninguna();
    }
}
