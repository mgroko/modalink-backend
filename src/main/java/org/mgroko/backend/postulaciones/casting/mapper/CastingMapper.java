package org.mgroko.backend.postulaciones.casting.mapper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.mgroko.backend.modelo.CaracteristicaPerfil;
import org.mgroko.backend.modelo.PolaroidPerfil;
import org.mgroko.backend.modelo.TipoPolaroid;
import org.mgroko.backend.reemplazos.servicio.modelo.CaracteristicaEvaluada;
import org.mgroko.backend.reemplazos.servicio.modelo.HabilidadEvaluada;
import org.mgroko.backend.postulaciones.casting.dto.CaracteristicaEvaluadaCastingResponse;
import org.mgroko.backend.postulaciones.casting.dto.CaracteristicaPerfilCastingResponse;
import org.mgroko.backend.postulaciones.casting.dto.CaracteristicaRequeridaCastingResponse;
import org.mgroko.backend.postulaciones.casting.dto.HabilidadEvaluadaCastingResponse;
import org.mgroko.backend.postulaciones.casting.dto.PolaroidCastingResponse;
import org.mgroko.backend.postulaciones.casting.dto.TipoPolaroidCastingResponse;

public final class CastingMapper {

    private static final String PRIMER_PLANO_FRENTE = "PRIMER_PLANO_FRENTE";
    private static final String PRIMER_PLANO_PERFIL = "PRIMER_PLANO_PERFIL";
    private static final String CUERPO_COMPLETO_FRENTE = "CUERPO_COMPLETO_FRENTE";
    private static final String PREFIJO_CUERPO_COMPLETO = "CUERPO_COMPLETO_";

    private CastingMapper() {
    }

    public static CaracteristicaPerfilCastingResponse toCaracteristicaPerfil(
            CaracteristicaPerfil caracteristica) {

        var tecnica = caracteristica.getCaracteristicaTecnica();
        var valor = caracteristica.getValorCaracteristica();

        return new CaracteristicaPerfilCastingResponse(
                tecnica.getIdCaracteristica(),
                tecnica.getCodigo(),
                tecnica.getNombre(),
                tecnica.getTipoDato(),
                caracteristica.getValor(),
                valor != null ? valor.getIdValor() : null,
                valor != null ? valor.getEtiqueta() : null,
                valor != null ? valor.getColorHex() : null,
                unidad(tecnica));
    }

    public static List<PolaroidCastingResponse> toPolaroids(
            List<PolaroidPerfil> polaroids) {

        if (polaroids == null || polaroids.isEmpty()) {
            return List.of();
        }

        List<PolaroidPerfil> restantes = new ArrayList<>(polaroids);
        restantes.sort(Comparator.comparing(
                PolaroidPerfil::getOrden,
                Comparator.nullsLast(Integer::compareTo)));

        List<PolaroidPerfil> ordenadas = new ArrayList<>();
        agregarPorCodigo(restantes, ordenadas, PRIMER_PLANO_FRENTE);
        agregarPorCodigo(restantes, ordenadas, PRIMER_PLANO_PERFIL);
        agregarCuerpoEntero(restantes, ordenadas);
        ordenadas.addAll(restantes);

        List<PolaroidCastingResponse> respuesta = new ArrayList<>();
        int prioridad = 1;
        for (PolaroidPerfil polaroid : ordenadas) {
            respuesta.add(new PolaroidCastingResponse(
                    polaroid.getIdPolaroid(),
                    polaroid.getImagen() != null ? polaroid.getImagen().getIdImagen() : null,
                    polaroid.getImagen() != null ? polaroid.getImagen().getUrl() : null,
                    polaroid.getOrden(),
                    polaroid.getFechaRegistro(),
                    prioridad++,
                    toTipoPolaroid(polaroid.getTipoPolaroid())));
        }
        return respuesta;
    }

    public static CaracteristicaEvaluadaCastingResponse toCaracteristicaEvaluada(
            CaracteristicaEvaluada evaluada,
            CaracteristicaPerfil actual,
            Map<Long, CaracteristicaRequeridaCastingResponse> requeridas) {

        CaracteristicaRequeridaCastingResponse requerida =
                requeridas.get(evaluada.idCaracteristica());

        String unidad = actual != null
                ? unidad(actual.getCaracteristicaTecnica())
                : null;
        if (unidad == null && requerida != null) {
            unidad = requerida.unidad();
        }

        return new CaracteristicaEvaluadaCastingResponse(
                evaluada.idCaracteristica(),
                evaluada.codigo(),
                evaluada.nombre(),
                evaluada.requerida(),
                evaluada.cumple(),
                evaluada.valorPedido(),
                valorPerfil(actual),
                unidad);
    }

    public static HabilidadEvaluadaCastingResponse toHabilidadEvaluada(
            HabilidadEvaluada evaluada) {
        return new HabilidadEvaluadaCastingResponse(
                evaluada.idHabilidad(),
                evaluada.nombre(),
                evaluada.cumple());
    }

    private static TipoPolaroidCastingResponse toTipoPolaroid(TipoPolaroid tipo) {
        if (tipo == null) {
            return null;
        }
        String grupoNombre = tipo.getGrupo() != null ? tipo.getGrupo().getNombre() : null;
        String grupoCodigo = tipo.getGrupo() != null ? tipo.getGrupo().getCodigo() : null;
        return new TipoPolaroidCastingResponse(
                tipo.getIdTipoPolaroid(),
                tipo.getCodigo(),
                tipo.getNombre(),
                grupoCodigo,
                grupoNombre,
                nombreCompleto(grupoNombre, tipo.getNombre()));
    }

    private static String nombreCompleto(String grupo, String nombre) {
        if (grupo == null || grupo.isBlank()) {
            return nombre;
        }
        if (nombre == null || nombre.isBlank() || grupo.equalsIgnoreCase(nombre)) {
            return grupo;
        }
        return grupo + " · " + nombre;
    }

    private static void agregarPorCodigo(
            List<PolaroidPerfil> restantes,
            List<PolaroidPerfil> ordenadas,
            String codigo) {

        for (PolaroidPerfil polaroid : restantes) {
            if (polaroid.getTipoPolaroid() != null
                    && codigo.equals(polaroid.getTipoPolaroid().getCodigo())) {
                ordenadas.add(polaroid);
                restantes.remove(polaroid);
                return;
            }
        }
    }

    private static void agregarCuerpoEntero(
            List<PolaroidPerfil> restantes,
            List<PolaroidPerfil> ordenadas) {

        for (PolaroidPerfil polaroid : restantes) {
            if (polaroid.getTipoPolaroid() != null
                    && CUERPO_COMPLETO_FRENTE.equals(
                            polaroid.getTipoPolaroid().getCodigo())) {
                ordenadas.add(polaroid);
                restantes.remove(polaroid);
                return;
            }
        }

        for (PolaroidPerfil polaroid : restantes) {
            if (polaroid.getTipoPolaroid() != null
                    && polaroid.getTipoPolaroid().getCodigo() != null
                    && polaroid.getTipoPolaroid().getCodigo()
                            .startsWith(PREFIJO_CUERPO_COMPLETO)) {
                ordenadas.add(polaroid);
                restantes.remove(polaroid);
                return;
            }
        }
    }

    private static String valorPerfil(CaracteristicaPerfil caracteristica) {
        if (caracteristica == null) {
            return null;
        }
        var valor = caracteristica.getValorCaracteristica();
        if (valor != null && valor.getEtiqueta() != null
                && !valor.getEtiqueta().isBlank()) {
            return valor.getEtiqueta();
        }
        return caracteristica.getValor();
    }

    private static String unidad(
            org.mgroko.backend.modelo.CaracteristicaTecnica caracteristica) {
        if (caracteristica == null || caracteristica.getUnidadMedida() == null) {
            return null;
        }
        String simbolo = caracteristica.getUnidadMedida().getSimbolo();
        return simbolo != null && !simbolo.isBlank()
                ? simbolo
                : caracteristica.getUnidadMedida().getNombre();
    }
}
