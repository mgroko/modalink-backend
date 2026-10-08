package org.mgroko.backend.repositorio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.mgroko.backend.modelo.Genero;
import org.mgroko.backend.modelo.RolGlobal;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.Privacidad;
import org.mgroko.backend.perfiles.dto.CrearPerfilRequest;
import org.mgroko.backend.perfiles.dto.PerfilResponse;
import org.mgroko.backend.perfiles.servicio.CrearPerfilService;
import org.mgroko.backend.proyectos.dto.CrearProyectoRequest;
import org.mgroko.backend.proyectos.dto.CrearRequerimientoCaractRequest;
import org.mgroko.backend.proyectos.dto.CrearRequerimientoGralRequest;
import org.mgroko.backend.proyectos.dto.ProyectoResponse;
import org.mgroko.backend.proyectos.exception.RequerimientoInvalidoException;
import org.mgroko.backend.proyectos.servicio.CrearProyectoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * Test de integración del UC-24 completo contra PostgreSQL real: crea un
 * proyecto con requerimientos de personal (NUMERICO con rango, ENUMERADO con
 * valores) y verifica las filas persistidas, incluida la FK compuesta de
 * requerimiento_gral_caract_valor.
 */
@SpringBootTest
@Transactional
class CrearProyectoRequerimientosIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private GeneroRepository generoRepository;

    @Autowired
    private RolGlobalRepository rolGlobalRepository;

    @Autowired
    private PerfilRepository perfilRepository;

    @Autowired
    private ProfesionRepository profesionRepository;

    @Autowired
    private CaracteristicaTecnicaRepository caracteristicaTecnicaRepository;

    @Autowired
    private ValorCaracteristicaRepository valorCaracteristicaRepository;

    @Autowired
    private CrearPerfilService crearPerfilService;

    @Autowired
    private CrearProyectoService crearProyectoService;

    @Autowired
    private RequerimientoGralProyectoRepository requerimientoGralProyectoRepository;

    @Autowired
    private RequerimientoGralCaractValorRepository requerimientoGralCaractValorRepository;

    @Autowired
    private MoodboardRepository moodboardRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long crearPerfilModelo(String correo, String dni) {
        RolGlobal rol = rolGlobalRepository.findByNombre("Usuario").orElseThrow();
        Genero genero = generoRepository.findByCodigo("mujer").orElseThrow();
        Usuario usuario = usuarioRepository.saveAndFlush(Usuario.builder()
                .nombre("Maria")
                .apellido("Flores")
                .dni(dni)
                .fechaNacimiento(LocalDate.now().minusYears(25))
                .correo(correo)
                .rolGlobal(rol)
                .genero(genero)
                .build());

        Long idProfesionModelo = profesionRepository.buscar("%modelo%", "modelo").get(0).getIdProfesion();
        PerfilResponse perfil = crearPerfilService.crear(usuario.getIdUsuario(),
                new CrearPerfilRequest("Luna", idProfesionModelo, "Modelo profesional.", List.of()));
        this.idUsuarioActual = usuario.getIdUsuario();
        return perfil.idPerfil();
    }

    private Long idUsuarioActual;

    private void aceptarTyPerfil(Long idPerfil) {
        jdbcTemplate.update(
                "INSERT INTO perfil_tyc (id_perfil, id_tyc, descripcion, fecha_inicio) VALUES (?, 1, 'TyC UC-24', NOW())",
                idPerfil);
    }

    private Long idCaracteristica(String codigo) {
        return caracteristicaTecnicaRepository.findAllByOrderByCodigo().stream()
                .filter(c -> c.getCodigo().equals(codigo))
                .findFirst().orElseThrow().getIdCaracteristica();
    }

    @Test
    void crear_proyectoConRequerimientosNUMERICOyENUMERADO_persisteFilasReales() {
        Long idPerfil = crearPerfilModelo("req.gral1@example.com", "66660001");
        aceptarTyPerfil(idPerfil);

        Long idProfesionModelo = profesionRepository.buscar("%modelo%", "modelo").get(0).getIdProfesion();

        Long altura = idCaracteristica("ALTURA");
        Long colorOjos = idCaracteristica("COLOR_OJOS");
        Long valorOjosVerde = valorCaracteristicaRepository
                .findByCaracteristicaTecnica_IdCaracteristicaOrderByEtiqueta(colorOjos).stream()
                .filter(v -> v.getEtiqueta().equalsIgnoreCase("Verde"))
                .findFirst().orElseThrow().getIdValor();

        CrearRequerimientoGralRequest requerimiento = new CrearRequerimientoGralRequest(
                2, idProfesionModelo, "Dos modelos para pasarela",
                List.of(
                        new CrearRequerimientoCaractRequest(altura, BigDecimal.valueOf(160), BigDecimal.valueOf(180), null),
                        new CrearRequerimientoCaractRequest(colorOjos, null, null, List.of(valorOjosVerde))),
                List.of(1L, 4L));

        CrearProyectoRequest request = new CrearProyectoRequest(
                "Desfile Int", "Producción completa", Privacidad.PUBLICO,
                LocalDate.now().plusDays(10), LocalDate.now().plusDays(30), false,
                null, null,
                List.of(requerimiento),
                null);

        ProyectoResponse response = crearProyectoService.crear(idUsuarioActual, idPerfil, request);

        assertNotNull(response.idProyecto());
        assertEquals(1, response.requerimientosGral().size());

        List<org.mgroko.backend.modelo.RequerimientoGralProyecto> guardados =
                requerimientoGralProyectoRepository.findByProyectoIdProyecto(response.idProyecto());
        assertEquals(1, guardados.size());
        org.mgroko.backend.modelo.RequerimientoGralProyecto rg = guardados.get(0);
        assertEquals(2, rg.getCantidad());
        assertEquals(idProfesionModelo, rg.getProfesion().getIdProfesion());
        assertEquals(2, rg.getCaracteristicas().size());
        assertEquals(2, rg.getHabilidades().size());

        Long caractId = rg.getCaracteristicas().stream()
                .filter(c -> c.getCaracteristica().getIdCaracteristica().equals(colorOjos))
                .findFirst().orElseThrow().getIdReqGralCaract();

        boolean valorPersistido = requerimientoGralCaractValorRepository.findAll().stream()
                .anyMatch(v -> v.getIdValor().equals(valorOjosVerde)
                        && v.getIdCaracteristica().equals(colorOjos)
                        && v.getIdReqGralCaract().equals(caractId));
        assertTrue(valorPersistido, "El valor ENUMERADO debe persistirse con la FK compuesta correcta");

        Long habilidades = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM requerimiento_gral_habilidad WHERE id_requerimiento_gral = ?",
                Long.class, rg.getIdRequerimientoGral());
        assertEquals(2L, habilidades);
    }

    @Test
    void crear_requerimientoConCaracteristicaDuplicada_lanzaRequerimientoInvalido_noDataIntegrity() {
        Long idPerfil = crearPerfilModelo("req.gral2@example.com", "66660002");
        aceptarTyPerfil(idPerfil);

        Long idProfesionModelo = profesionRepository.buscar("%modelo%", "modelo").get(0).getIdProfesion();
        Long altura = idCaracteristica("ALTURA");

        CrearRequerimientoGralRequest requerimiento = new CrearRequerimientoGralRequest(
                1, idProfesionModelo, null,
                List.of(
                        new CrearRequerimientoCaractRequest(altura, BigDecimal.valueOf(160), BigDecimal.valueOf(180), null),
                        new CrearRequerimientoCaractRequest(altura, BigDecimal.valueOf(150), BigDecimal.valueOf(170), null)),
                null);

        CrearProyectoRequest request = new CrearProyectoRequest(
                "Desfile Dup", "Duplicada", Privacidad.PUBLICO,
                LocalDate.now().plusDays(5), null, false,
                null, null,
                List.of(requerimiento),
                null);

        assertThrows(RequerimientoInvalidoException.class,
                () -> crearProyectoService.crear(idUsuarioActual, idPerfil, request));
    }
}
