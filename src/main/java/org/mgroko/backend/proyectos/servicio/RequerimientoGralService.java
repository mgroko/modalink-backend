package org.mgroko.backend.proyectos.servicio;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mgroko.backend.modelo.CaracteristicaTecnica;
import org.mgroko.backend.modelo.Habilidad;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RequerimientoGralCaract;
import org.mgroko.backend.modelo.RequerimientoGralCaractValor;
import org.mgroko.backend.modelo.RequerimientoGralProyecto;
import org.mgroko.backend.modelo.ValorCaracteristicaId;
import org.mgroko.backend.perfiles.exception.CaracteristicaNoEncontradaException;
import org.mgroko.backend.perfiles.exception.CaracteristicaProfesionNoCoincideException;
import org.mgroko.backend.perfiles.exception.ProfesionNoEncontradaException;
import org.mgroko.backend.perfiles.exception.ValorCaracteristicaNoEncontradoException;
import org.mgroko.backend.proyectos.dto.CaracteristicaRequerimientoResponse;
import org.mgroko.backend.proyectos.dto.CrearRequerimientoCaractRequest;
import org.mgroko.backend.proyectos.dto.RequerimientoGralResponse;
import org.mgroko.backend.proyectos.exception.HabilidadNoEncontradaException;
import org.mgroko.backend.proyectos.exception.RequerimientoInvalidoException;
import org.mgroko.backend.repositorio.CaracteristicaTecnicaRepository;
import org.mgroko.backend.repositorio.HabilidadRepository;
import org.mgroko.backend.repositorio.ProfesionRepository;
import org.mgroko.backend.repositorio.RequerimientoGralCaractValorRepository;
import org.mgroko.backend.repositorio.RequerimientoGralProyectoRepository;
import org.mgroko.backend.repositorio.ValorCaracteristicaRepository;
import org.springframework.stereotype.Service;

/**
 * Persistencia y mapeo de los requerimientos de personal de un proyecto
 * (UC-24 Crear y UC-36 Modificar comparten estas reglas).
 */
@Service
public class RequerimientoGralService {

    private final ProfesionRepository profesionRepository;
    private final CaracteristicaTecnicaRepository caracteristicaTecnicaRepository;
    private final ValorCaracteristicaRepository valorCaracteristicaRepository;
    private final HabilidadRepository habilidadRepository;
    private final RequerimientoGralProyectoRepository requerimientoGralProyectoRepository;
    private final RequerimientoGralCaractValorRepository requerimientoGralCaractValorRepository;

    public RequerimientoGralService(
            ProfesionRepository profesionRepository,
            CaracteristicaTecnicaRepository caracteristicaTecnicaRepository,
            ValorCaracteristicaRepository valorCaracteristicaRepository,
            HabilidadRepository habilidadRepository,
            RequerimientoGralProyectoRepository requerimientoGralProyectoRepository,
            RequerimientoGralCaractValorRepository requerimientoGralCaractValorRepository) {
        this.profesionRepository = profesionRepository;
        this.caracteristicaTecnicaRepository = caracteristicaTecnicaRepository;
        this.valorCaracteristicaRepository = valorCaracteristicaRepository;
        this.habilidadRepository = habilidadRepository;
        this.requerimientoGralProyectoRepository = requerimientoGralProyectoRepository;
        this.requerimientoGralCaractValorRepository = requerimientoGralCaractValorRepository;
    }

    /**
     * Crea un requerimiento de personal para el proyecto con sus características
     * y habilidades, y devuelve su representación para la respuesta.
     */
    public RequerimientoGralResponse persistir(
            Proyecto proyecto,
            Long idProfesion,
            Integer cantidad,
            String descripcion,
            List<CrearRequerimientoCaractRequest> caracteristicas,
            List<Long> habilidades) {

        Profesion profesion = profesionRepository.findById(idProfesion)
                .orElseThrow(() -> new ProfesionNoEncontradaException(
                        "Profesión no encontrada: " + idProfesion));

        RequerimientoGralProyecto requerimiento = RequerimientoGralProyecto.builder()
                .proyecto(proyecto)
                .profesion(profesion)
                .cantidad(cantidad)
                .descripcion(descripcion != null ? descripcion.trim() : null)
                .build();

        Map<Long, List<Long>> idsValoresPorCaracteristica = new HashMap<>();
        List<RequerimientoGralCaract> caracteristicasEntidad =
                construirCaracteristicas(requerimiento, caracteristicas, idsValoresPorCaracteristica);
        requerimiento.setCaracteristicas(caracteristicasEntidad);

        Set<Habilidad> habilidadesEntidad = resolverHabilidades(habilidades);
        requerimiento.setHabilidades(habilidadesEntidad);

        RequerimientoGralProyecto guardado = requerimientoGralProyectoRepository.save(requerimiento);

        persistirValoresCaracteristicas(caracteristicasEntidad, idsValoresPorCaracteristica);

        return toResponse(guardado, idsValoresPorCaracteristica);
    }

    /**
     * Elimina un requerimiento junto con sus valores de características
     * enumeradas (que no tienen cascade desde el requerimiento).
     */
    public void eliminar(RequerimientoGralProyecto requerimiento) {
        for (RequerimientoGralCaract caract : requerimiento.getCaracteristicas()) {
            List<RequerimientoGralCaractValor> valores =
                    requerimientoGralCaractValorRepository.findByIdReqGralCaract(caract.getIdReqGralCaract());
            if (!valores.isEmpty()) {
                requerimientoGralCaractValorRepository.deleteAll(valores);
            }
        }
        requerimientoGralProyectoRepository.delete(requerimiento);
    }

    private RequerimientoGralResponse toResponse(
            RequerimientoGralProyecto guardado,
            Map<Long, List<Long>> idsValoresPorCaracteristica) {

        List<CaracteristicaRequerimientoResponse> caractsResponse = new ArrayList<>();
        for (RequerimientoGralCaract caract : guardado.getCaracteristicas()) {
            Long idCaract = caract.getCaracteristica().getIdCaracteristica();
            caractsResponse.add(new CaracteristicaRequerimientoResponse(
                    idCaract,
                    caract.getCaracteristica().getCodigo(),
                    caract.getCaracteristica().getTipoDato(),
                    caract.getValorMin(),
                    caract.getValorMax(),
                    idsValoresPorCaracteristica.getOrDefault(idCaract, List.of())));
        }

        List<Long> idsHabilidades = guardado.getHabilidades().stream()
                .map(Habilidad::getIdHabilidad)
                .sorted()
                .toList();

        int cantidad = guardado.getCantidad() != null ? guardado.getCantidad() : 0;
        return new RequerimientoGralResponse(
                guardado.getIdRequerimientoGral(),
                guardado.getCantidad(),
                guardado.getDescripcion(),
                guardado.getProfesion().getIdProfesion(),
                guardado.getProfesion().getNombre(),
                caractsResponse,
                idsHabilidades,
                Boolean.FALSE,
                0L,
                cantidad);
    }

    private List<RequerimientoGralCaract> construirCaracteristicas(
            RequerimientoGralProyecto requerimiento,
            List<CrearRequerimientoCaractRequest> caracteristicasRequest,
            Map<Long, List<Long>> idsValoresPorCaracteristica) {

        List<RequerimientoGralCaract> resultado = new ArrayList<>();
        if (caracteristicasRequest == null || caracteristicasRequest.isEmpty()) {
            return resultado;
        }

        Set<Long> vistas = new HashSet<>();
        for (CrearRequerimientoCaractRequest carReq : caracteristicasRequest) {
            if (!vistas.add(carReq.idCaracteristica())) {
                throw new RequerimientoInvalidoException(
                        "La característica técnica " + carReq.idCaracteristica() + " está duplicada en el requerimiento.");
            }

            CaracteristicaTecnica ct = caracteristicaTecnicaRepository.findById(carReq.idCaracteristica())
                    .orElseThrow(() -> new CaracteristicaNoEncontradaException(
                            "Característica técnica no encontrada: " + carReq.idCaracteristica()));

            if (!ct.getProfesion().getIdProfesion().equals(requerimiento.getProfesion().getIdProfesion())) {
                throw new CaracteristicaProfesionNoCoincideException(
                        "La característica técnica " + carReq.idCaracteristica()
                                + " no corresponde a la profesión del requerimiento.");
            }

            validarRangoYValores(ct, carReq);

            if (carReq.valores() != null && !carReq.valores().isEmpty()) {
                idsValoresPorCaracteristica.put(ct.getIdCaracteristica(), carReq.valores());
            }

            resultado.add(RequerimientoGralCaract.builder()
                    .requerimientoGral(requerimiento)
                    .caracteristica(ct)
                    .valorMin(carReq.valorMin())
                    .valorMax(carReq.valorMax())
                    .build());
        }

        return resultado;
    }

    private void validarRangoYValores(CaracteristicaTecnica ct, CrearRequerimientoCaractRequest carReq) {
        boolean tieneRango = carReq.valorMin() != null || carReq.valorMax() != null;
        boolean tieneValores = carReq.valores() != null && !carReq.valores().isEmpty();

        if (CaracteristicaTecnica.TIPO_NUMERICO.equals(ct.getTipoDato())) {
            if (carReq.valorMin() == null || carReq.valorMax() == null) {
                throw new RequerimientoInvalidoException(
                        "La característica numérica " + ct.getCodigo() + " exige valorMin y valorMax.");
            }
            if (carReq.valorMin().compareTo(BigDecimal.ZERO) < 0 || carReq.valorMax().compareTo(BigDecimal.ZERO) < 0) {
                throw new RequerimientoInvalidoException(
                        "La característica " + ct.getCodigo() + " no admite valores negativos.");
            }
            if (carReq.valorMin().compareTo(carReq.valorMax()) > 0) {
                throw new RequerimientoInvalidoException(
                        "La característica " + ct.getCodigo() + ": valorMin no puede ser mayor que valorMax.");
            }
            if (tieneValores) {
                throw new RequerimientoInvalidoException(
                        "La característica numérica " + ct.getCodigo() + " no admite valores enumerados.");
            }
        } else {
            if (tieneRango) {
                throw new RequerimientoInvalidoException(
                        "La característica " + ct.getCodigo() + " (" + ct.getTipoDato()
                                + ") no admite rango numérico (valorMin/valorMax).");
            }
            if (CaracteristicaTecnica.TIPO_ENUMERADO.equals(ct.getTipoDato())) {
                if (!tieneValores) {
                    throw new RequerimientoInvalidoException(
                            "La característica enumerada " + ct.getCodigo() + " exige al menos un valor.");
                }
                for (Long idValor : carReq.valores()) {
                    ValorCaracteristicaId valorId = new ValorCaracteristicaId(idValor, ct.getIdCaracteristica());
                    valorCaracteristicaRepository.findById(valorId)
                            .orElseThrow(() -> new ValorCaracteristicaNoEncontradoException(
                                    "El valor " + idValor + " no corresponde a la característica " + ct.getCodigo() + "."));
                }
            } else if (tieneValores) {
                throw new RequerimientoInvalidoException(
                        "La característica " + ct.getCodigo() + " (" + ct.getTipoDato()
                                + ") no admite valores enumerados.");
            }
        }
    }

    private void persistirValoresCaracteristicas(
            List<RequerimientoGralCaract> caracteristicas,
            Map<Long, List<Long>> idsValoresPorCaracteristica) {
        for (RequerimientoGralCaract caract : caracteristicas) {
            Long idCaracteristica = caract.getCaracteristica().getIdCaracteristica();
            List<Long> idsValores = idsValoresPorCaracteristica.get(idCaracteristica);
            if (idsValores == null || idsValores.isEmpty()) {
                continue;
            }
            for (Long idValor : idsValores) {
                requerimientoGralCaractValorRepository.save(RequerimientoGralCaractValor.builder()
                        .idValor(idValor)
                        .idCaracteristica(idCaracteristica)
                        .idReqGralCaract(caract.getIdReqGralCaract())
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
