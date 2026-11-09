package org.mgroko.backend.contratos.servicio;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.mgroko.backend.contratos.dto.ConflictoContratoResponse;
import org.mgroko.backend.contratos.dto.ModificarContratoRequest;
import org.mgroko.backend.contratos.exception.ContratoNoEncontradoException;
import org.mgroko.backend.contratos.exception.ContratoNoGenerableException;
import org.mgroko.backend.contratos.exception.ContratoNoModificableException;
import org.mgroko.backend.contratos.exception.TipoAcuerdoContratoNoEncontradoException;
import org.mgroko.backend.contratos.mapper.ContratoMapper;
import org.mgroko.backend.modelo.Contrato;
import org.mgroko.backend.modelo.FirmaContrato;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.TipoAcuerdoContrato;
import org.mgroko.backend.modelo.enums.EstadoContrato;
import org.mgroko.backend.modelo.enums.EstadoFirmaContrato;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.repositorio.ContratoRepository;
import org.mgroko.backend.repositorio.FirmaContratoRepository;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
import org.mgroko.backend.repositorio.TipoAcuerdoContratoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Carga y valida el contexto de UC-53. Reutiliza las validaciones de acceso de
 * UC-39 y agrega las reglas propias de la modificacion: estado, firmas
 * registradas, destinatarios y duplicados activos.
 */
@Service
public class ContratoModificacionContextoService {

    private final ContratoAccesoService accesoService;
    private final ContratoRepository contratoRepository;
    private final FirmaContratoRepository firmaContratoRepository;
    private final MiembroProyectoRepository miembroRepository;
    private final TipoAcuerdoContratoRepository tipoAcuerdoRepository;
    private final GeneradorTextoContratoService generadorTextoService;

    public ContratoModificacionContextoService(
            ContratoAccesoService accesoService,
            ContratoRepository contratoRepository,
            FirmaContratoRepository firmaContratoRepository,
            MiembroProyectoRepository miembroRepository,
            TipoAcuerdoContratoRepository tipoAcuerdoRepository,
            GeneradorTextoContratoService generadorTextoService) {
        this.accesoService = accesoService;
        this.contratoRepository = contratoRepository;
        this.firmaContratoRepository = firmaContratoRepository;
        this.miembroRepository = miembroRepository;
        this.tipoAcuerdoRepository = tipoAcuerdoRepository;
        this.generadorTextoService = generadorTextoService;
    }

    @Transactional(readOnly = true)
    public ContextoModificacion cargarParaPreview(
            Long idUsuario,
            Long idPerfilActivo,
            Long idProyecto,
            Long idContrato,
            ModificarContratoRequest request) {
        return cargar(idUsuario, idPerfilActivo, idProyecto, idContrato, request, false);
    }

    @Transactional
    public ContextoModificacion cargarParaModificar(
            Long idUsuario,
            Long idPerfilActivo,
            Long idProyecto,
            Long idContrato,
            ModificarContratoRequest request) {
        return cargar(idUsuario, idPerfilActivo, idProyecto, idContrato, request, true);
    }

    private ContextoModificacion cargar(
            Long idUsuario,
            Long idPerfilActivo,
            Long idProyecto,
            Long idContrato,
            ModificarContratoRequest request,
            boolean bloquear) {

        ContratoAccesoService.ContextoAcceso acceso =
                accesoService.cargar(idUsuario, idPerfilActivo, idProyecto);

        Contrato contrato = bloquear
                ? contratoRepository.findByIdForUpdate(idContrato)
                        .orElseThrow(() -> new ContratoNoEncontradoException(idContrato))
                : contratoRepository.findByIdConPartes(idContrato)
                        .orElseThrow(() -> new ContratoNoEncontradoException(idContrato));

        if (contrato.getProyecto() == null
                || !idProyecto.equals(contrato.getProyecto().getIdProyecto())) {
            throw new ContratoNoEncontradoException(idContrato);
        }

        if (contrato.getEstado() != EstadoContrato.PENDIENTE_FIRMA) {
            throw new ContratoNoModificableException(
                    "CONTRATO_NO_PENDIENTE_FIRMA",
                    "Solo se puede modificar un contrato en estado PENDIENTE_FIRMA.");
        }

        Proyecto proyecto = contrato.getProyecto();
        if (proyecto.getEstado() != EstadoProyecto.PUBLICADO
                && proyecto.getEstado() != EstadoProyecto.CONFIRMADO) {
            throw new ContratoNoModificableException(
                    "PROYECTO_NO_APTO_PARA_CONTRATO",
                    "Solo se puede modificar un contrato de un proyecto PUBLICADO o CONFIRMADO.");
        }

        List<FirmaContrato> firmas = firmaContratoRepository.findConPartes(idContrato);

        boolean firmasRegistradas = firmas.stream()
                .anyMatch(f -> f.getEstadoFirma() == EstadoFirmaContrato.FIRMADO
                        || f.getEstadoFirma() == EstadoFirmaContrato.RECHAZADO
                        || f.getEstadoFirma() == EstadoFirmaContrato.INVALIDADA);

        if (firmasRegistradas) {
            throw new ContratoNoModificableException(
                    "CONTRATO_CON_FIRMAS_REGISTRADAS",
                    "No se puede modificar un contrato con firmas registradas.");
        }

        TipoAcuerdoContrato tipo = tipoAcuerdoRepository
                .findByIdTipoAcuerdoAndActivoTrue(request.idTipoAcuerdo())
                .orElseThrow(() -> new TipoAcuerdoContratoNoEncontradoException(
                        request.idTipoAcuerdo()));

        List<MiembroProyecto> miembros = miembroRepository
                .buscarActivosDeProyectoConPerfilProfesionYRol(idProyecto);

        if (miembros.isEmpty()) {
            throw new ContratoNoGenerableException(
                    "PROYECTO_SIN_MIEMBROS_ACTIVOS",
                    "El proyecto no tiene miembros activos para modificar el contrato.");
        }

        MiembroProyecto emisor = contrato.getMiembroEmisor();
        if (emisor == null) {
            throw new ContratoNoModificableException(
                    "CONTRATO_SIN_EMISOR",
                    "El contrato no tiene un director emisor válido.");
        }

        List<Long> ids = request.idsMiembro() == null
                ? List.of()
                : request.idsMiembro().stream()
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();

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

        List<Long> idsValidos = destinatarios.stream()
                .map(MiembroProyecto::getIdMiembro)
                .toList();

        if (idsValidos.isEmpty()) {
            conflictos.add(new ConflictoContratoResponse(
                    "CONTRATO_SIN_DESTINATARIOS",
                    null,
                    null,
                    "Debe seleccionar al menos un miembro destinatario."));
        } else {
            List<Long> idsDuplicados = contratoRepository
                    .findMiembrosConContratoActivo(idProyecto, tipo.getIdTipoAcuerdo(), idsValidos);

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
        }

        String texto;
        if (request.texto() != null && !request.texto().isBlank()) {
            texto = request.texto();
            generadorTextoService.validarTexto(texto);
        } else {
            texto = generadorTextoService.generar(
                    proyecto,
                    tipo,
                    emisor,
                    destinatarios,
                    request.clausulasAdicionales());
        }

        String nombre = resolverNombre(contrato, proyecto, tipo, request.nombre());
        String descripcion = request.descripcion() != null
                ? request.descripcion()
                : contrato.getDescripcion();

        return new ContextoModificacion(
                contrato,
                firmas,
                proyecto,
                tipo,
                emisor,
                List.copyOf(destinatarios),
                List.copyOf(conflictos),
                texto,
                nombre,
                descripcion,
                acceso.perfil());
    }

    private String resolverNombre(
            Contrato contrato,
            Proyecto proyecto,
            TipoAcuerdoContrato tipo,
            String nombreSolicitado) {

        if (nombreSolicitado != null && !nombreSolicitado.isBlank()) {
            return nombreSolicitado;
        }

        boolean cambioTipo = contrato.getTipoAcuerdo() == null
                || !contrato.getTipoAcuerdo().getIdTipoAcuerdo()
                        .equals(tipo.getIdTipoAcuerdo());

        return cambioTipo
                ? generadorTextoService.generarNombre(proyecto, tipo)
                : contrato.getNombre();
    }

    public record ContextoModificacion(
            Contrato contrato,
            List<FirmaContrato> firmas,
            Proyecto proyecto,
            TipoAcuerdoContrato tipoAcuerdo,
            MiembroProyecto emisor,
            List<MiembroProyecto> destinatarios,
            List<ConflictoContratoResponse> conflictos,
            String texto,
            String nombre,
            String descripcion,
            Perfil perfil) {
    }
}
