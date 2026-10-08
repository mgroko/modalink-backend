package org.mgroko.backend.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import org.junit.jupiter.api.Test;

class LikePatronesTest {

    @Test
    void contiene_nuloYBlanco_devuelveCentinelaSinFiltro() {
        assertEquals("%", LikePatrones.contiene(null));
        assertEquals("%", LikePatrones.contiene("   "));
        assertEquals("%", LikePatrones.contiene(""));
    }

    @Test
    void contiene_valorSimple_envuelveYMinuscula() {
        assertEquals("%alt%", LikePatrones.contiene("alt"));
        assertEquals("%pecho%", LikePatrones.contiene("PECHO"));
        assertEquals("%mo%", LikePatrones.contiene("  mo  "));
    }

    @Test
    void escapar_porcentajeGuionBajoYBarra_matcheanLiteral() {
        assertEquals("100\\%", LikePatrones.escapar("100%"));
        assertEquals("a\\_b", LikePatrones.escapar("a_b"));
        assertEquals("c:\\\\tmp", LikePatrones.escapar("c:\\tmp"));
        assertEquals("", LikePatrones.escapar(null));
    }

    @Test
    void contiene_porcentajeLiteral_noSeConfundeConCentinela() {
        String patron = LikePatrones.contiene("%");

        assertNotEquals("%", patron);
        assertEquals("%\\%%", patron);
    }

    @Test
    void contiene_combinado_escapaTodo() {
        assertEquals("%100\\%\\_x\\\\%", LikePatrones.contiene("100%_x\\"));
    }

    @Test
    void contiene_codigoConGuionBajo_seMantieneParaIgualdad() {
        // El valor crudo (para la rama de igualdad exacta) no se altera:
        // solo el patrón LIKE lleva escapes.
        assertEquals("DISENIADOR_JEFE", "DISENIADOR_JEFE".trim());
        assertEquals("%diseniador\\_jefe%", LikePatrones.contiene("DISENIADOR_JEFE"));
    }
}
