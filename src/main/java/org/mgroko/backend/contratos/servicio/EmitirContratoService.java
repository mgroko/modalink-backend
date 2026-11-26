package org.mgroko.backend.contratos.servicio;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.mgroko.backend.auditoria.servicio.AuditarOperacion;
import org.mgroko.backend.contratos.dto.ConflictoContratoResponse;
import org.mgroko.backend.contratos.dto.ContratoDetalleResponse;
import org.mgroko.backend.contratos.exception.ContratoNoEncontradoException;
import org.mgroko.backend.contratos.exception.ContratoNoGenerableException;
import org.mgroko.backend.contratos.exception.ContratoNoModificableException;
import org.mgroko.backend.contratos.mapper.ContratoMapper;
import org.mgroko.backend.modelo.Contrato;
import org.mgroko.backend.modelo.FirmaContrato;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.enums.EstadoContrato;
import org.mgroko.backend.modelo.enums.EstadoParticipacion;
import org.mgroko.backend.modelo.enums.RolParteContrato;
import org.mgroko.backend.repositorio.ContratoRepository;
import org.mgroko.backend.repositorio.FirmaContratoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

/**
 * Emite un contrato en BORRADOR y lo deja en PENDIENTE_FIRMA.
 *
 * <p>Esta es la unica operacion del flujo que notifica a las partes. Los
 * triggers de la base crean la version definitiva, la firma del director, el
 * evento EMITIDO y las notificaciones.</p>
 */
@Service
public class EmitirContratoService {

    private final ContratoAccesoService accesoService;
    private final ContratoRepository contratoRepository;
    private final FirmaContratoRepository firmaContratoRepository;
    private final GeneradorTextoContratoService generadorTextoService;
    private final ObtenerContratoService obtenerContratoService;
    private final EntityManager entityManager;
    private final Clock reloj;

    public EmitirContratoService(
            ContratoAccesoService accesoService,
            ContratoRepository contratoRepository,
            FirmaContratoRepository firmaContratoRepository,
            GeneradorTextoContratoService generadorTextoService,
            ObtenerContratoService obtenerContratoService,
            EntityManager entityManager,
            Clock reloj) {
        this.accesoService = accesoService;
        this.contratoRepository = contratoRepository;
        this.firmaContratoRepository = firmaContratoRepository;
        this.generadorTextoService = generadorTextoService;
        this.obtenerContratoService = obtenerContratoService;
        this.entityManager = entityManager;
        this.reloj = reloj;
    }

    @Transactional
    @AuditarOperacion
    public ContratoDetalleResponse emitir(
            Long idUsuario,
            Long idPerfilActivo,
            Long idProyecto,
            Long idContrato) {

        accesoService.cargar(idUsuario, idPerfilActivo, idProyecto);

        Contrato contrato = contratoRepository.findByIdForUpdate(idContrato)
                .orElseThrow(() -> new ContratoNoEncontradoException(idContrato));

        if (contrato.getProyecto() == null
                || !idProyecto.equals(contrato.getProyecto().getIdProyecto())) {
            throw new ContratoNoEncontradoException(idContrato);
        }

        if (contrato.getEstado() != EstadoContrato.BORRADOR) {
            throw new ContratoNoModificableException(
                    "CONTRATO_NO_BORRADOR",
                    "Solo se puede emitir un contrato en estado BORRADOR.");
        }

        if (contrato.getTipoAcuerdo() == null
                || !Boolean.TRUE.equals(contrato.getTipoAcuerdo().getActivo())) {
            throw new ContratoNoGenerableException(
                    "TIPO_ACUERDO_INVALIDO",
                    "El tipo de acuerdo del contrato no existe o no esta activo.");
        }

        List<FirmaContrato> firmas =
                firmaContratoRepository.findConPartes(idContrato);

        List<FirmaContrato> destinatarios = firmas.stream()
                .filter(this::esDestinatario)
                .toList();

        if (destinatarios.isEmpty()) {
            throw new ContratoNoGenerableException(
                    "CONTRATO_SIN_DESTINATARIOS",
                    "Debe seleccionar al menos un miembro destinatario.");
        }

        List<ConflictoContratoResponse> invalidos = new ArrayList<>();
        for (FirmaContrato firma : destinatarios) {
            MiembroProyecto miembro = firma.getMiembro();
            if (miembro == null
                    || miembro.getEstadoParticipacion() != EstadoParticipacion.ACTIVO
                    || miembro.getProyecto() == null
                    || !idProyecto.equals(miembro.getProyecto().getIdProyecto())) {
                invalidos.add(new ConflictoContratoResponse(
                        "DESTINATARIO_INVALIDO",
                        firma.getIdMiembro(),
                        nombreParte(firma),
                        "El miembro " + firma.getIdMiembro()
                                + " ya no es un miembro activo del proyecto."));
            }
        }

        if (!invalidos.isEmpty()) {
            throw new ContratoNoGenerableException(
                    "DESTINATARIO_INVALIDO",
                    "La seleccion contiene miembros invalidos para este proyecto.",
                    invalidos);
        }

        generadorTextoService.validarTexto(contrato.getTexto());

        List<Long> idsDestinatarios = destinatarios.stream()
                .map(FirmaContrato::getIdMiembro)
                .distinct()
                .toList();

        List<Long> idsDuplicados = contratoRepository
                .findMiembrosConContratoActivo(
                        idProyecto,
                        contrato.getTipoAcuerdo().getIdTipoAcuerdo(),
                        idsDestinatarios);

        if (!idsDuplicados.isEmpty()) {
            Map<Long, FirmaContrato> firmasPorMiembro = destinatarios.stream()
                    .collect(Collectors.toMap(
                            FirmaContrato::getIdMiembro,
                            Function.identity(),
                            (a, b) -> a));

            List<ConflictoContratoResponse> conflictos = idsDuplicados.stream()
                    .map(id -> {
                        FirmaContrato firma = firmasPorMiembro.get(id);
                        String nombre = firma != null ? nombreParte(firma) : "Miembro " + id;
                        return new ConflictoContratoResponse(
                                "CONTRATO_DUPLICADO_ACTIVO",
                                id,
                                nombre,
                                nombre + " ya tiene un contrato ACTIVO de este tipo en el proyecto.");
                    })
                    .toList();

            throw new ContratoNoGenerableException(
                    "CONTRATO_DUPLICADO_ACTIVO",
                    "Uno o mas destinatarios ya tienen un contrato ACTIVO de este tipo en el proyecto.",
                    conflictos);
        }

        contrato.setEstado(EstadoContrato.PENDIENTE_FIRMA);
        contrato.setFechaEmision(LocalDateTime.now(reloj));
        contratoRepository.saveAndFlush(contrato);

        entityManager.flush();
        entityManager.clear();

        return obtenerContratoService.obtener(
                idUsuario,
                idPerfilActivo,
                idProyecto,
                idContrato);
    }

    private boolean esDestinatario(FirmaContrato firma) {
        return firma.getRolParte() == RolParteContrato.DESTINATARIO
                || firma.getRolParte() == RolParteContrato.DIRECTOR_DESTINATARIO;
    }

    private String nombreParte(FirmaContrato firma) {
        return firma.getMiembro() != null
                ? ContratoMapper.nombreCompleto(firma.getMiembro().getPerfil())
                : "Miembro " + firma.getIdMiembro();
    }
}
