package org.mgroko.backend.contratos.servicio;

import java.time.LocalDate;
import java.util.List;

import org.mgroko.backend.contratos.dto.ContratoActivoMiembroResponse;
import org.mgroko.backend.contratos.dto.EmisorContratoResponse;
import org.mgroko.backend.contratos.dto.MiembroDestinatarioResponse;
import org.mgroko.backend.contratos.dto.OpcionesGeneracionContratoResponse;
import org.mgroko.backend.contratos.dto.TipoAcuerdoContratoResponse;
import org.mgroko.backend.contratos.mapper.ContratoMapper;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Planificacion;
import org.mgroko.backend.repositorio.ContratoRepository;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
import org.mgroko.backend.repositorio.PlanificacionRepository;
import org.mgroko.backend.repositorio.TipoAcuerdoContratoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Devuelve los datos que el frontend necesita para los pasos 2 y 3 de UC-39:
 * proyecto, tipos de acuerdo, miembros activos y contratos ACTIVOS por tipo.
 */
@Service
public class ObtenerOpcionesContratoService {

    private final ContratoAccesoService accesoService;
    private final TipoAcuerdoContratoRepository tipoAcuerdoRepository;
    private final MiembroProyectoRepository miembroRepository;
    private final ContratoRepository contratoRepository;
    private final PlanificacionRepository planificacionRepository;

    public ObtenerOpcionesContratoService(
            ContratoAccesoService accesoService,
            TipoAcuerdoContratoRepository tipoAcuerdoRepository,
            MiembroProyectoRepository miembroRepository,
            ContratoRepository contratoRepository,
            PlanificacionRepository planificacionRepository) {
        this.accesoService = accesoService;
        this.tipoAcuerdoRepository = tipoAcuerdoRepository;
        this.miembroRepository = miembroRepository;
        this.contratoRepository = contratoRepository;
        this.planificacionRepository = planificacionRepository;
    }

    @Transactional(readOnly = true)
    public OpcionesGeneracionContratoResponse obtener(
            Long idUsuario,
            Long idPerfilActivo,
            Long idProyecto) {

        ContratoAccesoService.ContextoAcceso acceso =
                accesoService.cargar(idUsuario, idPerfilActivo, idProyecto);

        List<MiembroProyecto> miembros = miembroRepository
                .buscarActivosDeProyectoConPerfilProfesionYRol(idProyecto);

        MiembroProyecto emisor = miembros.stream()
                .filter(m -> m.getPerfil() != null
                        && idPerfilActivo.equals(m.getPerfil().getIdPerfil()))
                .findFirst()
                .orElse(null);

        LocalDate fechaEntrega = planificacionRepository
                .findByProyectoIdProyecto(idProyecto)
                .map(Planificacion::getFechaEntrega)
                .orElse(null);

        List<TipoAcuerdoContratoResponse> tipos = tipoAcuerdoRepository
                .findByActivoTrueOrderByNombreAsc()
                .stream()
                .map(ContratoMapper::toTipoAcuerdoResponse)
                .toList();

        List<MiembroDestinatarioResponse> miembrosResponse = miembros.stream()
                .map(m -> ContratoMapper.toMiembroResponse(m, idPerfilActivo))
                .toList();

        List<ContratoActivoMiembroResponse> activos = contratoRepository
                .findActivosPorMiembroDeProyecto(idProyecto)
                .stream()
                .map(ContratoMapper::toContratoActivoResponse)
                .toList();

        EmisorContratoResponse emisorResponse = ContratoMapper.toEmisorResponse(emisor);

        return new OpcionesGeneracionContratoResponse(
                ContratoMapper.toProyectoResponse(acceso.proyecto(), fechaEntrega),
                emisorResponse,
                tipos,
                miembrosResponse,
                activos);
    }
}
