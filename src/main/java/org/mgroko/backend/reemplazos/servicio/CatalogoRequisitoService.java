package org.mgroko.backend.reemplazos.servicio;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mgroko.backend.modelo.CaracteristicaTecnica;
import org.mgroko.backend.modelo.Habilidad;
import org.mgroko.backend.modelo.RequerimientoActCaract;
import org.mgroko.backend.modelo.RequerimientoActCaractValor;
import org.mgroko.backend.modelo.RequerimientoActividad;
import org.mgroko.backend.modelo.RequerimientoGralCaract;
import org.mgroko.backend.modelo.RequerimientoGralCaractValor;
import org.mgroko.backend.modelo.RequerimientoGralProyecto;
import org.mgroko.backend.modelo.ValorCaracteristica;
import org.mgroko.backend.reemplazos.exception.ReemplazoNoGestionableException;
import org.mgroko.backend.reemplazos.servicio.modelo.CaracteristicaRequerida;
import org.mgroko.backend.reemplazos.servicio.modelo.RequisitoReemplazo;
import org.mgroko.backend.repositorio.RequerimientoActCaractValorRepository;
import org.mgroko.backend.repositorio.RequerimientoActividadRepository;
import org.mgroko.backend.repositorio.RequerimientoGralCaractValorRepository;
import org.mgroko.backend.repositorio.RequerimientoGralProyectoRepository;
import org.mgroko.backend.repositorio.ValorCaracteristicaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Carga y normaliza el requerimiento de una vacante.
 */
@Service
public class CatalogoRequisitoService {

    private final RequerimientoActividadRepository requerimientoActividadRepository;
    private final RequerimientoActCaractValorRepository requerimientoActCaractValorRepository;
    private final RequerimientoGralProyectoRepository requerimientoGralProyectoRepository;
    private final RequerimientoGralCaractValorRepository requerimientoGralCaractValorRepository;
    private final ValorCaracteristicaRepository valorCaracteristicaRepository;

    public CatalogoRequisitoService(
            RequerimientoActividadRepository requerimientoActividadRepository,
            RequerimientoActCaractValorRepository requerimientoActCaractValorRepository,
            RequerimientoGralProyectoRepository requerimientoGralProyectoRepository,
            RequerimientoGralCaractValorRepository requerimientoGralCaractValorRepository,
            ValorCaracteristicaRepository valorCaracteristicaRepository) {
        this.requerimientoActividadRepository = requerimientoActividadRepository;
        this.requerimientoActCaractValorRepository = requerimientoActCaractValorRepository;
        this.requerimientoGralProyectoRepository = requerimientoGralProyectoRepository;
        this.requerimientoGralCaractValorRepository = requerimientoGralCaractValorRepository;
        this.valorCaracteristicaRepository = valorCaracteristicaRepository;
    }

    @Transactional(readOnly = true)
    public RequisitoReemplazo cargarActividad(Long idRequerimiento) {
        RequerimientoActividad requerimiento = requerimientoActividadRepository
                .findConDetalleByIds(List.of(idRequerimiento))
                .stream()
                .findFirst()
                .orElseThrow(() -> new ReemplazoNoGestionableException(
                        "El requerimiento de actividad " + idRequerimiento
                                + " ya no está activo."));

        Map<Long, Set<Long>> valores = valoresActividad(requerimiento.getCaracteristicas());

        List<CaracteristicaRequerida> caracteristicas = new ArrayList<>();
        for (RequerimientoActCaract caracteristica : requerimiento.getCaracteristicas()) {
            Set<Long> idsValores = valores.getOrDefault(
                    caracteristica.getCaracteristica().getIdCaracteristica(),
                    Set.of());
            caracteristicas.add(new CaracteristicaRequerida(
                    caracteristica.getCaracteristica().getIdCaracteristica(),
                    caracteristica.getCaracteristica().getCodigo(),
                    caracteristica.getCaracteristica().getNombre(),
                    caracteristica.getCaracteristica().getTipoDato(),
                    caracteristica.getValorMin(),
                    caracteristica.getValorMax(),
                    idsValores,
                    etiquetasValores(caracteristica.getCaracteristica(), idsValores)));
        }
        caracteristicas.sort(java.util.Comparator.comparing(
                CaracteristicaRequerida::idCaracteristica));

        return new RequisitoReemplazo(
                requerimiento.getProfesion().getIdProfesion(),
                requerimiento.getProfesion().getNombre(),
                idsHabilidades(requerimiento.getHabilidades()),
                nombresHabilidades(requerimiento.getHabilidades()),
                caracteristicas);
    }

    @Transactional(readOnly = true)
    public RequisitoReemplazo cargarGeneral(Long idRequerimiento) {
        RequerimientoGralProyecto requerimiento = requerimientoGralProyectoRepository
                .findConDetalleByIds(List.of(idRequerimiento))
                .stream()
                .findFirst()
                .orElseThrow(() -> new ReemplazoNoGestionableException(
                        "El requerimiento general " + idRequerimiento
                                + " ya no está activo."));

        Map<Long, Set<Long>> valores = valoresGeneral(requerimiento.getCaracteristicas());

        List<CaracteristicaRequerida> caracteristicas = new ArrayList<>();
        for (RequerimientoGralCaract caracteristica : requerimiento.getCaracteristicas()) {
            Set<Long> idsValores = valores.getOrDefault(
                    caracteristica.getCaracteristica().getIdCaracteristica(),
                    Set.of());
            caracteristicas.add(new CaracteristicaRequerida(
                    caracteristica.getCaracteristica().getIdCaracteristica(),
                    caracteristica.getCaracteristica().getCodigo(),
                    caracteristica.getCaracteristica().getNombre(),
                    caracteristica.getCaracteristica().getTipoDato(),
                    caracteristica.getValorMin(),
                    caracteristica.getValorMax(),
                    idsValores,
                    etiquetasValores(caracteristica.getCaracteristica(), idsValores)));
        }
        caracteristicas.sort(java.util.Comparator.comparing(
                CaracteristicaRequerida::idCaracteristica));

        return new RequisitoReemplazo(
                requerimiento.getProfesion().getIdProfesion(),
                requerimiento.getProfesion().getNombre(),
                idsHabilidades(requerimiento.getHabilidades()),
                nombresHabilidades(requerimiento.getHabilidades()),
                caracteristicas);
    }

    private Map<Long, Set<Long>> valoresActividad(
            List<RequerimientoActCaract> caracteristicas) {

        Map<Long, Set<Long>> resultado = new HashMap<>();
        List<Long> idsFilas = caracteristicas.stream()
                .map(RequerimientoActCaract::getIdReqActCaract)
                .filter(java.util.Objects::nonNull)
                .toList();

        if (idsFilas.isEmpty()) {
            return resultado;
        }

        for (RequerimientoActCaractValor valor
                : requerimientoActCaractValorRepository.findByIdReqActCaractIn(idsFilas)) {
            resultado.computeIfAbsent(
                    valor.getIdCaracteristica(),
                    id -> new HashSet<>()).add(valor.getIdValor());
        }

        return resultado;
    }

    private Map<Long, Set<Long>> valoresGeneral(
            List<RequerimientoGralCaract> caracteristicas) {

        Map<Long, Set<Long>> resultado = new HashMap<>();
        List<Long> idsFilas = caracteristicas.stream()
                .map(RequerimientoGralCaract::getIdReqGralCaract)
                .filter(java.util.Objects::nonNull)
                .toList();

        if (idsFilas.isEmpty()) {
            return resultado;
        }

        for (RequerimientoGralCaractValor valor
                : requerimientoGralCaractValorRepository.findByIdReqGralCaractIn(idsFilas)) {
            resultado.computeIfAbsent(
                    valor.getIdCaracteristica(),
                    id -> new HashSet<>()).add(valor.getIdValor());
        }

        return resultado;
    }

    private Map<Long, String> etiquetasValores(
            CaracteristicaTecnica caracteristica,
            Set<Long> idsValores) {

        if (caracteristica == null
                || !CaracteristicaTecnica.TIPO_ENUMERADO.equals(
                        caracteristica.getTipoDato())
                || idsValores == null
                || idsValores.isEmpty()) {
            return Map.of();
        }

        Map<Long, String> etiquetas = new HashMap<>();
        for (ValorCaracteristica valor : valorCaracteristicaRepository
                .findByCaracteristicaTecnica_IdCaracteristicaOrderByEtiqueta(
                        caracteristica.getIdCaracteristica())) {
            if (valor.getIdValor() != null
                    && idsValores.contains(valor.getIdValor())
                    && valor.getEtiqueta() != null
                    && !valor.getEtiqueta().isBlank()) {
                etiquetas.put(valor.getIdValor(), valor.getEtiqueta());
            }
        }
        return etiquetas;
    }

    private Set<Long> idsHabilidades(Set<Habilidad> habilidades) {
        if (habilidades == null || habilidades.isEmpty()) {
            return Set.of();
        }
        return habilidades.stream()
                .map(Habilidad::getIdHabilidad)
                .collect(java.util.stream.Collectors.toSet());
    }

    private Map<Long, String> nombresHabilidades(Set<Habilidad> habilidades) {
        if (habilidades == null || habilidades.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> resultado = new HashMap<>();
        for (Habilidad habilidad : habilidades) {
            resultado.put(habilidad.getIdHabilidad(), habilidad.getNombre());
        }
        return resultado;
    }
}
