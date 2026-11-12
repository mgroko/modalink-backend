package org.mgroko.backend.reemplazos.mapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.mgroko.backend.modelo.CaracteristicaPerfil;
import org.mgroko.backend.modelo.Habilidad;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.RequerimientoActividad;
import org.mgroko.backend.modelo.RequerimientoGralProyecto;
import org.mgroko.backend.modelo.ValorCaracteristica;
import org.mgroko.backend.reemplazos.dto.CandidatoReemplazoResponse;
import org.mgroko.backend.reemplazos.dto.CaracteristicaEvaluadaResponse;
import org.mgroko.backend.reemplazos.dto.HabilidadEvaluadaResponse;
import org.mgroko.backend.reemplazos.dto.InvitacionReemplazoResponse;
import org.mgroko.backend.reemplazos.dto.ReemplazoResumenResponse;
import org.mgroko.backend.reemplazos.modelo.Reemplazo;
import org.mgroko.backend.reemplazos.modelo.ReemplazoCandidato;
import org.mgroko.backend.reemplazos.servicio.modelo.CaracteristicaEvaluada;
import org.mgroko.backend.reemplazos.servicio.modelo.HabilidadEvaluada;
import org.mgroko.backend.reemplazos.servicio.modelo.ResultadoConcordancia;
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
                        ? idRequerimientoActividad(requerimientoActividad)
                        : null,
                requerimientoGeneral != null
                        ? idRequerimientoGral(requerimientoGeneral)
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
        return toCandidato(candidato, null);
    }

    public static CandidatoReemplazoResponse toCandidato(
            ReemplazoCandidato candidato,
            ResultadoConcordancia evaluacion) {

        Perfil perfil = candidato.getPerfil();
        var usuario = perfil != null ? perfil.getUsuario() : null;
        var ubicacion = usuario != null ? usuario.getUbicacion() : null;
        var ciudad = ubicacion != null ? ubicacion.getCiudad() : null;

        List<String> habilidades = perfil != null && perfil.getHabilidades() != null
                ? perfil.getHabilidades().stream()
                        .filter(Objects::nonNull)
                        .sorted(Comparator.comparing(
                                Habilidad::getNombre,
                                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                        .map(Habilidad::getNombre)
                        .toList()
                : List.of();

        List<HabilidadEvaluadaResponse> habilidadesEvaluadas = evaluacion != null
                ? toHabilidadesEvaluadas(
                        perfil,
                        evaluacion.habilidadesEvaluadas())
                : null;

        List<CaracteristicaEvaluadaResponse> caracteristicas = evaluacion != null
                ? toCaracteristicasEvaluadas(
                        perfil,
                        evaluacion.caracteristicasEvaluadas())
                : toCaracteristicasSinEvaluar(perfil);

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
                candidato.getMotivoDecision(),
                candidato.getIdTycAceptado(),
                habilidadesEvaluadas,
                evaluacion != null ? evaluacion.requisitosCumplidos() : null,
                evaluacion != null ? evaluacion.requisitosTotales() : null,
                evaluacion != null ? evaluacion.cumpleTodo() : null);
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
                candidato.getMensaje(),
                candidato.getFechaRespuesta(),
                candidato.getMotivoRespuesta());
    }

    /**
     * Combina las habilidades del perfil con las que pidió la vacante.
     *
     * <p>Las requeridas quedan primero (cumplidas o faltantes) y el resto se
     * muestra como información adicional sin estado de requisito.</p>
     */
    private static List<HabilidadEvaluadaResponse> toHabilidadesEvaluadas(
            Perfil perfil,
            List<HabilidadEvaluada> requeridas) {

        Map<Long, HabilidadEvaluada> porId = new HashMap<>();
        for (HabilidadEvaluada requerida : requeridas) {
            if (requerida.idHabilidad() != null) {
                porId.put(requerida.idHabilidad(), requerida);
            }
        }

        List<HabilidadEvaluadaResponse> resultado = new ArrayList<>();
        Set<Long> agregadas = new HashSet<>();

        if (perfil != null && perfil.getHabilidades() != null) {
            perfil.getHabilidades().stream()
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparing(
                            Habilidad::getNombre,
                            Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                    .forEach(habilidad -> {
                        Long id = habilidad.getIdHabilidad();
                        HabilidadEvaluada requerida = porId.get(id);
                        resultado.add(new HabilidadEvaluadaResponse(
                                id,
                                habilidad.getNombre(),
                                requerida != null,
                                requerida != null ? requerida.cumple() : null));
                        if (id != null) {
                            agregadas.add(id);
                        }
                    });
        }

        for (HabilidadEvaluada requerida : requeridas) {
            if (requerida.idHabilidad() != null
                    && agregadas.contains(requerida.idHabilidad())) {
                continue;
            }
            resultado.add(new HabilidadEvaluadaResponse(
                    requerida.idHabilidad(),
                    requerida.nombre(),
                    Boolean.TRUE,
                    Boolean.FALSE));
        }

        resultado.sort(Comparator
                .comparing((HabilidadEvaluadaResponse habilidad) ->
                        !Boolean.TRUE.equals(habilidad.requerida()))
                .thenComparing(
                        habilidad -> habilidad.nombre(),
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
        return resultado;
    }

    /**
     * Combina las características del perfil con las que pidió la vacante.
     * Las requeridas que el perfil no declaró se agregan como faltantes para
     * que la UI pueda mostrarlas en naranja con el valor pedido.
     */
    private static List<CaracteristicaEvaluadaResponse> toCaracteristicasEvaluadas(
            Perfil perfil,
            List<CaracteristicaEvaluada> requeridas) {

        Map<Long, CaracteristicaEvaluada> porId = new HashMap<>();
        for (CaracteristicaEvaluada requerida : requeridas) {
            if (requerida.idCaracteristica() != null) {
                porId.put(requerida.idCaracteristica(), requerida);
            }
        }

        List<CaracteristicaEvaluadaResponse> resultado = new ArrayList<>();
        Set<Long> agregadas = new HashSet<>();

        if (perfil != null && perfil.getCaracteristicas() != null) {
            perfil.getCaracteristicas().stream()
                    .filter(Objects::nonNull)
                    .filter(caracteristica ->
                            caracteristica.getCaracteristicaTecnica() != null)
                    .sorted(Comparator.comparing(
                            caracteristica -> caracteristica
                                    .getCaracteristicaTecnica().getCodigo(),
                            Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                    .forEach(caracteristica -> {
                        Long id = caracteristica.getCaracteristicaTecnica()
                                .getIdCaracteristica();
                        resultado.add(toCaracteristicaEvaluada(
                                caracteristica,
                                porId.get(id)));
                        if (id != null) {
                            agregadas.add(id);
                        }
                    });
        }

        for (CaracteristicaEvaluada requerida : requeridas) {
            if (requerida.idCaracteristica() != null
                    && agregadas.contains(requerida.idCaracteristica())) {
                continue;
            }
            resultado.add(new CaracteristicaEvaluadaResponse(
                    requerida.idCaracteristica(),
                    requerida.codigo(),
                    requerida.nombre(),
                    null,
                    null,
                    null,
                    null,
                    requerida.valorPedido(),
                    Boolean.TRUE,
                    Boolean.FALSE));
        }

        resultado.sort(Comparator
                .comparing((CaracteristicaEvaluadaResponse caracteristica) ->
                        !Boolean.TRUE.equals(caracteristica.requerida()))
                .thenComparing(
                        caracteristica -> caracteristica.codigo(),
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
        return resultado;
    }

    private static List<CaracteristicaEvaluadaResponse> toCaracteristicasSinEvaluar(
            Perfil perfil) {

        if (perfil == null || perfil.getCaracteristicas() == null) {
            return List.of();
        }

        return perfil.getCaracteristicas().stream()
                .filter(Objects::nonNull)
                .filter(caracteristica ->
                        caracteristica.getCaracteristicaTecnica() != null)
                .sorted(Comparator.comparing(
                        caracteristica -> caracteristica
                                .getCaracteristicaTecnica().getCodigo(),
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .map(ReemplazoMapper::toCaracteristicaSinEvaluar)
                .toList();
    }

    private static CaracteristicaEvaluadaResponse toCaracteristicaEvaluada(
            CaracteristicaPerfil caracteristica,
            CaracteristicaEvaluada requerida) {

        ValorCaracteristica valor = caracteristica.getValorCaracteristica();
        return new CaracteristicaEvaluadaResponse(
                caracteristica.getCaracteristicaTecnica().getIdCaracteristica(),
                caracteristica.getCaracteristicaTecnica().getCodigo(),
                caracteristica.getCaracteristicaTecnica().getNombre(),
                caracteristica.getValor(),
                valor != null ? valor.getIdValor() : null,
                valor != null ? valor.getEtiqueta() : null,
                valor != null ? valor.getColorHex() : null,
                requerida != null ? requerida.valorPedido() : null,
                requerida != null,
                requerida != null ? requerida.cumple() : null);
    }

    private static CaracteristicaEvaluadaResponse toCaracteristicaSinEvaluar(
            CaracteristicaPerfil caracteristica) {

        ValorCaracteristica valor = caracteristica.getValorCaracteristica();
        return new CaracteristicaEvaluadaResponse(
                caracteristica.getCaracteristicaTecnica().getIdCaracteristica(),
                caracteristica.getCaracteristicaTecnica().getCodigo(),
                caracteristica.getCaracteristicaTecnica().getNombre(),
                caracteristica.getValor(),
                valor != null ? valor.getIdValor() : null,
                valor != null ? valor.getEtiqueta() : null,
                valor != null ? valor.getColorHex() : null,
                null,
                null,
                null);
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

    /**
     * Los reemplazos cerrados apuntan a un snapshot para liberar el
     * requerimiento activo. La API sigue exponiendo el identificador del
     * requerimiento de origen para no romper filtros ni historiales de UI.
     */
    private static Long idRequerimientoActividad(RequerimientoActividad requerimiento) {
        if (requerimiento.getFechaArchivado() != null
                && requerimiento.getIdRequerimientoOrigen() != null) {
            return requerimiento.getIdRequerimientoOrigen();
        }
        return requerimiento.getIdRequerimiento();
    }

    private static Long idRequerimientoGral(RequerimientoGralProyecto requerimiento) {
        if (requerimiento.getFechaArchivado() != null
                && requerimiento.getIdRequerimientoOrigen() != null) {
            return requerimiento.getIdRequerimientoOrigen();
        }
        return requerimiento.getIdRequerimientoGral();
    }
}
