package org.mgroko.backend.publicaciones.servicio;

import java.util.ArrayList;
import java.util.List;

import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Publicacion;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.perfiles.exception.PerfilNoEncontradoException;
import org.mgroko.backend.proyectos.servicio.ProyectoVisibilidadService;
import org.mgroko.backend.publicaciones.dto.PublicacionResponse;
import org.mgroko.backend.publicaciones.especificacion.PublicacionSpecifications;
import org.mgroko.backend.publicaciones.mapper.PublicacionMapper;
import org.mgroko.backend.repositorio.ConexionRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.PublicacionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Feed de publicaciones de los contactos activos del perfil, con opción de
 * incluir las publicaciones propias.
 */
@Service
public class ListarFeedPublicacionesService {

    private static final int TAMANIO_MAXIMO = 50;

    private final PerfilRepository perfilRepository;
    private final ConexionRepository conexionRepository;
    private final PublicacionRepository publicacionRepository;
    private final ProyectoVisibilidadService proyectoVisibilidadService;

    public ListarFeedPublicacionesService(
            PerfilRepository perfilRepository,
            ConexionRepository conexionRepository,
            PublicacionRepository publicacionRepository,
            ProyectoVisibilidadService proyectoVisibilidadService) {
        this.perfilRepository = perfilRepository;
        this.conexionRepository = conexionRepository;
        this.publicacionRepository = publicacionRepository;
        this.proyectoVisibilidadService = proyectoVisibilidadService;
    }

    @Transactional(readOnly = true)
    public PaginaResponse<PublicacionResponse> listar(
            Long idUsuario,
            Long idPerfilActivo,
            boolean incluirPropias,
            int pagina,
            int tamanio) {

        validarPerfilActivo(idUsuario, idPerfilActivo);

        Pageable pageable = PageRequest.of(
                Math.max(pagina, 0),
                Math.min(Math.max(tamanio, 1), TAMANIO_MAXIMO),
                Sort.by(
                        Sort.Order.desc("fechaPublicacion"),
                        Sort.Order.desc("idPublicacion")));

        List<Long> idsPerfiles = new ArrayList<>(
                conexionRepository.buscarIdsContrapartesActivas(idPerfilActivo));

        if (incluirPropias) {
            idsPerfiles.add(idPerfilActivo);
        }

        idsPerfiles = idsPerfiles.stream().distinct().toList();

        if (idsPerfiles.isEmpty()) {
            return PaginaResponse.fromPage(new PageImpl<>(List.of(), pageable, 0));
        }

        Page<Publicacion> publicaciones = publicacionRepository.findAll(
                PublicacionSpecifications.conPerfiles(idsPerfiles),
                pageable);

        Page<PublicacionResponse> respuesta = publicaciones.map(publicacion -> {
            boolean puedeVerProyecto = publicacion.getProyecto() != null
                    && proyectoVisibilidadService.puedeVer(
                            publicacion.getProyecto(), idPerfilActivo);
            return PublicacionMapper.toResponse(publicacion, puedeVerProyecto);
        });

        return PaginaResponse.fromPage(respuesta);
    }

    private void validarPerfilActivo(Long idUsuario, Long idPerfilActivo) {
        if (idPerfilActivo == null) {
            throw new PerfilActivoNoSeleccionadoException();
        }

        Perfil perfil = perfilRepository.findByIdPerfilAndUsuarioIdUsuario(idPerfilActivo, idUsuario)
                .orElseThrow(PerfilNoEncontradoException::new);

        if (perfil.getEstado() != EstadoPerfil.Activo) {
            throw new PerfilEnBajaException("El perfil no se encuentra activo.");
        }
    }
}
