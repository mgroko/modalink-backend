package org.mgroko.backend.contratos.servicio;

import org.mgroko.backend.auditoria.servicio.AuditarOperacion;
import org.mgroko.backend.contratos.exception.ContratoNoEncontradoException;
import org.mgroko.backend.contratos.exception.ContratoNoModificableException;
import org.mgroko.backend.modelo.Contrato;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.enums.EstadoContrato;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.perfiles.exception.PerfilNoEncontradoException;
import org.mgroko.backend.proyectos.servicio.ProyectoSecurityService;
import org.mgroko.backend.repositorio.ContratoRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

/**
 * UC-54: baja logica de un contrato.
 *
 * <p>La eliminacion se permite desde cualquier estado de contrato mientras el
 * proyecto no este FINALIZADO ni CANCELADO. El estado final es ELIMINADO y los
 * triggers registran la fecha de baja, cancelan las firmas pendientes, crean
 * el evento ELIMINADO y notifican CONTRATO_ELIMINADO a las partes.</p>
 */
@Service
public class EliminarContratoService {

    private final PerfilRepository perfilRepository;
    private final ContratoRepository contratoRepository;
    private final ProyectoSecurityService proyectoSecurityService;
    private final EntityManager entityManager;

    public EliminarContratoService(
            PerfilRepository perfilRepository,
            ContratoRepository contratoRepository,
            ProyectoSecurityService proyectoSecurityService,
            EntityManager entityManager) {
        this.perfilRepository = perfilRepository;
        this.contratoRepository = contratoRepository;
        this.proyectoSecurityService = proyectoSecurityService;
        this.entityManager = entityManager;
    }

    @Transactional
    @AuditarOperacion
    public void eliminar(
            Long idUsuario,
            Long idPerfilActivo,
            Long idProyecto,
            Long idContrato) {

        if (idPerfilActivo == null) {
            throw new PerfilActivoNoSeleccionadoException();
        }

        Perfil perfil = perfilRepository
                .findByIdPerfilAndUsuarioIdUsuario(idPerfilActivo, idUsuario)
                .orElseThrow(PerfilNoEncontradoException::new);

        if (perfil.getEstado() != EstadoPerfil.Activo) {
            throw new PerfilEnBajaException("El perfil no se encuentra activo.");
        }

        Contrato contrato = contratoRepository.findByIdForUpdate(idContrato)
                .orElseThrow(() -> new ContratoNoEncontradoException(idContrato));

        if (contrato.getProyecto() == null
                || !idProyecto.equals(contrato.getProyecto().getIdProyecto())) {
            throw new ContratoNoEncontradoException(idContrato);
        }

        proyectoSecurityService.validarDirector(idProyecto, idPerfilActivo);

        // La baja es idempotente: si ya esta eliminado no se vuelve a notificar.
        if (contrato.getEstado() == EstadoContrato.ELIMINADO) {
            return;
        }

        EstadoProyecto estadoProyecto = contrato.getProyecto().getEstado();
        if (estadoProyecto == EstadoProyecto.FINALIZADO
                || estadoProyecto == EstadoProyecto.CANCELADO) {
            throw new ContratoNoModificableException(
                    "PROYECTO_NO_APTO_PARA_ELIMINACION",
                    "No se puede eliminar un contrato de un proyecto finalizado o cancelado.");
        }

        contrato.setEstado(EstadoContrato.ELIMINADO);
        contratoRepository.saveAndFlush(contrato);

        entityManager.flush();
        entityManager.clear();
    }
}
