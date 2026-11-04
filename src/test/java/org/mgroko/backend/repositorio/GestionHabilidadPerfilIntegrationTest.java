package org.mgroko.backend.repositorio;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.modelo.Habilidad;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.perfiles.dto.HabilidadResponse;
import org.mgroko.backend.perfiles.servicio.GestionHabilidadPerfilService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

@SpringBootTest
@Transactional
class GestionHabilidadPerfilIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private GestionHabilidadPerfilService gestionHabilidadPerfilService;

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
    private EntityManager entityManager;

    @Test
    void agregarYQuitarHabilidad_persisteYDesasociaEnBase() {
        String sufijo = String.valueOf(System.nanoTime());
        Usuario usuario = usuarioRepository.saveAndFlush(Usuario.builder()
                .nombre("Perfil")
                .apellido("Habilidad")
                .dni(sufijo.substring(sufijo.length() - 8))
                .fechaNacimiento(LocalDate.now().minusYears(25))
                .correo("perfil.habilidad." + sufijo + "@example.com")
                .rolGlobal(rolGlobalRepository.findByNombre("Usuario").orElseThrow())
                .genero(generoRepository.findByCodigo("mujer").orElseThrow())
                .build());

        Profesion profesion = profesionRepository.buscar("%modelo%", "modelo").get(0);
        Perfil perfil = perfilRepository.saveAndFlush(Perfil.builder()
                .nombreArtistico("Perfil Habilidad " + sufijo)
                .biografia("Perfil para probar gestión de habilidades.")
                .estado(EstadoPerfil.Activo)
                .usuario(usuario)
                .profesion(profesion)
                .build());

        Habilidad habilidad = habilidadRepository.saveAndFlush(Habilidad.builder()
                .codigo("HAB_PERFIL_" + sufijo)
                .nombre("Habilidad Perfil " + sufijo)
                .descripcion("Habilidad para probar el perfil.")
                .build());

        gestionHabilidadPerfilService.agregar(
                usuario.getIdUsuario(), perfil.getIdPerfil(), habilidad.getIdHabilidad());

        entityManager.flush();
        entityManager.clear();

        assertThat(gestionHabilidadPerfilService.listar(
                usuario.getIdUsuario(), perfil.getIdPerfil()))
                .extracting(HabilidadResponse::idHabilidad)
                .containsExactly(habilidad.getIdHabilidad());

        gestionHabilidadPerfilService.quitar(
                usuario.getIdUsuario(), perfil.getIdPerfil(), habilidad.getIdHabilidad());

        entityManager.flush();
        entityManager.clear();

        assertThat(gestionHabilidadPerfilService.listar(
                usuario.getIdUsuario(), perfil.getIdPerfil()))
                .isEmpty();
    }
}
