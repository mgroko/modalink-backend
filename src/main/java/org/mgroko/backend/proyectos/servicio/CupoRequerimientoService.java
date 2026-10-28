package org.mgroko.backend.proyectos.servicio;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.mgroko.backend.modelo.RequerimientoActividad;
import org.mgroko.backend.modelo.RequerimientoGralProyecto;
import org.mgroko.backend.proyectos.dto.CupoRequerimiento;
import org.mgroko.backend.repositorio.AsignacionActividadRepository;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
import org.mgroko.backend.repositorio.proyeccion.CubiertosPorRequerimiento;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fuente única de verdad para el cupo de un requerimiento.
 *
 * <p>La cobertura se mide por personas que hoy ocupan el requerimiento:
 * miembros activos para requerimientos generales y asignaciones de miembros
 * activos para requerimientos de actividad. Las postulaciones aceptadas son
 * histórico y no se usan para el conteo.</p>
 */
@Service
public class CupoRequerimientoService {

    private final MiembroProyectoRepository miembroProyectoRepository;
    private final AsignacionActividadRepository asignacionActividadRepository;

    public CupoRequerimientoService(
            MiembroProyectoRepository miembroProyectoRepository,
            AsignacionActividadRepository asignacionActividadRepository) {
        this.miembroProyectoRepository = miembroProyectoRepository;
        this.asignacionActividadRepository = asignacionActividadRepository;
    }

    @Transactional(readOnly = true)
    public CupoRequerimiento calcularGeneral(RequerimientoGralProyecto requerimiento) {
        if (requerimiento == null || requerimiento.getIdRequerimientoGral() == null) {
            return construir(requerimiento != null ? requerimiento.getCantidad() : null, 0);
        }

        long cubiertos = miembroProyectoRepository
                .countActivosPorRequerimientoGral(requerimiento.getIdRequerimientoGral());
        return construir(requerimiento.getCantidad(), cubiertos);
    }

    @Transactional(readOnly = true)
    public CupoRequerimiento calcularActividad(RequerimientoActividad requerimiento) {
        if (requerimiento == null || requerimiento.getIdRequerimiento() == null) {
            return construir(requerimiento != null ? requerimiento.getCantidad() : null, 0);
        }

        long cubiertos = asignacionActividadRepository
                .countActivasPorRequerimientoActividad(requerimiento.getIdRequerimiento());
        return construir(requerimiento.getCantidad(), cubiertos);
    }

    /**
     * Calcula el cupo de varios requerimientos de actividad con una sola
     * consulta de conteo. Los requerimientos sin asignaciones quedan con
     * {@code cubiertos = 0}.
     */
    @Transactional(readOnly = true)
    public Map<Long, CupoRequerimiento> calcularActividades(
            Collection<RequerimientoActividad> requerimientos) {

        if (requerimientos == null || requerimientos.isEmpty()) {
            return Map.of();
        }

        List<Long> ids = requerimientos.stream()
                .filter(Objects::nonNull)
                .map(RequerimientoActividad::getIdRequerimiento)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, Long> cubiertosPorRequerimiento = new HashMap<>();
        if (!ids.isEmpty()) {
            for (CubiertosPorRequerimiento fila
                    : asignacionActividadRepository.countActivasPorRequerimientoIds(ids)) {
                if (fila.getIdRequerimiento() != null) {
                    cubiertosPorRequerimiento.put(
                            fila.getIdRequerimiento(),
                            fila.getCubiertos() != null ? fila.getCubiertos() : 0L);
                }
            }
        }

        Map<Long, CupoRequerimiento> resultado = new HashMap<>();
        for (RequerimientoActividad requerimiento : requerimientos) {
            if (requerimiento == null || requerimiento.getIdRequerimiento() == null) {
                continue;
            }
            resultado.put(
                    requerimiento.getIdRequerimiento(),
                    construir(
                            requerimiento.getCantidad(),
                            cubiertosPorRequerimiento.getOrDefault(
                                    requerimiento.getIdRequerimiento(), 0L)));
        }
        return resultado;
    }

    private CupoRequerimiento construir(Integer cantidad, long cubiertos) {
        int requeridos = cantidad != null ? cantidad : 0;
        long cubiertosNormalizados = Math.max(0, cubiertos);
        int faltantes = (int) Math.max(0, requeridos - cubiertosNormalizados);
        boolean satisfecho = cubiertosNormalizados >= requeridos;

        return new CupoRequerimiento(requeridos, cubiertosNormalizados, faltantes, satisfecho);
    }
}
