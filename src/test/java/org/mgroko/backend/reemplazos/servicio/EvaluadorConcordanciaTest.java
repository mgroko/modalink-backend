package org.mgroko.backend.reemplazos.servicio;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.modelo.CaracteristicaTecnica;
import org.mgroko.backend.reemplazos.servicio.modelo.CandidatoPerfil;
import org.mgroko.backend.reemplazos.servicio.modelo.CaracteristicaRequerida;
import org.mgroko.backend.reemplazos.servicio.modelo.RequisitoReemplazo;
import org.mgroko.backend.reemplazos.servicio.modelo.ValorPerfil;

class EvaluadorConcordanciaTest {

    private static final Long ID_ALTURA = 1L;

    @Test
    void dentroDelRangoOrdenaPrimeroElValorMasAlto() {
        RequisitoReemplazo requisito = requisitoAltura("180", "190");

        var altura180 = EvaluadorConcordancia.evaluar(
                requisito,
                candidato(altura("180")));
        var altura190 = EvaluadorConcordancia.evaluar(
                requisito,
                candidato(altura("190")));

        assertThat(altura190.puntaje()).isGreaterThan(altura180.puntaje());
        assertThat(altura190.alternativo()).isFalse();
        assertThat(altura180.alternativo()).isFalse();
    }

    @Test
    void fueraDelRangoQuedaComoAlternativoConMenorPuntaje() {
        RequisitoReemplazo requisito = requisitoAltura("180", "190");

        var dentro = EvaluadorConcordancia.evaluar(
                requisito,
                candidato(altura("185")));
        var fuera = EvaluadorConcordancia.evaluar(
                requisito,
                candidato(altura("179")));

        assertThat(dentro.puntaje()).isGreaterThan(fuera.puntaje());
        assertThat(fuera.alternativo()).isTrue();
        assertThat(fuera.faltantes()).isNotEmpty();
    }

    @Test
    void rangoExactoPrefiereElValorYDespuesElInferior() {
        RequisitoReemplazo requisito = requisitoAltura("190", "190");

        var exacto = EvaluadorConcordancia.evaluar(
                requisito,
                candidato(altura("190")));
        var inferior = EvaluadorConcordancia.evaluar(
                requisito,
                candidato(altura("189")));
        var superior = EvaluadorConcordancia.evaluar(
                requisito,
                candidato(altura("191")));

        assertThat(exacto.puntaje()).isGreaterThan(inferior.puntaje());
        assertThat(inferior.puntaje()).isGreaterThan(superior.puntaje());
    }

    @Test
    void habilidadFaltanteMarcaAlternativoSinExcluir() {
        RequisitoReemplazo requisito = new RequisitoReemplazo(
                1L,
                "Modelo",
                Set.of(10L),
                Map.of(10L, "Posing"),
                List.of());

        var resultado = EvaluadorConcordancia.evaluar(
                requisito,
                candidato(altura("190")));

        assertThat(resultado.puntaje()).isLessThan(new BigDecimal("100.00"));
        assertThat(resultado.alternativo()).isTrue();
        assertThat(resultado.faltantes())
                .anyMatch(texto -> texto.contains("Posing"));
    }

    @Test
    void informaRequisitosCumplidosYValorPedidoDeLaVacante() {
        Long idHabilidad = 10L;
        Long idColor = 2L;
        RequisitoReemplazo requisito = new RequisitoReemplazo(
                1L,
                "Modelo",
                Set.of(idHabilidad),
                Map.of(idHabilidad, "Desfile en Pasarela"),
                List.of(new CaracteristicaRequerida(
                        idColor,
                        "COLOR_CABELLO",
                        "Color de cabello",
                        CaracteristicaTecnica.TIPO_ENUMERADO,
                        null,
                        null,
                        Set.of(99L),
                        Map.of(99L, "Pelirrojo"))));

        CandidatoPerfil candidato = new CandidatoPerfil(
                1L,
                1L,
                "Abril",
                "Abril",
                "Pérez",
                1L,
                "Modelo",
                null,
                10L,
                20L,
                "Posadas",
                "Misiones",
                Set.of(idHabilidad),
                Map.of(idColor, new ValorPerfil("Castaño", 50L)));

        var resultado = EvaluadorConcordancia.evaluar(requisito, candidato);

        assertThat(resultado.requisitosTotales()).isEqualTo(2);
        assertThat(resultado.requisitosCumplidos()).isEqualTo(1);
        assertThat(resultado.cumpleTodo()).isFalse();
        assertThat(resultado.habilidadesEvaluadas())
                .anySatisfy(habilidad -> {
                    assertThat(habilidad.nombre())
                            .isEqualTo("Desfile en Pasarela");
                    assertThat(habilidad.cumple()).isTrue();
                });
        assertThat(resultado.caracteristicasEvaluadas())
                .anySatisfy(caracteristica -> {
                    assertThat(caracteristica.codigo())
                            .isEqualTo("COLOR_CABELLO");
                    assertThat(caracteristica.cumple()).isFalse();
                    assertThat(caracteristica.valorPedido())
                            .isEqualTo("Pelirrojo");
                });
    }

    private RequisitoReemplazo requisitoAltura(String min, String max) {
        return new RequisitoReemplazo(
                1L,
                "Modelo",
                Set.of(),
                Map.of(),
                List.of(new CaracteristicaRequerida(
                        ID_ALTURA,
                        "ALTURA",
                        "Altura",
                        CaracteristicaTecnica.TIPO_NUMERICO,
                        new BigDecimal(min),
                        new BigDecimal(max),
                        Set.of(),
                        Map.of())));
    }

    private CandidatoPerfil candidato(ValorPerfil altura) {
        return new CandidatoPerfil(
                1L,
                1L,
                "Luna",
                "Luna",
                "Pérez",
                1L,
                "Modelo",
                null,
                10L,
                20L,
                "Posadas",
                "Misiones",
                Set.of(),
                Map.of(ID_ALTURA, altura));
    }

    private ValorPerfil altura(String valor) {
        return new ValorPerfil(valor, null);
    }
}
