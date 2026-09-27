package org.mgroko.backend.ubicacion.servicio;

import java.math.BigDecimal;

import org.mgroko.backend.modelo.Ciudad;
import org.mgroko.backend.modelo.Pais;
import org.mgroko.backend.modelo.Provincia;
import org.mgroko.backend.modelo.Ubicacion;
import org.mgroko.backend.repositorio.CiudadRepository;
import org.mgroko.backend.repositorio.PaisRepository;
import org.mgroko.backend.repositorio.ProvinciaRepository;
import org.mgroko.backend.repositorio.UbicacionRepository;
import org.mgroko.backend.ubicacion.exception.ProvinciaSinLocalidadException;
import org.mgroko.backend.ubicacion.georef.LocalidadGeoref;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Transforma una localidad del catálogo de Georef en la jerarquía
 * Pais -> Provincia -> Ciudad -> Ubicacion normalizada según ModaLinkBD.sql.
 * Si la misma ciudad y ubicación ya existen, se reutilizan.
 */
@Service
public class UbicacionService {

    private static final String CODIGO_PAIS_DEFAULT = "AR";
    private static final String NOMBRE_PAIS_DEFAULT = "Argentina";

    private final UbicacionRepository ubicacionRepository;
    private final CiudadRepository ciudadRepository;
    private final ProvinciaRepository provinciaRepository;
    private final PaisRepository paisRepository;
    private final GeorefCatalogoService catalogoGeoref;

    public UbicacionService(
            UbicacionRepository ubicacionRepository,
            CiudadRepository ciudadRepository,
            ProvinciaRepository provinciaRepository,
            PaisRepository paisRepository,
            GeorefCatalogoService catalogoGeoref) {
        this.ubicacionRepository = ubicacionRepository;
        this.ciudadRepository = ciudadRepository;
        this.provinciaRepository = provinciaRepository;
        this.paisRepository = paisRepository;
        this.catalogoGeoref = catalogoGeoref;
    }

    /**
     * Devuelve la {@code Ubicacion} que corresponde a la localidad de Georef
     * indicada, creándola si todavía no existe.
     *
     * @param localidadId id de la localidad en el catálogo de Georef
     * @return la fila de ubicación nueva o ya existente
     */
    @Transactional
    public Ubicacion obtenerOCrear(String localidadId) {
        return obtenerOCrear(localidadId, null);
    }

    /**
     * Devuelve la {@code Ubicacion} que corresponde a una localidad de Georef,
     * creándola si todavía no existe.
     *
     * @param localidadId id de la localidad en el catálogo de Georef
     * @param provinciaId id de la provincia en el catálogo de Georef (opcional)
     * @return la fila de ubicación nueva o ya existente
     */
    @Transactional
    public Ubicacion obtenerOCrear(String localidadId, String provinciaId) {
        boolean tieneProvincia = provinciaId != null && !provinciaId.isBlank();
        boolean tieneLocalidad = localidadId != null && !localidadId.isBlank();

        if (tieneProvincia && !tieneLocalidad) {
            throw new ProvinciaSinLocalidadException(
                    "No se puede guardar una ubicación indicando provincia sin localidad.");
        }

        LocalidadGeoref localidad = catalogoGeoref.obtenerLocalidad(localidadId);
        return ubicacionRepository
                .findByLocalidadAndProvincia(localidad.nombre(), localidad.provincia().nombre())
                .orElseGet(() -> crear(localidad));
    }

    private Ubicacion crear(LocalidadGeoref localidad) {
        Pais pais = paisRepository.findByCodigoIso(CODIGO_PAIS_DEFAULT)
                .orElseGet(() -> paisRepository.save(Pais.builder()
                        .codigoIso(CODIGO_PAIS_DEFAULT)
                        .nombre(NOMBRE_PAIS_DEFAULT)
                        .activo(true)
                        .build()));

        Provincia provincia = provinciaRepository.findByNombre(localidad.provincia().nombre())
                .orElseGet(() -> provinciaRepository.save(Provincia.builder()
                        .nombre(localidad.provincia().nombre())
                        .idExterno(localidad.provincia().id())
                        .fuenteApi("GEOREF")
                        .activo(true)
                        .pais(pais)
                        .build()));

        BigDecimal lat = BigDecimal.valueOf(localidad.centroide().lat());
        BigDecimal lon = BigDecimal.valueOf(localidad.centroide().lon());

        Ciudad ciudad = ciudadRepository.findByNombreAndProvincia_IdProvincia(localidad.nombre(), provincia.getIdProvincia())
                .orElseGet(() -> ciudadRepository.save(Ciudad.builder()
                        .nombre(localidad.nombre())
                        .idExterno(localidad.id())
                        .fuenteApi("GEOREF")
                        .activo(true)
                        .latitudDefecto(lat)
                        .longitudDefecto(lon)
                        .provincia(provincia)
                        .build()));

        Ubicacion nueva = Ubicacion.builder()
                .ciudad(ciudad)
                .latitud(lat)
                .longitud(lon)
                .build();

        return ubicacionRepository.save(nueva);
    }
}