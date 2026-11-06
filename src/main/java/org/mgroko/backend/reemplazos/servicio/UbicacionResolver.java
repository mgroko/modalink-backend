package org.mgroko.backend.reemplazos.servicio;

import java.util.Objects;

import org.mgroko.backend.modelo.Ubicacion;
import org.mgroko.backend.reemplazos.enums.NivelUbicacion;
import org.mgroko.backend.reemplazos.enums.TipoReemplazo;
import org.mgroko.backend.reemplazos.modelo.Reemplazo;
import org.mgroko.backend.reemplazos.servicio.modelo.CandidatoPerfil;
import org.mgroko.backend.reemplazos.servicio.modelo.UbicacionEfectiva;
import org.springframework.stereotype.Component;

/**
 * Resuelve la ubicación contra la que se ordenan los candidatos.
 *
 * <p>La actividad manda si tiene ubicación. Si no, se usa la ubicación del
 * proyecto, que es obligatoria en el flujo normal. No se usa la ubicación del
 * director.</p>
 */
@Component
public class UbicacionResolver {

    public UbicacionEfectiva resolver(Reemplazo reemplazo) {
        if (reemplazo == null) {
            return null;
        }

        Ubicacion ubicacion = null;
        if (reemplazo.getTipo() == TipoReemplazo.ACTIVIDAD
                && reemplazo.getActividad() != null) {
            ubicacion = reemplazo.getActividad().getUbicacion();
        }

        if (ubicacion == null && reemplazo.getProyecto() != null) {
            ubicacion = reemplazo.getProyecto().getUbicacion();
        }

        if (ubicacion == null || ubicacion.getCiudad() == null) {
            return null;
        }

        var ciudad = ubicacion.getCiudad();
        var provincia = ciudad.getProvincia();

        return new UbicacionEfectiva(
                ciudad.getIdCiudad(),
                provincia != null ? provincia.getIdProvincia() : null,
                ciudad.getNombre(),
                provincia != null ? provincia.getNombre() : null);
    }

    public NivelUbicacion nivel(
            UbicacionEfectiva ubicacionEfectiva,
            CandidatoPerfil candidato) {

        if (ubicacionEfectiva == null || candidato == null
                || candidato.idCiudad() == null) {
            return NivelUbicacion.OTRA;
        }

        if (Objects.equals(ubicacionEfectiva.idCiudad(), candidato.idCiudad())) {
            return NivelUbicacion.MISMA_CIUDAD;
        }

        if (ubicacionEfectiva.idProvincia() != null
                && Objects.equals(ubicacionEfectiva.idProvincia(), candidato.idProvincia())) {
            return NivelUbicacion.MISMA_PROVINCIA;
        }

        return NivelUbicacion.OTRA;
    }
}
