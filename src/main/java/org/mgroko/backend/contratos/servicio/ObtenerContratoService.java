package org.mgroko.backend.contratos.servicio;

import java.util.List;
import java.util.Optional;

import org.mgroko.backend.contratos.dto.ContratoDetalleResponse;
import org.mgroko.backend.contratos.exception.ContratoNoEncontradoException;
import org.mgroko.backend.contratos.mapper.ContratoMapper;
import org.mgroko.backend.modelo.Contrato;
import org.mgroko.backend.modelo.FirmaContrato;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.enums.EstadoContrato;
import org.mgroko.backend.modelo.enums.EstadoFirmaContrato;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.RolParteContrato;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.perfiles.exception.PerfilNoEncontradoException;
import org.mgroko.backend.proyectos.exception.AccesoDenegadoProyectoException;
import org.mgroko.backend.proyectos.servicio.ProyectoSecurityService;
import org.mgroko.backend.repositorio.ContratoRepository;
import org.mgroko.backend.repositorio.FirmaContratoRepository;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Detalle de contrato para UC-68 y UC-53.
 *
 * <p>Visibilidad: director activo del proyecto o parte firmante. El detalle
 * incluye el texto legal completo y los flags de accion para el actor.</p>
 */
@Service
public class ObtenerContratoService {

    private final PerfilRepository perfilRepository;
    private final ContratoRepository contratoRepository;
    private final FirmaContratoRepository firmaContratoRepository;
    private final MiembroProyectoRepository miembroProyectoRepository;
    private final ProyectoSecurityService proyectoSecurityService;

    public ObtenerContratoService(
            PerfilRepository perfilRepository,
            ContratoRepository contratoRepository,
            FirmaContratoRepository firmaContratoRepository,
            MiembroProyectoRepository miembroProyectoRepository,
            ProyectoSecurityService proyectoSecurityService) {
        this.perfilRepository = perfilRepository;
        this.contratoRepository = contratoRepository;
        this.firmaContratoRepository = firmaContratoRepository;
        this.miembroProyectoRepository = miembroProyectoRepository;
        this.proyectoSecurityService = proyectoSecurityService;
    }

    @Transactional(readOnly = true)
    public ContratoDetalleResponse obtener(
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

        Contrato contrato = contratoRepository.findByIdConPartes(idContrato)
                .orElseThrow(() -> new ContratoNoEncontradoException(idContrato));

        if (contrato.getProyecto() == null
                || !idProyecto.equals(contrato.getProyecto().getIdProyecto())) {
            throw new ContratoNoEncontradoException(idContrato);
        }

        List<FirmaContrato> firmas = firmaContratoRepository.findConPartes(idContrato);

        Optional<MiembroProyecto> miembroActivo = miembroProyectoRepository
                .findMiembroActivoConRol(idProyecto, idPerfilActivo);
        Long idMiembroActivo = miembroActivo
                .map(MiembroProyecto::getIdMiembro)
                .orElse(null);

        FirmaContrato miFirmaActiva = idMiembroActivo == null
                ? null
                : firmas.stream()
                        .filter(f -> idMiembroActivo.equals(f.getIdMiembro()))
                        .findFirst()
                        .orElse(null);

        boolean firmaPendiente = miFirmaActiva != null
                && miFirmaActiva.getEstadoFirma() == EstadoFirmaContrato.PENDIENTE;

        boolean esDirector = proyectoSecurityService.esDirector(idProyecto, idPerfilActivo);
        boolean esBorrador = contrato.getEstado() == EstadoContrato.BORRADOR;

        // Un borrador solo lo ve el director (y el administrador por su area).
        // Aunque tenga filas de firma para reconstruir las partes, no se expone
        // a los destinatarios hasta que se emite.
        boolean esParte = !esBorrador
                && firmas.stream()
                        .anyMatch(f -> f.getMiembro() != null
                                && f.getMiembro().getPerfil() != null
                                && idPerfilActivo.equals(
                                        f.getMiembro().getPerfil().getIdPerfil()));

        if (!esDirector && !esParte) {
            throw new AccesoDenegadoProyectoException(
                    "No tienes acceso a este contrato.");
        }

        boolean firmasRegistradas = firmas.stream()
                .anyMatch(f -> f.getEstadoFirma() == EstadoFirmaContrato.FIRMADO
                        || f.getEstadoFirma() == EstadoFirmaContrato.RECHAZADO
                        || f.getEstadoFirma() == EstadoFirmaContrato.INVALIDADA);

        boolean proyectoApto = contrato.getProyecto().getEstado() == EstadoProyecto.PUBLICADO
                || contrato.getProyecto().getEstado() == EstadoProyecto.CONFIRMADO;

        boolean estadoEditable = esBorrador
                || contrato.getEstado() == EstadoContrato.PENDIENTE_FIRMA;
        boolean estadoPendienteFirma =
                contrato.getEstado() == EstadoContrato.PENDIENTE_FIRMA;

        boolean puedeModificarse = esDirector
                && estadoEditable
                && proyectoApto
                && !firmasRegistradas;

        boolean puedeFirmar = miembroActivo.isPresent()
                && estadoPendienteFirma
                && proyectoApto
                && firmaPendiente;

        boolean puedeRechazar = puedeFirmar;

        boolean tieneDestinatarios = firmas.stream()
                .anyMatch(f -> f.getRolParte() == RolParteContrato.DESTINATARIO
                        || f.getRolParte() == RolParteContrato.DIRECTOR_DESTINATARIO);

        boolean textoValido = contrato.getTexto() != null
                && !contrato.getTexto().isBlank();

        boolean tipoActivo = contrato.getTipoAcuerdo() != null
                && Boolean.TRUE.equals(contrato.getTipoAcuerdo().getActivo());

        boolean duplicadoActivo = false;
        if (esBorrador && tieneDestinatarios && contrato.getTipoAcuerdo() != null) {
            List<Long> idsDestinatarios = firmas.stream()
                    .filter(f -> f.getRolParte() == RolParteContrato.DESTINATARIO
                            || f.getRolParte() == RolParteContrato.DIRECTOR_DESTINATARIO)
                    .map(FirmaContrato::getIdMiembro)
                    .distinct()
                    .toList();
            duplicadoActivo = !contratoRepository
                    .findMiembrosConContratoActivo(
                            idProyecto,
                            contrato.getTipoAcuerdo().getIdTipoAcuerdo(),
                            idsDestinatarios)
                    .isEmpty();
        }

        String motivoNoEmitible = null;
        if (!esDirector) {
            motivoNoEmitible = "SIN_PERMISO";
        } else if (!esBorrador) {
            motivoNoEmitible = "CONTRATO_NO_BORRADOR";
        } else if (!proyectoApto) {
            motivoNoEmitible = "PROYECTO_NO_APTO_PARA_CONTRATO";
        } else if (!tieneDestinatarios) {
            motivoNoEmitible = "CONTRATO_SIN_DESTINATARIOS";
        } else if (!textoValido) {
            motivoNoEmitible = "CONTRATO_TEXTO_INVALIDO";
        } else if (!tipoActivo) {
            motivoNoEmitible = "TIPO_ACUERDO_INVALIDO";
        } else if (duplicadoActivo) {
            motivoNoEmitible = "CONTRATO_DUPLICADO_ACTIVO";
        }

        boolean puedeEmitir = esDirector && motivoNoEmitible == null;

        boolean proyectoEliminable =
                contrato.getProyecto().getEstado() != EstadoProyecto.FINALIZADO
                        && contrato.getProyecto().getEstado() != EstadoProyecto.CANCELADO;
        boolean contratoEliminado =
                contrato.getEstado() == EstadoContrato.ELIMINADO;

        boolean puedeEliminar = esDirector
                && proyectoEliminable
                && !contratoEliminado;

        String motivoNoEliminable = null;
        if (!esDirector) {
            motivoNoEliminable = "SIN_PERMISO";
        } else if (contratoEliminado) {
            motivoNoEliminable = "CONTRATO_YA_ELIMINADO";
        } else if (!proyectoEliminable) {
            motivoNoEliminable = "PROYECTO_NO_APTO_PARA_ELIMINACION";
        }

        String motivoNoModificable = null;
        if (!esDirector) {
            motivoNoModificable = "SIN_PERMISO";
        } else if (!estadoEditable) {
            motivoNoModificable = "CONTRATO_NO_EDITABLE";
        } else if (!proyectoApto) {
            motivoNoModificable = "PROYECTO_NO_APTO_PARA_CONTRATO";
        } else if (firmasRegistradas) {
            motivoNoModificable = "CONTRATO_CON_FIRMAS_REGISTRADAS";
        }

        return ContratoMapper.toDetalleResponse(
                contrato,
                firmas,
                idPerfilActivo,
                idMiembroActivo,
                puedeModificarse,
                motivoNoModificable,
                puedeEmitir,
                motivoNoEmitible,
                puedeFirmar,
                puedeRechazar,
                puedeEliminar,
                motivoNoEliminable);
    }
}
