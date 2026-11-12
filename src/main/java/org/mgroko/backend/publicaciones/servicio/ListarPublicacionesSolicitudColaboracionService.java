package org.mgroko.backend.publicaciones.servicio;

import java.util.List;

import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoPublicacion;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.publicaciones.dto.DireccionSolicitudColaboracion;
import org.mgroko.backend.publicaciones.dto.PublicacionSolicitudColaboracionResponse;
import org.mgroko.backend.publicaciones.mapper.SolicitudColaboracionMapper;
import org.mgroko.backend.repositorio.SolicitudColaboracionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC-65: publicaciones disponibles para el selector del filtro por publicación.
 *
 * <p>Devuelve únicamente las publicaciones que aparecen en solicitudes del
 * perfil activo en la dirección indicada. En "Enviadas" son publicaciones
 * propias; en "Recibidas" son publicaciones de los perfiles remitentes. Se
 * aplican las mismas reglas de visibilidad que la búsqueda de solicitudes.</p>
 */
@Service
public class ListarPublicacionesSolicitudColaboracionService {

    private final SolicitudColaboracionRepository solicitudColaboracionRepository;

    public ListarPublicacionesSolicitudColaboracionService(
            SolicitudColaboracionRepository solicitudColaboracionRepository) {
        this.solicitudColaboracionRepository = solicitudColaboracionRepository;
    }

    @Transactional(readOnly = true)
    public List<PublicacionSolicitudColaboracionResponse> listar(
            Long idPerfilActivo,
            DireccionSolicitudColaboracion direccion) {

        if (idPerfilActivo == null) {
            throw new PerfilActivoNoSeleccionadoException();
        }

        boolean incluirRecibidas = direccion == null
                || direccion == DireccionSolicitudColaboracion.RECIBIDA;
        boolean incluirEnviadas = direccion == null
                || direccion == DireccionSolicitudColaboracion.ENVIADA;

        return solicitudColaboracionRepository
                .findPublicacionesDistintasParaFiltro(
                        idPerfilActivo,
                        incluirRecibidas,
                        incluirEnviadas,
                        EstadoPublicacion.ACTIVA,
                        EstadoPerfil.Activo,
                        EstadoUsuario.Activo)
                .stream()
                .map(SolicitudColaboracionMapper::toPublicacionResumen)
                .toList();
    }
}
