package org.mgroko.backend.proyectos.servicio;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RequerimientoGralProyecto;
import org.mgroko.backend.modelo.enums.EstadoParticipacion;
import org.mgroko.backend.modelo.enums.RolProyectoUsuario;
import org.mgroko.backend.postulaciones.exception.RequerimientoNoEncontradoException;
import org.mgroko.backend.proyectos.dto.CupoRequerimiento;
import org.mgroko.backend.proyectos.evento.CupoRequerimientoGralModificadoEvent;
import org.mgroko.backend.proyectos.exception.RequerimientoGralNoAsignableException;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
import org.mgroko.backend.repositorio.RequerimientoGralProyectoRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resuelve a qué requerimiento general se vincula una membresía.
 *
 * <p>Nunca crea requerimientos: solo selecciona uno existente, activo, de la
 * misma profesión y con cupo disponible. Si no hay candidato, deja la membresía
 * con {@code requerimientoGral = null}.</p>
 */
@Service
public class RequerimientoGralAsignacionService {

    private final RequerimientoGralProyectoRepository requerimientoGralProyectoRepository;
    private final MiembroProyectoRepository miembroProyectoRepository;
    private final CupoRequerimientoService cupoRequerimientoService;
    private final ApplicationEventPublisher eventPublisher;

    public RequerimientoGralAsignacionService(
            RequerimientoGralProyectoRepository requerimientoGralProyectoRepository,
            MiembroProyectoRepository miembroProyectoRepository,
            CupoRequerimientoService cupoRequerimientoService,
            ApplicationEventPublisher eventPublisher) {
        this.requerimientoGralProyectoRepository = requerimientoGralProyectoRepository;
        this.miembroProyectoRepository = miembroProyectoRepository;
        this.cupoRequerimientoService = cupoRequerimientoService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Resuelve el requerimiento para una incorporación.
     *
     * <p>Si llega un requerimiento explícito, se respeta cuando sigue activo,
     * pertenece al proyecto, coincide la profesión y tiene cupo. Si está lleno,
     * archivado o ya no existe, se busca un candidato compatible. Si no hay,
     * devuelve {@code null}.</p>
     */
    @Transactional
    public RequerimientoGralProyecto resolverParaIncorporacion(
            Proyecto proyecto,
            Perfil perfil,
            Long idRequerimientoExplicito) {

        if (proyecto == null || proyecto.getIdProyecto() == null
                || perfil == null || perfil.getProfesion() == null
                || perfil.getProfesion().getIdProfesion() == null) {
            return null;
        }

        Long idProfesion = perfil.getProfesion().getIdProfesion();
        List<RequerimientoGralProyecto> requerimientos =
                requerimientoGralProyectoRepository.findByProyectoIdProyectoForUpdate(
                        proyecto.getIdProyecto());

        RequerimientoGralProyecto explicito = null;
        if (idRequerimientoExplicito != null) {
            explicito = requerimientos.stream()
                    .filter(requerimiento -> idRequerimientoExplicito.equals(
                            requerimiento.getIdRequerimientoGral()))
                    .findFirst()
                    .orElse(null);

            if (explicito == null) {
                // Puede estar archivado o pertenecer a otro proyecto.
                RequerimientoGralProyecto existente = requerimientoGralProyectoRepository
                        .findById(idRequerimientoExplicito)
                        .orElse(null);
                if (existente != null
                        && existente.getProyecto() != null
                        && !proyecto.getIdProyecto().equals(
                                existente.getProyecto().getIdProyecto())) {
                    throw new RequerimientoNoEncontradoException(
                            "El requerimiento no pertenece al proyecto indicado.");
                }
            } else if (explicito.getProfesion() == null
                    || !idProfesion.equals(explicito.getProfesion().getIdProfesion())) {
                throw new RequerimientoGralNoAsignableException(
                        "El perfil no tiene la profesión requerida por el requerimiento general.",
                        "PROFESION_INCOMPATIBLE");
            }
        }

        Map<Long, CupoRequerimiento> cupos =
                cupoRequerimientoService.calcularGenerales(requerimientos);

        if (explicito != null) {
            CupoRequerimiento cupo = cupos.get(explicito.getIdRequerimientoGral());
            if (cupo != null && cupo.faltantes() > 0) {
                return explicito;
            }
        }

        return requerimientos.stream()
                .filter(requerimiento -> requerimiento.getProfesion() != null
                        && idProfesion.equals(
                                requerimiento.getProfesion().getIdProfesion()))
                .filter(requerimiento -> {
                    CupoRequerimiento cupo = cupos.get(
                            requerimiento.getIdRequerimientoGral());
                    return cupo != null && cupo.faltantes() > 0;
                })
                .min(Comparator.comparing(
                        RequerimientoGralProyecto::getIdRequerimientoGral))
                .orElse(null);
    }

    /**
     * Vincula una membresía activa que todavía no tiene requerimiento.
     * Los Directores y los miembros inactivos no consumen cupo.
     */
    @Transactional
    public void vincularSiCorresponde(MiembroProyecto miembro) {
        if (miembro == null || miembro.getIdMiembro() == null
                || miembro.getEstadoParticipacion() != EstadoParticipacion.ACTIVO
                || miembro.getRequerimientoGral() != null
                || miembro.getProyecto() == null
                || miembro.getPerfil() == null) {
            return;
        }

        if (RolProyectoUsuario.desde(miembro.getRolProyecto())
                == RolProyectoUsuario.DIRECTOR) {
            return;
        }

        RequerimientoGralProyecto candidato = resolverParaIncorporacion(
                miembro.getProyecto(), miembro.getPerfil(), null);
        if (candidato == null) {
            return;
        }

        miembro.setRequerimientoGral(candidato);
        miembroProyectoRepository.saveAndFlush(miembro);
        publicarCupoModificado(candidato.getIdRequerimientoGral());
    }

    public void publicarCupoModificado(Long idRequerimientoGral) {
        if (idRequerimientoGral != null) {
            eventPublisher.publishEvent(new CupoRequerimientoGralModificadoEvent(
                    idRequerimientoGral));
        }
    }
}
