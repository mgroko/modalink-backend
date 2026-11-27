package org.mgroko.backend.repositorio;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.modelo.Genero;
import org.mgroko.backend.modelo.RolGlobal;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.modelo.enums.Privacidad;
import org.mgroko.backend.perfiles.dto.CrearPerfilRequest;
import org.mgroko.backend.perfiles.dto.PerfilResponse;
import org.mgroko.backend.perfiles.servicio.CrearPerfilService;
import org.mgroko.backend.proyectos.dto.CrearMoodboardRequest;
import org.mgroko.backend.proyectos.dto.CrearProyectoRequest;
import org.mgroko.backend.proyectos.dto.CrearRequerimientoGralRequest;
import org.mgroko.backend.proyectos.dto.InformeProyectoResponse;
import org.mgroko.backend.proyectos.dto.InformeProyectoResponse.AreaInforme;
import org.mgroko.backend.proyectos.pdf.InformeProyectoPdfRenderer;
import org.mgroko.backend.proyectos.servicio.GenerarInformeProyectoService;
import org.mgroko.backend.ubicacion.dto.UbicacionRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integración del informe ejecutivo de proyecto contra PostgreSQL real.
 */
@SpringBootTest
@Transactional
class InformeProyectoIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolGlobalRepository rolGlobalRepository;

    @Autowired
    private GeneroRepository generoRepository;

    @Autowired
    private ProfesionRepository profesionRepository;

    @Autowired
    private CrearPerfilService crearPerfilService;

    @Autowired
    private org.mgroko.backend.proyectos.servicio.CrearProyectoService crearProyectoService;

    @Autowired
    private GenerarInformeProyectoService generarInformeProyectoService;

    @Autowired
    private InformeProyectoPdfRenderer informeProyectoPdfRenderer;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void generaInformeParaProyectoRealYExportaPdf() {
        DirectorCreado director = crearPerfil(
                "informe26.dir@example.com",
                "77826001",
                "Director Informe");

        var proyecto = crearProyectoService.crear(
                director.idUsuario(),
                director.idPerfil(),
                proyectoCompleto("Proyecto Informe"));

        InformeProyectoResponse informe = generarInformeProyectoService.generar(
                director.idUsuario(),
                director.idPerfil(),
                proyecto.idProyecto());

        assertThat(informe.proyecto().nombre()).isEqualTo("Proyecto Informe");
        assertThat(informe.modo()).isEqualTo("BORRADOR");
        assertThat(informe.resumen().areas())
                .extracting(AreaInforme::codigo)
                .containsExactly("CRONOGRAMA", "EQUIPO", "CONTRATOS", "BRIEF");
        assertThat(informe.resumen().areas())
                .filteredOn(area -> "CRONOGRAMA".equals(area.codigo()))
                .singleElement()
                .extracting(AreaInforme::estado)
                .isEqualTo(org.mgroko.backend.proyectos.dto.EstadoAreaInforme.NO_APLICA);
        assertThat(informe.metricas().completitudBrief()).isNotNull();
        assertThat(informe.generadoPor().nombreArtistico()).isEqualTo("Director Informe");

        ByteArrayOutputStream pdf = new ByteArrayOutputStream();
        informeProyectoPdfRenderer.render(informe, pdf);

        assertThat(pdf.toByteArray())
                .startsWith("%PDF".getBytes(StandardCharsets.US_ASCII));
        assertThat(pdf.size()).isGreaterThan(2_000);
    }

    private DirectorCreado crearPerfil(String correo, String dni, String nombreArtistico) {
        RolGlobal rol = rolGlobalRepository.findByNombre("Usuario").orElseThrow();
        Genero genero = generoRepository.findByCodigo("mujer").orElseThrow();

        Usuario usuario = usuarioRepository.saveAndFlush(Usuario.builder()
                .nombre("Informe")
                .apellido("Test")
                .dni(dni)
                .fechaNacimiento(LocalDate.now().minusYears(26))
                .correo(correo)
                .estado(EstadoUsuario.Activo)
                .rolGlobal(rol)
                .genero(genero)
                .build());

        PerfilResponse perfil = crearPerfilService.crear(
                usuario.getIdUsuario(),
                new CrearPerfilRequest(
                        nombreArtistico,
                        idProfesionModelo(),
                        "Perfil de prueba del informe.",
                        List.of()));

        jdbcTemplate.update(
                "INSERT INTO perfil_tyc (id_perfil, descripcion, fecha_inicio, fecha_fin) "
                        + "VALUES (?, 'TyC informe', NOW() - INTERVAL '1 day', NOW())",
                perfil.idPerfil());

        return new DirectorCreado(usuario.getIdUsuario(), perfil.idPerfil());
    }

    private Long idProfesionModelo() {
        return profesionRepository.buscar("%modelo%", "modelo").get(0).getIdProfesion();
    }

    private CrearProyectoRequest proyectoCompleto(String nombre) {
        return new CrearProyectoRequest(
                nombre,
                "Proyecto para probar el informe ejecutivo.",
                Privacidad.PUBLICO,
                LocalDate.now().plusDays(30),
                true,
                new UbicacionRequest("0602801002"),
                null,
                List.of(new CrearRequerimientoGralRequest(
                        2,
                        idProfesionModelo(),
                        "Dos modelos",
                        null,
                        null)),
                new CrearMoodboardRequest("Telas frías, tonos neutros"));
    }

    private record DirectorCreado(Long idUsuario, Long idPerfil) {
    }
}
