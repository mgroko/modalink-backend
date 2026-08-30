package org.mgroko.backend.perfiles.servicio;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.mgroko.backend.modelo.CaracteristicaPerfil;
import org.mgroko.backend.modelo.CaracteristicaPerfilId;
import org.mgroko.backend.modelo.CaracteristicaTecnica;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.ValorCaracteristica;
import org.mgroko.backend.perfiles.dto.CaracteristicaPerfilRequest;
import org.mgroko.backend.perfiles.exception.CaracteristicaDuplicateException;
import org.mgroko.backend.perfiles.exception.CaracteristicaNoEncontradaException;
import org.mgroko.backend.perfiles.exception.CaracteristicaProfesionNoCoincideException;
import org.mgroko.backend.perfiles.exception.CaracteristicaValorNoCoincideException;
import org.mgroko.backend.perfiles.exception.IdValorObligatorioException;
import org.mgroko.backend.perfiles.exception.ValorCaracteristicaNoEncontradoException;
import org.mgroko.backend.perfiles.exception.ValorObligatorioException;
import org.mgroko.backend.repositorio.CaracteristicaTecnicaRepository;
import org.mgroko.backend.repositorio.ValorCaracteristicaRepository;
import org.springframework.stereotype.Component;

/**
 * Componente compartido que construye las entidades {@link CaracteristicaPerfil}
 * a partir de los datos recibidos en el formulario. Es reutilizado por la creación
 * (UC-10) y la edición (UC-11) de perfiles.
 */
@Component
public class CaracteristicaPerfilHelper {

    private final CaracteristicaTecnicaRepository caracteristicaTecnicaRepository;
    private final ValorCaracteristicaRepository valorCaracteristicaRepository;

    public CaracteristicaPerfilHelper(CaracteristicaTecnicaRepository caracteristicaTecnicaRepository,
            ValorCaracteristicaRepository valorCaracteristicaRepository) {
        this.caracteristicaTecnicaRepository = caracteristicaTecnicaRepository;
        this.valorCaracteristicaRepository = valorCaracteristicaRepository;
    }

    public List<CaracteristicaPerfil> construir(Perfil perfil, Profesion profesion,
            List<CaracteristicaPerfilRequest> caracteristicas) {
        if (caracteristicas == null || caracteristicas.isEmpty()) {
            return List.of();
        }

        Set<Long> vistas = new HashSet<>();
        List<CaracteristicaPerfil> resultado = new java.util.ArrayList<>();

        for (CaracteristicaPerfilRequest car : caracteristicas) {
            if (!vistas.add(car.idCaracteristica())) {
                throw new CaracteristicaDuplicateException(
                        "La característica técnica " + car.idCaracteristica() + " está duplicada.");
            }

            CaracteristicaTecnica ct = caracteristicaTecnicaRepository.findById(car.idCaracteristica())
                    .orElseThrow(() -> new CaracteristicaNoEncontradaException(
                            "Característica técnica no encontrada: " + car.idCaracteristica()));

            if (!ct.getProfesion().getIdProfesion().equals(profesion.getIdProfesion())) {
                throw new CaracteristicaProfesionNoCoincideException(
                        "La característica técnica " + car.idCaracteristica()
                                + " no corresponde a la profesión del perfil.");
            }

            CaracteristicaPerfil.CaracteristicaPerfilBuilder builder = CaracteristicaPerfil.builder()
                    .id(new CaracteristicaPerfilId(null, ct.getIdCaracteristica()))
                    .perfil(perfil)
                    .caracteristicaTecnica(ct)
                    .fechaRegistro(LocalDate.now());

            if (CaracteristicaTecnica.TIPO_ENUMERADO.equals(ct.getTipoDato())) {
                if (car.idValor() == null) {
                    throw new IdValorObligatorioException(
                            "Para la característica " + ct.getCodigo()
                                    + " (ENUMERADO) debe enviarse idValor.");
                }
                ValorCaracteristica vc = valorCaracteristicaRepository.findById(car.idValor())
                        .orElseThrow(() -> new ValorCaracteristicaNoEncontradoException(
                                "Valor de característica no encontrado: " + car.idValor()));
                if (!vc.getCaracteristicaTecnica().getIdCaracteristica().equals(ct.getIdCaracteristica())) {
                    throw new CaracteristicaValorNoCoincideException(
                            "El valor " + car.idValor()
                                    + " no corresponde a la característica técnica " + ct.getCodigo() + ".");
                }
                builder.valorCaracteristica(vc);
            } else {
                if (car.valor() == null || car.valor().isBlank()) {
                    throw new ValorObligatorioException(
                            "Para la característica " + ct.getCodigo()
                                    + " (" + ct.getTipoDato() + ") debe enviarse valor.");
                }
                builder.valor(car.valor());
            }

            resultado.add(builder.build());
        }

        return resultado;
    }
}