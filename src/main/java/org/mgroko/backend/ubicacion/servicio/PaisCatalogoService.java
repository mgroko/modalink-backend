package org.mgroko.backend.ubicacion.servicio;

import org.mgroko.backend.modelo.ConfiguracionSistema;
import org.mgroko.backend.modelo.Pais;
import org.mgroko.backend.repositorio.ConfiguracionSistemaRepository;
import org.mgroko.backend.repositorio.PaisRepository;
import org.mgroko.backend.ubicacion.catalogo.FuenteCatalogo;
import org.mgroko.backend.ubicacion.exception.PaisNoConfiguradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resuelve el país al que pertenece una fuente de catálogo geográfico.
 *
 * <p>El país no viaja en el catálogo: depende de la fuente y no de la localidad.
 * Georef, por ejemplo, es un catálogo de un solo país y su API no lo entrega.
 * Por eso el país de cada fuente se declara en {@code configuracion_sistema} y
 * esta clase es el único lugar del sistema que sabe traducir
 * {@link FuenteCatalogo} a una clave de configuración.</p>
 *
 * <p>La clave se deriva del enum ({@code CATALOGO_<FUENTE>_PAIS_ISO}) en lugar de
 * escribirse a mano, así que dar de alta una fuente nueva es agregar su fila en
 * {@code configuracion_sistema}, sin tocar esta clase. El nombre del país no se
 * configura: vive en {@code pais.nombre}, que es la fuente de verdad.</p>
 *
 * <p>Si el país no se puede determinar la operación falla. No se inventa ni se
 * crea un país sin definir, porque {@code provincia.id_pais} es NOT NULL y una
 * provincia sin país real contamina el catálogo para siempre.</p>
 */
@Service
public class PaisCatalogoService {

    private static final String PREFIJO_CLAVE = "CATALOGO_";
    private static final String SUFIJO_CLAVE = "_PAIS_ISO";

    private final ConfiguracionSistemaRepository configuracionSistemaRepository;
    private final PaisRepository paisRepository;

    public PaisCatalogoService(
            ConfiguracionSistemaRepository configuracionSistemaRepository,
            PaisRepository paisRepository) {
        this.configuracionSistemaRepository = configuracionSistemaRepository;
        this.paisRepository = paisRepository;
    }

    /**
     * Devuelve el país declarado para la fuente indicada.
     *
     * @param fuente fuente de catálogo geográfico
     * @return el país activo configurado para esa fuente
     * @throws PaisNoConfiguradoException si la clave no existe, está vacía, o
     *                                   apunta a un país inexistente o inactivo
     */
    @Transactional(readOnly = true)
    public Pais resolver(FuenteCatalogo fuente) {
        String clave = clavePaisIso(fuente);

        String codigoIso = configuracionSistemaRepository.findByClave(clave)
                .map(ConfiguracionSistema::getValor)
                .filter(valor -> valor != null && !valor.isBlank())
                .orElseThrow(() -> new PaisNoConfiguradoException(
                        "No se pudo determinar el pais de la fuente " + fuente.name()
                                + ": falta el valor de '" + clave + "' en configuracion_sistema."));

        String codigoNormalizado = codigoIso.trim().toUpperCase();

        return paisRepository.findByCodigoIsoAndActivoTrue(codigoNormalizado)
                .orElseThrow(() -> new PaisNoConfiguradoException(
                        "La clave '" + clave + "' declara el codigo ISO '" + codigoNormalizado
                                + "', que no corresponde a un pais activo en la base."));
    }

    private String clavePaisIso(FuenteCatalogo fuente) {
        return PREFIJO_CLAVE + fuente.name() + SUFIJO_CLAVE;
    }
}
