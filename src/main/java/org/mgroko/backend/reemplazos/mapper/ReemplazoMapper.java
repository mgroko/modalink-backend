package org.mgroko.backend.reemplazos.mapper;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.mgroko.backend.modelo.CaracteristicaPerfil;
import org.mgroko.backend.modelo.Habilidad;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.ValorCaracteristica;
import org.mgroko.backend.perfiles.dto.CaracteristicaResponse;
import org.mgroko.backend.reemplazos.dto.CandidatoReemplazoResponse;
import org.mgroko.backend.reemplazos.dto.InvitacionReemplazoResponse;
import org.mgroko.backend.reemplazos.dto.ReemplazoResumenResponse;
import org.mgroko.backend.reemplazos.modelo.Reemplazo;
import org.mgroko.backend.reemplazos.modelo.ReemplazoCandidato;
import org.mgroko.backend.ubicacion.mapper.UbicacionMapper;

public final class ReemplazoMapper {

    private ReemplazoMapper() {
    }

    public static ReemplazoResumenResponse toResumen(
            Reemplazo reemplazo,
            int totalCandidatos,
            int totalInvitados) {

        var requerimientoActividad = reemplazo.getRequerimientoActividad();
        var requerimientoGeneral = reemplazo.getRequerimientoGral();

        return new ReemplazoResumenResponse(
                reemplazo.getIdReemplazo(),
                reemplazo.getTipo(),
                reemplazo.getProyecto().getIdProyecto(),
                reemplazo.getProyecto().getNombre(),
                reemplazo.getActividad() != null
                        ? reemplazo.getActividad().getIdActividad()
                        : null,
                reemplazo.getActividad() != null
                        ? reemplazo.getActividad().getNombre()
                        : null,
                requerimientoActividad != null
                        ? requerimientoActividad.getIdRequerimiento()
                        : null,
                requerimientoGeneral != null
                        ? requerimientoGeneral.getIdRequerimientoGral()
                        : null,
                requerimientoActividad != null
                        ? requerimientoActividad.getProfesion().getNombre()
                        : requerimientoGeneral != null
                                ? requerimientoGeneral.getProfesion().getNombre()
                                : null,
                reemplazo.getPerfilBaja() != null
                        ? reemplazo.getPerfilBaja().getNombreArtistico()
                        : null,
                reemplazo.getOrigen(),
                reemplazo.getEstado(),
                reemplazo.getFechaCreacion(),
                reemplazo.getFechaCierre(),
                reemplazo.getMotivoCierre(),
                totalCandidatos,
                totalInvitados);
    }

    public static CandidatoReemplazoResponse toCandidato(ReemplazoCandidato candidato) {
        Perfil perfil = candidato.getPerfil();
        var usuario = perfil != null ? perfil.getUsuario() : null;
        var ubicacion = usuario != null ? usuario.getUbicacion() : null;
        var ciudad = ubicacion != null ? ubicacion.getCiudad() : null;

        List<String> habilidades = perfil != null && perfil.getHabilidades() != null
                ? perfil.getHabilidades().stream()
                        .filter(java.util.Objects::nonNull)
                        .sorted(Comparator.comparing(Habilidad::getNombre))
                        .map(Habilidad::getNombre)
                        .toList()
                : List.of();

        List<CaracteristicaResponse> caracteristicas = perfil != null
                && perfil.getCaracteristicas() != null
                        ? perfil.getCaracteristicas().stream()
                                .sorted(Comparator.comparing(
                                        c -> c.getCaracteristicaTecnica().getCodigo()))
                                .map(ReemplazoMapper::toCaracteristica)
                                .toList()
                        : List.of();

        return new CandidatoReemplazoResponse(
                candidato.getIdCandidato(),
                perfil != null ? perfil.getIdPerfil() : null,
                usuario != null ? usuario.getIdUsuario() : null,
                perfil != null ? perfil.getNombreArtistico() : null,
                usuario != null ? usuario.getNombre() : null,
                usuario != null ? usuario.getApellido() : null,
                perfil != null && perfil.getProfesion() != null
                        ? perfil.getProfesion().getNombre()
                        : null,
                perfil != null && perfil.getImagen() != null
                        ? perfil.getImagen().getUrl()
                        : null,
                UbicacionMapper.toCiudadResponse(ciudad),
                habilidades,
                caracteristicas,
                candidato.getPuntaje(),
                candidato.getNivelUbicacion(),
                Boolean.TRUE.equals(candidato.getEsAlternativo()),
                separarFaltantes(candidato.getDetalleFaltantes()),
                candidato.getTipoDisponibilidad(),
                candidato.getInicioPropuesto(),
                candidato.getFinPropuesto(),
                candidato.getEstado(),
                candidato.getMensaje(),
                candidato.getFechaInvitacion(),
                candidato.getFechaRespuesta(),
                candidato.getFechaDecision(),
                candidato.getMotivoDecision());
    }

    public static InvitacionReemplazoResponse toInvitacion(ReemplazoCandidato candidato) {
        Reemplazo reemplazo = candidato.getReemplazo();
        var actividad = reemplazo.getActividad();
        var requerimientoActividad = reemplazo.getRequerimientoActividad();
        var requerimientoGeneral = reemplazo.getRequerimientoGral();

        return new InvitacionReemplazoResponse(
                candidato.getIdCandidato(),
                reemplazo.getIdReemplazo(),
                reemplazo.getTipo(),
                candidato.getEstado(),
                reemplazo.getProyecto().getIdProyecto(),
                reemplazo.getProyecto().getNombre(),
                actividad != null ? actividad.getIdActividad() : null,
                actividad != null ? actividad.getNombre() : null,
                requerimientoActividad != null
                        ? requerimientoActividad.getProfesion().getNombre()
                        : requerimientoGeneral != null
                                ? requerimientoGeneral.getProfesion().getNombre()
                                : null,
                candidato.getFechaInvitacion(),
                actividad != null ? actividad.getFechaHoraInicio() : null,
                actividad != null ? actividad.getDuracionMinutos() : null,
                candidato.getMensaje());
    }

    private static CaracteristicaResponse toCaracteristica(CaracteristicaPerfil caracteristica) {
        ValorCaracteristica valor = caracteristica.getValorCaracteristica();
        return new CaracteristicaResponse(
                caracteristica.getCaracteristicaTecnica().getIdCaracteristica(),
                caracteristica.getCaracteristicaTecnica().getCodigo(),
                caracteristica.getValor(),
                valor != null ? valor.getIdValor() : null,
                valor != null ? valor.getEtiqueta() : null,
                valor != null ? valor.getColorHex() : null);
    }

    private static List<String> separarFaltantes(String detalle) {
        if (detalle == null || detalle.isBlank()) {
            return List.of();
        }
        return Arrays.stream(detalle.split("\\R"))
                .map(String::trim)
                .filter(texto -> !texto.isBlank())
                .toList();
    }
}
