package org.mgroko.backend.admin.servicio;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.mgroko.backend.admin.dto.AdminCaracteristicaTecnicaRequest;
import org.mgroko.backend.admin.dto.AdminValorCaracteristicaRequest;
import org.mgroko.backend.admin.exception.CaracteristicaCodigoDuplicadoException;
import org.mgroko.backend.admin.exception.CaracteristicaEnUsoException;
import org.mgroko.backend.admin.exception.CaracteristicaTecnicaNoEncontradaException;
import org.mgroko.backend.admin.exception.TipoDatoInvalidoException;
import org.mgroko.backend.admin.exception.ValorCaracteristicaAdminNoEncontradoException;
import org.mgroko.backend.admin.exception.ValorCodigoDuplicadoException;
import org.mgroko.backend.admin.exception.ValorEnUsoException;
import org.mgroko.backend.modelo.CaracteristicaTecnica;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.ValorCaracteristica;
import org.mgroko.backend.perfiles.dto.CaracteristicaTecnicaResponse;
import org.mgroko.backend.perfiles.exception.ProfesionNoEncontradaException;
import org.mgroko.backend.perfiles.mapper.CaracteristicaTecnicaMapper;
import org.mgroko.backend.perfiles.mapper.ValorCaracteristicaMapper;
import org.mgroko.backend.perfiles.dto.ValorCaracteristicaResponse;
import org.mgroko.backend.repositorio.CaracteristicaPerfilRepository;
import org.mgroko.backend.repositorio.CaracteristicaTecnicaRepository;
import org.mgroko.backend.repositorio.ProfesionRepository;
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

    public AdminCaracteristicaTecnicaService(CaracteristicaTecnicaRepository caracteristicaTecnicaRepository,
            ValorCaracteristicaRepository valorCaracteristicaRepository,
            CaracteristicaPerfilRepository caracteristicaPerfilRepository,
            ProfesionRepository profesionRepository) {
        this.caracteristicaTecnicaRepository = caracteristicaTecnicaRepository;
        this.valorCaracteristicaRepository = valorCaracteristicaRepository;
        this.caracteristicaPerfilRepository = caracteristicaPerfilRepository;
        this.profesionRepository = profesionRepository;
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

        if (caracteristicaTecnicaRepository.existsByCodigo(request.codigo().trim())) {
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

        CaracteristicaTecnica caracteristica = CaracteristicaTecnica.builder()
                .codigo(request.codigo().trim())
                .unidad(request.unidad() != null ? request.unidad().trim() : null)
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

        String codigo = request.codigo().trim();
        if (!codigo.equalsIgnoreCase(caracteristica.getCodigo())
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

        caracteristica.setCodigo(codigo);
        caracteristica.setUnidad(request.unidad() != null ? request.unidad().trim() : null);
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
        String codigo = request.codigo().trim();
        if (valorCaracteristicaRepository.existsByCaracteristicaTecnicaIdCaracteristicaAndCodigo(
                idCaracteristica, codigo)) {
            throw new ValorCodigoDuplicadoException(
                    "La característica ya posee un valor con el código " + codigo + ".");
        }

        ValorCaracteristica valor = ValorCaracteristica.builder()
                .caracteristicaTecnica(caracteristica)
                .codigo(codigo)
                .colorHex(request.colorHex())
                .build();
        return ValorCaracteristicaMapper.toResponse(valorCaracteristicaRepository.save(valor));
    }

    @Transactional
    public ValorCaracteristicaResponse actualizarValor(Long idValor,
                                                       AdminValorCaracteristicaRequest request) {
        ValorCaracteristica valor = valorCaracteristicaRepository.findById(idValor)
                .orElseThrow(() -> new ValorCaracteristicaAdminNoEncontradoException(
                        "Valor de característica no encontrado: " + idValor));
        Long idCaracteristica = valor.getCaracteristicaTecnica().getIdCaracteristica();
        String codigo = request.codigo().trim();
        if (!codigo.equals(valor.getCodigo())
                && valorCaracteristicaRepository.existsByCaracteristicaTecnicaIdCaracteristicaAndCodigo(
                        idCaracteristica, codigo)) {
            throw new ValorCodigoDuplicadoException(
                    "La característica ya posee un valor con el código " + codigo + ".");
        }

        valor.setCodigo(codigo);
        valor.setColorHex(request.colorHex());
        return ValorCaracteristicaMapper.toResponse(valorCaracteristicaRepository.save(valor));
    }

    @Transactional
    public void eliminarValor(Long idValor) {
        ValorCaracteristica valor = valorCaracteristicaRepository.findById(idValor)
                .orElseThrow(() -> new ValorCaracteristicaAdminNoEncontradoException(
                        "Valor de característica no encontrado: " + idValor));
        if (caracteristicaPerfilRepository.existsByValorCaracteristicaIdValor(idValor)) {
            throw new ValorEnUsoException(
                    "El valor está en uso por perfiles y no puede eliminarse.");
        }
        valorCaracteristicaRepository.delete(valor);
    }

    private void agregarValoresIniciales(CaracteristicaTecnica caracteristica,
                                         List<AdminValorCaracteristicaRequest> valores) {
        Set<String> vistos = new HashSet<>();
        for (AdminValorCaracteristicaRequest v : valores) {
            String codigo = v.codigo().trim();
            if (!vistos.add(codigo)) {
                throw new ValorCodigoDuplicadoException(
                        "Valor de catálogo duplicado en la solicitud: " + codigo + ".");
            }
            valorCaracteristicaRepository.save(ValorCaracteristica.builder()
                    .caracteristicaTecnica(caracteristica)
                    .codigo(codigo)
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