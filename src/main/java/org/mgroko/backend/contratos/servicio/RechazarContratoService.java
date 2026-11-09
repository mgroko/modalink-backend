package org.mgroko.backend.contratos.servicio;

import org.mgroko.backend.auditoria.servicio.AuditarOperacion;
import org.mgroko.backend.contratos.dto.ContratoDetalleResponse;
import org.mgroko.backend.contratos.dto.RechazarContratoRequest;
import org.mgroko.backend.contratos.exception.ContratoNoModificableException;
import org.mgroko.backend.modelo.enums.EstadoFirmaContrato;
import org.mgroko.backend.repositorio.FirmaContratoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

/**
 * UC-55 (extension confirmada): rechaza el contrato con un motivo.
 *
 * <p>El trigger de firma setea fecha_rechazo y version; el trigger de estado
 * pasa el contrato a RECHAZADO_POR_PARTES, cancela las firmas pendientes y
 * notifica CONTRATO_RECHAZADO a partes y director.</p>
 */
@Service
public class RechazarContratoService {

    private final ContratoFirmaContextoService contextoService;
    private final FirmaContratoRepository firmaContratoRepository;
    private final ObtenerContratoService obtenerContratoService;
    private final EntityManager entityManager;

    public RechazarContratoService(
            ContratoFirmaContextoService contextoService,
            FirmaContratoRepository firmaContratoRepository,
            ObtenerContratoService obtenerContratoService,
            EntityManager entityManager) {
        this.contextoService = contextoService;
        this.firmaContratoRepository = firmaContratoRepository;
        this.obtenerContratoService = obtenerContratoService;
        this.entityManager = entityManager;
    }

    @Transactional
    @AuditarOperacion
    public ContratoDetalleResponse rechazar(
            Long idUsuario,
            Long idPerfilActivo,
            Long idProyecto,
            Long idContrato,
            RechazarContratoRequest request) {

        String motivo = request.motivo() == null ? null : request.motivo().trim();
        if (motivo == null || motivo.isBlank()) {
            throw new ContratoNoModificableException(
                    "MOTIVO_RECHAZO_OBLIGATORIO",
                    "El motivo de rechazo es obligatorio.");
        }

        ContratoFirmaContextoService.ContextoFirma contexto =
                contextoService.cargar(idUsuario, idPerfilActivo, idProyecto, idContrato);

        contexto.firma().setEstadoFirma(EstadoFirmaContrato.RECHAZADO);
        contexto.firma().setMotivoRechazo(motivo);
        firmaContratoRepository.saveAndFlush(contexto.firma());

        entityManager.flush();
        entityManager.clear();

        return obtenerContratoService.obtener(
                idUsuario,
                idPerfilActivo,
                idProyecto,
                idContrato);
    }
}
