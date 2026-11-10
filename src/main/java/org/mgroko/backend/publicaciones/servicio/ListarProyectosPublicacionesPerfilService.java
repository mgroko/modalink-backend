package org.mgroko.backend.publicaciones.servicio;

import java.util.List;

import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.enums.EstadoPublicacion;
import org.mgroko.backend.perfiles.servicio.VerPerfilService;
import org.mgroko.backend.proyectos.servicio.ProyectoVisibilidadService;
import org.mgroko.backend.publicaciones.dto.ProyectoPublicacionFiltroResponse;
import org.mgroko.backend.repositorio.PublicacionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Opciones del selector de proyecto para buscar publicaciones de un perfil.
 * Solo devuelve proyectos que tienen al menos una publicación activa y que
 * son visibles para el perfil activo del viewer.
 */
@Service
public class ListarProyectosPublicacionesPerfilService {

    private final VerPerfilService verPerfilService;
    private final PublicacionRepository publicacionRepository;
    private final ProyectoVisibilidadService proyectoVisibilidadService;

    public ListarProyectosPublicacionesPerfilService(
            VerPerfilService verPerfilService,
            PublicacionRepository publicacionRepository,
            ProyectoVisibilidadService proyectoVisibilidadService) {
        this.verPerfilService = verPerfilService;
        this.publicacionRepository = publicacionRepository;
        this.proyectoVisibilidadService = proyectoVisibilidadService;
    }

    @Transactional(readOnly = true)
    public List<ProyectoPublicacionFiltroResponse> listar(
            Long idPerfil,
            Long idUsuarioAutenticado,
            Long idPerfilActivo) {

        verPerfilService.obtenerPerfilVisible(idPerfil, idUsuarioAutenticado);

        return publicacionRepository
                .findProyectosConPublicaciones(idPerfil, EstadoPublicacion.ACTIVA)
                .stream()
                .filter(proyecto -> proyectoVisibilidadService.puedeVer(proyecto, idPerfilActivo))
                .map(this::toResponse)
                .toList();
    }

    private ProyectoPublicacionFiltroResponse toResponse(Proyecto proyecto) {
        return new ProyectoPublicacionFiltroResponse(
                proyecto.getIdProyecto(),
                proyecto.getNombre());
    }
}
