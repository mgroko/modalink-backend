package org.mgroko.backend.actividades.servicio;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mgroko.backend.actividades.dto.CrearRequerimientoActividadRequest;
import org.mgroko.backend.actividades.dto.RequerimientoActividadResponse;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.CaracteristicaTecnica;
import org.mgroko.backend.modelo.Habilidad;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.RequerimientoActCaract;
import org.mgroko.backend.modelo.RequerimientoActCaractValor;
import org.mgroko.backend.modelo.RequerimientoActividad;
import org.mgroko.backend.modelo.ValorCaracteristicaId;
import org.mgroko.backend.perfiles.exception.CaracteristicaNoEncontradaException;
import org.mgroko.backend.perfiles.exception.CaracteristicaProfesionNoCoincideException;
import org.mgroko.backend.perfiles.exception.ProfesionNoEncontradaException;
import org.mgroko.backend.perfiles.exception.ValorCaracteristicaNoEncontradoException;
import org.mgroko.backend.proyectos.dto.CaracteristicaRequerimientoResponse;
import org.mgroko.backend.proyectos.dto.CrearRequerimientoCaractRequest;
import org.mgroko.backend.proyectos.dto.CupoRequerimiento;
import org.mgroko.backend.proyectos.exception.HabilidadNoEncontradaException;
import org.mgroko.backend.proyectos.exception.RequerimientoInvalidoException;
import org.mgroko.backend.proyectos.servicio.CupoRequerimientoService;
import org.mgroko.backend.repositorio.CaracteristicaTecnicaRepository;
import org.mgroko.backend.repositorio.HabilidadRepository;
import org.mgroko.backend.repositorio.ProfesionRepository;
import org.mgroko.backend.repositorio.RequerimientoActCaractValorRepository;
import org.mgroko.backend.repositorio.RequerimientoActividadRepository;
import org.mgroko.backend.repositorio.ValorCaracteristicaRepository;
import org.springframework.stereotype.Service;

@Service
public class RequerimientoActividadService {

    private final ProfesionRepository profesionRepository;
    private final CaracteristicaTecnicaRepository caracteristicaTecnicaRepository;
    private final ValorCaracteristicaRepository valorCaracteristicaRepository;
    private final HabilidadRepository habilidadRepository;
    private final RequerimientoActividadRepository requerimientoActividadRepository;
    private final RequerimientoActCaractValorRepository requerimientoActCaractValorRepository;
    private final CupoRequerimientoService cupoRequerimientoService;

    public RequerimientoActividadService(
            ProfesionRepository profesionRepository,
            CaracteristicaTecnicaRepository caracteristicaTecnicaRepository,
            ValorCaracteristicaRepository valorCaracteristicaRepository,
            HabilidadRepository habilidadRepository,
            RequerimientoActividadRepository requerimientoActividadRepository,
            RequerimientoActCaractValorRepository requerimientoActCaractValorRepository,
            CupoRequerimientoService cupoRequerimientoService) {
        this.profesionRepository = profesionRepository;
        this.caracteristicaTecnicaRepository = caracteristicaTecnicaRepository;
        this.valorCaracteristicaRepository = valorCaracteristicaRepository;
        this.habilidadRepository = habilidadRepository;
        this.requerimientoActividadRepository = requerimientoActividadRepository;
        this.requerimientoActCaractValorRepository = requerimientoActCaractValorRepository;
        this.cupoRequerimientoService = cupoRequerimientoService;
    }

    public RequerimientoActividadResponse persistir(
            Actividad actividad,
            CrearRequerimientoActividadRequest request) {

        Profesion profesion = profesionRepository.findById(request.idProfesion())
                .orElseThrow(() -> new ProfesionNoEncontradaException(
                        "Profesion no encontrada: " + request.idProfesion()));

        RequerimientoActividad requerimiento = RequerimientoActividad.builder()
                .actividad(actividad)
                .profesion(profesion)
                .cantidad(request.cantidad())
                .descripcion(request.descripcion() != null ? request.descripcion().trim() : null)
                .build();

        Map<Long, List<Long>> idsValoresPorCaracteristica = new HashMap<>();
        List<RequerimientoActCaract> caracteristicasEntidad = construirCaracteristicas(
                requerimiento,
                request.caracteristicas(),
                idsValoresPorCaracteristica);
        requerimiento.setCaracteristicas(caracteristicasEntidad);
        requerimiento.setHabilidades(resolverHabilidades(request.habilidades()));

        RequerimientoActividad guardado = requerimientoActividadRepository.save(requerimiento);

        persistirValoresCaracteristicas(caracteristicasEntidad, idsValoresPorCaracteristica);

        return toResponse(
                guardado,
                idsValoresPorCaracteristica,
                cupoRequerimientoService.calcularActividad(guardado),
                false);
    }

    public RequerimientoActividadResponse actualizar(
            RequerimientoActividad requerimiento,
            CrearRequerimientoActividadRequest request) {

        Profesion profesion = profesionRepository.findById(request.idProfesion())
                .orElseThrow(() -> new ProfesionNoEncontradaException(
                        "Profesion no encontrada: " + request.idProfesion()));

        requerimiento.setProfesion(profesion);
        requerimiento.setCantidad(request.cantidad());
        requerimiento.setDescripcion(
                request.descripcion() != null ? request.descripcion().trim() : null);

        for (RequerimientoActCaract caracteristica : requerimiento.getCaracteristicas()) {
            requerimientoActCaractValorRepository
                    .deleteByIdReqActCaract(caracteristica.getIdReqActCaract());
        }
        requerimiento.getCaracteristicas().clear();

        Map<Long, List<Long>> idsValoresPorCaracteristica = new HashMap<>();
        List<RequerimientoActCaract> caracteristicasEntidad = construirCaracteristicas(
                requerimiento,
                request.caracteristicas(),
                idsValoresPorCaracteristica);
        requerimiento.getCaracteristicas().addAll(caracteristicasEntidad);

        requerimiento.getHabilidades().clear();
        requerimiento.getHabilidades().addAll(resolverHabilidades(request.habilidades()));

        RequerimientoActividad guardado = requerimientoActividadRepository
                .saveAndFlush(requerimiento);

        persistirValoresCaracteristicas(caracteristicasEntidad, idsValoresPorCaracteristica);

        return toResponse(
                guardado,
                idsValoresPorCaracteristica,
                cupoRequerimientoService.calcularActividad(guardado),
                false);
    }

    public void eliminar(RequerimientoActividad requerimiento) {
        for (RequerimientoActCaract caracteristica : requerimiento.getCaracteristicas()) {
            requerimientoActCaractValorRepository
                    .deleteByIdReqActCaract(caracteristica.getIdReqActCaract());
        }
        requerimientoActividadRepository.delete(requerimiento);
    }

    /**
     * Lista los requerimientos para la vista de detalle/edición. Incluye el
     * cupo, pero {@code puedePostularse} siempre es {@code false}: la
     * elegibilidad del candidato se consulta en la búsqueda de actividades.
     */
    public List<RequerimientoActividadResponse> listarPorActividad(Long idActividad) {
        List<RequerimientoActividad> requerimientos =
                requerimientoActividadRepository.findByActividad_IdActividad(idActividad);

        if (requerimientos.isEmpty()) {
            return List.of();
        }

        Map<Long, CupoRequerimiento> cupos =
                cupoRequerimientoService.calcularActividades(requerimientos);

        List<Long> idsCaracteristicas = requerimientos.stream()
                .flatMap(requerimiento -> requerimiento.getCaracteristicas().stream())
                .map(RequerimientoActCaract::getIdReqActCaract)
                .toList();

        Map<Long, List<RequerimientoActCaractValor>> valoresPorCaracteristica =
                new HashMap<>();
        if (!idsCaracteristicas.isEmpty()) {
            for (RequerimientoActCaractValor valor : requerimientoActCaractValorRepository
                    .findByIdReqActCaractIn(idsCaracteristicas)) {
                valoresPorCaracteristica
                        .computeIfAbsent(valor.getIdReqActCaract(), id -> new ArrayList<>())
                        .add(valor);
            }
        }

        List<RequerimientoActividadResponse> resultado = new ArrayList<>();
        for (RequerimientoActividad requerimiento : requerimientos) {
            Map<Long, List<Long>> idsValoresPorCaracteristica = new HashMap<>();
            for (RequerimientoActCaract caracteristica : requerimiento.getCaracteristicas()) {
                List<Long> idsValores = valoresPorCaracteristica
                        .getOrDefault(caracteristica.getIdReqActCaract(), List.of())
                        .stream()
                        .map(RequerimientoActCaractValor::getIdValor)
                        .toList();
                idsValoresPorCaracteristica.put(
                        caracteristica.getCaracteristica().getIdCaracteristica(),
                        idsValores);
            }
            CupoRequerimiento cupo = cupos.get(requerimiento.getIdRequerimiento());
            if (cupo == null) {
                int cantidad = requerimiento.getCantidad() != null
                        ? requerimiento.getCantidad()
                        : 0;
                cupo = new CupoRequerimiento(cantidad, 0, cantidad, false);
            }
            resultado.add(toResponse(
                    requerimiento,
                    idsValoresPorCaracteristica,
                    cupo,
                    false));
        }

        return resultado;
    }

    private RequerimientoActividadResponse toResponse(
            RequerimientoActividad guardado,
            Map<Long, List<Long>> idsValoresPorCaracteristica,
            CupoRequerimiento cupo,
            boolean puedePostularse) {

        List<CaracteristicaRequerimientoResponse> caracteristicas = new ArrayList<>();
        for (RequerimientoActCaract caracteristicaRequerimiento : guardado.getCaracteristicas()) {
            CaracteristicaTecnica caracteristica = caracteristicaRequerimiento.getCaracteristica();
            Long idCaracteristica = caracteristica.getIdCaracteristica();
            caracteristicas.add(new CaracteristicaRequerimientoResponse(
                    idCaracteristica,
                    caracteristica.getCodigo(),
                    caracteristica.getTipoDato(),
                    caracteristicaRequerimiento.getValorMin(),
                    caracteristicaRequerimiento.getValorMax(),
                    idsValoresPorCaracteristica.getOrDefault(idCaracteristica, List.of())));
        }

        List<Long> habilidades = guardado.getHabilidades().stream()
                .map(Habilidad::getIdHabilidad)
                .sorted()
                .toList();

        return new RequerimientoActividadResponse(
                guardado.getIdRequerimiento(),
                guardado.getCantidad(),
                guardado.getDescripcion(),
                guardado.getProfesion().getIdProfesion(),
                guardado.getProfesion().getNombre(),
                caracteristicas,
                habilidades,
                cupo.cubiertos(),
                cupo.faltantes(),
                cupo.satisfecho(),
                puedePostularse);
    }

    private List<RequerimientoActCaract> construirCaracteristicas(
            RequerimientoActividad requerimiento,
            List<CrearRequerimientoCaractRequest> caracteristicasRequest,
            Map<Long, List<Long>> idsValoresPorCaracteristica) {

        List<RequerimientoActCaract> resultado = new ArrayList<>();
        if (caracteristicasRequest == null || caracteristicasRequest.isEmpty()) {
            return resultado;
        }

        Set<Long> vistas = new HashSet<>();
        for (CrearRequerimientoCaractRequest caracteristicaRequest : caracteristicasRequest) {
            if (!vistas.add(caracteristicaRequest.idCaracteristica())) {
                throw new RequerimientoInvalidoException(
                        "La caracteristica tecnica " + caracteristicaRequest.idCaracteristica()
                                + " esta duplicada en el requerimiento.");
            }

            CaracteristicaTecnica caracteristica = caracteristicaTecnicaRepository
                    .findById(caracteristicaRequest.idCaracteristica())
                    .orElseThrow(() -> new CaracteristicaNoEncontradaException(
                            "Caracteristica tecnica no encontrada: "
                                    + caracteristicaRequest.idCaracteristica()));

            if (!caracteristica.getProfesion().getIdProfesion()
                    .equals(requerimiento.getProfesion().getIdProfesion())) {
                throw new CaracteristicaProfesionNoCoincideException(
                        "La caracteristica tecnica " + caracteristicaRequest.idCaracteristica()
                                + " no corresponde a la profesion del requerimiento.");
            }

            validarRangoYValores(caracteristica, caracteristicaRequest);

            if (caracteristicaRequest.valores() != null && !caracteristicaRequest.valores().isEmpty()) {
                idsValoresPorCaracteristica.put(
                        caracteristica.getIdCaracteristica(),
                        caracteristicaRequest.valores());
            }

            resultado.add(RequerimientoActCaract.builder()
                    .requerimientoActividad(requerimiento)
                    .caracteristica(caracteristica)
                    .valorMin(caracteristicaRequest.valorMin())
                    .valorMax(caracteristicaRequest.valorMax())
                    .build());
        }

        return resultado;
    }

    private void validarRangoYValores(
            CaracteristicaTecnica caracteristica,
            CrearRequerimientoCaractRequest request) {

        boolean tieneRango = request.valorMin() != null || request.valorMax() != null;
        boolean tieneValores = request.valores() != null && !request.valores().isEmpty();

        if (CaracteristicaTecnica.TIPO_NUMERICO.equals(caracteristica.getTipoDato())) {
            if (request.valorMin() == null || request.valorMax() == null) {
                throw new RequerimientoInvalidoException(
                        "La caracteristica numerica " + caracteristica.getCodigo()
                                + " exige valorMin y valorMax.");
            }
            if (request.valorMin().compareTo(BigDecimal.ZERO) < 0
                    || request.valorMax().compareTo(BigDecimal.ZERO) < 0) {
                throw new RequerimientoInvalidoException(
                        "La caracteristica " + caracteristica.getCodigo()
                                + " no admite valores negativos.");
            }
            if (request.valorMin().compareTo(request.valorMax()) > 0) {
                throw new RequerimientoInvalidoException(
                        "La caracteristica " + caracteristica.getCodigo()
                                + ": valorMin no puede ser mayor que valorMax.");
            }
            if (tieneValores) {
                throw new RequerimientoInvalidoException(
                        "La caracteristica numerica " + caracteristica.getCodigo()
                                + " no admite valores enumerados.");
            }
            return;
        }

        if (tieneRango) {
            throw new RequerimientoInvalidoException(
                    "La caracteristica " + caracteristica.getCodigo() + " ("
                            + caracteristica.getTipoDato()
                            + ") no admite rango numerico (valorMin/valorMax).");
        }

        if (CaracteristicaTecnica.TIPO_ENUMERADO.equals(caracteristica.getTipoDato())) {
            if (!tieneValores) {
                throw new RequerimientoInvalidoException(
                        "La caracteristica enumerada " + caracteristica.getCodigo()
                                + " exige al menos un valor.");
            }
            for (Long idValor : request.valores()) {
                ValorCaracteristicaId valorId = new ValorCaracteristicaId(
                        idValor, caracteristica.getIdCaracteristica());
                valorCaracteristicaRepository.findById(valorId)
                        .orElseThrow(() -> new ValorCaracteristicaNoEncontradoException(
                                "El valor " + idValor + " no corresponde a la caracteristica "
                                        + caracteristica.getCodigo() + "."));
            }
        } else if (tieneValores) {
            throw new RequerimientoInvalidoException(
                    "La caracteristica " + caracteristica.getCodigo() + " ("
                            + caracteristica.getTipoDato()
                            + ") no admite valores enumerados.");
        }
    }

    private void persistirValoresCaracteristicas(
            List<RequerimientoActCaract> caracteristicas,
            Map<Long, List<Long>> idsValoresPorCaracteristica) {

        for (RequerimientoActCaract caracteristica : caracteristicas) {
            Long idCaracteristica = caracteristica.getCaracteristica().getIdCaracteristica();
            List<Long> idsValores = idsValoresPorCaracteristica.get(idCaracteristica);
            if (idsValores == null || idsValores.isEmpty()) {
                continue;
            }
            for (Long idValor : idsValores) {
                requerimientoActCaractValorRepository.save(RequerimientoActCaractValor.builder()
                        .idReqActCaract(caracteristica.getIdReqActCaract())
                        .idCaracteristica(idCaracteristica)
                        .idValor(idValor)
                        .build());
            }
        }
    }

    private Set<Habilidad> resolverHabilidades(List<Long> idsHabilidades) {
        Set<Habilidad> resultado = new HashSet<>();
        if (idsHabilidades == null || idsHabilidades.isEmpty()) {
            return resultado;
        }
        for (Long idHabilidad : idsHabilidades) {
            Habilidad habilidad = habilidadRepository.findById(idHabilidad)
                    .orElseThrow(() -> new HabilidadNoEncontradaException(idHabilidad));
            resultado.add(habilidad);
        }
        return resultado;
    }
}
