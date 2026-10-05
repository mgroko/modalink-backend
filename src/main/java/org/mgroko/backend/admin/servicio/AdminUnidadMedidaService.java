package org.mgroko.backend.admin.servicio;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.mgroko.backend.admin.dto.AdminUnidadMedidaRequest;
import org.mgroko.backend.admin.dto.UnidadMedidaResponse;
import org.mgroko.backend.admin.exception.TipoDatoInvalidoException;
import org.mgroko.backend.admin.exception.UnidadMedidaDuplicadaException;
import org.mgroko.backend.admin.exception.UnidadMedidaEnUsoException;
import org.mgroko.backend.admin.exception.UnidadMedidaNoEncontradaException;
import org.mgroko.backend.modelo.CaracteristicaTecnica;
import org.mgroko.backend.modelo.UnidadMedida;
import org.mgroko.backend.repositorio.CaracteristicaTecnicaRepository;
import org.mgroko.backend.repositorio.UnidadMedidaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC-74 — Gestionar unidades de medida.
 *
 * Las unidades solo admiten características de tipo NUMERICO o TEXTO: el check
 * {@code chk_caract_tipo_dato_enum_id_unidad} de la BD obliga a que toda
 * característica ENUMERADO tenga {@code id_unidad NULL}, por lo que una unidad
 * "ENUMERADO" sería inasignable.
 */
@Service
public class AdminUnidadMedidaService {

    public static final String TIPO_NUMERICO = "NUMERICO";
    public static final String TIPO_TEXTO = "TEXTO";

    private static final Set<String> TIPOS_PERMITIDOS = Set.of(TIPO_NUMERICO, TIPO_TEXTO);

    private final UnidadMedidaRepository unidadMedidaRepository;
    private final CaracteristicaTecnicaRepository caracteristicaTecnicaRepository;

    public AdminUnidadMedidaService(UnidadMedidaRepository unidadMedidaRepository,
            CaracteristicaTecnicaRepository caracteristicaTecnicaRepository) {
        this.unidadMedidaRepository = unidadMedidaRepository;
        this.caracteristicaTecnicaRepository = caracteristicaTecnicaRepository;
    }

    @Transactional(readOnly = true)
    public List<UnidadMedidaResponse> listar(String tipoDato) {
        String tipo = normalizarTipoDatoFiltro(tipoDato);

        return unidadMedidaRepository.findAll().stream()
                .filter(u -> tipo == null || tipo.equalsIgnoreCase(u.getTipoDatoPermitido()))
                .sorted(Comparator.comparing(UnidadMedida::getNombre,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .map(AdminUnidadMedidaService::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public UnidadMedidaResponse obtener(Long idUnidad) {
        return toResponse(buscarOFallar(idUnidad));
    }

    @Transactional
    public UnidadMedidaResponse crear(AdminUnidadMedidaRequest request) {
        String nombre = request.nombre().trim();
        String simbolo = request.simbolo().trim();
        String tipoDatoPermitido = validarTipoDatoPermitido(request.tipoDatoPermitido());

        if (unidadMedidaRepository.existsByNombreIgnoreCase(nombre)) {
            throw new UnidadMedidaDuplicadaException(
                    "Ya existe una unidad de medida con el nombre " + nombre + ".");
        }
        if (unidadMedidaRepository.existsBySimboloIgnoreCase(simbolo)) {
            throw new UnidadMedidaDuplicadaException(
                    "Ya existe una unidad de medida con el símbolo " + simbolo + ".");
        }

        UnidadMedida unidad = UnidadMedida.builder()
                .nombre(nombre)
                .simbolo(simbolo)
                .tipoDatoPermitido(tipoDatoPermitido)
                .build();

        return toResponse(unidadMedidaRepository.save(unidad));
    }

    @Transactional
    public UnidadMedidaResponse actualizar(Long idUnidad, AdminUnidadMedidaRequest request) {
        UnidadMedida unidad = buscarOFallar(idUnidad);
        String nombre = request.nombre().trim();
        String simbolo = request.simbolo().trim();
        String tipoDatoPermitido = validarTipoDatoPermitido(request.tipoDatoPermitido());

        if (unidadMedidaRepository.existsByNombreIgnoreCaseAndIdUnidadNot(nombre, idUnidad)) {
            throw new UnidadMedidaDuplicadaException(
                    "Ya existe una unidad de medida con el nombre " + nombre + ".");
        }
        if (unidadMedidaRepository.existsBySimboloIgnoreCaseAndIdUnidadNot(simbolo, idUnidad)) {
            throw new UnidadMedidaDuplicadaException(
                    "Ya existe una unidad de medida con el símbolo " + simbolo + ".");
        }

        if (!tipoDatoPermitido.equalsIgnoreCase(unidad.getTipoDatoPermitido())) {
            validarTipoCompatibleConCaracteristicas(idUnidad, tipoDatoPermitido);
        }

        unidad.setNombre(nombre);
        unidad.setSimbolo(simbolo);
        unidad.setTipoDatoPermitido(tipoDatoPermitido);

        return toResponse(unidadMedidaRepository.save(unidad));
    }

    @Transactional
    public void eliminar(Long idUnidad) {
        UnidadMedida unidad = buscarOFallar(idUnidad);
        if (caracteristicaTecnicaRepository.existsByUnidadMedidaIdUnidad(idUnidad)) {
            throw new UnidadMedidaEnUsoException(
                    "La unidad de medida está asociada a una característica técnica y no puede eliminarse.");
        }
        unidadMedidaRepository.delete(unidad);
    }

    /**
     * Impide que una unidad en uso quede con un tipo de dato incompatible con las
     * características técnicas que la referencian.
     */
    private void validarTipoCompatibleConCaracteristicas(Long idUnidad, String tipoDatoPermitido) {
        List<CaracteristicaTecnica> asociadas = caracteristicaTecnicaRepository.findAllByUnidadMedidaIdUnidad(idUnidad);
        List<String> incompatibles = asociadas.stream()
                .filter(c -> !tipoDatoPermitido.equalsIgnoreCase(c.getTipoDato()))
                .map(CaracteristicaTecnica::getCodigo)
                .toList();

        if (!incompatibles.isEmpty()) {
            throw new UnidadMedidaEnUsoException(
                    "La unidad de medida está en uso por características de tipo "
                            + asociadas.stream()
                                    .map(CaracteristicaTecnica::getTipoDato)
                                    .distinct()
                                    .toList()
                            + " y no puede cambiarse a " + tipoDatoPermitido
                            + ". Características afectadas: " + incompatibles + ".");
        }
    }

    private String validarTipoDatoPermitido(String tipoDatoPermitido) {
        String tipo = tipoDatoPermitido != null ? tipoDatoPermitido.trim().toUpperCase(Locale.ROOT) : null;
        if (tipo == null || !TIPOS_PERMITIDOS.contains(tipo)) {
            throw new TipoDatoInvalidoException("tipoDatoPermitido debe ser NUMERICO o TEXTO.");
        }
        return tipo;
    }

    private String normalizarTipoDatoFiltro(String tipoDato) {
        if (tipoDato == null || tipoDato.isBlank()) {
            return null;
        }
        return validarTipoDatoPermitido(tipoDato);
    }

    private UnidadMedida buscarOFallar(Long idUnidad) {
        return unidadMedidaRepository.findById(idUnidad)
                .orElseThrow(() -> new UnidadMedidaNoEncontradaException(
                        "Unidad de medida no encontrada: " + idUnidad));
    }

    private static UnidadMedidaResponse toResponse(UnidadMedida unidad) {
        return new UnidadMedidaResponse(
                unidad.getIdUnidad(),
                unidad.getNombre(),
                unidad.getSimbolo(),
                unidad.getTipoDatoPermitido());
    }
}
