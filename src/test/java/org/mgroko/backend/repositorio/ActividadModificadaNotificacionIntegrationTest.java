package org.mgroko.backend.repositorio;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mgroko.backend.actividades.dto.ActividadResponse;
import org.mgroko.backend.actividades.dto.ModificarActividadRequest;
import org.mgroko.backend.actividades.enums.UnidadDuracion;
import org.mgroko.backend.actividades.servicio.ModificarActividadService;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.AsignacionActividad;
import org.mgroko.backend.modelo.Ciudad;
import org.mgroko.backend.modelo.Genero;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Pais;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.PerfilTyc;
import org.mgroko.backend.modelo.Planificacion;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.Provincia;
import org.mgroko.backend.modelo.RequerimientoActividad;
import org.mgroko.backend.modelo.RolGlobal;
import org.mgroko.backend.modelo.RolProyecto;
import org.mgroko.backend.modelo.Ubicacion;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoActividad;
import org.mgroko.backend.modelo.enums.EstadoParticipacion;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.Privacidad;
import org.mgroko.backend.notificaciones.servicio.NotificacionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@SpringBootTest
class ActividadModificadaNotificacionIntegrationTest extends AbstractPostgresIntegrationTest {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private ModificarActividadService modificarActividadService;

    @Autowired
    private NotificacionRepository notificacionRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolGlobalRepository rolGlobalRepository;

    @Autowired
    private GeneroRepository generoRepository;

    @Autowired
    private PaisRepository paisRepository;

    @Autowired
    private ProvinciaRepository provinciaRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Test
    void alConfirmarLaModificacion_seRegistraLaNotificacionDespuesDelCommit() {
        Escenario escenario = transactionTemplate.execute(status -> crearEscenario());

        ModificarActividadRequest request = new ModificarActividadRequest(
                "Montaje final",
                "Descripcion actualizada",
                1,
                UnidadDuracion.HORAS,
                null,
                List.of(),
                List.of(),
                EstadoActividad.EN_CURSO);

        ActividadResponse response = modificarActividadService.modificar(
                escenario.idUsuario(),
                escenario.idPerfil(),
                escenario.idProyecto(),
                escenario.idActividad(),
                request);

        assertThat(response.estado()).isEqualTo("EN_CURSO");

        var notificaciones = notificacionRepository
                .findByUsuarioDestino_IdUsuarioAndPerfilContexto_IdPerfil(
                        escenario.idUsuario(),
                        escenario.idPerfil(),
                        PageRequest.of(0, 10));

        assertThat(notificaciones.getContent())
                .filteredOn(notificacion -> NotificacionService.TIPO_ACTIVIDAD_MODIFICADA
                        .equals(notificacion.getTipo()))
                .singleElement()
                .satisfies(notificacion -> {
                    assertThat(notificacion.getEntidadTipo()).isEqualTo("ACTIVIDAD");
                    assertThat(notificacion.getEntidadId()).isEqualTo(escenario.idActividad());
                    assertThat(notificacion.getIdProyecto()).isEqualTo(escenario.idProyecto());
                });
    }

    private Escenario crearEscenario() {
        String sufijo = String.valueOf(System.nanoTime());
        RolGlobal rol = rolGlobalRepository.findByNombre("Usuario").orElseThrow();
        Genero genero = generoRepository.findByCodigo("mujer").orElseThrow();

        Usuario usuario = usuarioRepository.saveAndFlush(Usuario.builder()
                .nombre("Ana")
                .apellido("Gomez")
                .dni(sufijo.substring(sufijo.length() - 8))
                .fechaNacimiento(LocalDate.now().minusYears(22))
                .correo("uc50.notif." + sufijo + "@example.com")
                .rolGlobal(rol)
                .genero(genero)
                .build());

        Profesion profesion = em.find(Profesion.class, 2L);

        Perfil perfil = Perfil.builder()
                .nombreArtistico("Luna UC50 Notif")
                .biografia("Perfil de test UC50 notificacion")
                .estado(EstadoPerfil.Activo)
                .usuario(usuario)
                .profesion(profesion)
                .build();
        em.persist(perfil);

        em.persist(PerfilTyc.builder()
                .id(new org.mgroko.backend.modelo.PerfilTycId(perfil.getIdPerfil(), 1L))
                .perfil(perfil)
                .descripcion("T&C Base")
                .fechaInicio(LocalDateTime.now())
                .build());

        Pais pais = paisRepository.findByCodigoIso("AR").orElseGet(() -> {
            Pais nuevo = Pais.builder().codigoIso("AR").nombre("Argentina").activo(true).build();
            em.persist(nuevo);
            return nuevo;
        });

        Provincia provincia = provinciaRepository
                .findByNombreAndPais_IdPais("Buenos Aires", pais.getIdPais())
                .orElseGet(() -> {
                    Provincia nueva = Provincia.builder()
                            .nombre("Buenos Aires")
                            .activo(true)
                            .pais(pais)
                            .idExterno("uc50notif")
                            .fuenteApi("uc50")
                            .build();
                    em.persist(nueva);
                    return nueva;
                });

        Ciudad ciudad = Ciudad.builder()
                .nombre("CABA UC50 Notif")
                .activo(true)
                .provincia(provincia)
                .idExterno("uc50notif")
                .fuenteApi("uc50")
                .build();
        em.persist(ciudad);

        Ubicacion ubicacion = Ubicacion.builder().ciudad(ciudad).build();
        em.persist(ubicacion);

        Proyecto proyecto = Proyecto.builder()
                .nombre("Proyecto UC50 Notif " + sufijo)
                .descripcion("Proyecto de test UC50 notificacion")
                .fechaInicio(LocalDate.now())
                .estado(EstadoProyecto.BORRADOR)
                .privacidad(Privacidad.PRIVADO)
                .build();
        em.persist(proyecto);

        Planificacion planificacion = Planificacion.builder()
                .proyecto(proyecto)
                .fechaEntrega(LocalDate.now().plusDays(30))
                .build();
        em.persist(planificacion);

        RolProyecto rolDirector = em.createQuery(
                "SELECT r FROM RolProyecto r WHERE r.codigo = 'DIRECTOR'", RolProyecto.class)
                .getSingleResult();

        MiembroProyecto miembro = MiembroProyecto.builder()
                .proyecto(proyecto)
                .perfil(perfil)
                .rolProyecto(rolDirector)
                .idTycAceptado(1L)
                .estadoParticipacion(EstadoParticipacion.Activo)
                .build();
        em.persist(miembro);

        Actividad actividad = Actividad.builder()
                .nombre("Montaje")
                .descripcion("Descripcion original")
                .duracionMinutos(60)
                .fechaHoraInicio(LocalDateTime.now().plusDays(5))
                .estado(EstadoActividad.PENDIENTE)
                .planificacion(planificacion)
                .ubicacion(ubicacion)
                .build();
        em.persist(actividad);

        RequerimientoActividad requerimiento = RequerimientoActividad.builder()
                .actividad(actividad)
                .profesion(profesion)
                .cantidad(1)
                .descripcion("Requerimiento original")
                .build();
        em.persist(requerimiento);

        em.persist(AsignacionActividad.builder()
                .miembro(miembro)
                .actividad(actividad)
                .build());

        em.flush();

        return new Escenario(
                usuario.getIdUsuario(),
                perfil.getIdPerfil(),
                proyecto.getIdProyecto(),
                actividad.getIdActividad());
    }

    private record Escenario(
            Long idUsuario,
            Long idPerfil,
            Long idProyecto,
            Long idActividad) {
    }
}
