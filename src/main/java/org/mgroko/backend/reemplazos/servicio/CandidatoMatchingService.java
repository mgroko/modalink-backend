package org.mgroko.backend.reemplazos.servicio;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mgroko.backend.modelo.CaracteristicaPerfil;
import org.mgroko.backend.modelo.Habilidad;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.reemplazos.enums.TipoReemplazo;
import org.mgroko.backend.reemplazos.modelo.Reemplazo;
import org.mgroko.backend.reemplazos.servicio.modelo.CandidatoEvaluado;
import org.mgroko.backend.reemplazos.servicio.modelo.CandidatoPerfil;
import org.mgroko.backend.reemplazos.servicio.modelo.DisponibilidadCandidato;
import org.mgroko.backend.reemplazos.servicio.modelo.RequisitoReemplazo;
import org.mgroko.backend.reemplazos.servicio.modelo.ValorPerfil;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.springframework.stereotype.Service;

/**
 * Busca, evalúa y ordena candidatos para una vacante.
 */
@Service
public class CandidatoMatchingService {

    private final PerfilRepository perfilRepository;
    private final CandidatoDisponibilidadService disponibilidadService;
    private final UbicacionResolver ubicacionResolver;

    public CandidatoMatchingService(
            PerfilRepository perfilRepository,
            CandidatoDisponibilidadService disponibilidadService,
            UbicacionResolver ubicacionResolver) {
        this.perfilRepository = perfilRepository;
        this.disponibilidadService = disponibilidadService;
        this.ubicacionResolver = ubicacionResolver;
    }

    public List<CandidatoEvaluado> buscar(
            Reemplazo reemplazo,
            RequisitoReemplazo requisito,
            int limite) {

        List<Perfil> perfiles = reemplazo.getTipo() == TipoReemplazo.ACTIVIDAD
                ? perfilRepository.buscarCandidatosReemplazoActividad(
                        requisito.idProfesion(),
                        reemplazo.getProyecto().getIdProyecto(),
                        reemplazo.getActividad().getIdActividad())
                : perfilRepository.buscarCandidatosReemplazoGeneral(
                        requisito.idProfesion(),
                        reemplazo.getProyecto().getIdProyecto());

        var ubicacionEfectiva = ubicacionResolver.resolver(reemplazo);
        Long idUsuarioBaja = reemplazo.getPerfilBaja() != null
                && reemplazo.getPerfilBaja().getUsuario() != null
                        ? reemplazo.getPerfilBaja().getUsuario().getIdUsuario()
                        : null;

        List<CandidatoEvaluado> evaluados = new ArrayList<>();
        for (Perfil perfil : perfiles) {
            if (perfil.getUsuario() == null || perfil.getUsuario().getIdUsuario() == null) {
                continue;
            }
            if (idUsuarioBaja != null
                    && idUsuarioBaja.equals(perfil.getUsuario().getIdUsuario())) {
                continue;
            }

            CandidatoPerfil candidato = toCandidatoPerfil(perfil);
            var concordancia = EvaluadorConcordancia.evaluar(requisito, candidato);
            var nivelUbicacion = ubicacionResolver.nivel(ubicacionEfectiva, candidato);

            DisponibilidadCandidato disponibilidad;
            if (reemplazo.getTipo() == TipoReemplazo.ACTIVIDAD) {
                disponibilidad = disponibilidadService.evaluar(
                        reemplazo.getProyecto(),
                        reemplazo.getActividad(),
                        candidato.idUsuario());
                if (!disponibilidad.disponible()) {
                    continue;
                }
            } else {
                disponibilidad = DisponibilidadCandidato.noAplica();
            }

            evaluados.add(new CandidatoEvaluado(
                    candidato,
                    concordancia,
                    nivelUbicacion,
                    disponibilidad));
        }

        evaluados.sort(comparador());
        if (limite > 0 && evaluados.size() > limite) {
            return List.copyOf(evaluados.subList(0, limite));
        }
        return List.copyOf(evaluados);
    }

    private Comparator<CandidatoEvaluado> comparador() {
        return Comparator
                .comparingInt((CandidatoEvaluado c) ->
                        c.disponibilidad() != null && c.disponibilidad().tipo() != null
                                ? c.disponibilidad().tipo().getOrden()
                                : 99)
                .thenComparingInt(c -> c.nivelUbicacion() != null
                        ? c.nivelUbicacion().getOrden()
                        : 99)
                .thenComparing(
                        c -> c.concordancia().puntaje(),
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(
                        c -> c.perfil().nombreArtistico(),
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                .thenComparing(
                        c -> c.perfil().idPerfil(),
                        Comparator.nullsLast(Long::compareTo));
    }

    public CandidatoPerfil toCandidatoPerfil(Perfil perfil) {
        var usuario = perfil.getUsuario();
        var ubicacion = usuario.getUbicacion();
        var ciudad = ubicacion != null ? ubicacion.getCiudad() : null;
        var provincia = ciudad != null ? ciudad.getProvincia() : null;

        Set<Long> idsHabilidades = new HashSet<>();
        if (perfil.getHabilidades() != null) {
            perfil.getHabilidades().stream()
                    .filter(java.util.Objects::nonNull)
                    .map(Habilidad::getIdHabilidad)
                    .forEach(idsHabilidades::add);
        }

        Map<Long, ValorPerfil> caracteristicas = new HashMap<>();
        if (perfil.getCaracteristicas() != null) {
            for (CaracteristicaPerfil caracteristica : perfil.getCaracteristicas()) {
                if (caracteristica.getCaracteristicaTecnica() == null) {
                    continue;
                }
                caracteristicas.put(
                        caracteristica.getCaracteristicaTecnica().getIdCaracteristica(),
                        new ValorPerfil(
                                caracteristica.getValor(),
                                caracteristica.getIdValor()));
            }
        }

        return new CandidatoPerfil(
                perfil.getIdPerfil(),
                usuario.getIdUsuario(),
                perfil.getNombreArtistico(),
                usuario.getNombre(),
                usuario.getApellido(),
                perfil.getProfesion() != null
                        ? perfil.getProfesion().getIdProfesion()
                        : null,
                perfil.getProfesion() != null
                        ? perfil.getProfesion().getNombre()
                        : null,
                perfil.getImagen() != null ? perfil.getImagen().getUrl() : null,
                ciudad != null ? ciudad.getIdCiudad() : null,
                provincia != null ? provincia.getIdProvincia() : null,
                ciudad != null ? ciudad.getNombre() : null,
                provincia != null ? provincia.getNombre() : null,
                idsHabilidades,
                caracteristicas);
    }
}
