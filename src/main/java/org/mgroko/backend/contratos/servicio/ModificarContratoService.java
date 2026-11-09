package org.mgroko.backend.contratos.servicio;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.mgroko.backend.auditoria.servicio.AuditarOperacion;
import org.mgroko.backend.contratos.dto.ConflictoContratoResponse;
import org.mgroko.backend.contratos.dto.ContratoDetalleResponse;
import org.mgroko.backend.contratos.dto.ModificarContratoRequest;
import org.mgroko.backend.contratos.exception.ContratoNoGenerableException;
import org.mgroko.backend.modelo.Contrato;
import org.mgroko.backend.repositorio.ContratoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

/**
 * UC-53: modifica un contrato PENDIENTE_FIRMA, reconcilia sus firmas y deja que
 * los triggers registren la version, el evento MODIFICADO y la notificacion
 * CONTRATO_MODIFICADO.
 */
@Service
public class ModificarContratoService {

    private final ContratoModificacionContextoService contextoService;
    private final ContratoFirmaReconciliacionService firmaReconciliacionService;
    private final ContratoRepository contratoRepository;
    private final ObtenerContratoService obtenerContratoService;
    private final EntityManager entityManager;
    private final Clock reloj;

    public ModificarContratoService(
            ContratoModificacionContextoService contextoService,
            ContratoFirmaReconciliacionService firmaReconciliacionService,
            ContratoRepository contratoRepository,
            ObtenerContratoService obtenerContratoService,
            EntityManager entityManager,
            Clock reloj) {
        this.contextoService = contextoService;
        this.firmaReconciliacionService = firmaReconciliacionService;
        this.contratoRepository = contratoRepository;
        this.obtenerContratoService = obtenerContratoService;
        this.entityManager = entityManager;
        this.reloj = reloj;
    }

    @Transactional
    @AuditarOperacion
    public ContratoDetalleResponse modificar(
            Long idUsuario,
            Long idPerfilActivo,
            Long idProyecto,
            Long idContrato,
            ModificarContratoRequest request) {

        ContratoModificacionContextoService.ContextoModificacion contexto =
                contextoService.cargarParaModificar(
                        idUsuario,
                        idPerfilActivo,
                        idProyecto,
                        idContrato,
                        request);

        if (!contexto.conflictos().isEmpty()) {
            throw conflictoException(contexto.conflictos());
        }

        firmaReconciliacionService.reconciliar(
                contexto.contrato(),
                contexto.firmas(),
                contexto.destinatarios());

        Contrato contrato = contexto.contrato();
        contrato.setTipoAcuerdo(contexto.tipoAcuerdo());
        contrato.setNombre(contexto.nombre());
        contrato.setDescripcion(contexto.descripcion());
        contrato.setTexto(contexto.texto());
        // Se setea siempre para que el trigger AFTER de modificacion se dispare
        // aunque solo hayan cambiado las partes.
        contrato.setFechaModificacion(LocalDateTime.now(reloj));

        contratoRepository.saveAndFlush(contrato);

        entityManager.flush();
        entityManager.clear();

        return obtenerContratoService.obtener(
                idUsuario,
                idPerfilActivo,
                idProyecto,
                idContrato);
    }

    private ContratoNoGenerableException conflictoException(
            List<ConflictoContratoResponse> conflictos) {

        boolean duplicado = conflictos.stream()
                .anyMatch(c -> "CONTRATO_DUPLICADO_ACTIVO".equals(c.codigo()));
        boolean sinDestinatarios = conflictos.stream()
                .anyMatch(c -> "CONTRATO_SIN_DESTINATARIOS".equals(c.codigo()));

        String codigo = duplicado
                ? "CONTRATO_DUPLICADO_ACTIVO"
                : sinDestinatarios
                        ? "CONTRATO_SIN_DESTINATARIOS"
                        : "DESTINATARIO_INVALIDO";

        String mensaje = duplicado
                ? "Uno o mas destinatarios ya tienen un contrato ACTIVO de este tipo en el proyecto."
                : sinDestinatarios
                        ? "Debe seleccionar al menos un miembro destinatario."
                        : "La seleccion contiene miembros invalidos para este proyecto.";

        return new ContratoNoGenerableException(codigo, mensaje, conflictos);
    }
}
