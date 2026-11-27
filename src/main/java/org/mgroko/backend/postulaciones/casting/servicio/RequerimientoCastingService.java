package org.mgroko.backend.postulaciones.casting.servicio;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.mgroko.backend.modelo.CaracteristicaTecnica;
import org.mgroko.backend.modelo.Habilidad;
import org.mgroko.backend.modelo.RequerimientoActCaract;
import org.mgroko.backend.modelo.RequerimientoActCaractValor;
import org.mgroko.backend.modelo.RequerimientoActividad;
import org.mgroko.backend.modelo.RequerimientoGralCaract;
import org.mgroko.backend.modelo.RequerimientoGralCaractValor;
import org.mgroko.backend.modelo.RequerimientoGralProyecto;
import org.mgroko.backend.modelo.ValorCaracteristica;
import org.mgroko.backend.postulaciones.casting.dto.CaracteristicaRequeridaCastingResponse;
import org.mgroko.backend.postulaciones.casting.dto.HabilidadCastingResponse;
import org.mgroko.backend.postulaciones.casting.dto.RequerimientoCastingResponse;
import org.mgroko.backend.postulaciones.casting.dto.ValorRequeridoCastingResponse;
import org.mgroko.backend.postulaciones.dto.TipoPostulacion;
import org.mgroko.backend.postulaciones.exception.RequerimientoNoEncontradoException;
import org.mgroko.backend.proyectos.dto.CupoRequerimiento;
import org.mgroko.backend.proyectos.servicio.CupoRequerimientoService;
import org.mgroko.backend.repositorio.RequerimientoActCaractValorRepository;
import org.mgroko.backend.repositorio.RequerimientoActividadRepository;
import org.mgroko.backend.repositorio.RequerimientoGralCaractValorRepository;
import org.mgroko.backend.repositorio.RequerimientoGralProyectoRepository;
import org.mgroko.backend.repositorio.ValorCaracteristicaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Arma la cabecera de un tablero de casting: requerimiento, cupo,
 * características pedidas, habilidades y datos de la actividad.
 */
@Service
public class RequerimientoCastingService {

    private final RequerimientoGralProyectoRepository requerimientoGralRepository;
    private final RequerimientoActividadRepository requerimientoActividadRepository;
    private final RequerimientoGralCaractValorRepository gralCaractValorRepository;
    private final RequerimientoActCaractValorRepository actCaractValorRepository;
    private final ValorCaracteristicaRepository valorCaracteristicaRepository;
    private final CupoRequerimientoService cupoRequerimientoService;

    public RequerimientoCastingService(
            RequerimientoGralProyectoRepository requerimientoGralRepository,
            RequerimientoActividadRepository requerimientoActividadRepository,
            RequerimientoGralCaractValorRepository gralCaractValorRepository,
            RequerimientoActCaractValorRepository actCaractValorRepository,
            ValorCaracteristicaRepository valorCaracteristicaRepository,
            CupoRequerimientoService cupoRequerimientoService) {
        this.requerimientoGralRepository = requerimientoGralRepository;
        this.requerimientoActividadRepository = requerimientoActividadRepository;
        this.gralCaractValorRepository = gralCaractValorRepository;
        this.actCaractValorRepository = actCaractValorRepository;
        this.valorCaracteristicaRepository = valorCaracteristicaRepository;
        this.cupoRequerimientoService = cupoRequerimientoService;
    }

    @Transactional(readOnly = true)
    public RequerimientoCastingResponse cargar(
            Long idProyecto,
            TipoPostulacion tipo,
            Long idRequerimiento,
            long postulacionesPendientes) {

        if (tipo == null || idRequerimiento == null) {
            throw new RequerimientoNoEncontradoException(
                    "El requerimiento indicado no existe o no pertenece al proyecto.");
        }

        return tipo == TipoPostulacion.GENERAL
                ? cargarGeneral(idProyecto, idRequerimiento, postulacionesPendientes)
                : cargarActividad(idProyecto, idRequerimiento, postulacionesPendientes);
    }

    private RequerimientoCastingResponse cargarGeneral(
            Long idProyecto,
            Long idRequerimiento,
            long postulacionesPendientes) {

        RequerimientoGralProyecto requerimiento = requerimientoGralRepository
                .findByIdConDetalleCasting(idRequerimiento)
                .orElseThrow(() -> new RequerimientoNoEncontradoException(
                        "El requerimiento general " + idRequerimiento + " no existe."));

        if (requerimiento.getProyecto() == null
                || !Objects.equals(requerimiento.getProyecto().getIdProyecto(), idProyecto)) {
            throw new RequerimientoNoEncontradoException(
                    "El requerimiento no pertenece al proyecto indicado.");
        }

        CupoRequerimiento cupo = cupoRequerimientoService.calcularGeneral(requerimiento);

        List<RequerimientoGralCaract> filas = new ArrayList<>(requerimiento.getCaracteristicas());
        filas.sort(Comparator.comparing(
                fila -> fila.getCaracteristica().getCodigo(),
                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));

        Map<Long, List<Long>> valoresPorFila = valoresGenerales(filas);
        Map<Long, Map<Long, ValorCaracteristica>> catalogo =
                catalogoValores(filas.stream()
                        .map(fila -> fila.getCaracteristica().getIdCaracteristica())
                        .toList());

        List<CaracteristicaRequeridaCastingResponse> caracteristicas = new ArrayList<>();
        for (RequerimientoGralCaract fila : filas) {
            CaracteristicaTecnica caracteristica = fila.getCaracteristica();
            List<Long> idsValores = valoresPorFila.getOrDefault(
                    fila.getIdReqGralCaract(), List.of());
            caracteristicas.add(toCaracteristica(
                    caracteristica,
                    fila.getValorMin(),
                    fila.getValorMax(),
                    idsValores,
                    catalogo));
        }

        return new RequerimientoCastingResponse(
                TipoPostulacion.GENERAL,
                requerimiento.getIdRequerimientoGral(),
                null,
                null,
                requerimiento.getDescripcion(),
                requerimiento.getProfesion().getIdProfesion(),
                requerimiento.getProfesion().getCodigo(),
                requerimiento.getProfesion().getNombre(),
                requerimiento.getCantidad(),
                cupo.cubiertos(),
                cupo.faltantes(),
                cupo.satisfecho(),
                cupo.satisfecho(),
                postulacionesPendientes,
                false,
                null,
                null,
                null,
                caracteristicas,
                habilidades(requerimiento.getHabilidades()));
    }

    private RequerimientoCastingResponse cargarActividad(
            Long idProyecto,
            Long idRequerimiento,
            long postulacionesPendientes) {

        RequerimientoActividad requerimiento = requerimientoActividadRepository
                .findByIdConDetalleCasting(idRequerimiento)
                .orElseThrow(() -> new RequerimientoNoEncontradoException(
                        "El requerimiento de actividad " + idRequerimiento + " no existe."));

        if (requerimiento.getActividad() == null
                || requerimiento.getActividad().getPlanificacion() == null
                || requerimiento.getActividad().getPlanificacion().getProyecto() == null
                || !Objects.equals(
                        requerimiento.getActividad().getPlanificacion()
                                .getProyecto().getIdProyecto(),
                        idProyecto)) {
            throw new RequerimientoNoEncontradoException(
                    "El requerimiento no pertenece al proyecto indicado.");
        }

        CupoRequerimiento cupo = cupoRequerimientoService.calcularActividad(requerimiento);

        List<RequerimientoActCaract> filas = new ArrayList<>(requerimiento.getCaracteristicas());
        filas.sort(Comparator.comparing(
                fila -> fila.getCaracteristica().getCodigo(),
                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));

        Map<Long, List<Long>> valoresPorFila = valoresActividad(filas);
        Map<Long, Map<Long, ValorCaracteristica>> catalogo =
                catalogoValores(filas.stream()
                        .map(fila -> fila.getCaracteristica().getIdCaracteristica())
                        .toList());

        List<CaracteristicaRequeridaCastingResponse> caracteristicas = new ArrayList<>();
        for (RequerimientoActCaract fila : filas) {
            CaracteristicaTecnica caracteristica = fila.getCaracteristica();
            List<Long> idsValores = valoresPorFila.getOrDefault(
                    fila.getIdReqActCaract(), List.of());
            caracteristicas.add(toCaracteristica(
                    caracteristica,
                    fila.getValorMin(),
                    fila.getValorMax(),
                    idsValores,
                    catalogo));
        }

        return new RequerimientoCastingResponse(
                TipoPostulacion.ACTIVIDAD,
                requerimiento.getIdRequerimiento(),
                requerimiento.getActividad().getIdActividad(),
                requerimiento.getActividad().getNombre(),
                requerimiento.getDescripcion(),
                requerimiento.getProfesion().getIdProfesion(),
                requerimiento.getProfesion().getCodigo(),
                requerimiento.getProfesion().getNombre(),
                requerimiento.getCantidad(),
                cupo.cubiertos(),
                cupo.faltantes(),
                cupo.satisfecho(),
                cupo.satisfecho(),
                postulacionesPendientes,
                true,
                requerimiento.getActividad().getFechaHoraInicio(),
                requerimiento.getActividad().getFechaHoraFin(),
                requerimiento.getActividad().getDuracionMinutos(),
                caracteristicas,
                habilidades(requerimiento.getHabilidades()));
    }

    private Map<Long, List<Long>> valoresGenerales(List<RequerimientoGralCaract> filas) {
        List<Long> ids = filas.stream()
                .map(RequerimientoGralCaract::getIdReqGralCaract)
                .filter(Objects::nonNull)
                .toList();
        if (ids.isEmpty()) {
            return Map.of();
        }

        Map<Long, List<Long>> valores = new HashMap<>();
        for (RequerimientoGralCaractValor valor
                : gralCaractValorRepository.findByIdReqGralCaractIn(ids)) {
            valores.computeIfAbsent(valor.getIdReqGralCaract(), id -> new ArrayList<>())
                    .add(valor.getIdValor());
        }
        valores.values().forEach(lista -> lista.sort(Comparator.naturalOrder()));
        return valores;
    }

    private Map<Long, List<Long>> valoresActividad(List<RequerimientoActCaract> filas) {
        List<Long> ids = filas.stream()
                .map(RequerimientoActCaract::getIdReqActCaract)
                .filter(Objects::nonNull)
                .toList();
        if (ids.isEmpty()) {
            return Map.of();
        }

        Map<Long, List<Long>> valores = new HashMap<>();
        for (RequerimientoActCaractValor valor
                : actCaractValorRepository.findByIdReqActCaractIn(ids)) {
            valores.computeIfAbsent(valor.getIdReqActCaract(), id -> new ArrayList<>())
                    .add(valor.getIdValor());
        }
        valores.values().forEach(lista -> lista.sort(Comparator.naturalOrder()));
        return valores;
    }

    private Map<Long, Map<Long, ValorCaracteristica>> catalogoValores(
            List<Long> idsCaracteristica) {

        List<Long> ids = idsCaracteristica.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return Map.of();
        }

        Map<Long, Map<Long, ValorCaracteristica>> catalogo = new HashMap<>();
        for (ValorCaracteristica valor
                : valorCaracteristicaRepository
                        .findByCaracteristicaTecnica_IdCaracteristicaIn(ids)) {
            if (valor.getCaracteristicaTecnica() == null
                    || valor.getCaracteristicaTecnica().getIdCaracteristica() == null
                    || valor.getIdValor() == null) {
                continue;
            }
            catalogo.computeIfAbsent(
                    valor.getCaracteristicaTecnica().getIdCaracteristica(),
                    id -> new LinkedHashMap<>())
                    .put(valor.getIdValor(), valor);
        }
        return catalogo;
    }

    private CaracteristicaRequeridaCastingResponse toCaracteristica(
            CaracteristicaTecnica caracteristica,
            java.math.BigDecimal valorMin,
            java.math.BigDecimal valorMax,
            List<Long> idsValores,
            Map<Long, Map<Long, ValorCaracteristica>> catalogo) {

        Map<Long, ValorCaracteristica> valoresCaracteristica = catalogo.getOrDefault(
                caracteristica.getIdCaracteristica(), Map.of());

        List<ValorRequeridoCastingResponse> valores = idsValores.stream()
                .map(valoresCaracteristica::get)
                .filter(Objects::nonNull)
                .map(valor -> new ValorRequeridoCastingResponse(
                        valor.getIdValor(),
                        valor.getEtiqueta(),
                        valor.getColorHex()))
                .toList();

        return new CaracteristicaRequeridaCastingResponse(
                caracteristica.getIdCaracteristica(),
                caracteristica.getCodigo(),
                caracteristica.getNombre(),
                caracteristica.getTipoDato(),
                valorMin,
                valorMax,
                unidad(caracteristica),
                valores);
    }

    private List<HabilidadCastingResponse> habilidades(java.util.Set<Habilidad> habilidades) {
        return habilidades.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(
                        Habilidad::getNombre,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .map(habilidad -> new HabilidadCastingResponse(
                        habilidad.getIdHabilidad(),
                        habilidad.getNombre()))
                .toList();
    }

    private String unidad(CaracteristicaTecnica caracteristica) {
        if (caracteristica.getUnidadMedida() == null) {
            return null;
        }
        String simbolo = caracteristica.getUnidadMedida().getSimbolo();
        return simbolo != null && !simbolo.isBlank()
                ? simbolo
                : caracteristica.getUnidadMedida().getNombre();
    }
}
