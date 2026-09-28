package org.mgroko.backend.ubicacion.servicio;

import java.math.BigDecimal;

import org.mgroko.backend.modelo.Ciudad;
import org.mgroko.backend.modelo.Provincia;
import org.mgroko.backend.modelo.Ubicacion;
import org.mgroko.backend.repositorio.CiudadRepository;
import org.mgroko.backend.repositorio.ProvinciaRepository;
import org.mgroko.backend.repositorio.UbicacionRepository;
import org.mgroko.backend.ubicacion.catalogo.LocalidadCatalogo;
import org.mgroko.backend.ubicacion.exception.LocalidadSinProvinciaException;
import org.mgroko.backend.ubicacion.exception.ProvinciaSinLocalidadException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Transforma una localidad del catálogo de geolocalización en la jerarquía
 * Pais -> Provincia -> Ciudad -> Ubicacion normalizada según ModaLinkBD.sql.
 * Si la misma ciudad y ubicación ya existen, se reutilizan.
 * El código es agnóstico a la fuente API (GEOREF, GEONAMES, GOOGLEMAPS, etc).
 *
 * <p>Las altas se idempotentan por la clave natural del catálogo
 * ({@code fuente_api} + {@code id_externo}), nunca por nombre: el nombre de una
 * provincia o de una ciudad se repite entre países y entre catálogos. Por eso
 * no existe una búsqueda previa por {@code (nombre ciudad, nombre provincia)}:
 * esa consulta no filtraba por país ni por fuente, y podía devolver la fila de
 * otra provincia, de otro país o de otro catálogo.</p>
 *
 * <p>El país no se busca en el camino de lectura: solo hace falta para dar de
 * alta una provincia, y eso lo resuelve {@link PaisCatalogoService}.</p>
 */
@Service
public class UbicacionService {

    private final UbicacionRepository ubicacionRepository;
    private final CiudadRepository ciudadRepository;
    private final ProvinciaRepository provinciaRepository;
    private final PaisCatalogoService paisCatalogoService;
    private final GeorefCatalogoService catalogoGeoref;

    public UbicacionService(
            UbicacionRepository ubicacionRepository,
            CiudadRepository ciudadRepository,
            ProvinciaRepository provinciaRepository,
            PaisCatalogoService paisCatalogoService,
            GeorefCatalogoService catalogoGeoref) {
        this.ubicacionRepository = ubicacionRepository;
        this.ciudadRepository = ciudadRepository;
        this.provinciaRepository = provinciaRepository;
        this.paisCatalogoService = paisCatalogoService;
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
     * Devuelve la {@code Ubicacion} que corresponde a una localidad de geolocalización,
     * creándola si todavía no existe.
     *
     * @param localidadId id de la localidad en el catálogo (puede ser GEOREF, GEONAMES, GOOGLEMAPS, etc)
     * @param provinciaId id de la provincia opcional (depende de la fuente API)
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

        // Busca en catálogo interno; la fuente API se determina al momento de crear/sincronizar
        LocalidadCatalogo localidad = catalogoGeoref.obtenerLocalidad(localidadId);
        return crear(localidad);
    }

    /**
     * Resuelve la ciudad por clave natural y reutiliza su ubicacion si ya tiene
     * una. Es el unico camino de alta y de reutilizacion: no hay una busqueda
     * previa por nombre que pueda cortocircuitar con la fila de otra ciudad.
     */
    private Ubicacion crear(LocalidadCatalogo localidad) {
        validarProvincia(localidad);

        String fuenteApi = localidad.fuente().codigo();
        BigDecimal latitud = localidad.latitud();
        BigDecimal longitud = localidad.longitud();

        Ciudad ciudad = ciudadRepository
                .findByFuenteApiAndIdExterno(fuenteApi, localidad.idExterno())
                .orElseGet(() -> crearCiudad(localidad, fuenteApi, latitud, longitud));

        return ubicacionRepository
                .findByCiudad_IdCiudad(ciudad.getIdCiudad())
                .orElseGet(() -> ubicacionRepository.save(Ubicacion.builder()
                        .ciudad(ciudad)
                        .build()));
    }

    private Ciudad crearCiudad(LocalidadCatalogo localidad, String fuenteApi, BigDecimal latitud, BigDecimal longitud) {
        // El pais solo se resuelve al dar de alta la provincia, nunca al buscarla.
        Provincia provincia = provinciaRepository
                .findByFuenteApiAndIdExterno(fuenteApi, localidad.idProvincia())
                .orElseGet(() -> provinciaRepository.save(Provincia.builder()
                        .nombre(localidad.nombreProvincia())
                        .idExterno(localidad.idProvincia())
                        .fuenteApi(fuenteApi)
                        .activo(true)
                        .pais(paisCatalogoService.resolver(localidad.fuente()))
                        .build()));

        return ciudadRepository.save(Ciudad.builder()
                .nombre(localidad.nombre())
                .idExterno(localidad.idExterno())
                .fuenteApi(fuenteApi)
                .activo(true)
                .latitudDefecto(latitud)
                .longitudDefecto(longitud)
                .provincia(provincia)
                .build());
    }

    private void validarProvincia(LocalidadCatalogo localidad) {
        if (localidad.idProvincia() == null || localidad.idProvincia().isBlank()
                || localidad.nombreProvincia() == null || localidad.nombreProvincia().isBlank()) {
            throw new LocalidadSinProvinciaException(
                    "La localidad '" + localidad.nombre() + "' de la fuente " + localidad.fuente().codigo()
                            + " no tiene provincia, y es obligatoria para darla de alta.");
        }
    }
}
