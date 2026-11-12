package org.mgroko.backend.auditoria.servicio;

import java.time.LocalDate;
import java.util.List;

import org.mgroko.backend.auditoria.dto.AuditoriaFiltroResuelto;
import org.mgroko.backend.auditoria.dto.AuditoriaResumenEntidadResponse;
import org.mgroko.backend.auditoria.dto.AuditoriaResumenResponse;
import org.mgroko.backend.auditoria.dto.AuditoriaResumenTipoResponse;
import org.mgroko.backend.auditoria.especificacion.AuditoriaSpecifications;
import org.mgroko.backend.modelo.TipoEventoAuditoria;
import org.mgroko.backend.repositorio.AuditoriaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC-67: resumen agregado por tipo de evento y por entidad afectada.
 */
@Service
public class AuditoriaResumenService {

    public static final String SIN_TIPO = "SIN_TIPO";
    public static final String SIN_TIPO_NOMBRE = "Sin tipo (histórico)";

    private final AuditoriaRepository auditoriaRepository;
    private final AuditoriaFiltroService auditoriaFiltroService;

    public AuditoriaResumenService(
            AuditoriaRepository auditoriaRepository,
            AuditoriaFiltroService auditoriaFiltroService) {
        this.auditoriaRepository = auditoriaRepository;
        this.auditoriaFiltroService = auditoriaFiltroService;
    }

    @Transactional(readOnly = true)
    public AuditoriaResumenResponse obtenerResumen(
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            List<String> tiposEvento,
            String operador,
            String texto) {

        AuditoriaFiltroResuelto filtro =
                auditoriaFiltroService.resolver(
                        fechaDesde, fechaHasta, tiposEvento, operador, texto);

        long total = auditoriaRepository.count(AuditoriaSpecifications.conFiltros(
                filtro.fechaDesde(), filtro.fechaHasta(), filtro.tipos(),
                filtro.operador(), filtro.texto()));

        List<AuditoriaResumenTipoResponse> porTipo = auditoriaRepository
                .contarPorTipo(
                        filtro.fechaDesde(), filtro.fechaHasta(), filtro.tipos(),
                        filtro.operador(), filtro.texto())
                .stream()
                .map(fila -> {
                    String codigo = (String) fila[0];
                    long cantidad = ((Number) fila[1]).longValue();
                    return new AuditoriaResumenTipoResponse(
                            codigo == null ? SIN_TIPO : codigo,
                            nombreTipo(codigo, filtro),
                            cantidad);
                })
                .toList();

        List<AuditoriaResumenEntidadResponse> porEntidad = auditoriaRepository
                .contarPorEntidad(
                        filtro.fechaDesde(), filtro.fechaHasta(), filtro.tipos(),
                        filtro.operador(), filtro.texto())
                .stream()
                .map(fila -> new AuditoriaResumenEntidadResponse(
                        (String) fila[0],
                        ((Number) fila[1]).longValue()))
                .toList();

        return new AuditoriaResumenResponse(
                fechaDesde,
                fechaHasta,
                filtro.tiposAplicados(),
                total,
                porTipo,
                porEntidad);
    }

    private String nombreTipo(String codigo, AuditoriaFiltroResuelto filtro) {
        if (codigo == null) {
            return SIN_TIPO_NOMBRE;
        }
        TipoEventoAuditoria tipo = filtro.catalogo().get(codigo);
        return tipo != null ? tipo.getNombre() : codigo;
    }
}
