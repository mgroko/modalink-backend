package org.mgroko.backend.publicaciones.servicio;

import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Publicacion;
import org.mgroko.backend.proyectos.servicio.ProyectoVisibilidadService;
import org.mgroko.backend.publicaciones.dto.PublicacionResponse;
import org.mgroko.backend.publicaciones.exception.PublicacionNoEncontradaException;
import org.mgroko.backend.publicaciones.mapper.PublicacionMapper;
import org.mgroko.backend.repositorio.PublicacionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC-21, paso 2: recupera la información de una publicación propia para
 * mostrarla en el formulario de edición.
 */
@Service
public class ObtenerPublicacionService {

    private final PublicacionRepository publicacionRepository;
    private final PublicacionDatosService publicacionDatosService;
    private final ProyectoVisibilidadService proyectoVisibilidadService;

    public ObtenerPublicacionService(
            PublicacionRepository publicacionRepository,
            PublicacionDatosService publicacionDatosService,
            ProyectoVisibilidadService proyectoVisibilidadService) {
        this.publicacionRepository = publicacionRepository;
        this.publicacionDatosService = publicacionDatosService;
        this.proyectoVisibilidadService = proyectoVisibilidadService;
    }

    @Transactional(readOnly = true)
    public PublicacionResponse obtener(Long idUsuario, Long idPerfilActivo, Long idPublicacion) {
        Perfil perfil = publicacionDatosService.validarPerfilActivo(idUsuario, idPerfilActivo);

        Publicacion publicacion = publicacionRepository
                .findByIdPublicacionAndPerfilIdPerfil(idPublicacion, perfil.getIdPerfil())
                .orElseThrow(() -> new PublicacionNoEncontradaException(idPublicacion));

        boolean puedeVerProyecto = publicacion.getProyecto() != null
                && proyectoVisibilidadService.puedeVer(
                        publicacion.getProyecto(), perfil.getIdPerfil());

        return PublicacionMapper.toResponsePropia(publicacion, puedeVerProyecto);
    }
}
