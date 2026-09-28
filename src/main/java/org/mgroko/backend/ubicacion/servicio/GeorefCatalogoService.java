package org.mgroko.backend.ubicacion.servicio;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

import org.mgroko.backend.ubicacion.catalogo.FuenteCatalogo;
import org.mgroko.backend.ubicacion.catalogo.LocalidadCatalogo;
import org.mgroko.backend.ubicacion.catalogo.ProvinciaCatalogo;
import org.mgroko.backend.ubicacion.exception.LocalidadNoEncontradaException;
import org.mgroko.backend.ubicacion.georef.LocalidadGeoref;
import org.mgroko.backend.ubicacion.georef.LocalidadesGeorefResponse;
import org.mgroko.backend.ubicacion.georef.ProvinciaGeoref;
import org.mgroko.backend.ubicacion.georef.ProvinciaGeorefRef;
import org.mgroko.backend.ubicacion.georef.ProvinciasGeorefResponse;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Adaptador del catálogo de Georef. Es el único punto que conoce la forma del
 * JSON versionado en {@code resources/georef/}: hacia afuera expone records de
 * dominio ({@link LocalidadCatalogo}, {@link ProvinciaCatalogo}) y los records
 * de {@code ubicacion.georef} no escapan de esta clase.
 *
 * <p>Los archivos se cargan una única vez al arrancar y se sirven desde memoria;
 * no se vuelve a consultar a la API externa (ver {@code scripts/descargar-georef.sh}).</p>
 *
 * <p>Georef es un catálogo exclusivo de un país y su API no entrega esa
 * información, así que este adaptador no fabrica el país ni lo agrega a los
 * records: dejar esa resolución para el consumidor, que usa el valor de la
 * fuente cuando viene y el país por defecto cuando la fuente no lo entrega.</p>
 */
@Service
public class GeorefCatalogoService implements CatalogoGeograficoService {

    private static final String RUTA_PROVINCIAS = "classpath:georef/provincias.json";
    private static final String RUTA_LOCALIDADES = "classpath:georef/localidades.json";

    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;

    private List<ProvinciaGeoref> provincias;
    private List<LocalidadGeoref> localidades;

    public GeorefCatalogoService(ObjectMapper objectMapper, ResourceLoader resourceLoader) {
        this.objectMapper = objectMapper;
        this.resourceLoader = resourceLoader;
        cargarCatalogo();
    }

    private void cargarCatalogo() {
        this.provincias = leer(RUTA_PROVINCIAS, ProvinciasGeorefResponse.class).provincias();
        this.localidades = leer(RUTA_LOCALIDADES, LocalidadesGeorefResponse.class).localidades();
    }

    private <T> T leer(String ruta, Class<T> tipo) {
        Resource resource = resourceLoader.getResource(ruta);
        try (InputStream input = resource.getInputStream()) {
            return objectMapper.readValue(input, tipo);
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo cargar el catálogo Georef: " + ruta, ex);
        }
    }

    /**
     * Devuelve todas las provincias ordenadas alfabéticamente por nombre.
     */
    public List<ProvinciaCatalogo> listarProvincias() {
        return provincias.stream()
                .sorted(Comparator.comparing(ProvinciaGeoref::nombre))
                .map(this::toCatalogo)
                .toList();
    }

    /**
     * Busca localidades en el catálogo. Ambos filtros son opcionales.
     *
     * @param provinciaId id de la provincia (Georef) para acotar la búsqueda
     * @param nombre      texto a buscar dentro del nombre de la localidad
     * @return localidades que coinciden, ordenadas alfabéticamente por nombre
     */
    public List<LocalidadCatalogo> buscarLocalidades(String provinciaId, String nombre) {
        String patron = normalizar(nombre);
        String provincia = normalizar(provinciaId);

        return localidades.stream()
                .filter(l -> provincia.isBlank() || l.provincia().id().equals(provinciaId))
                .filter(l -> patron.isBlank() || normalizar(l.nombre()).contains(patron))
                .sorted(Comparator.comparing(LocalidadGeoref::nombre))
                .map(this::toCatalogo)
                .toList();
    }

    /**
     * Busca una localidad por su id de Georef.
     *
     * @throws LocalidadNoEncontradaException si el id no existe en el catálogo
     */
    public LocalidadCatalogo obtenerLocalidad(String id) {
        return localidades.stream()
                .filter(l -> l.id().equals(id))
                .findFirst()
                .map(this::toCatalogo)
                .orElseThrow(() -> new LocalidadNoEncontradaException(
                        "Localidad no encontrada con id: " + id));
    }

    private ProvinciaCatalogo toCatalogo(ProvinciaGeoref provincia) {
        return new ProvinciaCatalogo(
                provincia.id(),
                provincia.nombre(),
                FuenteCatalogo.GEOREF,
                BigDecimal.valueOf(provincia.centroide().lat()),
                BigDecimal.valueOf(provincia.centroide().lon()));
    }

    private LocalidadCatalogo toCatalogo(LocalidadGeoref localidad) {
        ProvinciaGeorefRef provincia = localidad.provincia();
        return new LocalidadCatalogo(
                localidad.id(),
                localidad.nombre(),
                provincia != null ? provincia.id() : null,
                provincia != null ? provincia.nombre() : null,
                FuenteCatalogo.GEOREF,
                BigDecimal.valueOf(localidad.centroide().lat()),
                BigDecimal.valueOf(localidad.centroide().lon()));
    }

    private String normalizar(String valor) {
        return valor == null ? "" : valor.trim().toLowerCase();
    }
}
