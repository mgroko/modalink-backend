package org.mgroko.backend.publicaciones.servicio;

import java.util.Map;

import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.modelo.Publicacion;
import org.mgroko.backend.modelo.enums.EstadoPublicacion;
import org.mgroko.backend.modelo.enums.OrigenColaboracion;
import org.mgroko.backend.perfiles.servicio.VerPerfilService;
import org.mgroko.backend.proyectos.servicio.ProyectoVisibilidadService;
import org.mgroko.backend.publicaciones.dto.BuscarPublicacionesFiltro;
import org.mgroko.backend.publicaciones.dto.PublicacionResponse;
import org.mgroko.backend.publicaciones.dto.ResumenInteraccionPublicacion;
import org.mgroko.backend.publicaciones.especificacion.PublicacionSpecifications;
import org.mgroko.backend.publicaciones.exception.RangoFechasPublicacionInvalidoException;
import org.mgroko.backend.publicaciones.mapper.PublicacionMapper;
import org.mgroko.backend.repositorio.PublicacionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListarPublicacionesPerfilService {

    private static final int TAMANIO_MAXIMO = 50;

    private final VerPerfilService verPerfilService;
    private final PublicacionRepository publicacionRepository;
    private final ProyectoVisibilidadService proyectoVisibilidadService;
    private final PublicacionInteraccionResumenService interaccionResumenService;
    private final ColaboradorPublicacionOrigenService colaboradorOrigenService;

    public ListarPublicacionesPerfilService(
            VerPerfilService verPerfilService,
            PublicacionRepository publicacionRepository,
            ProyectoVisibilidadService proyectoVisibilidadService,
            PublicacionInteraccionResumenService interaccionResumenService,
            ColaboradorPublicacionOrigenService colaboradorOrigenService) {
        this.verPerfilService = verPerfilService;
        this.publicacionRepository = publicacionRepository;
        this.proyectoVisibilidadService = proyectoVisibilidadService;
        this.interaccionResumenService = interaccionResumenService;
        this.colaboradorOrigenService = colaboradorOrigenService;
    }

    @Transactional(readOnly = true)
    public PaginaResponse<PublicacionResponse> listar(
            Long idPerfil,
            Long idUsuarioAutenticado,
            Long idPerfilActivo,
            int pagina,
            int tamanio) {
        return listar(idPerfil, idUsuarioAutenticado, idPerfilActivo, null, pagina, tamanio);
    }

    /**
     * UC-22: listado con filtros opcionales. Sin filtros mantiene el mismo
     * comportamiento que el listado simple del portfolio.
     */
    @Transactional(readOnly = true)
    public PaginaResponse<PublicacionResponse> listar(
            Long idPerfil,
            Long idUsuarioAutenticado,
            Long idPerfilActivo,
            BuscarPublicacionesFiltro filtro,
            int pagina,
            int tamanio) {

        validarRangoFechas(filtro);

        // Aplica las reglas de visibilidad de UC-14: perfil activo, usuario
        // activo y perfil propio; lanza 404 si el perfil no es visible.
        verPerfilService.obtenerPerfilVisible(idPerfil, idUsuarioAutenticado);

        Sort orden = Sort.by(
                Sort.Order.desc("fechaPublicacion"),
                Sort.Order.desc("idPublicacion"));

        Pageable pageable = PageRequest.of(
                Math.max(pagina, 0),
                Math.min(Math.max(tamanio, 1), TAMANIO_MAXIMO),
                orden);

        Page<Publicacion> publicaciones;
        if (filtro == null || filtro.estaVacio()) {
            publicaciones = publicacionRepository
                    .findByPerfilIdPerfilAndEstadoOrderByFechaPublicacionDesc(
                            idPerfil, EstadoPublicacion.ACTIVA, pageable);
        } else {
            publicaciones = publicacionRepository.findAll(
                    PublicacionSpecifications.conFiltros(idPerfil, filtro),
                    pageable);
        }

        Map<Long, ResumenInteraccionPublicacion> resumenes =
                interaccionResumenService.resumir(
                        publicaciones.getContent().stream()
                                .map(Publicacion::getIdPublicacion)
                                .toList(),
                        idPerfilActivo);

        Map<Long, Map<Long, OrigenColaboracion>> origenes =
                colaboradorOrigenService.obtenerOrigenes(
                        publicaciones.getContent().stream()
                                .map(Publicacion::getIdPublicacion)
                                .toList());

        Page<PublicacionResponse> respuesta = publicaciones.map(publicacion -> {
            boolean puedeVerProyecto = publicacion.getProyecto() != null
                    && proyectoVisibilidadService.puedeVer(publicacion.getProyecto(), idPerfilActivo);
            return PublicacionMapper.toResponse(
                    publicacion,
                    puedeVerProyecto,
                    resumenes.getOrDefault(
                            publicacion.getIdPublicacion(),
                            ResumenInteraccionPublicacion.vacio()),
                    origenes.getOrDefault(publicacion.getIdPublicacion(), Map.of()));
        });

        return PaginaResponse.fromPage(respuesta);
    }

    private void validarRangoFechas(BuscarPublicacionesFiltro filtro) {
        if (filtro != null
                && filtro.fechaDesde() != null
                && filtro.fechaHasta() != null
                && filtro.fechaDesde().isAfter(filtro.fechaHasta())) {
            throw new RangoFechasPublicacionInvalidoException();
        }
    }
}
