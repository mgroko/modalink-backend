package org.mgroko.backend.proyectos.servicio;

import org.mgroko.backend.modelo.RequerimientoActividad;
import org.mgroko.backend.modelo.RequerimientoGralProyecto;
import org.mgroko.backend.proyectos.dto.CupoRequerimiento;
import org.mgroko.backend.repositorio.AsignacionActividadRepository;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
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

    private CupoRequerimiento construir(Integer cantidad, long cubiertos) {
        int requeridos = cantidad != null ? cantidad : 0;
        long cubiertosNormalizados = Math.max(0, cubiertos);
        int faltantes = (int) Math.max(0, requeridos - cubiertosNormalizados);
        boolean satisfecho = cubiertosNormalizados >= requeridos;

        return new CupoRequerimiento(requeridos, cubiertosNormalizados, faltantes, satisfecho);
    }
}
