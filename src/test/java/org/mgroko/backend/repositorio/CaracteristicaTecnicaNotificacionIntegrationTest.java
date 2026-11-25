package org.mgroko.backend.repositorio;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mgroko.backend.admin.dto.AdminCaracteristicaTecnicaRequest;
import org.mgroko.backend.admin.servicio.AdminCaracteristicaTecnicaService;
import org.mgroko.backend.modelo.Genero;
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
class CaracteristicaTecnicaNotificacionIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private AdminCaracteristicaTecnicaService adminCaracteristicaTecnicaService;

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
    private CaracteristicaTecnicaRepository caracteristicaTecnicaRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;

    private final Set<Long> caracteristicasCreadas = new HashSet<>();

    @BeforeEach
    void setUp() {
        transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @AfterEach
    void limpiarCaracteristicasCreadas() {
        // La base es compartida por todos los tests de integración: las
        // características creadas acá deben eliminarse para no alterar los
        // conteos de otros tests (p. ej. CaracteristicaTecnicaRepositoryIntegrationTest).
        if (!caracteristicasCreadas.isEmpty()) {
            caracteristicaTecnicaRepository.deleteAllById(caracteristicasCreadas);
            caracteristicasCreadas.clear();
        }
    }

    @Test
    void alCrearCaracteristica_soloSeNotificaALaProfesionCorrespondiente() {
        Escenario escenario = crearEscenario();

        String codigo = "CAR_NOTIF_" + escenario.sufijo();
        var creada = adminCaracteristicaTecnicaService.crear(new AdminCaracteristicaTecnicaRequest(
                codigo,
                "Altura " + escenario.sufijo(),
                null,
                escenario.idProfesionFotografo(),
                "TEXTO",
                null));
        caracteristicasCreadas.add(creada.idCaracteristica());

        List<org.mgroko.backend.modelo.Notificacion> delFotografo = notificacionesDe(
                escenario.idUsuarioFotografo(), escenario.idPerfilFotografo());
        List<org.mgroko.backend.modelo.Notificacion> delModelo = notificacionesDe(
                escenario.idUsuarioModelo(), escenario.idPerfilModelo());

        assertThat(delFotografo)
                .filteredOn(n -> NotificacionService.TIPO_CARACTERISTICA_TECNICA_CREADA
                        .equals(n.getTipo()))
                .singleElement()
                .satisfies(n -> {
                    assertThat(n.getEntidadTipo())
                            .isEqualTo(NotificacionService.ENTIDAD_CARACTERISTICA_TECNICA);
                    assertThat(n.getPerfilContexto().getIdPerfil())
                            .isEqualTo(escenario.idPerfilFotografo());
                });

        assertThat(delModelo)
                .noneMatch(n -> NotificacionService.TIPO_CARACTERISTICA_TECNICA_CREADA
                        .equals(n.getTipo()));
    }

    @Test
    void alModificarDosVecesLaMismaCaracteristica_deduplicaNoLeidaPorPerfil() {
        Escenario escenario = crearEscenario();
        String codigo = "CAR_DEDUP_" + escenario.sufijo();

        var creada = adminCaracteristicaTecnicaService.crear(new AdminCaracteristicaTecnicaRequest(
                codigo,
                "Característica " + escenario.sufijo(),
                null,
                escenario.idProfesionFotografo(),
                "TEXTO",
                null));
        caracteristicasCreadas.add(creada.idCaracteristica());

        adminCaracteristicaTecnicaService.actualizar(creada.idCaracteristica(),
                new AdminCaracteristicaTecnicaRequest(
                        codigo,
                        "Primera modificación " + escenario.sufijo(),
                        null,
                        escenario.idProfesionFotografo(),
                        "TEXTO",
                        null));

        adminCaracteristicaTecnicaService.actualizar(creada.idCaracteristica(),
                new AdminCaracteristicaTecnicaRequest(
                        codigo,
                        "Segunda modificación " + escenario.sufijo(),
                        null,
                        escenario.idProfesionFotografo(),
                        "TEXTO",
                        null));

        List<org.mgroko.backend.modelo.Notificacion> modificadas = notificacionesDe(
                escenario.idUsuarioFotografo(), escenario.idPerfilFotografo()).stream()
                .filter(n -> NotificacionService.TIPO_CARACTERISTICA_TECNICA_MODIFICADA
                        .equals(n.getTipo()))
                .filter(n -> creada.idCaracteristica().equals(n.getEntidadId()))
                .filter(n -> Boolean.FALSE.equals(n.getLeida()))
                .toList();

        assertThat(modificadas)
                .singleElement()
                .satisfies(n -> assertThat(n.getMensaje())
                        .contains("Segunda modificación"));
    }

    private List<org.mgroko.backend.modelo.Notificacion> notificacionesDe(
            Long idUsuario, Long idPerfil) {
        return notificacionRepository
                .findByUsuarioDestino_IdUsuarioAndPerfilContexto_IdPerfil(
                        idUsuario, idPerfil, PageRequest.of(0, 100))
                .getContent();
    }

    private Escenario crearEscenario() {
        return transactionTemplate.execute(status -> {
            String sufijo = String.valueOf(System.nanoTime());
            String base = sufijo.substring(sufijo.length() - 7);

            RolGlobal rol = rolGlobalRepository.findByNombre("Usuario").orElseThrow();
            Genero genero = generoRepository.findByCodigo("mujer").orElseThrow();
            Profesion fotografo = profesionRepository.findById(1L).orElseThrow();
            Profesion modelo = profesionRepository.findById(2L).orElseThrow();

            Usuario usuarioFotografo = usuarioRepository.saveAndFlush(Usuario.builder()
                    .nombre("Foto")
                    .apellido("Notif")
                    .dni("9" + base)
                    .fechaNacimiento(LocalDate.now().minusYears(25))
                    .correo("carac.foto." + sufijo + "@example.com")
                    .rolGlobal(rol)
                    .genero(genero)
                    .build());

            Usuario usuarioModelo = usuarioRepository.saveAndFlush(Usuario.builder()
                    .nombre("Modelo")
                    .apellido("Notif")
                    .dni("8" + base)
                    .fechaNacimiento(LocalDate.now().minusYears(25))
                    .correo("carac.modelo." + sufijo + "@example.com")
                    .rolGlobal(rol)
                    .genero(genero)
                    .build());

            Perfil perfilFotografo = perfilRepository.saveAndFlush(Perfil.builder()
                    .nombreArtistico("Foto Notif " + sufijo)
                    .biografia("Perfil para probar notificaciones de características.")
                    .estado(EstadoPerfil.Activo)
                    .usuario(usuarioFotografo)
                    .profesion(fotografo)
                    .build());

            Perfil perfilModelo = perfilRepository.saveAndFlush(Perfil.builder()
                    .nombreArtistico("Modelo Notif " + sufijo)
                    .biografia("Perfil para probar notificaciones de características.")
                    .estado(EstadoPerfil.Activo)
                    .usuario(usuarioModelo)
                    .profesion(modelo)
                    .build());

            return new Escenario(
                    sufijo,
                    fotografo.getIdProfesion(),
                    usuarioFotografo.getIdUsuario(),
                    perfilFotografo.getIdPerfil(),
                    usuarioModelo.getIdUsuario(),
                    perfilModelo.getIdPerfil());
        });
    }

    private record Escenario(
            String sufijo,
            Long idProfesionFotografo,
            Long idUsuarioFotografo,
            Long idPerfilFotografo,
            Long idUsuarioModelo,
            Long idPerfilModelo) {
    }
}
