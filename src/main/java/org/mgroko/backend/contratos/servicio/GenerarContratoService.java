package org.mgroko.backend.contratos.servicio;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.mgroko.backend.auditoria.servicio.AuditarOperacion;
import org.mgroko.backend.contratos.dto.ContratoGeneradoResponse;
import org.mgroko.backend.contratos.dto.GenerarContratoRequest;
import org.mgroko.backend.contratos.exception.ContratoNoEncontradoException;
import org.mgroko.backend.contratos.exception.ContratoNoGenerableException;
import org.mgroko.backend.contratos.mapper.ContratoMapper;
import org.mgroko.backend.modelo.Contrato;
import org.mgroko.backend.modelo.FirmaContrato;
import org.mgroko.backend.modelo.enums.EstadoContrato;
import org.mgroko.backend.modelo.enums.EstadoFirmaContrato;
import org.mgroko.backend.modelo.enums.RolParteContrato;
import org.mgroko.backend.repositorio.ContratoRepository;
import org.mgroko.backend.repositorio.FirmaContratoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

/**
 * UC-39: genera y emite un contrato en estado PENDIENTE_FIRMA.
 *
 * <p>El contrato se inserta primero como BORRADOR porque el trigger
 * fn_validar_contrato_insert lo exige. Luego se agregan las firmas de los
 * destinatarios y, por ultimo, se cambia el estado a PENDIENTE_FIRMA. En esa
 * transicion la base de datos valida, setea fecha_emision, agrega la firma del
 * director, registra el evento EMITIDO y crea las notificaciones.</p>
 */
@Service
public class GenerarContratoService {

    private final ContratoContextoService contextoService;
    private final GeneradorTextoContratoService generadorTextoService;
    private final ContratoRepository contratoRepository;
    private final FirmaContratoRepository firmaContratoRepository;
    private final EntityManager entityManager;
    private final Clock reloj;

    public GenerarContratoService(
            ContratoContextoService contextoService,
            GeneradorTextoContratoService generadorTextoService,
            ContratoRepository contratoRepository,
            FirmaContratoRepository firmaContratoRepository,
            EntityManager entityManager,
            Clock reloj) {
        this.contextoService = contextoService;
        this.generadorTextoService = generadorTextoService;
        this.contratoRepository = contratoRepository;
        this.firmaContratoRepository = firmaContratoRepository;
        this.entityManager = entityManager;
        this.reloj = reloj;
    }

    @Transactional
    @AuditarOperacion
    public ContratoGeneradoResponse generar(
            Long idUsuario,
            Long idPerfilActivo,
            Long idProyecto,
            GenerarContratoRequest request) {

        ContratoContextoService.ContextoContrato contexto = contextoService.cargar(
                idUsuario,
                idPerfilActivo,
                idProyecto,
                request.idTipoAcuerdo(),
                request.idsMiembro());

        if (!contexto.conflictos().isEmpty()) {
            throw new ContratoNoGenerableException(
                    "CONTRATO_DUPLICADO_ACTIVO",
                    "Uno o mas destinatarios ya tienen un contrato ACTIVO de este tipo en el proyecto.",
                    contexto.conflictos());
        }

        String texto = generadorTextoService.generar(
                contexto,
                request.clausulasAdicionales());

        LocalDateTime ahora = LocalDateTime.now(reloj);

        Contrato contrato = Contrato.builder()
                .proyecto(contexto.proyecto())
                .tipoAcuerdo(contexto.tipoAcuerdo())
                .miembroEmisor(contexto.emisor())
                .nombre(generadorTextoService.generarNombre(contexto))
                .descripcion(generadorTextoService.generarDescripcion(contexto))
                .texto(texto)
                .versionActual(1)
                .fechaCreacion(ahora)
                .estado(EstadoContrato.BORRADOR)
                .build();

        contratoRepository.saveAndFlush(contrato);

        List<FirmaContrato> firmas = contexto.destinatarios().stream()
                .map(destinatario -> FirmaContrato.builder()
                        .idMiembro(destinatario.getIdMiembro())
                        .idContrato(contrato.getIdContrato())
                        .rolParte(RolParteContrato.DESTINATARIO)
                        .estadoFirma(EstadoFirmaContrato.PENDIENTE)
                        .build())
                .toList();

        firmaContratoRepository.saveAll(firmas);
        firmaContratoRepository.flush();

        contrato.setEstado(EstadoContrato.PENDIENTE_FIRMA);
        contrato.setFechaEmision(LocalDateTime.now(reloj));
        contratoRepository.saveAndFlush(contrato);

        // La base agrega la firma del director y las notificaciones. Se limpia
        // el contexto para leer el estado final sin valores cacheados.
        entityManager.flush();
        entityManager.clear();

        Contrato actualizado = contratoRepository
                .findByIdConPartes(contrato.getIdContrato())
                .orElseThrow(() -> new ContratoNoEncontradoException(contrato.getIdContrato()));

        List<FirmaContrato> partes = firmaContratoRepository
                .findConPartes(contrato.getIdContrato());

        return ContratoMapper.toContratoResponse(actualizado, partes);
    }
}
