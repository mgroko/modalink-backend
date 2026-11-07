package org.mgroko.backend.contratos.servicio;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.mgroko.backend.contratos.dto.ConflictoContratoResponse;
import org.mgroko.backend.contratos.exception.ContratoNoGenerableException;
import org.mgroko.backend.contratos.exception.TipoAcuerdoContratoNoEncontradoException;
import org.mgroko.backend.contratos.mapper.ContratoMapper;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.TipoAcuerdoContrato;
import org.mgroko.backend.proyectos.exception.AccesoDenegadoProyectoException;
import org.mgroko.backend.repositorio.ContratoRepository;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
import org.mgroko.backend.repositorio.TipoAcuerdoContratoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Carga y valida el contexto necesario para previsualizar o generar un
 * contrato: proyecto, tipo de acuerdo, emisor, destinatarios y conflictos por
 * contratos ACTIVOS del mismo tipo.
 */
@Service
public class ContratoContextoService {

    private final ContratoAccesoService accesoService;
    private final TipoAcuerdoContratoRepository tipoAcuerdoRepository;
    private final MiembroProyectoRepository miembroRepository;
    private final ContratoRepository contratoRepository;

    public ContratoContextoService(
            ContratoAccesoService accesoService,
            TipoAcuerdoContratoRepository tipoAcuerdoRepository,
            MiembroProyectoRepository miembroRepository,
            ContratoRepository contratoRepository) {
        this.accesoService = accesoService;
        this.tipoAcuerdoRepository = tipoAcuerdoRepository;
        this.miembroRepository = miembroRepository;
        this.contratoRepository = contratoRepository;
    }

    @Transactional(readOnly = true)
    public ContextoContrato cargar(
            Long idUsuario,
            Long idPerfilActivo,
            Long idProyecto,
            Long idTipoAcuerdo,
            List<Long> idsMiembro) {

        ContratoAccesoService.ContextoAcceso acceso =
                accesoService.cargar(idUsuario, idPerfilActivo, idProyecto);

        TipoAcuerdoContrato tipo = tipoAcuerdoRepository
                .findByIdTipoAcuerdoAndActivoTrue(idTipoAcuerdo)
                .orElseThrow(() -> new TipoAcuerdoContratoNoEncontradoException(idTipoAcuerdo));

        List<MiembroProyecto> miembros = miembroRepository
                .buscarActivosDeProyectoConPerfilProfesionYRol(idProyecto);

        if (miembros.isEmpty()) {
            throw new ContratoNoGenerableException(
                    "PROYECTO_SIN_MIEMBROS_ACTIVOS",
                    "El proyecto no tiene miembros activos para generar un contrato.");
        }

        MiembroProyecto emisor = miembros.stream()
                .filter(m -> m.getPerfil() != null
                        && idPerfilActivo.equals(m.getPerfil().getIdPerfil()))
                .findFirst()
                .orElseThrow(() -> new AccesoDenegadoProyectoException(
                        "No se encontro la membresia activa del director en el proyecto."));

        List<Long> ids = idsMiembro == null
                ? List.of()
                : idsMiembro.stream()
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();

        if (ids.isEmpty()) {
            throw new ContratoNoGenerableException(
                    "CONTRATO_SIN_DESTINATARIOS",
                    "Debe seleccionar al menos un miembro destinatario.");
        }

        Map<Long, MiembroProyecto> miembrosPorId = new LinkedHashMap<>();
        for (MiembroProyecto miembro : miembros) {
            miembrosPorId.put(miembro.getIdMiembro(), miembro);
        }

        List<MiembroProyecto> destinatarios = new ArrayList<>();
        List<ConflictoContratoResponse> conflictos = new ArrayList<>();

        for (Long idMiembro : ids) {
            MiembroProyecto miembro = miembrosPorId.get(idMiembro);
            if (miembro == null) {
                conflictos.add(new ConflictoContratoResponse(
                        "DESTINATARIO_INVALIDO",
                        idMiembro,
                        null,
                        "El miembro " + idMiembro
                                + " no pertenece al proyecto o no esta activo."));
            } else {
                destinatarios.add(miembro);
            }
        }

        if (!conflictos.isEmpty()) {
            throw new ContratoNoGenerableException(
                    "DESTINATARIO_INVALIDO",
                    "La seleccion contiene miembros invalidos para este proyecto.",
                    conflictos);
        }

        List<Long> idsDuplicados = contratoRepository
                .findMiembrosConContratoActivo(idProyecto, idTipoAcuerdo, ids);

        for (Long idMiembro : idsDuplicados) {
            MiembroProyecto miembro = miembrosPorId.get(idMiembro);
            String nombre = miembro != null
                    ? ContratoMapper.nombreCompleto(miembro.getPerfil())
                    : "Miembro " + idMiembro;
            conflictos.add(new ConflictoContratoResponse(
                    "CONTRATO_DUPLICADO_ACTIVO",
                    idMiembro,
                    nombre,
                    nombre + " ya tiene un contrato ACTIVO de este tipo en el proyecto."));
        }

        return new ContextoContrato(
                acceso.perfil(),
                acceso.proyecto(),
                tipo,
                emisor,
                List.copyOf(destinatarios),
                List.copyOf(conflictos));
    }

    public record ContextoContrato(
            Perfil perfil,
            Proyecto proyecto,
            TipoAcuerdoContrato tipoAcuerdo,
            MiembroProyecto emisor,
            List<MiembroProyecto> destinatarios,
            List<ConflictoContratoResponse> conflictos) {
    }
}
