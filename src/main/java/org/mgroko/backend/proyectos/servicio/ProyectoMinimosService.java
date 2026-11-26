package org.mgroko.backend.proyectos.servicio;

import org.mgroko.backend.modelo.enums.EstadoActividad;
import org.mgroko.backend.modelo.enums.EstadoParticipacion;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
import org.mgroko.backend.repositorio.RequerimientoActividadRepository;
import org.mgroko.backend.repositorio.RequerimientoGralProyectoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Valida los mínimos de un proyecto que pueden confirmarse y finalizarse sin
 * actividades: al menos dos miembros activos (el director y otra persona) y al
 * menos un requerimiento de personal.
 *
 * <p>Al confirmar se exige un requerimiento computable: general activo o de
 * actividad no archivado y en una actividad no cancelada. Al finalizar se
 * acepta también evidencia histórica, para no bloquear proyectos que cerraron
 * con sus actividades canceladas o archivadas.</p>
 */
@Service
public class ProyectoMinimosService {

    private final MiembroProyectoRepository miembroProyectoRepository;
    private final RequerimientoGralProyectoRepository requerimientoGralRepository;
    private final RequerimientoActividadRepository requerimientoActividadRepository;

    public ProyectoMinimosService(
            MiembroProyectoRepository miembroProyectoRepository,
            RequerimientoGralProyectoRepository requerimientoGralRepository,
            RequerimientoActividadRepository requerimientoActividadRepository) {
        this.miembroProyectoRepository = miembroProyectoRepository;
        this.requerimientoGralRepository = requerimientoGralRepository;
        this.requerimientoActividadRepository = requerimientoActividadRepository;
    }

    /**
     * El proyecto debe tener al menos dos miembros activos: el director actor
     * y otra persona. No se exige que el otro miembro tenga un rol distinto.
     */
    @Transactional(readOnly = true)
    public boolean tieneAlMenosDosMiembrosActivos(Long idProyecto) {
        return miembroProyectoRepository
                .countByProyectoIdProyectoAndEstadoParticipacion(
                        idProyecto, EstadoParticipacion.ACTIVO) > 1;
    }

    /**
     * Requerimiento computable para confirmar: general activo o de una
     * actividad no cancelada y no archivado.
     */
    @Transactional(readOnly = true)
    public boolean tieneRequerimientoComputable(Long idProyecto) {
        return requerimientoGralRepository
                .existsByProyectoIdProyectoAndFechaArchivadoIsNull(idProyecto)
                || requerimientoActividadRepository.existsByProyectoExcluyendoEstado(
                        idProyecto, EstadoActividad.CANCELADA);
    }

    /**
     * Evidencia histórica para finalizar: cualquier requerimiento general o de
     * actividad, aunque esté archivado o pertenezca a una actividad cancelada.
     */
    @Transactional(readOnly = true)
    public boolean tieneRequerimientoHistorico(Long idProyecto) {
        return requerimientoGralRepository.existsByProyectoIdProyecto(idProyecto)
                || requerimientoActividadRepository
                        .existsByProyectoIncluyendoArchivadosYCancelados(idProyecto);
    }
}
