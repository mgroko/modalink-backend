package org.mgroko.backend.contratos.servicio;

import org.mgroko.backend.auditoria.servicio.AuditarOperacion;
import org.mgroko.backend.contratos.dto.ContratoDetalleResponse;
import org.mgroko.backend.modelo.enums.EstadoFirmaContrato;
import org.mgroko.backend.repositorio.FirmaContratoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

/**
 * UC-55: registra la firma del perfil activo en un contrato PENDIENTE_FIRMA.
 *
 * <p>El trigger de firma setea fecha_firma y la version actual. Si con esta
 * firma todas las partes activas firmaron, el trigger de estado pasa el
 * contrato a ACTIVO y notifica CONTRATO_ACTIVO.</p>
 */
@Service
public class FirmarContratoService {

    private final ContratoFirmaContextoService contextoService;
    private final FirmaContratoRepository firmaContratoRepository;
    private final ObtenerContratoService obtenerContratoService;
    private final EntityManager entityManager;

    public FirmarContratoService(
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
    public ContratoDetalleResponse firmar(
            Long idUsuario,
            Long idPerfilActivo,
            Long idProyecto,
            Long idContrato) {

        ContratoFirmaContextoService.ContextoFirma contexto =
                contextoService.cargar(idUsuario, idPerfilActivo, idProyecto, idContrato);

        contexto.firma().setEstadoFirma(EstadoFirmaContrato.FIRMADO);
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
