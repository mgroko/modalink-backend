package org.mgroko.backend.repositorio;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mgroko.backend.admin.dto.AdminHabilidadRequest;
import org.mgroko.backend.admin.servicio.AdminHabilidadService;
import org.mgroko.backend.modelo.Genero;
import org.mgroko.backend.modelo.Habilidad;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.RolGlobal;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.notificaciones.servicio.NotificacionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
class HabilidadNotificacionIntegrationTest extends AbstractPostgresIntegrationTest {

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
    private NotificacionRepository notificacionRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Test
    void alModificarHabilidad_seNotificaCadaPerfilAsociadoDespuesDelCommit() {
        Escenario escenario = crearEscenario();

        adminHabilidadService.actualizar(escenario.idHabilidad(),
                new AdminHabilidadRequest(escenario.nombre() + " modificada",
                        "Descripción actualizada."));

        var notificaciones = notificacionRepository
                .findByUsuarioDestino_IdUsuarioAndPerfilContexto_IdPerfil(
                        escenario.idUsuario(), escenario.idPerfil(),
                        PageRequest.of(0, 20));

        assertThat(notificaciones.getContent())
                .filteredOn(n -> NotificacionService.TIPO_HABILIDAD_MODIFICADA
                        .equals(n.getTipo()))
                .singleElement()
                .satisfies(n -> {
                    assertThat(n.getEntidadTipo()).isEqualTo("HABILIDAD");
                    assertThat(n.getEntidadId()).isEqualTo(escenario.idHabilidad());
                    assertThat(n.getPerfilContexto().getIdPerfil())
                            .isEqualTo(escenario.idPerfil());
                    assertThat(n.getMensaje()).contains("modificada");
                });
    }

    @Test
    void alEliminarHabilidad_seNotificaCadaPerfilAsociadoDespuesDelCommit() {
        Escenario escenario = crearEscenario();

        adminHabilidadService.eliminar(escenario.idHabilidad());

        var notificaciones = notificacionRepository
                .findByUsuarioDestino_IdUsuarioAndPerfilContexto_IdPerfil(
                        escenario.idUsuario(), escenario.idPerfil(),
                        PageRequest.of(0, 20));

        assertThat(notificaciones.getContent())
                .filteredOn(n -> NotificacionService.TIPO_HABILIDAD_ELIMINADA
                        .equals(n.getTipo()))
                .singleElement()
                .satisfies(n -> {
                    assertThat(n.getEntidadTipo()).isEqualTo("HABILIDAD");
                    assertThat(n.getEntidadId()).isEqualTo(escenario.idHabilidad());
                    assertThat(n.getPerfilContexto().getIdPerfil())
                            .isEqualTo(escenario.idPerfil());
                    assertThat(n.getMensaje()).contains("dada de baja");
                });
    }

    private Escenario crearEscenario() {
        return transactionTemplate.execute(status -> {
            String sufijo = String.valueOf(System.nanoTime());
            RolGlobal rol = rolGlobalRepository.findByNombre("Usuario").orElseThrow();
            Genero genero = generoRepository.findByCodigo("mujer").orElseThrow();

            Usuario usuario = usuarioRepository.saveAndFlush(Usuario.builder()
                    .nombre("Habilidad")
                    .apellido("Notif")
                    .dni(sufijo.substring(sufijo.length() - 8))
                    .fechaNacimiento(LocalDate.now().minusYears(25))
                    .correo("habilidad.notif." + sufijo + "@example.com")
                    .rolGlobal(rol)
                    .genero(genero)
                    .build());

            Profesion profesion = profesionRepository.buscar("%modelo%", "modelo").get(0);

            Perfil perfil = perfilRepository.saveAndFlush(Perfil.builder()
                    .nombreArtistico("Perfil Notif " + sufijo)
                    .biografia("Perfil para probar notificaciones de habilidades.")
                    .estado(EstadoPerfil.Activo)
                    .usuario(usuario)
                    .profesion(profesion)
                    .build());

            Habilidad habilidad = habilidadRepository.saveAndFlush(Habilidad.builder()
                    .codigo("HAB_NOTIF_" + sufijo)
                    .nombre("Habilidad Notif " + sufijo)
                    .descripcion("Habilidad para probar notificaciones.")
                    .build());

            perfil.getHabilidades().add(habilidad);
            perfilRepository.saveAndFlush(perfil);

            return new Escenario(usuario.getIdUsuario(), perfil.getIdPerfil(),
                    habilidad.getIdHabilidad(), habilidad.getNombre());
        });
    }

    private record Escenario(Long idUsuario, Long idPerfil, Long idHabilidad, String nombre) {
    }
}
