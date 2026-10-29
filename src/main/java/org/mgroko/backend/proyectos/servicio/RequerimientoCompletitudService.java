package org.mgroko.backend.proyectos.servicio;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.mgroko.backend.modelo.RequerimientoActividad;
import org.mgroko.backend.modelo.RequerimientoGralProyecto;
import org.mgroko.backend.proyectos.dto.BloqueoConfirmacionResponse;
import org.mgroko.backend.proyectos.dto.CupoRequerimiento;
import org.mgroko.backend.proyectos.dto.RequerimientoCupoResponse;
import org.mgroko.backend.repositorio.RequerimientoActividadRepository;
import org.mgroko.backend.repositorio.RequerimientoGralProyectoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Valida la completitud del personal requerido para UC-60.
 *
 * <p>Un requerimiento está completo cuando los miembros o asignaciones
 * activas alcanzan la cantidad pedida. La fuente de verdad es
 * {@link CupoRequerimientoService}.</p>
 */
@Service
public class RequerimientoCompletitudService {

    private final RequerimientoGralProyectoRepository requerimientoGralRepository;
    private final RequerimientoActividadRepository requerimientoActividadRepository;
    private final CupoRequerimientoService cupoRequerimientoService;

    public RequerimientoCompletitudService(
            RequerimientoGralProyectoRepository requerimientoGralRepository,
            RequerimientoActividadRepository requerimientoActividadRepository,
            CupoRequerimientoService cupoRequerimientoService) {
        this.requerimientoGralRepository = requerimientoGralRepository;
        this.requerimientoActividadRepository = requerimientoActividadRepository;
        this.cupoRequerimientoService = cupoRequerimientoService;
    }

    @Transactional(readOnly = true)
    public Resultado validar(Long idProyecto, List<Long> idsActividadesNoCanceladas) {
        List<RequerimientoGralProyecto> generales =
                requerimientoGralRepository.findByProyectoIdProyecto(idProyecto);
        Map<Long, CupoRequerimiento> cuposGenerales =
                cupoRequerimientoService.calcularGenerales(generales);

        List<RequerimientoActividad> actividades = idsActividadesNoCanceladas == null
                || idsActividadesNoCanceladas.isEmpty()
                        ? List.of()
                        : requerimientoActividadRepository
                                .findConProfesionByActividadIds(idsActividadesNoCanceladas);
        Map<Long, CupoRequerimiento> cuposActividades =
                cupoRequerimientoService.calcularActividades(actividades);

        List<RequerimientoCupoResponse> generalesResponse = new ArrayList<>();
        List<RequerimientoCupoResponse> actividadesResponse = new ArrayList<>();
        List<BloqueoConfirmacionResponse> bloqueos = new ArrayList<>();

        for (RequerimientoGralProyecto requerimiento : generales) {
            CupoRequerimiento cupo = cuposGenerales.get(
                    requerimiento.getIdRequerimientoGral());
            if (cupo == null) {
                cupo = new CupoRequerimiento(0, 0, 0, false);
            }
            generalesResponse.add(new RequerimientoCupoResponse(
                    requerimiento.getIdRequerimientoGral(),
                    null,
                    null,
                    requerimiento.getProfesion() != null
                            ? requerimiento.getProfesion().getIdProfesion()
                            : null,
                    requerimiento.getProfesion() != null
                            ? requerimiento.getProfesion().getNombre()
                            : null,
                    requerimiento.getCantidad(),
                    cupo.cubiertos(),
                    cupo.faltantes(),
                    cupo.satisfecho()));

            if (requerimiento.getCantidad() == null || requerimiento.getCantidad() <= 0) {
                bloqueos.add(new BloqueoConfirmacionResponse(
                        "DATOS_INCONSISTENTES",
                        "El requerimiento general " + requerimiento.getIdRequerimientoGral()
                                + " no tiene una cantidad válida.",
                        "REQUERIMIENTO_GRAL",
                        requerimiento.getIdRequerimientoGral()));
            } else if (!cupo.satisfecho()) {
                bloqueos.add(new BloqueoConfirmacionResponse(
                        "REQUERIMIENTOS_INCOMPLETOS",
                        "El requerimiento general de "
                                + nombreProfesion(requerimiento.getProfesion() != null
                                        ? requerimiento.getProfesion().getNombre()
                                        : null)
                                + " necesita " + cupo.faltantes() + " integrante(s) más.",
                        "REQUERIMIENTO_GRAL",
                        requerimiento.getIdRequerimientoGral()));
            }
        }

        for (RequerimientoActividad requerimiento : actividades) {
            CupoRequerimiento cupo = cuposActividades.get(
                    requerimiento.getIdRequerimiento());
            if (cupo == null) {
                cupo = new CupoRequerimiento(0, 0, 0, false);
            }
            Long idActividad = requerimiento.getActividad() != null
                    ? requerimiento.getActividad().getIdActividad()
                    : null;
            String nombreActividad = requerimiento.getActividad() != null
                    ? requerimiento.getActividad().getNombre()
                    : null;
            actividadesResponse.add(new RequerimientoCupoResponse(
                    requerimiento.getIdRequerimiento(),
                    idActividad,
                    nombreActividad,
                    requerimiento.getProfesion() != null
                            ? requerimiento.getProfesion().getIdProfesion()
                            : null,
                    requerimiento.getProfesion() != null
                            ? requerimiento.getProfesion().getNombre()
                            : null,
                    requerimiento.getCantidad(),
                    cupo.cubiertos(),
                    cupo.faltantes(),
                    cupo.satisfecho()));

            if (requerimiento.getCantidad() == null || requerimiento.getCantidad() <= 0) {
                bloqueos.add(new BloqueoConfirmacionResponse(
                        "DATOS_INCONSISTENTES",
                        "El requerimiento de actividad " + requerimiento.getIdRequerimiento()
                                + " no tiene una cantidad válida.",
                        "REQUERIMIENTO_ACTIVIDAD",
                        requerimiento.getIdRequerimiento()));
            } else if (!cupo.satisfecho()) {
                bloqueos.add(new BloqueoConfirmacionResponse(
                        "REQUERIMIENTOS_INCOMPLETOS",
                        "El requerimiento de "
                                + nombreProfesion(requerimiento.getProfesion() != null
                                        ? requerimiento.getProfesion().getNombre()
                                        : null)
                                + " para la actividad '" + nombreActividad
                                + "' necesita " + cupo.faltantes() + " integrante(s) más.",
                        "REQUERIMIENTO_ACTIVIDAD",
                        requerimiento.getIdRequerimiento()));
            }
        }

        return new Resultado(
                List.copyOf(generalesResponse),
                List.copyOf(actividadesResponse),
                List.copyOf(bloqueos));
    }

    private String nombreProfesion(String nombre) {
        return nombre != null ? nombre : "profesión no informada";
    }

    public record Resultado(
            List<RequerimientoCupoResponse> generales,
            List<RequerimientoCupoResponse> actividades,
            List<BloqueoConfirmacionResponse> bloqueos) {
    }
}
