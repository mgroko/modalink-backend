package org.mgroko.backend.publicaciones.servicio;

import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Publicacion;
import org.mgroko.backend.publicaciones.dto.PublicacionOcultadaResponse;
import org.mgroko.backend.publicaciones.especificacion.PublicacionSpecifications;
import org.mgroko.backend.repositorio.PublicacionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC-69: búsqueda específica de publicaciones ocultas del perfil activo.
 *
 * <p>Es la única vista donde el dueño puede volver a ver una publicación
 * ocultada por el administrador. Solo expone título y descripción, y no
 * permite abrir, editar ni interactuar con la publicación; la única acción
 * disponible para el dueño es eliminarla mediante UC-20.</p>
 */
@Service
public class ListarPublicacionesOcultasService {

    private static final int TAMANIO_MAXIMO = 50;

    private final PublicacionRepository publicacionRepository;
    private final PublicacionDatosService publicacionDatosService;

    public ListarPublicacionesOcultasService(
            PublicacionRepository publicacionRepository,
            PublicacionDatosService publicacionDatosService) {
        this.publicacionRepository = publicacionRepository;
        this.publicacionDatosService = publicacionDatosService;
    }

    @Transactional(readOnly = true)
    public PaginaResponse<PublicacionOcultadaResponse> listar(
            Long idUsuario,
            Long idPerfilActivo,
            String texto,
            int pagina,
            int tamanio) {

        Perfil perfil = publicacionDatosService.validarPerfilActivo(
                idUsuario, idPerfilActivo);

        Pageable pageable = PageRequest.of(
                Math.max(pagina, 0),
                Math.min(Math.max(tamanio, 1), TAMANIO_MAXIMO),
                Sort.by(
                        Sort.Order.desc("fechaPublicacion"),
                        Sort.Order.desc("idPublicacion")));

        Page<Publicacion> publicaciones = publicacionRepository.findAll(
                PublicacionSpecifications.conFiltrosOcultas(
                        perfil.getIdPerfil(), texto),
                pageable);

        return PaginaResponse.fromPage(publicaciones.map(publicacion ->
                new PublicacionOcultadaResponse(
                        publicacion.getIdPublicacion(),
                        publicacion.getTitulo(),
                        publicacion.getDescripcion(),
                        publicacion.getFechaPublicacion(),
                        publicacion.getEstado())));
    }
}
