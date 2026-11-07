package org.mgroko.backend.contratos.servicio;

import org.mgroko.backend.contratos.exception.ContratoNoEncontradoException;
import org.mgroko.backend.contratos.exception.ContratoNoModificableException;
import org.mgroko.backend.modelo.Contrato;
import org.mgroko.backend.modelo.FirmaContrato;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.enums.EstadoContrato;
import org.mgroko.backend.modelo.enums.EstadoFirmaContrato;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.perfiles.exception.PerfilNoEncontradoException;
import org.mgroko.backend.proyectos.exception.AccesoDenegadoProyectoException;
import org.mgroko.backend.repositorio.ContratoRepository;
import org.mgroko.backend.repositorio.FirmaContratoRepository;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Contexto comun de UC-55: valida acceso, estado, membresia activa y firma
 * pendiente. Bloquea el contrato para serializar firmas concurrentes.
 */
@Service
public class ContratoFirmaContextoService {

    private final PerfilRepository perfilRepository;
    private final ContratoRepository contratoRepository;
    private final MiembroProyectoRepository miembroRepository;
    private final FirmaContratoRepository firmaContratoRepository;

    public ContratoFirmaContextoService(
            PerfilRepository perfilRepository,
            ContratoRepository contratoRepository,
            MiembroProyectoRepository miembroRepository,
            FirmaContratoRepository firmaContratoRepository) {
        this.perfilRepository = perfilRepository;
        this.contratoRepository = contratoRepository;
        this.miembroRepository = miembroRepository;
        this.firmaContratoRepository = firmaContratoRepository;
    }

    @Transactional
    public ContextoFirma cargar(
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

        if (contrato.getProyecto().getEstado() != EstadoProyecto.PUBLICADO
                && contrato.getProyecto().getEstado() != EstadoProyecto.CONFIRMADO) {
            throw new ContratoNoModificableException(
                    "PROYECTO_NO_APTO_PARA_CONTRATO",
                    "Solo se puede firmar un contrato de un proyecto PUBLICADO o CONFIRMADO.");
        }

        if (contrato.getEstado() != EstadoContrato.PENDIENTE_FIRMA) {
            throw new ContratoNoModificableException(
                    "CONTRATO_NO_PENDIENTE_FIRMA",
                    "Solo se puede firmar o rechazar un contrato en estado PENDIENTE_FIRMA.");
        }

        MiembroProyecto miembroActivo = miembroRepository
                .findMiembroActivoConRol(idProyecto, idPerfilActivo)
                .orElseThrow(() -> new AccesoDenegadoProyectoException(
                        "No eres un miembro activo del proyecto."));

        FirmaContrato firma = firmaContratoRepository
                .findByIdContratoAndIdMiembro(idContrato, miembroActivo.getIdMiembro())
                .orElseThrow(() -> new AccesoDenegadoProyectoException(
                        "No eres parte de este contrato."));

        if (firma.getEstadoFirma() == EstadoFirmaContrato.FIRMADO) {
            throw new ContratoNoModificableException(
                    "CONTRATO_YA_FIRMADO",
                    "El contrato ya fue firmado por tu perfil.");
        }

        if (firma.getEstadoFirma() == EstadoFirmaContrato.RECHAZADO) {
            throw new ContratoNoModificableException(
                    "CONTRATO_YA_RECHAZADO",
                    "El contrato ya fue rechazado por tu perfil.");
        }

        if (firma.getEstadoFirma() != EstadoFirmaContrato.PENDIENTE) {
            throw new ContratoNoModificableException(
                    "FIRMA_NO_PENDIENTE",
                    "Tu firma no está pendiente para este contrato.");
        }

        return new ContextoFirma(contrato, firma, miembroActivo);
    }

    public record ContextoFirma(
            Contrato contrato,
            FirmaContrato firma,
            MiembroProyecto miembroActivo) {
    }
}
