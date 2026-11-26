package org.mgroko.backend.contratos.servicio;

import java.time.Clock;
import java.time.LocalDateTime;

import org.mgroko.backend.auditoria.servicio.AuditarOperacion;
import org.mgroko.backend.contratos.dto.ContratoDetalleResponse;
import org.mgroko.backend.contratos.dto.GuardarContratoRequest;
import org.mgroko.backend.modelo.Contrato;
import org.mgroko.backend.modelo.enums.EstadoContrato;
import org.mgroko.backend.repositorio.ContratoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

/**
 * Crea un contrato en estado BORRADOR.
 *
 * <p>El alta no dispara notificaciones: el trigger de emision solo actua al
 * pasar a PENDIENTE_FIRMA. El director puede completar nombre, descripcion,
 * destinatarios y texto antes de emitir.</p>
 */
@Service
public class CrearContratoBorradorService {

    private final ContratoBorradorContextoService contextoService;
    private final ContratoPartesBorradorService partesService;
    private final ContratoRepository contratoRepository;
    private final ObtenerContratoService obtenerContratoService;
    private final EntityManager entityManager;
    private final Clock reloj;

    public CrearContratoBorradorService(
            ContratoBorradorContextoService contextoService,
            ContratoPartesBorradorService partesService,
            ContratoRepository contratoRepository,
            ObtenerContratoService obtenerContratoService,
            EntityManager entityManager,
            Clock reloj) {
        this.contextoService = contextoService;
        this.partesService = partesService;
        this.contratoRepository = contratoRepository;
        this.obtenerContratoService = obtenerContratoService;
        this.entityManager = entityManager;
        this.reloj = reloj;
    }

    @Transactional
    @AuditarOperacion
    public ContratoDetalleResponse crear(
            Long idUsuario,
            Long idPerfilActivo,
            Long idProyecto,
            GuardarContratoRequest request) {

        ContratoBorradorContextoService.ContextoBorrador contexto =
                contextoService.cargar(
                        idUsuario,
                        idPerfilActivo,
                        idProyecto,
                        request);

        Contrato contrato = Contrato.builder()
                .proyecto(contexto.proyecto())
                .tipoAcuerdo(contexto.tipoAcuerdo())
                .miembroEmisor(contexto.emisor())
                .nombre(contexto.nombre())
                .descripcion(contexto.descripcion())
                .texto(contexto.texto())
                .versionActual(1)
                .fechaCreacion(LocalDateTime.now(reloj))
                .estado(EstadoContrato.BORRADOR)
                .build();

        contratoRepository.saveAndFlush(contrato);

        partesService.sincronizar(
                contrato.getIdContrato(),
                contexto.emisor().getIdMiembro(),
                contexto.destinatarios());

        entityManager.flush();
        entityManager.clear();

        return obtenerContratoService.obtener(
                idUsuario,
                idPerfilActivo,
                idProyecto,
                contrato.getIdContrato());
    }
}
