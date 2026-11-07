package org.mgroko.backend.contratos.mapper;

import java.time.LocalDate;
import java.util.List;

import org.mgroko.backend.contratos.dto.ContratoActivoMiembroResponse;
import org.mgroko.backend.contratos.dto.ContratoBusquedaResponse;
import org.mgroko.backend.contratos.dto.ContratoDetalleResponse;
import org.mgroko.backend.contratos.dto.ContratoGeneradoResponse;
import org.mgroko.backend.contratos.dto.EmisorContratoResponse;
import org.mgroko.backend.contratos.dto.MiembroDestinatarioResponse;
import org.mgroko.backend.contratos.dto.ParteContratoResponse;
import org.mgroko.backend.contratos.dto.ParteContratoResumenResponse;
import org.mgroko.backend.contratos.dto.ProyectoContratoResponse;
import org.mgroko.backend.contratos.dto.TipoAcuerdoContratoResponse;
import org.mgroko.backend.modelo.Contrato;
import org.mgroko.backend.modelo.FirmaContrato;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.TipoAcuerdoContrato;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.repositorio.proyeccion.ContratoActivoProyeccion;
import org.mgroko.backend.repositorio.proyeccion.ContratoResumenProyeccion;

public final class ContratoMapper {

    private ContratoMapper() {
    }

    public static TipoAcuerdoContratoResponse toTipoAcuerdoResponse(
            TipoAcuerdoContrato tipo) {
        if (tipo == null) {
            return null;
        }
        return new TipoAcuerdoContratoResponse(
                tipo.getIdTipoAcuerdo(),
                tipo.getCodigo(),
                tipo.getNombre(),
                tipo.getDescripcion());
    }

    public static ProyectoContratoResponse toProyectoResponse(
            Proyecto proyecto,
            LocalDate fechaEntrega) {
        if (proyecto == null) {
            return null;
        }
        return new ProyectoContratoResponse(
                proyecto.getIdProyecto(),
                proyecto.getNombre(),
                proyecto.getDescripcion(),
                proyecto.getEstado() != null ? proyecto.getEstado().name() : null,
                proyecto.getFechaInicio(),
                fechaEntrega);
    }

    public static EmisorContratoResponse toEmisorResponse(MiembroProyecto miembro) {
        if (miembro == null) {
            return null;
        }
        Perfil perfil = miembro.getPerfil();
        Usuario usuario = perfil != null ? perfil.getUsuario() : null;
        return new EmisorContratoResponse(
                miembro.getIdMiembro(),
                perfil != null ? perfil.getIdPerfil() : null,
                perfil != null ? perfil.getNombreArtistico() : null,
                nombreCompleto(perfil),
                usuario != null ? usuario.getDni() : null);
    }

    public static MiembroDestinatarioResponse toMiembroResponse(
            MiembroProyecto miembro,
            Long idPerfilEmisor) {
        Perfil perfil = miembro.getPerfil();
        Usuario usuario = perfil != null ? perfil.getUsuario() : null;
        return new MiembroDestinatarioResponse(
                miembro.getIdMiembro(),
                perfil != null ? perfil.getIdPerfil() : null,
                perfil != null ? perfil.getNombreArtistico() : null,
                nombreCompleto(perfil),
                usuario != null ? usuario.getDni() : null,
                perfil != null && perfil.getProfesion() != null
                        ? perfil.getProfesion().getNombre()
                        : null,
                miembro.getRolProyecto() != null
                        ? miembro.getRolProyecto().getCodigo()
                        : null,
                perfil != null && idPerfilEmisor != null
                        && idPerfilEmisor.equals(perfil.getIdPerfil()));
    }

    public static ContratoActivoMiembroResponse toContratoActivoResponse(
            ContratoActivoProyeccion fila) {
        return new ContratoActivoMiembroResponse(
                fila.getIdMiembro(),
                fila.getIdTipoAcuerdo(),
                fila.getIdContrato(),
                fila.getNombre());
    }

    public static ParteContratoResponse toParteResponse(FirmaContrato firma) {
        MiembroProyecto miembro = firma.getMiembro();
        Perfil perfil = miembro != null ? miembro.getPerfil() : null;
        return new ParteContratoResponse(
                firma.getIdMiembro(),
                perfil != null ? perfil.getIdPerfil() : null,
                nombreCompleto(perfil),
                firma.getRolParte() != null ? firma.getRolParte().name() : null,
                firma.getEstadoFirma() != null ? firma.getEstadoFirma().name() : null);
    }

    public static ContratoGeneradoResponse toContratoResponse(
            Contrato contrato,
            List<FirmaContrato> firmas) {
        return new ContratoGeneradoResponse(
                contrato.getIdContrato(),
                contrato.getProyecto() != null
                        ? contrato.getProyecto().getIdProyecto()
                        : null,
                contrato.getNombre(),
                contrato.getDescripcion(),
                contrato.getEstado() != null ? contrato.getEstado().name() : null,
                contrato.getVersionActual(),
                contrato.getFechaCreacion(),
                contrato.getFechaEmision(),
                toTipoAcuerdoResponse(contrato.getTipoAcuerdo()),
                contrato.getTexto(),
                firmas == null ? List.of() : firmas.stream()
                        .map(ContratoMapper::toParteResponse)
                        .toList());
    }

    public static ParteContratoResumenResponse toParteResumen(FirmaContrato firma) {
        MiembroProyecto miembro = firma.getMiembro();
        Perfil perfil = miembro != null ? miembro.getPerfil() : null;
        return new ParteContratoResumenResponse(
                firma.getIdMiembro(),
                perfil != null ? perfil.getIdPerfil() : null,
                nombreCompleto(perfil),
                firma.getRolParte() != null ? firma.getRolParte().name() : null,
                firma.getEstadoFirma() != null ? firma.getEstadoFirma().name() : null,
                firma.getFechaFirma());
    }

    public static EmisorContratoResponse toEmisorResponse(FirmaContrato firma) {
        return firma != null ? toEmisorResponse(firma.getMiembro()) : null;
    }

    public static ContratoBusquedaResponse toBusquedaResponse(
            ContratoResumenProyeccion resumen,
            List<FirmaContrato> firmas,
            Long idPerfilActivo,
            boolean puedeModificar) {

        List<FirmaContrato> partes = firmas == null ? List.of() : firmas;
        FirmaContrato emisor = partes.stream()
                .filter(f -> resumen.getIdMiembroEmisor() != null
                        && resumen.getIdMiembroEmisor().equals(f.getIdMiembro()))
                .findFirst()
                .orElse(null);
        FirmaContrato miFirma = miFirma(partes, idPerfilActivo);

        return new ContratoBusquedaResponse(
                resumen.getIdContrato(),
                resumen.getNombre(),
                resumen.getIdProyecto(),
                resumen.getNombreProyecto(),
                resumen.getIdTipoAcuerdo(),
                resumen.getCodigoTipoAcuerdo(),
                resumen.getNombreTipoAcuerdo(),
                resumen.getEstado() != null ? resumen.getEstado().name() : null,
                resumen.getFechaCreacion(),
                resumen.getFechaEmision(),
                resumen.getFechaModificacion(),
                resumen.getVersionActual(),
                toEmisorResponse(emisor),
                partes.stream().map(ContratoMapper::toParteResumen).toList(),
                miFirma != null && miFirma.getEstadoFirma() != null
                        ? miFirma.getEstadoFirma().name()
                        : null,
                miFirma != null ? miFirma.getFechaFirma() : null,
                puedeModificar);
    }

    public static ContratoDetalleResponse toDetalleResponse(
            Contrato contrato,
            List<FirmaContrato> firmas,
            Long idPerfilActivo,
            boolean puedeModificarse,
            String motivoNoModificable) {

        List<FirmaContrato> partes = firmas == null ? List.of() : firmas;
        FirmaContrato miFirma = miFirma(partes, idPerfilActivo);
        Proyecto proyecto = contrato.getProyecto();
        TipoAcuerdoContrato tipo = contrato.getTipoAcuerdo();

        return new ContratoDetalleResponse(
                contrato.getIdContrato(),
                proyecto != null ? proyecto.getIdProyecto() : null,
                proyecto != null ? proyecto.getNombre() : null,
                proyecto != null && proyecto.getEstado() != null
                        ? proyecto.getEstado().name()
                        : null,
                tipo != null ? tipo.getIdTipoAcuerdo() : null,
                tipo != null ? tipo.getCodigo() : null,
                tipo != null ? tipo.getNombre() : null,
                contrato.getNombre(),
                contrato.getDescripcion(),
                contrato.getEstado() != null ? contrato.getEstado().name() : null,
                contrato.getVersionActual(),
                contrato.getFechaCreacion(),
                contrato.getFechaEmision(),
                contrato.getFechaModificacion(),
                contrato.getTexto(),
                toEmisorResponse(contrato.getMiembroEmisor()),
                partes.stream().map(ContratoMapper::toParteResumen).toList(),
                miFirma != null && miFirma.getEstadoFirma() != null
                        ? miFirma.getEstadoFirma().name()
                        : null,
                miFirma != null ? miFirma.getFechaFirma() : null,
                puedeModificarse,
                motivoNoModificable);
    }

    private static FirmaContrato miFirma(
            List<FirmaContrato> firmas,
            Long idPerfilActivo) {

        if (idPerfilActivo == null) {
            return null;
        }
        return firmas.stream()
                .filter(f -> f.getMiembro() != null
                        && f.getMiembro().getPerfil() != null
                        && idPerfilActivo.equals(f.getMiembro().getPerfil().getIdPerfil()))
                .findFirst()
                .orElse(null);
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
