package org.mgroko.backend.publicaciones.servicio;

import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.modelo.Publicacion;
import org.mgroko.backend.modelo.enums.EstadoPublicacion;
import org.mgroko.backend.perfiles.servicio.VerPerfilService;
import org.mgroko.backend.proyectos.servicio.ProyectoVisibilidadService;
import org.mgroko.backend.publicaciones.dto.PublicacionResponse;
import org.mgroko.backend.publicaciones.mapper.PublicacionMapper;
import org.mgroko.backend.repositorio.PublicacionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListarPublicacionesPerfilService {

    private static final int TAMANIO_MAXIMO = 50;

    private final VerPerfilService verPerfilService;
    private final PublicacionRepository publicacionRepository;
    private final ProyectoVisibilidadService proyectoVisibilidadService;

    public ListarPublicacionesPerfilService(
            VerPerfilService verPerfilService,
            PublicacionRepository publicacionRepository,
            ProyectoVisibilidadService proyectoVisibilidadService) {
        this.verPerfilService = verPerfilService;
        this.publicacionRepository = publicacionRepository;
        this.proyectoVisibilidadService = proyectoVisibilidadService;
    }

    @Transactional(readOnly = true)
    public PaginaResponse<PublicacionResponse> listar(
            Long idPerfil,
            Long idUsuarioAutenticado,
            Long idPerfilActivo,
            int pagina,
            int tamanio) {

        // Aplica las reglas de visibilidad de UC-14: perfil activo, usuario
        // activo y perfil propio; lanza 404 si el perfil no es visible.
        verPerfilService.obtenerPerfilVisible(idPerfil, idUsuarioAutenticado);

        Pageable pageable = PageRequest.of(
                Math.max(pagina, 0),
                Math.min(Math.max(tamanio, 1), TAMANIO_MAXIMO));

        Page<Publicacion> publicaciones = publicacionRepository
                .findByPerfilIdPerfilAndEstadoOrderByFechaPublicacionDesc(
                        idPerfil, EstadoPublicacion.ACTIVA, pageable);

        Page<PublicacionResponse> respuesta = publicaciones.map(publicacion -> {
            boolean puedeVerProyecto = publicacion.getProyecto() != null
                    && proyectoVisibilidadService.puedeVer(publicacion.getProyecto(), idPerfilActivo);
            return PublicacionMapper.toResponse(publicacion, puedeVerProyecto);
        });

        return PaginaResponse.fromPage(respuesta);
    }
}
