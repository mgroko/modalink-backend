package org.mgroko.backend.repositorio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mgroko.backend.admin.servicio.ConfiguracionSistemaService;
import org.mgroko.backend.modelo.Genero;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.PerfilTyc;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.RolGlobal;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.perfiles.dto.ActualizarTerminosCondicionesRequest;
import org.mgroko.backend.perfiles.dto.TerminosCondicionesResponse;
import org.mgroko.backend.perfiles.servicio.GestionTerminosCondicionesService;
import org.mgroko.backend.perfiles.servicio.VerPerfilService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC-23 contra PostgreSQL real: versión vigente única, cierre de versiones y
 * plantilla base.
 */
@SpringBootTest
@Transactional
class GestionTerminosCondicionesIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final AtomicInteger SECUENCIA = new AtomicInteger();

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PerfilRepository perfilRepository;

    @Autowired
    private ProfesionRepository profesionRepository;

    @Autowired
    private RolGlobalRepository rolGlobalRepository;

    @Autowired
    private GeneroRepository generoRepository;

    @Autowired
    private PerfilTycRepository perfilTycRepository;

    @Autowired
    private GestionTerminosCondicionesService service;

    @Autowired
    private ConfiguracionSistemaService configuracionSistemaService;

    @Autowired
    private VerPerfilService verPerfilService;

    private Perfil perfil;

    @BeforeEach
    void setUp() {
        int n = SECUENCIA.incrementAndGet();
        RolGlobal rol = rolGlobalRepository.findByNombre("Usuario").orElseThrow();
        Genero genero = generoRepository.findByCodigo("mujer").orElseThrow();

        Usuario usuario = usuarioRepository.saveAndFlush(Usuario.builder()
                .nombre("Tyc").apellido("Integracion")
                .dni("9" + String.format("%07d", n))
                .fechaNacimiento(LocalDate.now().minusYears(25))
                .correo("tyc" + n + "@example.com")
                .estado(EstadoUsuario.Activo)
                .rolGlobal(rol)
                .genero(genero)
                .build());

        Profesion profesion = profesionRepository.saveAndFlush(Profesion.builder()
                .codigo("PROF_TYC_" + n)
                .nombre("Profesion TyC " + n)
                .build());

        perfil = perfilRepository.saveAndFlush(Perfil.builder()
                .nombreArtistico("Perfil TyC " + n)
                .biografia("Perfil de prueba TyC")
                .estado(EstadoPerfil.Activo)
                .usuario(usuario)
                .profesion(profesion)
                .build());
    }

    @Test
    void guardaPrimeraVersionConIdGenerado() {
        TerminosCondicionesResponse response = service.actualizar(
                perfil.getUsuario().getIdUsuario(),
                perfil.getIdPerfil(),
                new ActualizarTerminosCondicionesRequest("Texto inicial"));

        assertNotNull(response.idTyc());
        assertFalse(response.esPlantilla());
        assertEquals("Texto inicial", response.descripcion());

        PerfilTyc vigente = perfilTycRepository
                .findFirstByPerfilIdPerfilAndFechaFinIsNullOrderByFechaInicioDesc(
                        perfil.getIdPerfil())
                .orElseThrow();
        assertEquals(response.idTyc(), vigente.getIdTyc());
        assertNull(vigente.getFechaFin());
    }

    @Test
    void cambioCierraVersionAnteriorYCreaNueva() {
        TerminosCondicionesResponse primera = service.actualizar(
                perfil.getUsuario().getIdUsuario(),
                perfil.getIdPerfil(),
                new ActualizarTerminosCondicionesRequest("Texto inicial"));

        TerminosCondicionesResponse segunda = service.actualizar(
                perfil.getUsuario().getIdUsuario(),
                perfil.getIdPerfil(),
                new ActualizarTerminosCondicionesRequest("Texto actualizado"));

        assertNotNull(segunda.idTyc());
        assertNotNull(segunda.idTyc());
        assertFalse(primera.idTyc().equals(segunda.idTyc()));

        List<PerfilTyc> versiones = perfilTycRepository
                .findByPerfilIdPerfilOrderByFechaInicioDesc(perfil.getIdPerfil());
        assertEquals(2, versiones.size());

        PerfilTyc vigente = versiones.stream()
                .filter(v -> v.getFechaFin() == null)
                .findFirst()
                .orElseThrow();
        assertEquals(segunda.idTyc(), vigente.getIdTyc());
        assertEquals("Texto actualizado", vigente.getDescripcion());

        PerfilTyc anterior = versiones.stream()
                .filter(v -> v.getFechaFin() != null)
                .findFirst()
                .orElseThrow();
        assertEquals(primera.idTyc(), anterior.getIdTyc());
        assertTrue(anterior.getFechaFin().isAfter(anterior.getFechaInicio()));
    }

    @Test
    void mismoTextoNoDuplicaVersion() {
        service.actualizar(
                perfil.getUsuario().getIdUsuario(),
                perfil.getIdPerfil(),
                new ActualizarTerminosCondicionesRequest("Texto estable"));

        service.actualizar(
                perfil.getUsuario().getIdUsuario(),
                perfil.getIdPerfil(),
                new ActualizarTerminosCondicionesRequest("  Texto estable  "));

        List<PerfilTyc> versiones = perfilTycRepository
                .findByPerfilIdPerfilOrderByFechaInicioDesc(perfil.getIdPerfil());
        assertEquals(1, versiones.size());
    }

    @Test
    void detalleDePerfilIncluyeLaVersionVigente() {
        service.actualizar(
                perfil.getUsuario().getIdUsuario(),
                perfil.getIdPerfil(),
                new ActualizarTerminosCondicionesRequest("Texto visible en el perfil"));

        var detalle = verPerfilService.obtenerDetalle(
                perfil.getIdPerfil(), perfil.getUsuario().getIdUsuario());

        assertNotNull(detalle.terminosCondiciones());
        assertEquals("Texto visible en el perfil",
                detalle.terminosCondiciones().descripcion());
        assertFalse(detalle.terminosCondiciones().esPlantilla());
    }

    @Test
    void sinVersionDevuelvePlantillaBase() {
        TerminosCondicionesResponse response = service.obtener(
                perfil.getUsuario().getIdUsuario(), perfil.getIdPerfil());

        assertTrue(response.esPlantilla());
        assertNull(response.idTyc());
        assertTrue(response.descripcion().startsWith("Mis condiciones de trabajo"));
    }

    @Test
    void plantillaBaseEstaSeedeada() {
        String plantilla = configuracionSistemaService.obtenerPlantillaTerminosCondiciones();

        assertTrue(plantilla.startsWith("Mis condiciones de trabajo"));
        assertTrue(plantilla.length() <= ConfiguracionSistemaService.PERFIL_TYC_MAX_CARACTERES);
    }
}
