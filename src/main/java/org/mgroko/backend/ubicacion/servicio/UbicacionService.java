package org.mgroko.backend.ubicacion.servicio;

import java.math.BigDecimal;

import org.mgroko.backend.modelo.Ubicacion;
import org.mgroko.backend.repositorio.UbicacionRepository;
import org.mgroko.backend.ubicacion.georef.LocalidadGeoref;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Transforma una localidad del catálogo de Georef en una fila de la tabla
 * {@code ubicacion}. La tabla es compartida (usuario, y más adelante
 * proyecto y actividad): si la misma localidad+provincia ya existe, se
 * reutiliza esa fila en lugar de duplicarla.
 */
@Service
public class UbicacionService {

    private static final String PAIS_DEFAULT = "Argentina";

    private final UbicacionRepository ubicacionRepository;
    private final GeorefCatalogoService catalogoGeoref;

    public UbicacionService(UbicacionRepository ubicacionRepository, GeorefCatalogoService catalogoGeoref) {
        this.ubicacionRepository = ubicacionRepository;
        this.catalogoGeoref = catalogoGeoref;
    }

    /**
     * Devuelve la {@code Ubicacion} que corresponde a la localidad de Georef
     * indicada, creándola si todavía no existe.
     *
     * @param localidadId id de la localidad en el catálogo de Georef
     * @return la fila de ubicación nueva o ya existente
     * @throws org.mgroko.backend.ubicacion.exception.LocalidadNoEncontradaException
     *         si el id no existe en el catálogo
     */
    @Transactional
    public Ubicacion obtenerOCrear(String localidadId) {
        LocalidadGeoref localidad = catalogoGeoref.obtenerLocalidad(localidadId);
        return ubicacionRepository
                .findByLocalidadAndProvincia(localidad.nombre(), localidad.provincia().nombre())
                .orElseGet(() -> crear(localidad));
    }

    private Ubicacion crear(LocalidadGeoref localidad) {
        Ubicacion nueva = Ubicacion.builder()
                .idGeoref(localidad.id())
                .localidad(localidad.nombre())
                .provincia(localidad.provincia().nombre())
                .pais(PAIS_DEFAULT)
                .latitud(BigDecimal.valueOf(localidad.centroide().lat()))
                .longitud(BigDecimal.valueOf(localidad.centroide().lon()))
                .build();
        return ubicacionRepository.save(nueva);
    }
}