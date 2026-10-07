package org.mgroko.backend.ubicacion.servicio;

import java.util.List;

import org.mgroko.backend.ubicacion.catalogo.LocalidadCatalogo;
import org.mgroko.backend.ubicacion.catalogo.ProvinciaCatalogo;

/**
 * Interfaz para servicios de catálogo geográfico.
 * Permite cambiar de proveedor (GEOREF, GEONAMES, etc.) sin modificar
 * las firmas de los servicios que dependen de él.
 */
public interface CatalogoGeograficoService {

    /**
     * Devuelve todas las provincias ordenadas alfabéticamente por nombre.
     */
    List<ProvinciaCatalogo> listarProvincias();

    /**
     * Busca localities en el catálogo. Ambos filtros son opcionales.
     *
     * @param provinciaId id de la provincia para acotar la búsqueda
     * @param nombre      texto a buscar dentro del nombre de la localidad
     * @return localidades que coinciden, ordenadas alfabéticamente por nombre
     */
    List<LocalidadCatalogo> buscarLocalidades(String provinciaId, String nombre);

    /**
     * Obtiene una localidad por su id del catálogo.
     *
     * @param id id de la localidad en el catálogo de origen
     * @return la localidad catalogo, o lanza excepción si no existe
     */
    LocalidadCatalogo obtenerLocalidad(String id);
}