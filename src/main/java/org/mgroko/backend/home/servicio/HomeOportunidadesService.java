package org.mgroko.backend.home.servicio;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.perfiles.exception.PerfilNoEncontradoException;
import org.mgroko.backend.requerimientos.dto.BuscarRequerimientosGlobalFiltro;
import org.mgroko.backend.requerimientos.dto.OportunidadesHomeResponse;
import org.mgroko.backend.requerimientos.dto.RequerimientoOportunidadResponse;
import org.mgroko.backend.requerimientos.dto.TipoRequerimiento;
import org.mgroko.backend.requerimientos.servicio.BuscarRequerimientosActividadGlobalService;
import org.mgroko.backend.requerimientos.servicio.BuscarRequerimientosGeneralesService;
import org.springframework.stereotype.Service;

/**
 * Bloque de oportunidades del Home.
 *
 * <p>Reutiliza los buscadores globales con filtros pensados para que las
 * tarjetas sean accionables: profesión del perfil y sólo postulables. Las
 * postulaciones previas se muestran con su estado y se ordenan después de las
 * oportunidades todavía no postuladas.</p>
 */
@Service
public class HomeOportunidadesService {

    private static final int LIMITE_DEFAULT = 5;
    private static final int LIMITE_MAXIMO = 20;

    private final BuscarRequerimientosGeneralesService buscarGeneralesService;
    private final BuscarRequerimientosActividadGlobalService buscarActividadService;

    public HomeOportunidadesService(
            BuscarRequerimientosGeneralesService buscarGeneralesService,
            BuscarRequerimientosActividadGlobalService buscarActividadService) {
        this.buscarGeneralesService = buscarGeneralesService;
        this.buscarActividadService = buscarActividadService;
    }

    public OportunidadesHomeResponse obtener(Long idPerfilActivo, int limite) {
        int limiteAjustado = ajustarLimite(limite);

        if (idPerfilActivo == null) {
            return OportunidadesHomeResponse.vacio(limiteAjustado);
        }

        BuscarRequerimientosGlobalFiltro filtro = new BuscarRequerimientosGlobalFiltro(
                null, null, null, null, null, null, null,
                Boolean.TRUE, Boolean.TRUE, Boolean.FALSE);

        PaginaResponse<RequerimientoOportunidadResponse> generales;
        PaginaResponse<RequerimientoOportunidadResponse> actividades;

        try {
            generales = buscarGeneralesService.buscar(
                    idPerfilActivo, filtro, 0, limiteAjustado, false);
            actividades = buscarActividadService.buscar(
                    idPerfilActivo, filtro, 0, limiteAjustado, false);
        } catch (PerfilEnBajaException | PerfilNoEncontradoException ex) {
            return OportunidadesHomeResponse.vacio(limiteAjustado);
        }

        List<RequerimientoOportunidadResponse> mezcla = new ArrayList<>();
        mezcla.addAll(generales.contenido());
        mezcla.addAll(actividades.contenido());
        mezcla.sort(comparadorOportunidades());

        List<RequerimientoOportunidadResponse> seleccion = mezcla.stream()
                .limit(limiteAjustado)
                .toList();

        return new OportunidadesHomeResponse(
                seleccion,
                generales.totalElementos(),
                actividades.totalElementos(),
                limiteAjustado);
    }

    private int ajustarLimite(int limite) {
        if (limite <= 0) {
            return LIMITE_DEFAULT;
        }
        return Math.min(limite, LIMITE_MAXIMO);
    }

    private Comparator<RequerimientoOportunidadResponse> comparadorOportunidades() {
        return Comparator
                .comparing((RequerimientoOportunidadResponse oportunidad) ->
                        Boolean.TRUE.equals(oportunidad.yaPostulado()))
                .thenComparing(this::fechaReferencia,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(
                        oportunidad -> oportunidad.faltantes() != null
                                ? oportunidad.faltantes()
                                : 0,
                        Comparator.reverseOrder())
                .thenComparing(RequerimientoOportunidadResponse::tipo)
                .thenComparing(RequerimientoOportunidadResponse::idRequerimiento);
    }

    private LocalDateTime fechaReferencia(RequerimientoOportunidadResponse oportunidad) {
        return oportunidad.tipo() == TipoRequerimiento.ACTIVIDAD
                ? oportunidad.fechaHoraInicioActividad()
                : oportunidad.fechaInicioProyecto();
    }
}
