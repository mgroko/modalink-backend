package org.mgroko.backend.perfiles.servicio;

import java.util.List;

import org.mgroko.backend.modelo.CalificacionPerfil;
import org.mgroko.backend.perfiles.dto.DistribucionResenasResponse;
import org.mgroko.backend.perfiles.dto.ResenaPerfilResponse;
import org.mgroko.backend.perfiles.dto.ResenasPerfilResponse;
import org.mgroko.backend.perfiles.dto.ResumenResenasResponse;
import org.mgroko.backend.perfiles.dto.ResumenResenasRow;
import org.mgroko.backend.perfiles.exception.ResenaNoEncontradaException;
import org.mgroko.backend.perfiles.mapper.ResenaPerfilMapper;
import org.mgroko.backend.repositorio.CalificacionPerfilRepository;
import org.mgroko.backend.common.dto.PaginaResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consulta de reseñas recibidas por un perfil (solo lectura).
 *
 * <p>La escritura de reseñas sigue perteneciendo al flujo de finalización de
 * proyecto (UC-37). Este servicio expone el listado y el detalle para la
 * ficha pública del perfil.</p>
 */
@Service
public class ConsultarResenasPerfilService {

    private static final int TAMANO_DEFAULT = 10;
    private static final int TAMANO_MAXIMO = 50;

    private final CalificacionPerfilRepository calificacionPerfilRepository;
    private final VerPerfilService verPerfilService;

    public ConsultarResenasPerfilService(
            CalificacionPerfilRepository calificacionPerfilRepository,
            VerPerfilService verPerfilService) {
        this.calificacionPerfilRepository = calificacionPerfilRepository;
        this.verPerfilService = verPerfilService;
    }

    @Transactional(readOnly = true)
    public ResenasPerfilResponse listar(
            Long idPerfil,
            Long idUsuarioAutenticado,
            int page,
            int size) {

        verPerfilService.obtenerPerfilVisible(idPerfil, idUsuarioAutenticado);

        int pagina = Math.max(0, page);
        int tamano = size <= 0 ? TAMANO_DEFAULT : Math.min(size, TAMANO_MAXIMO);

        Pageable pageable = PageRequest.of(
                pagina,
                tamano,
                Sort.by(
                        Sort.Order.desc("fechaRegistro"),
                        Sort.Order.desc("idCalificacion")));

        Page<CalificacionPerfil> paginaEntidades =
                calificacionPerfilRepository.findByPerfilReceptorIdPerfil(
                        idPerfil, pageable);

        Page<ResenaPerfilResponse> paginaResponse =
                paginaEntidades.map(ResenaPerfilMapper::toResponse);

        ResumenResenasResponse resumen = construirResumen(
                calificacionPerfilRepository.resumirPorPerfilReceptor(idPerfil));

        return new ResenasPerfilResponse(
                idPerfil,
                resumen,
                PaginaResponse.fromPage(paginaResponse));
    }

    @Transactional(readOnly = true)
    public ResenaPerfilResponse obtener(
            Long idPerfil,
            Long idCalificacion,
            Long idUsuarioAutenticado) {

        verPerfilService.obtenerPerfilVisible(idPerfil, idUsuarioAutenticado);

        CalificacionPerfil calificacion = calificacionPerfilRepository
                .findByIdCalificacionAndPerfilReceptorIdPerfil(
                        idCalificacion, idPerfil)
                .orElseThrow(ResenaNoEncontradaException::new);

        return ResenaPerfilMapper.toResponse(calificacion);
    }

    private ResumenResenasResponse construirResumen(ResumenResenasRow row) {
        long total = row != null && row.total() != null ? row.total() : 0L;
        double promedio = row != null && row.promedio() != null ? row.promedio() : 0.0;

        List<DistribucionResenasResponse> distribucion = List.of(
                new DistribucionResenasResponse(5, valor(row != null ? row.cinco() : null)),
                new DistribucionResenasResponse(4, valor(row != null ? row.cuatro() : null)),
                new DistribucionResenasResponse(3, valor(row != null ? row.tres() : null)),
                new DistribucionResenasResponse(2, valor(row != null ? row.dos() : null)),
                new DistribucionResenasResponse(1, valor(row != null ? row.uno() : null)));

        double promedioRedondeado = Math.round(promedio * 100.0) / 100.0;

        return new ResumenResenasResponse(promedioRedondeado, total, distribucion);
    }

    private long valor(Long cantidad) {
        return cantidad != null ? cantidad : 0L;
    }
}
