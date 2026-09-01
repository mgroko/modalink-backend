package org.mgroko.backend.perfiles.servicio;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.modelo.CaracteristicaPerfil;
import org.mgroko.backend.modelo.CaracteristicaTecnica;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.ValorCaracteristica;
import org.mgroko.backend.perfiles.dto.CaracteristicaPerfilRequest;
import org.mgroko.backend.perfiles.exception.ValorNumericoNegativoException;
import org.mgroko.backend.repositorio.CaracteristicaTecnicaRepository;
import org.mgroko.backend.repositorio.ValorCaracteristicaRepository;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CaracteristicaPerfilHelperTest {

    @Mock
    private CaracteristicaTecnicaRepository caracteristicaTecnicaRepository;

    @Mock
    private ValorCaracteristicaRepository valorCaracteristicaRepository;

    @InjectMocks
    private CaracteristicaPerfilHelper helper;

    private Profesion profesionModelo() {
        return Profesion.builder().idProfesion(2L).nombre("modelo").build();
    }

    private CaracteristicaTecnica caracteristicaNumerico() {
        return CaracteristicaTecnica.builder()
                .idCaracteristica(13L)
                .codigo("medida_pecho")
                .unidad("cm")
                .tipoDato(CaracteristicaTecnica.TIPO_NUMERICO)
                .profesion(profesionModelo())
                .build();
    }

    private CaracteristicaTecnica caracteristicaTexto() {
        return CaracteristicaTecnica.builder()
                .idCaracteristica(11L)
                .codigo("altura")
                .tipoDato(CaracteristicaTecnica.TIPO_TEXTO)
                .profesion(profesionModelo())
                .build();
    }

    private CaracteristicaTecnica caracteristicaEnumerado() {
        return CaracteristicaTecnica.builder()
                .idCaracteristica(12L)
                .codigo("color_ojos")
                .tipoDato(CaracteristicaTecnica.TIPO_ENUMERADO)
                .profesion(profesionModelo())
                .build();
    }

    private void configurarCaso(CaracteristicaTecnica caracteristica) {
        when(caracteristicaTecnicaRepository.findById(caracteristica.getIdCaracteristica()))
                .thenReturn(Optional.of(caracteristica));
    }

    @Test
    void construir_numericoValorNegativo_lanzaExcepcion() {
        configurarCaso(caracteristicaNumerico());
        Perfil perfil = Perfil.builder().build();

        assertThrows(ValorNumericoNegativoException.class,
                () -> helper.construir(perfil, profesionModelo(),
                        List.of(new CaracteristicaPerfilRequest(13L, "-84", null))));
    }

    @Test
    void construir_numericoNoEsNumero_lanzaExcepcion() {
        configurarCaso(caracteristicaNumerico());
        Perfil perfil = Perfil.builder().build();

        assertThrows(ValorNumericoNegativoException.class,
                () -> helper.construir(perfil, profesionModelo(),
                        List.of(new CaracteristicaPerfilRequest(13L, "abc", null))));
    }

    @Test
    void construir_numericoValorValido_construye() {
        configurarCaso(caracteristicaNumerico());
        Perfil perfil = Perfil.builder().build();

        List<CaracteristicaPerfil> resultado = helper.construir(perfil, profesionModelo(),
                List.of(new CaracteristicaPerfilRequest(13L, "84", null)));

        assertEquals(1, resultado.size());
        assertEquals("84", resultado.get(0).getValor());
    }

    @Test
    void construir_textoValorLibre_noValidaNumerico() {
        configurarCaso(caracteristicaTexto());
        Perfil perfil = Perfil.builder().build();

        List<CaracteristicaPerfil> resultado = helper.construir(perfil, profesionModelo(),
                List.of(new CaracteristicaPerfilRequest(11L, "texto-libre", null)));

        assertEquals(1, resultado.size());
        assertEquals("texto-libre", resultado.get(0).getValor());
    }

    @Test
    void construir_enumeradoConIdValor_construye() {
        configurarCaso(caracteristicaEnumerado());
        CaracteristicaTecnica enumerado = caracteristicaEnumerado();
        ValorCaracteristica valorMarron = ValorCaracteristica.builder()
                .idValor(20L)
                .codigo("marron")
                .caracteristicaTecnica(enumerado)
                .build();
        when(valorCaracteristicaRepository.findById(20L)).thenReturn(Optional.of(valorMarron));
        Perfil perfil = Perfil.builder().build();

        List<CaracteristicaPerfil> resultado = helper.construir(perfil, profesionModelo(),
                List.of(new CaracteristicaPerfilRequest(12L, null, 20L)));

        assertEquals(1, resultado.size());
        assertEquals(20L, resultado.get(0).getValorCaracteristica().getIdValor());
    }
}
