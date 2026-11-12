package org.mgroko.backend.publicaciones.servicio;

import java.util.List;

import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.modelo.ColaboracionAutomatica;
import org.mgroko.backend.modelo.enums.EstadoColaboracionAutomatica;
import org.mgroko.backend.publicaciones.dto.BuscarColaboracionesAutomaticasFiltro;
import org.mgroko.backend.publicaciones.dto.ColaboracionAutomaticaResponse;
import org.mgroko.backend.publicaciones.especificacion.ColaboracionAutomaticaSpecifications;
import org.mgroko.backend.publicaciones.exception.FiltroSolicitudColaboracionInvalidoException;
import org.mgroko.backend.publicaciones.mapper.ColaboracionAutomaticaMapper;
import org.mgroko.backend.publicaciones.exception.ColaboracionAutomaticaNoEncontradaException;
import org.mgroko.backend.repositorio.ColaboracionAutomaticaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Listado y detalle de colaboraciones automáticas del perfil activo.
 */
@Service
public class BuscarColaboracionAutomaticaService {

    private static final int TAMANO_PAGINA_POR_DEFECTO = 20;
    private static final int TAMANO_PAGINA_MAXIMO = 100;

    private final ColaboracionAutomaticaRepository colaboracionRepository;

    public BuscarColaboracionAutomaticaService(
            ColaboracionAutomaticaRepository colaboracionRepository) {
        this.colaboracionRepository = colaboracionRepository;
    }

    @Transactional(readOnly = true)
    public PaginaResponse<ColaboracionAutomaticaResponse> buscar(
            Long idPerfilActivo,
            BuscarColaboracionesAutomaticasFiltro filtro,
            int page,
            int size) {

        validarFechas(filtro);
        EstadoColaboracionAutomatica estado = resolverEstado(filtro);

        Pageable pageable = PageRequest.of(
                Math.max(0, page),
                normalizarTamano(size),
                Sort.by(
                        Sort.Order.desc("fechaDeteccion"),
                        Sort.Order.desc("idColaboracionAutomatica")));

        Page<ColaboracionAutomatica> pagina = colaboracionRepository.findAll(
                ColaboracionAutomaticaSpecifications.conFiltros(
                        idPerfilActivo, filtro, estado),
                pageable);

        List<ColaboracionAutomaticaResponse> contenido = pagina.getContent().stream()
                .map(colaboracion -> ColaboracionAutomaticaMapper.toResponse(
                        colaboracion, idPerfilActivo))
                .toList();

        return PaginaResponse.fromPage(
                new PageImpl<>(contenido, pageable, pagina.getTotalElements()));
    }

    @Transactional(readOnly = true)
    public ColaboracionAutomaticaResponse obtener(
            Long idPerfilActivo,
            Long idColaboracionAutomatica) {

        ColaboracionAutomatica colaboracion = colaboracionRepository
                .findByIdConDetalle(idColaboracionAutomatica)
                .orElseThrow(ColaboracionAutomaticaNoEncontradaException::new);

        boolean participa = colaboracion.getPerfilA().getIdPerfil().equals(idPerfilActivo)
                || colaboracion.getPerfilB().getIdPerfil().equals(idPerfilActivo);

        if (!participa) {
            throw new ColaboracionAutomaticaNoEncontradaException();
        }

        return ColaboracionAutomaticaMapper.toResponse(colaboracion, idPerfilActivo);
    }

    private EstadoColaboracionAutomatica resolverEstado(
            BuscarColaboracionesAutomaticasFiltro filtro) {

        if (filtro == null) {
            return EstadoColaboracionAutomatica.PENDIENTE;
        }

        if (Boolean.TRUE.equals(filtro.todosLosEstados())) {
            if (filtro.estado() != null) {
                throw new FiltroSolicitudColaboracionInvalidoException(
                        "No se puede combinar todosLosEstados con un estado específico.");
            }
            return null;
        }

        return filtro.estado() != null
                ? filtro.estado()
                : EstadoColaboracionAutomatica.PENDIENTE;
    }

    private void validarFechas(BuscarColaboracionesAutomaticasFiltro filtro) {
        if (filtro != null && filtro.fechaDesde() != null
                && filtro.fechaHasta() != null
                && filtro.fechaDesde().isAfter(filtro.fechaHasta())) {
            throw new FiltroSolicitudColaboracionInvalidoException(
                    "El rango de fechas es inválido.");
        }
    }

    private int normalizarTamano(int size) {
        if (size <= 0) {
            return TAMANO_PAGINA_POR_DEFECTO;
        }
        return Math.min(size, TAMANO_PAGINA_MAXIMO);
    }
}
