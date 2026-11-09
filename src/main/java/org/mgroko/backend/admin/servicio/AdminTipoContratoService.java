package org.mgroko.backend.admin.servicio;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mgroko.backend.admin.dto.AdminTipoContratoCreateRequest;
import org.mgroko.backend.admin.dto.AdminTipoContratoEstadoRequest;
import org.mgroko.backend.admin.dto.AdminTipoContratoResponse;
import org.mgroko.backend.admin.dto.AdminTipoContratoResumenResponse;
import org.mgroko.backend.admin.dto.AdminTipoContratoUpdateRequest;
import org.mgroko.backend.admin.dto.ValidacionPlantillaResponse;
import org.mgroko.backend.admin.dto.ValidarPlantillaRequest;
import org.mgroko.backend.admin.exception.PlantillaContratoInvalidaException;
import org.mgroko.backend.admin.exception.TipoContratoDuplicadoException;
import org.mgroko.backend.admin.exception.TipoContratoEnUsoException;
import org.mgroko.backend.admin.exception.TipoContratoNoEncontradoException;
import org.mgroko.backend.admin.exception.TipoContratoNoModificableException;
import org.mgroko.backend.admin.exception.UltimoTipoContratoActivoException;
import org.mgroko.backend.admin.mapper.AdminTipoContratoMapper;
import org.mgroko.backend.auditoria.servicio.AuditarOperacion;
import org.mgroko.backend.common.util.CodigoTipoContrato;
import org.mgroko.backend.modelo.TipoAcuerdoContrato;
import org.mgroko.backend.modelo.enums.EstadoContrato;
import org.mgroko.backend.repositorio.ContratoRepository;
import org.mgroko.backend.repositorio.TipoAcuerdoContratoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC-76 — Gestionar tipos de contrato.
 *
 * <p>Reglas de negocio:</p>
 * <ul>
 *   <li>El código se genera desde el nombre y es inmutable.</li>
 *   <li>El nombre y la plantilla no se pueden modificar si existen contratos
 *       en estado {@code BORRADOR}, {@code PENDIENTE_FIRMA} o {@code ACTIVO}.</li>
 *   <li>La descripción siempre puede modificarse.</li>
 *   <li>Un tipo puede desactivarse si no tiene contratos pendientes de firma
 *       y no es el último tipo activo. Los contratos ACTIVO o cerrados no
 *       impiden la desactivación.</li>
 *   <li>El borrado físico solo se permite cuando no existe ningún contrato
 *       asociado.</li>
 * </ul>
 */
@Service
public class AdminTipoContratoService {

    static final int MAX_PLANTILLA = 100_000;

    private static final Set<String> PLACEHOLDERS_PERMITIDOS = Set.of(
            "proyecto",
            "director",
            "partes",
            "clausulas_adicionales",
            "tipo");

    private static final Pattern PLACEHOLDER =
            Pattern.compile("\\{\\{([^{}]*)\\}\\}");

    private static final List<EstadoContrato> ESTADOS_VIVOS = List.of(
            EstadoContrato.BORRADOR,
            EstadoContrato.PENDIENTE_FIRMA,
            EstadoContrato.ACTIVO);

    private static final List<EstadoContrato> ESTADOS_PENDIENTES = List.of(
            EstadoContrato.BORRADOR,
            EstadoContrato.PENDIENTE_FIRMA);

    private final TipoAcuerdoContratoRepository tipoAcuerdoRepository;
    private final ContratoRepository contratoRepository;

    public AdminTipoContratoService(
            TipoAcuerdoContratoRepository tipoAcuerdoRepository,
            ContratoRepository contratoRepository) {
        this.tipoAcuerdoRepository = tipoAcuerdoRepository;
        this.contratoRepository = contratoRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminTipoContratoResumenResponse> listar(
            String nombre, boolean incluirInactivos) {

        String filtro = normalizarFiltro(nombre);
        return tipoAcuerdoRepository.findAllByOrderByNombreAsc().stream()
                .filter(tipo -> incluirInactivos || Boolean.TRUE.equals(tipo.getActivo()))
                .filter(tipo -> filtro == null
                        || normalizar(tipo.getNombre()).toLowerCase(Locale.ROOT)
                                .contains(filtro))
                .sorted(Comparator.comparing(
                        TipoAcuerdoContrato::getNombre,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .map(tipo -> AdminTipoContratoMapper.toResumen(tipo, calcularUso(tipo)))
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminTipoContratoResponse obtener(Long idTipoAcuerdo) {
        TipoAcuerdoContrato tipo = buscarOFallar(idTipoAcuerdo);
        return AdminTipoContratoMapper.toResponse(tipo, calcularUso(tipo));
    }

    @Transactional
    @AuditarOperacion
    public AdminTipoContratoResponse crear(AdminTipoContratoCreateRequest request) {
        String nombre = normalizarNombre(request.nombre());
        String descripcion = normalizarDescripcion(request.descripcion());
        String plantilla = normalizarPlantilla(request.plantilla());

        validarPlantillaParaGuardar(plantilla);

        if (tipoAcuerdoRepository.existsByNombreIgnoreCase(nombre)) {
            throw new TipoContratoDuplicadoException(
                    "Ya existe un tipo de contrato con el nombre " + nombre + ".");
        }

        boolean activo = request.activo() == null || request.activo();
        if (!activo && tipoAcuerdoRepository.countByActivoTrue() == 0) {
            throw new UltimoTipoContratoActivoException(
                    "Debe existir al menos un tipo de contrato activo.");
        }

        TipoAcuerdoContrato tipo = TipoAcuerdoContrato.builder()
                .codigo(generarCodigoUnico(nombre))
                .nombre(nombre)
                .descripcion(descripcion)
                .plantilla(plantilla)
                .activo(activo)
                .build();

        TipoAcuerdoContrato guardado = tipoAcuerdoRepository.save(tipo);
        return AdminTipoContratoMapper.toResponse(guardado, calcularUso(guardado));
    }

    @Transactional
    @AuditarOperacion
    public AdminTipoContratoResponse actualizar(
            Long idTipoAcuerdo, AdminTipoContratoUpdateRequest request) {

        TipoAcuerdoContrato tipo = buscarOFallar(idTipoAcuerdo);
        String nombre = normalizarNombre(request.nombre());
        String descripcion = normalizarDescripcion(request.descripcion());
        String plantilla = normalizarPlantilla(request.plantilla());

        validarPlantillaParaGuardar(plantilla);

        if (tipoAcuerdoRepository.existsByNombreIgnoreCaseAndIdTipoAcuerdoNot(
                nombre, idTipoAcuerdo)) {
            throw new TipoContratoDuplicadoException(
                    "Ya existe un tipo de contrato con el nombre " + nombre + ".");
        }

        AdminTipoContratoMapper.Uso uso = calcularUso(tipo);
        boolean cambiaNombre = !nombre.equals(normalizarNombre(tipo.getNombre()));
        boolean cambiaPlantilla = !plantilla.equals(normalizarPlantilla(tipo.getPlantilla()));

        if (uso.contratosVivos() > 0 && (cambiaNombre || cambiaPlantilla)) {
            throw new TipoContratoNoModificableException(
                    "TIPO_CONTRATO_CON_CONTRATOS_VIVOS",
                    "No se puede modificar el nombre ni la plantilla porque el tipo "
                            + "tiene contratos pendientes o activos asociados.");
        }

        tipo.setNombre(nombre);
        tipo.setDescripcion(descripcion);
        tipo.setPlantilla(plantilla);

        TipoAcuerdoContrato guardado = tipoAcuerdoRepository.save(tipo);
        return AdminTipoContratoMapper.toResponse(guardado, calcularUso(guardado));
    }

    @Transactional
    @AuditarOperacion
    public AdminTipoContratoResponse cambiarEstado(
            Long idTipoAcuerdo, AdminTipoContratoEstadoRequest request) {

        TipoAcuerdoContrato tipo = buscarOFallar(idTipoAcuerdo);
        boolean activo = Boolean.TRUE.equals(request.activo());

        if (Boolean.TRUE.equals(tipo.getActivo()) == activo) {
            return AdminTipoContratoMapper.toResponse(tipo, calcularUso(tipo));
        }

        if (activo) {
            if (tipoAcuerdoRepository.existsByNombreIgnoreCaseAndActivoTrueAndIdTipoAcuerdoNot(
                    tipo.getNombre(), idTipoAcuerdo)) {
                throw new TipoContratoDuplicadoException(
                        "Ya existe un tipo de contrato activo con el nombre "
                                + tipo.getNombre() + ".");
            }
        } else {
            AdminTipoContratoMapper.Uso uso = calcularUso(tipo);
            if (uso.contratosPendientes() > 0) {
                throw new TipoContratoNoModificableException(
                        "TIPO_CONTRATO_CON_CONTRATOS_PENDIENTES",
                        "No se puede desactivar el tipo porque tiene contratos "
                                + "pendientes de firma.");
            }
            if (esUltimoActivo(tipo)) {
                throw new UltimoTipoContratoActivoException(
                        "No se puede desactivar el último tipo de contrato activo.");
            }
        }

        tipo.setActivo(activo);
        TipoAcuerdoContrato guardado = tipoAcuerdoRepository.save(tipo);
        return AdminTipoContratoMapper.toResponse(guardado, calcularUso(guardado));
    }

    @Transactional
    @AuditarOperacion
    public void eliminar(Long idTipoAcuerdo) {
        TipoAcuerdoContrato tipo = buscarOFallar(idTipoAcuerdo);

        if (contratoRepository.existsByTipoAcuerdo_IdTipoAcuerdo(idTipoAcuerdo)) {
            throw new TipoContratoEnUsoException(
                    "El tipo de contrato tiene contratos asociados y no puede eliminarse. "
                            + "Podés desactivarlo para retirarlo del catálogo de generación.");
        }

        if (esUltimoActivo(tipo)) {
            throw new UltimoTipoContratoActivoException(
                    "No se puede eliminar el último tipo de contrato activo.");
        }

        tipoAcuerdoRepository.delete(tipo);
    }

    @Transactional(readOnly = true)
    public ValidacionPlantillaResponse validarPlantilla(ValidarPlantillaRequest request) {
        return validarPlantilla(request.plantilla());
    }

    ValidacionPlantillaResponse validarPlantilla(String plantilla) {
        String texto = plantilla == null ? "" : plantilla;
        int longitud = texto.codePointCount(0, texto.length());

        List<String> desconocidos = new ArrayList<>();
        List<String> formatoInvalido = new ArrayList<>();

        Matcher matcher = PLACEHOLDER.matcher(texto);
        while (matcher.find()) {
            String raw = matcher.group(0);
            String contenido = matcher.group(1);
            String token = contenido == null ? "" : contenido.trim();

            if (!PLACEHOLDERS_PERMITIDOS.contains(token)) {
                if (!desconocidos.contains(raw)) {
                    desconocidos.add(raw);
                }
            } else if (!raw.equals("{{" + token + "}}")) {
                if (!formatoInvalido.contains(raw)) {
                    formatoInvalido.add(raw);
                }
            }
        }

        List<String> advertencias = new ArrayList<>();
        if (!texto.contains("{{proyecto}}")) {
            advertencias.add("La plantilla no incluye {{proyecto}}.");
        }
        if (!texto.contains("{{director}}")) {
            advertencias.add("La plantilla no incluye {{director}}.");
        }
        if (!texto.contains("{{partes}}")) {
            advertencias.add("La plantilla no incluye {{partes}}.");
        }

        boolean valido = !texto.isBlank()
                && longitud <= MAX_PLANTILLA
                && desconocidos.isEmpty()
                && formatoInvalido.isEmpty();

        return new ValidacionPlantillaResponse(
                valido,
                longitud,
                List.copyOf(desconocidos),
                List.copyOf(formatoInvalido),
                List.copyOf(advertencias),
                texto.contains("{{clausulas_adicionales}}"));
    }

    private void validarPlantillaParaGuardar(String plantilla) {
        ValidacionPlantillaResponse validacion = validarPlantilla(plantilla);
        if (validacion.valido()) {
            return;
        }

        List<String> errores = new ArrayList<>();
        if (plantilla == null || plantilla.isBlank()) {
            errores.add("La plantilla es obligatoria.");
        }
        if (validacion.longitud() > MAX_PLANTILLA) {
            errores.add("La plantilla no puede superar los "
                    + MAX_PLANTILLA + " caracteres.");
        }
        if (!validacion.placeholdersDesconocidos().isEmpty()) {
            errores.add("Placeholders desconocidos: "
                    + String.join(", ", validacion.placeholdersDesconocidos()) + ".");
        }
        if (!validacion.placeholdersConFormatoInvalido().isEmpty()) {
            errores.add("Placeholders con formato inválido: "
                    + String.join(", ", validacion.placeholdersConFormatoInvalido()) + ".");
        }

        throw new PlantillaContratoInvalidaException(
                "La plantilla del contrato no es válida.",
                errores,
                validacion);
    }

    private AdminTipoContratoMapper.Uso calcularUso(TipoAcuerdoContrato tipo) {
        Long id = tipo.getIdTipoAcuerdo();
        long asociados = contratoRepository.countByTipoAcuerdo_IdTipoAcuerdo(id);
        long vivos = contratoRepository.countByTipoAcuerdo_IdTipoAcuerdoAndEstadoIn(
                id, ESTADOS_VIVOS);
        long pendientes = contratoRepository.countByTipoAcuerdo_IdTipoAcuerdoAndEstadoIn(
                id, ESTADOS_PENDIENTES);
        boolean ultimoActivo = esUltimoActivo(tipo);

        boolean puedeEditar = vivos == 0;
        boolean puedeDesactivar = Boolean.TRUE.equals(tipo.getActivo())
                && pendientes == 0
                && !ultimoActivo;
        boolean puedeEliminar = asociados == 0 && !ultimoActivo;

        String motivoNoEditable = puedeEditar
                ? null
                : "Tiene contratos pendientes o activos asociados.";

        String motivoNoDesactivable;
        if (!Boolean.TRUE.equals(tipo.getActivo())) {
            motivoNoDesactivable = "El tipo ya está inactivo.";
        } else if (ultimoActivo) {
            motivoNoDesactivable = "No se puede desactivar el último tipo activo.";
        } else if (pendientes > 0) {
            motivoNoDesactivable = "Tiene contratos pendientes de firma.";
        } else {
            motivoNoDesactivable = null;
        }

        String motivoNoEliminable;
        if (asociados > 0) {
            motivoNoEliminable = "Tiene " + asociados + " contrato(s) asociado(s).";
        } else if (ultimoActivo) {
            motivoNoEliminable = "No se puede eliminar el último tipo activo.";
        } else {
            motivoNoEliminable = null;
        }

        return new AdminTipoContratoMapper.Uso(
                asociados,
                vivos,
                pendientes,
                puedeEditar,
                puedeDesactivar,
                puedeEliminar,
                motivoNoEditable,
                motivoNoDesactivable,
                motivoNoEliminable);
    }

    private boolean esUltimoActivo(TipoAcuerdoContrato tipo) {
        return Boolean.TRUE.equals(tipo.getActivo())
                && tipoAcuerdoRepository.countByActivoTrue() <= 1;
    }

    private TipoAcuerdoContrato buscarOFallar(Long idTipoAcuerdo) {
        return tipoAcuerdoRepository.findById(idTipoAcuerdo)
                .orElseThrow(() -> new TipoContratoNoEncontradoException(idTipoAcuerdo));
    }

    private String generarCodigoUnico(String nombre) {
        String base = CodigoTipoContrato.normalizar(nombre);
        String candidato = base;
        int sufijo = 2;

        while (tipoAcuerdoRepository.existsByCodigoIgnoreCase(candidato)) {
            String sufijoTexto = "_" + sufijo++;
            candidato = CodigoTipoContrato.recortarParaSufijo(base, sufijoTexto)
                    + sufijoTexto;
        }

        return candidato;
    }

    private static String normalizarNombre(String nombre) {
        return nombre == null ? "" : nombre.trim();
    }

    private static String normalizarDescripcion(String descripcion) {
        if (descripcion == null) {
            return null;
        }
        String valor = descripcion.trim();
        return valor.isEmpty() ? null : valor;
    }

    private static String normalizarPlantilla(String plantilla) {
        return plantilla == null ? null : plantilla.trim();
    }

    private static String normalizar(String valor) {
        return valor == null ? "" : valor.trim();
    }

    private static String normalizarFiltro(String nombre) {
        String valor = normalizar(nombre).toLowerCase(Locale.ROOT);
        return valor.isEmpty() ? null : valor;
    }
}
