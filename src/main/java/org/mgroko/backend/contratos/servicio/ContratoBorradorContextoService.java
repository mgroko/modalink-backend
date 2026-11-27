package org.mgroko.backend.contratos.servicio;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.mgroko.backend.contratos.dto.ConflictoContratoResponse;
import org.mgroko.backend.contratos.dto.GuardarContratoRequest;
import org.mgroko.backend.contratos.exception.ContratoNoGenerableException;
import org.mgroko.backend.contratos.exception.TipoAcuerdoContratoNoEncontradoException;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.TipoAcuerdoContrato;
import org.mgroko.backend.proyectos.exception.AccesoDenegadoProyectoException;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
import org.mgroko.backend.repositorio.TipoAcuerdoContratoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Carga y valida el contexto de un contrato en borrador.
 *
 * <p>A diferencia del contexto de emision, el borrador puede estar incompleto:
 * no exige destinatarios ni texto. Solo valida el acceso del director, el
 * proyecto, el tipo de acuerdo y que los destinatarios seleccionados sean
 * miembros activos del proyecto.</p>
 */
@Service
public class ContratoBorradorContextoService {

    private final ContratoAccesoService accesoService;
    private final TipoAcuerdoContratoRepository tipoAcuerdoRepository;
    private final MiembroProyectoRepository miembroRepository;
    private final ContratoTextoResolverService textoResolverService;
    private final GeneradorTextoContratoService generadorTextoService;

    public ContratoBorradorContextoService(
            ContratoAccesoService accesoService,
            TipoAcuerdoContratoRepository tipoAcuerdoRepository,
            MiembroProyectoRepository miembroRepository,
            ContratoTextoResolverService textoResolverService,
            GeneradorTextoContratoService generadorTextoService) {
        this.accesoService = accesoService;
        this.tipoAcuerdoRepository = tipoAcuerdoRepository;
        this.miembroRepository = miembroRepository;
        this.textoResolverService = textoResolverService;
        this.generadorTextoService = generadorTextoService;
    }

    @Transactional(readOnly = true)
    public ContextoBorrador cargar(
            Long idUsuario,
            Long idPerfilActivo,
            Long idProyecto,
            GuardarContratoRequest request) {

        ContratoAccesoService.ContextoAcceso acceso =
                accesoService.cargar(idUsuario, idPerfilActivo, idProyecto);

        TipoAcuerdoContrato tipo = tipoAcuerdoRepository
                .findByIdTipoAcuerdoAndActivoTrue(request.idTipoAcuerdo())
                .orElseThrow(() -> new TipoAcuerdoContratoNoEncontradoException(
                        request.idTipoAcuerdo()));

        List<MiembroProyecto> miembros = miembroRepository
                .buscarActivosDeProyectoConPerfilProfesionYRol(idProyecto);

        if (miembros.isEmpty()) {
            throw new ContratoNoGenerableException(
                    "PROYECTO_SIN_MIEMBROS_ACTIVOS",
                    "El proyecto no tiene miembros activos para crear un contrato.");
        }

        MiembroProyecto emisor = miembros.stream()
                .filter(m -> m.getPerfil() != null
                        && idPerfilActivo.equals(m.getPerfil().getIdPerfil()))
                .findFirst()
                .orElseThrow(() -> new AccesoDenegadoProyectoException(
                        "No se encontro la membresia activa del director en el proyecto."));

        Map<Long, MiembroProyecto> miembrosPorId = new LinkedHashMap<>();
        for (MiembroProyecto miembro : miembros) {
            miembrosPorId.put(miembro.getIdMiembro(), miembro);
        }

        List<Long> ids = request.idsMiembro() == null
                ? List.of()
                : request.idsMiembro().stream()
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();

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

        String texto = textoResolverService.resolver(
                acceso.proyecto(),
                tipo,
                emisor,
                destinatarios,
                request,
                true);

        String nombre = resolverNombre(acceso.proyecto(), tipo, request.nombre());
        String descripcion = request.descripcion() != null
                ? request.descripcion()
                : generadorTextoService.generarDescripcion(acceso.proyecto());

        return new ContextoBorrador(
                acceso.perfil(),
                acceso.proyecto(),
                tipo,
                emisor,
                List.copyOf(destinatarios),
                texto,
                nombre,
                descripcion);
    }

    private String resolverNombre(
            Proyecto proyecto,
            TipoAcuerdoContrato tipo,
            String nombreSolicitado) {

        if (nombreSolicitado != null && !nombreSolicitado.isBlank()) {
            return nombreSolicitado.trim();
        }

        return generadorTextoService.generarNombre(proyecto, tipo);
    }

    public record ContextoBorrador(
            Perfil perfil,
            Proyecto proyecto,
            TipoAcuerdoContrato tipoAcuerdo,
            MiembroProyecto emisor,
            List<MiembroProyecto> destinatarios,
            String texto,
            String nombre,
            String descripcion) {
    }
}
