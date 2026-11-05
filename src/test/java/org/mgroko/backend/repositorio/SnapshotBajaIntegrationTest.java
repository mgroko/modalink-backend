package org.mgroko.backend.repositorio;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.AsignacionActividad;
import org.mgroko.backend.modelo.Genero;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Planificacion;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RequerimientoActividad;
import org.mgroko.backend.modelo.RequerimientoGralProyecto;
import org.mgroko.backend.modelo.RolGlobal;
import org.mgroko.backend.modelo.RolProyecto;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoActividad;
import org.mgroko.backend.modelo.enums.EstadoParticipacion;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.Privacidad;
import org.mgroko.backend.proyectos.dto.DarseDeBajaProyectoRequest;
import org.mgroko.backend.proyectos.servicio.DarseDeBajaProyectoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@SpringBootTest
@Transactional
class SnapshotBajaIntegrationTest extends AbstractPostgresIntegrationTest {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private DarseDeBajaProyectoService darseDeBajaProyectoService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolGlobalRepository rolGlobalRepository;

    @Autowired
    private GeneroRepository generoRepository;

    @Test
    void baja_creaSnapshotsYRepuntaHistorialSinModificarActivos() {
        Escenario escenario = crearEscenario();

        darseDeBajaProyectoService.darseDeBaja(
                escenario.idUsuario(),
                escenario.idPerfil(),
                escenario.idProyecto(),
                new DarseDeBajaProyectoRequest("Motivos personales"));

        em.flush();
        em.clear();

        MiembroProyecto miembro = em.find(MiembroProyecto.class, escenario.idMiembro());
        assertThat(miembro.getEstadoParticipacion())
                .isEqualTo(EstadoParticipacion.BAJA_VOLUNTARIA);
        assertThat(miembro.getFechaBaja()).isNotNull();
        assertThat(miembro.getPerfilAutorBaja().getIdPerfil())
                .isEqualTo(escenario.idPerfil());

        RequerimientoGralProyecto activoGral = em.find(
                RequerimientoGralProyecto.class, escenario.idRequerimientoGral());
        assertThat(activoGral).isNotNull();
        assertThat(activoGral.getFechaArchivado()).isNull();

        RequerimientoGralProyecto snapshotGral = em.find(
                RequerimientoGralProyecto.class, miembro.getRequerimientoGral()
                        .getIdRequerimientoGral());
        assertThat(snapshotGral.getFechaArchivado()).isNotNull();
        assertThat(snapshotGral.getIdRequerimientoOrigen())
                .isEqualTo(escenario.idRequerimientoGral());

        RequerimientoActividad activoActividad = em.find(
                RequerimientoActividad.class, escenario.idRequerimientoActividad());
        assertThat(activoActividad).isNotNull();
        assertThat(activoActividad.getFechaArchivado()).isNull();

        AsignacionActividad asignacion = em.find(
                AsignacionActividad.class, escenario.idAsignacion());
        RequerimientoActividad snapshotActividad =
                asignacion.getRequerimientoActividad();
        assertThat(snapshotActividad.getFechaArchivado()).isNotNull();
        assertThat(snapshotActividad.getIdRequerimientoOrigen())
                .isEqualTo(escenario.idRequerimientoActividad());
    }

    private Escenario crearEscenario() {
        RolGlobal rol = rolGlobalRepository.findByNombre("Usuario").orElseThrow();
        Genero genero = generoRepository.findByCodigo("mujer").orElseThrow();

        Usuario usuario = usuarioRepository.saveAndFlush(Usuario.builder()
                .nombre("Ana")
                .apellido("Baja")
                .dni("60606001")
                .fechaNacimiento(LocalDate.now().minusYears(25))
                .correo("snapshot.baja@example.com")
                .rolGlobal(rol)
                .genero(genero)
                .build());

        Usuario usuarioDirector = usuarioRepository.saveAndFlush(Usuario.builder()
                .nombre("Dario")
                .apellido("Director")
                .dni("60606002")
                .fechaNacimiento(LocalDate.now().minusYears(40))
                .correo("snapshot.director@example.com")
                .rolGlobal(rol)
                .genero(genero)
                .build());

        Profesion profesion = Profesion.builder()
                .codigo("PROF_SNAPSHOT")
                .nombre("Profesion Snapshot")
                .build();
        em.persist(profesion);

        Perfil perfil = Perfil.builder()
                .nombreArtistico("Luna Snapshot")
                .biografia("Perfil de baja")
                .estado(EstadoPerfil.Activo)
                .usuario(usuario)
                .profesion(profesion)
                .build();
        em.persist(perfil);

        Perfil perfilDirector = Perfil.builder()
                .nombreArtistico("Director Snapshot")
                .biografia("Perfil director")
                .estado(EstadoPerfil.Activo)
                .usuario(usuarioDirector)
                .profesion(profesion)
                .build();
        em.persist(perfilDirector);

        Proyecto proyecto = Proyecto.builder()
                .nombre("Proyecto Snapshot")
                .descripcion("Proyecto de baja")
                .estado(EstadoProyecto.Publicado)
                .privacidad(Privacidad.Publico)
                .build();
        em.persist(proyecto);

        Planificacion planificacion = Planificacion.builder()
                .proyecto(proyecto)
                .fechaEntrega(LocalDate.now().plusDays(30))
                .build();
        em.persist(planificacion);

        RolProyecto rolDirector = em.createQuery(
                "SELECT r FROM RolProyecto r WHERE r.codigo = 'DIRECTOR'",
                RolProyecto.class).getSingleResult();
        RolProyecto rolMiembro = em.createQuery(
                "SELECT r FROM RolProyecto r WHERE r.codigo = 'MIEMBRO'",
                RolProyecto.class).getSingleResult();

        em.persist(MiembroProyecto.builder()
                .proyecto(proyecto)
                .perfil(perfilDirector)
                .rolProyecto(rolDirector)
                .estadoParticipacion(EstadoParticipacion.Activo)
                .build());

        RequerimientoGralProyecto requerimientoGral =
                RequerimientoGralProyecto.builder()
                        .proyecto(proyecto)
                        .profesion(profesion)
                        .cantidad(1)
                        .descripcion("Requerimiento general activo")
                        .build();
        em.persist(requerimientoGral);

        MiembroProyecto miembro = MiembroProyecto.builder()
                .proyecto(proyecto)
                .perfil(perfil)
                .rolProyecto(rolMiembro)
                .estadoParticipacion(EstadoParticipacion.Activo)
                .requerimientoGral(requerimientoGral)
                .build();
        em.persist(miembro);

        Actividad actividad = Actividad.builder()
                .nombre("Actividad Snapshot")
                .descripcion("Actividad de baja")
                .duracionMinutos(60)
                .fechaHoraInicio(LocalDateTime.now().plusDays(5))
                .estado(EstadoActividad.Pendiente)
                .planificacion(planificacion)
                .build();
        em.persist(actividad);

        RequerimientoActividad requerimientoActividad =
                RequerimientoActividad.builder()
                        .actividad(actividad)
                        .profesion(profesion)
                        .cantidad(1)
                        .descripcion("Requerimiento de actividad activo")
                        .build();
        em.persist(requerimientoActividad);

        AsignacionActividad asignacion = AsignacionActividad.builder()
                .miembro(miembro)
                .actividad(actividad)
                .requerimientoActividad(requerimientoActividad)
                .build();
        em.persist(asignacion);

        em.flush();

        return new Escenario(
                usuario.getIdUsuario(),
                perfil.getIdPerfil(),
                proyecto.getIdProyecto(),
                miembro.getIdMiembro(),
                requerimientoGral.getIdRequerimientoGral(),
                requerimientoActividad.getIdRequerimiento(),
                asignacion.getIdAsignacionAct());
    }

    private record Escenario(
            Long idUsuario,
            Long idPerfil,
            Long idProyecto,
            Long idMiembro,
            Long idRequerimientoGral,
            Long idRequerimientoActividad,
            Long idAsignacion) {
    }
}
