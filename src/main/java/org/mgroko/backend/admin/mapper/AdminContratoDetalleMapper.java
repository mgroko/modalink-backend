package org.mgroko.backend.admin.mapper;

import java.util.List;

import org.mgroko.backend.admin.dto.AdminContratoDetalleResponse;
import org.mgroko.backend.admin.dto.AdminContratoResumenResponse;
import org.mgroko.backend.modelo.Contrato;
import org.mgroko.backend.modelo.FirmaContrato;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.TipoAcuerdoContrato;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoFirmaContrato;

/**
 * Arma las vistas administrativas de contratos.
 */
public final class AdminContratoDetalleMapper {

    private AdminContratoDetalleMapper() {
    }

    public static AdminContratoResumenResponse toResumen(
            Contrato contrato,
            List<FirmaContrato> firmas) {

        Proyecto proyecto = contrato.getProyecto();
        TipoAcuerdoContrato tipo = contrato.getTipoAcuerdo();
        List<FirmaContrato> partes = firmas == null ? List.of() : firmas;

        return new AdminContratoResumenResponse(
                contrato.getIdContrato(),
                contrato.getNombre(),
                contrato.getEstado() != null ? contrato.getEstado().name() : null,
                contrato.getVersionActual(),
                proyecto != null ? proyecto.getIdProyecto() : null,
                proyecto != null ? proyecto.getNombre() : null,
                proyecto != null && proyecto.getEstado() != null
                        ? proyecto.getEstado().name()
                        : null,
                tipo != null ? tipo.getIdTipoAcuerdo() : null,
                tipo != null ? tipo.getCodigo() : null,
                tipo != null ? tipo.getNombre() : null,
                contrato.getFechaCreacion(),
                contrato.getFechaEmision(),
                contrato.getFechaModificacion(),
                contrato.getFechaBaja(),
                nombreCompleto(contrato.getMiembroEmisor() != null
                        ? contrato.getMiembroEmisor().getPerfil()
                        : null),
                contarFirmados(partes),
                contarTotalPartes(partes));
    }

    public static AdminContratoDetalleResponse toDetalle(
            Contrato contrato,
            List<FirmaContrato> firmas) {

        Proyecto proyecto = contrato.getProyecto();
        TipoAcuerdoContrato tipo = contrato.getTipoAcuerdo();
        List<FirmaContrato> partes = firmas == null ? List.of() : firmas;

        return new AdminContratoDetalleResponse(
                contrato.getIdContrato(),
                contrato.getNombre(),
                contrato.getDescripcion(),
                contrato.getEstado() != null ? contrato.getEstado().name() : null,
                contrato.getVersionActual(),
                contrato.getFechaCreacion(),
                contrato.getFechaEmision(),
                contrato.getFechaModificacion(),
                contrato.getFechaBaja(),
                contrato.getFechaCancelacion(),
                contrato.getMotivoCancelacion(),
                contrato.getTexto(),
                proyecto != null
                        ? new AdminContratoDetalleResponse.ProyectoResponse(
                                proyecto.getIdProyecto(),
                                proyecto.getNombre(),
                                proyecto.getDescripcion(),
                                proyecto.getEstado() != null
                                        ? proyecto.getEstado().name()
                                        : null)
                        : null,
                tipo != null
                        ? new AdminContratoDetalleResponse.TipoAcuerdoResponse(
                                tipo.getIdTipoAcuerdo(),
                                tipo.getCodigo(),
                                tipo.getNombre(),
                                tipo.getDescripcion())
                        : null,
                toEmisor(contrato.getMiembroEmisor()),
                partes.stream().map(AdminContratoDetalleMapper::toParte).toList(),
                contarFirmados(partes),
                contarTotalPartes(partes));
    }

    private static AdminContratoDetalleResponse.EmisorResponse toEmisor(
            MiembroProyecto miembro) {
        if (miembro == null) {
            return null;
        }
        Perfil perfil = miembro.getPerfil();
        Usuario usuario = perfil != null ? perfil.getUsuario() : null;
        return new AdminContratoDetalleResponse.EmisorResponse(
                miembro.getIdMiembro(),
                perfil != null ? perfil.getIdPerfil() : null,
                perfil != null ? perfil.getNombreArtistico() : null,
                nombreCompleto(perfil),
                usuario != null ? usuario.getDni() : null);
    }

    private static AdminContratoDetalleResponse.ParteResponse toParte(
            FirmaContrato firma) {
        MiembroProyecto miembro = firma.getMiembro();
        Perfil perfil = miembro != null ? miembro.getPerfil() : null;
        Usuario usuario = perfil != null ? perfil.getUsuario() : null;
        return new AdminContratoDetalleResponse.ParteResponse(
                firma.getIdMiembro(),
                perfil != null ? perfil.getIdPerfil() : null,
                perfil != null ? perfil.getNombreArtistico() : null,
                nombreCompleto(perfil),
                usuario != null ? usuario.getDni() : null,
                firma.getRolParte() != null ? firma.getRolParte().name() : null,
                firma.getEstadoFirma() != null ? firma.getEstadoFirma().name() : null,
                firma.getFechaFirma());
    }

    private static int contarFirmados(List<FirmaContrato> firmas) {
        return (int) firmas.stream()
                .filter(f -> f.getEstadoFirma() == EstadoFirmaContrato.FIRMADO)
                .count();
    }

    private static int contarTotalPartes(List<FirmaContrato> firmas) {
        return (int) firmas.stream()
                .filter(f -> f.getEstadoFirma() == EstadoFirmaContrato.PENDIENTE
                        || f.getEstadoFirma() == EstadoFirmaContrato.FIRMADO)
                .count();
    }

    public static String nombreCompleto(Perfil perfil) {
        if (perfil == null) {
            return null;
        }
        Usuario usuario = perfil.getUsuario();
        if (usuario != null) {
            String nombre = (usuario.getNombre() != null ? usuario.getNombre() : "")
                    + " " + (usuario.getApellido() != null ? usuario.getApellido() : "");
            String limpio = nombre.trim();
            if (!limpio.isBlank()) {
                return limpio;
            }
        }
        return perfil.getNombreArtistico();
    }
}
