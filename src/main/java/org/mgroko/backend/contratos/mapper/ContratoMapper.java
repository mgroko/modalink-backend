package org.mgroko.backend.contratos.mapper;

import java.time.LocalDate;
import java.util.List;

import org.mgroko.backend.contratos.dto.ContratoActivoMiembroResponse;
import org.mgroko.backend.contratos.dto.ContratoGeneradoResponse;
import org.mgroko.backend.contratos.dto.EmisorContratoResponse;
import org.mgroko.backend.contratos.dto.MiembroDestinatarioResponse;
import org.mgroko.backend.contratos.dto.ParteContratoResponse;
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
