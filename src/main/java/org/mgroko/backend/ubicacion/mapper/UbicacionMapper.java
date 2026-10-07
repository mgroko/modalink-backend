package org.mgroko.backend.ubicacion.mapper;

import org.mgroko.backend.modelo.Ciudad;
import org.mgroko.backend.modelo.Pais;
import org.mgroko.backend.modelo.Provincia;
import org.mgroko.backend.modelo.Ubicacion;
import org.mgroko.backend.ubicacion.dto.CiudadResponse;
import org.mgroko.backend.ubicacion.dto.PaisResponse;
import org.mgroko.backend.ubicacion.dto.ProvinciaResponse;
import org.mgroko.backend.ubicacion.dto.UbicacionResponse;

/**
 * Traduce la cadena Pais -> Provincia -> Ciudad -> Ubicacion a la misma forma
 * anidada que usan los DTO de respuesta.
 *
 * Todos los metodos son tolerantes a null en cada eslabon: la respuesta debe
 * poder describir una ubicacion a medio.catalogar (por ejemplo una ciudad sin
 * provincia) sin romper la serializacion.
 */
public final class UbicacionMapper {

    private UbicacionMapper() {
    }

    public static UbicacionResponse toResponse(Ubicacion ubicacion) {
        if (ubicacion == null) {
            return null;
        }
        return new UbicacionResponse(
                ubicacion.getIdUbicacion(),
                ubicacion.getDireccion(),
                ubicacion.getCodigoPostal(),
                ubicacion.getLatitud(),
                ubicacion.getLongitud(),
                toCiudadResponse(ubicacion.getCiudad()));
    }

    public static CiudadResponse toCiudadResponse(Ciudad ciudad) {
        if (ciudad == null) {
            return null;
        }
        return new CiudadResponse(
                ciudad.getIdCiudad(),
                ciudad.getIdExterno(),
                ciudad.getFuenteApi(),
                ciudad.getNombre(),
                toProvinciaResponse(ciudad.getProvincia()));
    }

    public static ProvinciaResponse toProvinciaResponse(Provincia provincia) {
        if (provincia == null) {
            return null;
        }
        return new ProvinciaResponse(
                provincia.getIdProvincia(),
                provincia.getIdExterno(),
                provincia.getFuenteApi(),
                provincia.getNombre(),
                toPaisResponse(provincia.getPais()));
    }

    public static PaisResponse toPaisResponse(Pais pais) {
        if (pais == null) {
            return null;
        }
        return new PaisResponse(
                pais.getIdPais(),
                pais.getCodigoIso(),
                pais.getNombre());
    }
}
