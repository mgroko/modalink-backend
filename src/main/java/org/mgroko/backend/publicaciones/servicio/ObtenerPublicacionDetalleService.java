package org.mgroko.backend.publicaciones.servicio;

import java.util.Map;

import org.mgroko.backend.modelo.Publicacion;
import org.mgroko.backend.modelo.enums.EstadoPublicacion;
import org.mgroko.backend.modelo.enums.OrigenColaboracion;
import org.mgroko.backend.perfiles.servicio.VerPerfilService;
import org.mgroko.backend.proyectos.servicio.ProyectoVisibilidadService;
import org.mgroko.backend.publicaciones.dto.PublicacionResponse;
import org.mgroko.backend.publicaciones.dto.ResumenInteraccionPublicacion;
import org.mgroko.backend.publicaciones.exception.PublicacionNoEncontradaException;
import org.mgroko.backend.publicaciones.mapper.PublicacionMapper;
import org.mgroko.backend.repositorio.PublicacionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Detalle de publicación visible para el perfil activo.
 *
 * <p>A diferencia de UC-21, no exige que la publicación sea propia. Se usa
 * para el link de notificaciones de solicitudes de colaboración, que pueden
 * apuntar a publicaciones del perfil remitente o del destinatario.</p>
 */
@Service
public class ObtenerPublicacionDetalleService {

    private final PublicacionRepository publicacionRepository;
    private final PublicacionDatosService publicacionDatosService;
    private final ProyectoVisibilidadService proyectoVisibilidadService;
    private final VerPerfilService verPerfilService;
    private final PublicacionInteraccionResumenService interaccionResumenService;
    private final ColaboradorPublicacionOrigenService colaboradorOrigenService;

    public ObtenerPublicacionDetalleService(
            PublicacionRepository publicacionRepository,
            PublicacionDatosService publicacionDatosService,
            ProyectoVisibilidadService proyectoVisibilidadService,
            VerPerfilService verPerfilService,
            PublicacionInteraccionResumenService interaccionResumenService,
            ColaboradorPublicacionOrigenService colaboradorOrigenService) {
        this.publicacionRepository = publicacionRepository;
        this.publicacionDatosService = publicacionDatosService;
        this.proyectoVisibilidadService = proyectoVisibilidadService;
        this.verPerfilService = verPerfilService;
        this.interaccionResumenService = interaccionResumenService;
        this.colaboradorOrigenService = colaboradorOrigenService;
    }

    @Transactional(readOnly = true)
    public PublicacionResponse obtener(
            Long idUsuario,
            Long idPerfilActivo,
            Long idPublicacion) {

        publicacionDatosService.validarPerfilActivo(idUsuario, idPerfilActivo);

        Publicacion publicacion = publicacionRepository
                .findByIdPublicacion(idPublicacion)
                .orElseThrow(() -> new PublicacionNoEncontradaException(idPublicacion));

        if (publicacion.getEstado() != EstadoPublicacion.ACTIVA) {
            throw new PublicacionNoEncontradaException(idPublicacion);
        }

        // Mismas reglas de visibilidad de UC-14 para el perfil autor. Si la
        // publicación es propia, el autor se resuelve como propietario.
        VerPerfilService.PerfilVisible autor = verPerfilService
                .obtenerPerfilVisible(
                        publicacion.getPerfil().getIdPerfil(), idUsuario);

        boolean puedeVerProyecto = publicacion.getProyecto() != null
                && proyectoVisibilidadService.puedeVer(
                        publicacion.getProyecto(), idPerfilActivo);

        ResumenInteraccionPublicacion resumen =
                interaccionResumenService.resumirUna(idPublicacion, idPerfilActivo);

        Map<Long, OrigenColaboracion> origenes = colaboradorOrigenService
                .obtenerOrigenes(java.util.List.of(idPublicacion))
                .getOrDefault(idPublicacion, Map.of());

        return autor.esPropietario()
                ? PublicacionMapper.toResponsePropia(
                        publicacion, puedeVerProyecto, resumen, origenes)
                : PublicacionMapper.toResponse(
                        publicacion, puedeVerProyecto, resumen, origenes);
    }
}
