package org.mgroko.backend.admin.servicio;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.mgroko.backend.admin.dto.AdminCaracteristicaTecnicaRequest;
import org.mgroko.backend.admin.dto.AdminValorCaracteristicaRequest;
import org.mgroko.backend.admin.exception.CaracteristicaCodigoDuplicadoException;
import org.mgroko.backend.admin.exception.CaracteristicaEnUsoException;
import org.mgroko.backend.admin.exception.CaracteristicaTecnicaNoEncontradaException;
import org.mgroko.backend.admin.exception.TipoDatoInvalidoException;
import org.mgroko.backend.admin.exception.UnidadMedidaNoEncontradaException;
import org.mgroko.backend.admin.exception.ValorCaracteristicaAdminNoEncontradoException;
import org.mgroko.backend.admin.exception.ValorCodigoDuplicadoException;
import org.mgroko.backend.admin.exception.ValorEnUsoException;
import org.mgroko.backend.modelo.CaracteristicaTecnica;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.UnidadMedida;
import org.mgroko.backend.modelo.ValorCaracteristica;
import org.mgroko.backend.modelo.ValorCaracteristicaId;
import org.mgroko.backend.perfiles.dto.CaracteristicaTecnicaResponse;
import org.mgroko.backend.perfiles.exception.ProfesionNoEncontradaException;
import org.mgroko.backend.perfiles.mapper.CaracteristicaTecnicaMapper;
import org.mgroko.backend.perfiles.mapper.ValorCaracteristicaMapper;
import org.mgroko.backend.perfiles.dto.ValorCaracteristicaResponse;
import org.mgroko.backend.repositorio.CaracteristicaPerfilRepository;
import org.mgroko.backend.repositorio.CaracteristicaTecnicaRepository;
import org.mgroko.backend.repositorio.ProfesionRepository;
import org.mgroko.backend.repositorio.UnidadMedidaRepository;
import org.mgroko.backend.repositorio.ValorCaracteristicaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminCaracteristicaTecnicaService {

    private static final Set<String> TIPOS_VALIDOS =
            Set.of(CaracteristicaTecnica.TIPO_ENUMERADO,
                    CaracteristicaTecnica.TIPO_TEXTO,
                    CaracteristicaTecnica.TIPO_NUMERICO);

    private final CaracteristicaTecnicaRepository caracteristicaTecnicaRepository;
    private final ValorCaracteristicaRepository valorCaracteristicaRepository;
    private final CaracteristicaPerfilRepository caracteristicaPerfilRepository;
    private final ProfesionRepository profesionRepository;
    private final UnidadMedidaRepository unidadMedidaRepository;

    public AdminCaracteristicaTecnicaService(CaracteristicaTecnicaRepository caracteristicaTecnicaRepository,
            ValorCaracteristicaRepository valorCaracteristicaRepository,
            CaracteristicaPerfilRepository caracteristicaPerfilRepository,
            ProfesionRepository profesionRepository,
            UnidadMedidaRepository unidadMedidaRepository) {
        this.caracteristicaTecnicaRepository = caracteristicaTecnicaRepository;
        this.valorCaracteristicaRepository = valorCaracteristicaRepository;
        this.caracteristicaPerfilRepository = caracteristicaPerfilRepository;
        this.profesionRepository = profesionRepository;
        this.unidadMedidaRepository = unidadMedidaRepository;
    }

    @Transactional(readOnly = true)
    public List<CaracteristicaTecnicaResponse> listar() {
        return caracteristicaTecnicaRepository.findAllByOrderByCodigo().stream()
                .map(CaracteristicaTecnicaMapper::toResponse)
                .toList();
    }

    @Transactional
    public CaracteristicaTecnicaResponse crear(AdminCaracteristicaTecnicaRequest request) {
        validarTipoDato(request.tipoDato());

        if (caracteristicaTecnicaRepository.existsByCodigo(request.codigo().trim().toUpperCase(Locale.ROOT))) {
            throw new CaracteristicaCodigoDuplicadoException(
                    "Ya existe una característica técnica con el código " + request.codigo() + ".");
        }

        Profesion profesion = buscarProfesion(request.idProfesion());
        boolean enumerado = CaracteristicaTecnica.TIPO_ENUMERADO.equals(request.tipoDato());
        if (!enumerado && request.valores() != null && !request.valores().isEmpty()) {
            throw new TipoDatoInvalidoException(
                    "La característica " + request.tipoDato()
                            + " no admite valores de catálogo (valores).");
        }

        UnidadMedida unidad = resolverYValidarUnidad(request.idUnidad(), request.tipoDato());

        String nombre = request.nombre() != null ? request.nombre().trim() : null;

        CaracteristicaTecnica caracteristica = CaracteristicaTecnica.builder()
                .codigo(request.codigo().trim().toUpperCase(Locale.ROOT))
                .nombre(nombre)
                .unidadMedida(unidad)
                .tipoDato(request.tipoDato())
                .profesion(profesion)
                .valores(new ArrayList<>())
                .build();

        caracteristica = caracteristicaTecnicaRepository.save(caracteristica);

        if (enumerado && request.valores() != null) {
            agregarValoresIniciales(caracteristica, request.valores());
        }

        return CaracteristicaTecnicaMapper.toResponse(caracteristica);
    }

    @Transactional
    public CaracteristicaTecnicaResponse actualizar(Long idCaracteristica,
                                                    AdminCaracteristicaTecnicaRequest request) {
        CaracteristicaTecnica caracteristica = buscarOFallar(idCaracteristica);
        validarTipoDato(request.tipoDato());

        String codigo = request.codigo().trim().toUpperCase(Locale.ROOT);
        if (!codigo.equals(caracteristica.getCodigo().toUpperCase(Locale.ROOT))
                && caracteristicaTecnicaRepository.existsByCodigo(codigo)) {
            throw new CaracteristicaCodigoDuplicadoException(
                    "Ya existe una característica técnica con el código " + codigo + ".");
        }

        Profesion profesion = buscarProfesion(request.idProfesion());

        boolean pasaANoEnumerado =
                CaracteristicaTecnica.TIPO_ENUMERADO.equals(caracteristica.getTipoDato())
                        && !CaracteristicaTecnica.TIPO_ENUMERADO.equals(request.tipoDato());
        if (pasaANoEnumerado && !caracteristica.getValores().isEmpty()) {
            throw new CaracteristicaEnUsoException(
                    "La característica posee valores de catálogo; elimínelos antes de cambiar su tipo.");
        }
        if (!CaracteristicaTecnica.TIPO_ENUMERADO.equals(request.tipoDato())
                && request.valores() != null && !request.valores().isEmpty()) {
            throw new TipoDatoInvalidoException(
                    "La característica " + request.tipoDato() + " no admite valores de catálogo (valores).");
        }

        UnidadMedida unidad = resolverYValidarUnidad(request.idUnidad(), request.tipoDato());

        caracteristica.setCodigo(codigo);
        caracteristica.setNombre(request.nombre() != null ? request.nombre().trim() : null);
        caracteristica.setUnidadMedida(unidad);
        caracteristica.setTipoDato(request.tipoDato());
        caracteristica.setProfesion(profesion);

        return CaracteristicaTecnicaMapper.toResponse(caracteristicaTecnicaRepository.save(caracteristica));
    }

    @Transactional
    public void eliminar(Long idCaracteristica) {
        CaracteristicaTecnica caracteristica = buscarOFallar(idCaracteristica);
        if (caracteristicaPerfilRepository.existsByCaracteristicaTecnicaIdCaracteristica(idCaracteristica)) {
            throw new CaracteristicaEnUsoException(
                    "La característica técnica está en uso por perfiles y no puede eliminarse.");
        }
        if (!caracteristica.getValores().isEmpty()) {
            valorCaracteristicaRepository.deleteAll(caracteristica.getValores());
        }
        caracteristicaTecnicaRepository.delete(caracteristica);
    }

    @Transactional
    public ValorCaracteristicaResponse agregarValor(Long idCaracteristica,
                                                    AdminValorCaracteristicaRequest request) {
        CaracteristicaTecnica caracteristica = buscarOFallar(idCaracteristica);
        if (!CaracteristicaTecnica.TIPO_ENUMERADO.equals(caracteristica.getTipoDato())) {
            throw new TipoDatoInvalidoException(
                    "Solo las características de tipo ENUMERADO admiten valores de catálogo.");
        }
        String etiqueta = request.codigo().trim().toUpperCase(Locale.ROOT);
        if (valorCaracteristicaRepository.existsByCaracteristicaTecnica_IdCaracteristicaAndEtiqueta(
                idCaracteristica, etiqueta)) {
            throw new ValorCodigoDuplicadoException(
                    "La característica ya posee un valor con el código " + etiqueta + ".");
        }

        ValorCaracteristica valor = ValorCaracteristica.builder()
                .id(new ValorCaracteristicaId(null, idCaracteristica))
                .caracteristicaTecnica(caracteristica)
                .etiqueta(etiqueta)
                .colorHex(request.colorHex())
                .build();
        return ValorCaracteristicaMapper.toResponse(valorCaracteristicaRepository.save(valor));
    }

    @Transactional
    public ValorCaracteristicaResponse actualizarValor(Long idCaracteristica,
                                                       Long idValor,
                                                       AdminValorCaracteristicaRequest request) {
        ValorCaracteristicaId id = new ValorCaracteristicaId(idValor, idCaracteristica);
        ValorCaracteristica valor = valorCaracteristicaRepository.findById(id)
                .orElseThrow(() -> new ValorCaracteristicaAdminNoEncontradoException(
                        "Valor de característica no encontrado: " + idValor));
        String etiqueta = request.codigo().trim();
        if (!etiqueta.equals(valor.getEtiqueta())
                && valorCaracteristicaRepository.existsByCaracteristicaTecnica_IdCaracteristicaAndEtiqueta(
                        idCaracteristica, etiqueta)) {
            throw new ValorCodigoDuplicadoException(
                    "La característica ya posee un valor con el código " + etiqueta + ".");
        }

        valor.setEtiqueta(etiqueta);
        valor.setColorHex(request.colorHex());
        return ValorCaracteristicaMapper.toResponse(valorCaracteristicaRepository.save(valor));
    }

    @Transactional
    public void eliminarValor(Long idCaracteristica, Long idValor) {
        ValorCaracteristicaId id = new ValorCaracteristicaId(idValor, idCaracteristica);
        ValorCaracteristica valor = valorCaracteristicaRepository.findById(id)
                .orElseThrow(() -> new ValorCaracteristicaAdminNoEncontradoException(
                        "Valor de característica no encontrado: " + idValor));
        if (caracteristicaPerfilRepository.existsByValorCaracteristica_Id_IdValor(idValor)) {
            throw new ValorEnUsoException(
                    "El valor está en uso por perfiles y no puede eliminarse.");
        }
        valorCaracteristicaRepository.delete(valor);
    }

    private UnidadMedida resolverYValidarUnidad(Long idUnidad, String tipoDato) {
        if (idUnidad == null) {
            return null;
        }
        UnidadMedida unidad = unidadMedidaRepository.findById(idUnidad)
                .orElseThrow(() -> new UnidadMedidaNoEncontradaException(
                        "Unidad de medida no encontrada: " + idUnidad));

        if (unidad.getTipoDatoPermitido() != null
                && !unidad.getTipoDatoPermitido().equalsIgnoreCase(tipoDato)) {
            throw new TipoDatoInvalidoException(
                    "La unidad de medida " + unidad.getSimbolo()
                            + " solo está permitida para características de tipo "
                            + unidad.getTipoDatoPermitido() + ".");
        }
        return unidad;
    }

    private void agregarValoresIniciales(CaracteristicaTecnica caracteristica,
                                         List<AdminValorCaracteristicaRequest> valores) {
        Set<String> vistos = new HashSet<>();
        for (AdminValorCaracteristicaRequest v : valores) {
            String etiqueta = v.codigo().trim();
            if (!vistos.add(etiqueta)) {
                throw new ValorCodigoDuplicadoException(
                        "Valor de catálogo duplicado en la solicitud: " + etiqueta + ".");
            }
            valorCaracteristicaRepository.save(ValorCaracteristica.builder()
                    .id(new ValorCaracteristicaId(null, caracteristica.getIdCaracteristica()))
                    .caracteristicaTecnica(caracteristica)
                    .etiqueta(etiqueta)
                    .colorHex(v.colorHex())
                    .build());
        }
    }

    private CaracteristicaTecnica buscarOFallar(Long idCaracteristica) {
        return caracteristicaTecnicaRepository.findById(idCaracteristica)
                .orElseThrow(() -> new CaracteristicaTecnicaNoEncontradaException(
                        "Característica técnica no encontrada: " + idCaracteristica));
    }

    private Profesion buscarProfesion(Long idProfesion) {
        return profesionRepository.findById(idProfesion)
                .orElseThrow(() -> new ProfesionNoEncontradaException(
                        "Profesión no encontrada: " + idProfesion));
    }

    private void validarTipoDato(String tipoDato) {
        if (tipoDato == null || !TIPOS_VALIDOS.contains(tipoDato)) {
            throw new TipoDatoInvalidoException(
                    "tipoDato debe ser ENUMERADO, TEXTO o NUMERICO.");
        }
    }
}