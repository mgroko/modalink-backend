package org.mgroko.backend.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CodigoHabilidadTest {

    @Test
    void normalizar_quitaAcentosYUsaSeparadores() {
        assertEquals("FOTOGRAFIA_DE_ESTUDIO",
                CodigoHabilidad.normalizar("Fotografía de Estudio"));
    }

    @Test
    void normalizar_colapsaSimbolosYEspacios() {
        assertEquals("CORTE_CONFECCION",
                CodigoHabilidad.normalizar("  Corte & Confección  "));
    }

    @Test
    void normalizar_mantienePalabrasSeparadasPorEspacio() {
        assertEquals("CORTE_Y_CONFECCION",
                CodigoHabilidad.normalizar("Corte y Confección"));
    }

    @Test
    void normalizar_textoSinAlfanumericos_usaBasePorDefecto() {
        assertEquals("HABILIDAD", CodigoHabilidad.normalizar("!!!"));
    }

    @Test
    void normalizar_null_usaBasePorDefecto() {
        assertEquals("HABILIDAD", CodigoHabilidad.normalizar(null));
    }

    @Test
    void normalizar_recortaALongitudMaxima() {
        assertEquals(CodigoHabilidad.LONGITUD_MAXIMA,
                CodigoHabilidad.normalizar("a".repeat(80)).length());
    }

    @Test
    void recortarParaSufijo_respetaLongitudMaximaConSufijo() {
        String recortado = CodigoHabilidad.recortarParaSufijo("A".repeat(50), "_2");

        assertEquals(CodigoHabilidad.LONGITUD_MAXIMA - 2, recortado.length());
        assertEquals("A".repeat(48), recortado);
    }
}
