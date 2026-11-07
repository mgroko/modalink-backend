package org.mgroko.backend.contratos.servicio;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.TipoAcuerdoContrato;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.Privacidad;

class GeneradorTextoContratoServiceTest {

    private final GeneradorTextoContratoService service =
            new GeneradorTextoContratoService();

    @Test
    void generar_reemplazaPlaceholdersYAnexaClausulas() {
        ContratoContextoService.ContextoContrato contexto = contexto(
                "CONTRATO DE {{tipo}}\n\nProyecto: {{proyecto}}\n"
                        + "Director: {{director}}\nPartes:\n{{partes}}\n\n"
                        + "{{clausulas_adicionales}}");

        String texto = service.generar(contexto, List.of("Primera clausula", "Segunda clausula"));

        assertThat(texto)
                .contains("CONTRATO DE Cesión de derechos de imagen")
                .contains("Proyecto: Editorial Primavera — Producción editorial")
                .contains("Director: Ana Pérez (DNI 30111222) — @Ana")
                .contains("Beto Gómez (DNI 33111222) — @Beto (Modelo)")
                .contains("1. Primera clausula")
                .contains("2. Segunda clausula");
    }

    @Test
    void generar_conservaLiteralmenteLosPlaceholdersDentroDeLasClausulas() {
        ContratoContextoService.ContextoContrato contexto = contexto(
                "Proyecto: {{proyecto}}\n{{clausulas_adicionales}}");

        String texto = service.generar(
                contexto,
                List.of("La parte mencionada en {{proyecto}} conserva sus derechos."));

        assertThat(texto).contains("La parte mencionada en {{proyecto}} conserva sus derechos.");
    }

    @Test
    void generar_sinPlantillaUsaPlantillaGenerica() {
        ContratoContextoService.ContextoContrato contexto = contexto(null);

        String texto = service.generar(contexto, List.of());

        assertThat(texto)
                .contains("CONTRATO DE Cesión de derechos de imagen")
                .contains("Sin cláusulas adicionales.");
    }

    @Test
    void generarNombre_truncaA50Caracteres() {
        ContratoContextoService.ContextoContrato contexto = contexto("{{proyecto}}");

        String nombre = service.generarNombre(contexto);

        assertThat(nombre.codePointCount(0, nombre.length())).isLessThanOrEqualTo(50);
    }

    @Test
    void generarDescripcion_truncaA200Caracteres() {
        ContratoContextoService.ContextoContrato contexto = contexto("{{proyecto}}");

        String descripcion = service.generarDescripcion(contexto);

        assertThat(descripcion.codePointCount(0, descripcion.length())).isLessThanOrEqualTo(200);
    }

    private ContratoContextoService.ContextoContrato contexto(String plantilla) {
        Profesion profesion = Profesion.builder()
                .nombre("Modelo")
                .build();

        Usuario usuarioDirector = Usuario.builder()
                .nombre("Ana")
                .apellido("Pérez")
                .dni("30111222")
                .build();
        Usuario usuarioParte = Usuario.builder()
                .nombre("Beto")
                .apellido("Gómez")
                .dni("33111222")
                .build();

        Perfil perfilDirector = Perfil.builder()
                .nombreArtistico("Ana")
                .usuario(usuarioDirector)
                .profesion(profesion)
                .build();
        Perfil perfilParte = Perfil.builder()
                .nombreArtistico("Beto")
                .usuario(usuarioParte)
                .profesion(profesion)
                .build();

        Proyecto proyecto = Proyecto.builder()
                .nombre("Editorial Primavera")
                .descripcion("Producción editorial")
                .estado(EstadoProyecto.PUBLICADO)
                .privacidad(Privacidad.PUBLICO)
                .build();

        TipoAcuerdoContrato tipo = TipoAcuerdoContrato.builder()
                .nombre("Cesión de derechos de imagen")
                .plantilla(plantilla)
                .build();

        return new ContratoContextoService.ContextoContrato(
                perfilDirector,
                proyecto,
                tipo,
                MiembroProyecto.builder().perfil(perfilDirector).build(),
                List.of(MiembroProyecto.builder().perfil(perfilParte).build()),
                List.of());
    }
}
