package org.mgroko.backend.repositorio;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.mgroko.backend.admin.dto.AdminHabilidadRequest;
import org.mgroko.backend.admin.servicio.AdminHabilidadService;
import org.mgroko.backend.modelo.Habilidad;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.RolGlobal;
import org.mgroko.backend.modelo.Usuario;import org.mgroko.backend.perfiles.dto.CrearPerfilRequest;
import org.mgroko.backend.perfiles.dto.PerfilResponse;
import org.mgroko.backend.perfiles.servicio.CrearPerfilService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

@SpringBootTest
@Transactional
class AdminHabilidadIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private AdminHabilidadService adminHabilidadService;

    @Autowired
    private HabilidadRepository habilidadRepository;

    @Autowired
    private PerfilRepository perfilRepository;

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
    private EntityManager entityManager;

    @Test
    void eliminar_habilidadAsociadaAPerfil_limpiaAsociacionYBorraHabilidad() {
        Usuario usuario = usuarioRepository.saveAndFlush(Usuario.builder()
                .nombre("Habilidad")
                .apellido("Test")
                .dni("90990001")
                .fechaNacimiento(LocalDate.now().minusYears(25))
                .correo("habilidad.test@example.com")
                .rolGlobal(rolGlobalRepository.findByNombre("Usuario").orElseThrow())
                .genero(generoRepository.findByCodigo("mujer").orElseThrow())
                .build());

        Profesion profesion = profesionRepository.buscar("%modelo%", "modelo").get(0);
        PerfilResponse perfilResponse = crearPerfilService.crear(usuario.getIdUsuario(),
                new CrearPerfilRequest("Perfil Habilidad", profesion.getIdProfesion(),
                        "Perfil para probar la baja de habilidades.", List.of()));

        Long idHabilidad = adminHabilidadService.crear(
                new AdminHabilidadRequest("Habilidad de Baja", "Prueba de borrado.")).idHabilidad();

        Perfil perfil = perfilRepository.findById(perfilResponse.idPerfil()).orElseThrow();
        Habilidad habilidad = habilidadRepository.findById(idHabilidad).orElseThrow();
        perfil.getHabilidades().add(habilidad);
        perfilRepository.saveAndFlush(perfil);

        entityManager.flush();
        entityManager.clear();

        adminHabilidadService.eliminar(idHabilidad);

        entityManager.flush();
        entityManager.clear();

        assertTrue(habilidadRepository.findById(idHabilidad).isEmpty());

        Perfil perfilSinHabilidad = perfilRepository.findById(perfilResponse.idPerfil()).orElseThrow();
        assertFalse(perfilSinHabilidad.getHabilidades().stream()
                .anyMatch(h -> h.getIdHabilidad().equals(idHabilidad)));
    }
}
