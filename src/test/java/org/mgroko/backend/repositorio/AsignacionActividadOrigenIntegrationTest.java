package org.mgroko.backend.repositorio;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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
import org.mgroko.backend.modelo.RolGlobal;
import org.mgroko.backend.modelo.RolProyecto;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoActividad;
import org.mgroko.backend.modelo.enums.EstadoParticipacion;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.Privacidad;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@SpringBootTest
@Transactional
class AsignacionActividadOrigenIntegrationTest extends AbstractPostgresIntegrationTest {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private AsignacionActividadRepository asignacionActividadRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolGlobalRepository rolGlobalRepository;

    @Autowired
    private GeneroRepository generoRepository;

    @Test
    void consultasDeOrigen_ignoranRequerimientosArchivadosYCuentanAsignacionesDirectas() {
        Long idActividad = crearActividadConAsignacionARequerimiento(false);
        assertThat(asignacionActividadRepository.existsAsignacionActiva(idActividad))
                .isTrue();
        assertThat(asignacionActividadRepository
                .findIdsActividadesConAsignacionesActivas(List.of(idActividad)))
                .contains(idActividad);

        archivarRequerimientoDeActividad(idActividad);
        assertThat(asignacionActividadRepository.existsAsignacionActiva(idActividad))
                .isFalse();
        assertThat(asignacionActividadRepository
                .findIdsActividadesConAsignacionesActivas(List.of(idActividad)))
                .isEmpty();

        Long idActividadDirecta = crearActividadConAsignacionARequerimiento(true);
        assertThat(asignacionActividadRepository.existsAsignacionActiva(idActividadDirecta))
                .isTrue();
        assertThat(asignacionActividadRepository
                .findIdsActividadesConAsignacionesActivas(List.of(idActividadDirecta)))
                .contains(idActividadDirecta);
    }

    private Long crearActividadConAsignacionARequerimiento(boolean sinRequerimiento) {
        RolGlobal rol = rolGlobalRepository.findByNombre("Usuario").orElseThrow();
        Genero genero = generoRepository.findByCodigo("mujer").orElseThrow();

        Usuario usuario = usuarioRepository.saveAndFlush(Usuario.builder()
                .nombre("Origen")
                .apellido("Test")
                .dni(sinRequerimiento ? "70707002" : "70707001")
                .fechaNacimiento(LocalDate.now().minusYears(25))
                .correo(sinRequerimiento
                        ? "origen.directa@example.com"
                        : "origen.requerimiento@example.com")
                .rolGlobal(rol)
                .genero(genero)
                .build());

        Profesion profesion = Profesion.builder()
                .codigo("PROF_ORIGEN_" + (sinRequerimiento ? "DIRECTA" : "REQ"))
                .nombre("Profesion Origen " + (sinRequerimiento ? "Directa" : "Req"))
                .build();
        em.persist(profesion);

        Perfil perfil = Perfil.builder()
                .nombreArtistico("Perfil Origen")
                .biografia("Perfil de test")
                .estado(EstadoPerfil.Activo)
                .usuario(usuario)
                .profesion(profesion)
                .build();
        em.persist(perfil);

        Proyecto proyecto = Proyecto.builder()
                .nombre("Proyecto Origen " + (sinRequerimiento ? "Directa" : "Req"))
                .descripcion("Proyecto de test")
                .estado(EstadoProyecto.Confirmado)
                .fechaInicio(LocalDate.now())
                .privacidad(Privacidad.Publico)
                .build();
        em.persist(proyecto);

        Planificacion planificacion = Planificacion.builder()
                .proyecto(proyecto)
                .fechaEntrega(LocalDate.now().plusDays(30))
                .build();
        em.persist(planificacion);

        RolProyecto rolMiembro = em.createQuery(
                "SELECT r FROM RolProyecto r WHERE r.codigo = 'MIEMBRO'",
                RolProyecto.class).getSingleResult();

        MiembroProyecto miembro = MiembroProyecto.builder()
                .proyecto(proyecto)
                .perfil(perfil)
                .rolProyecto(rolMiembro)
                .estadoParticipacion(EstadoParticipacion.Activo)
                .build();
        em.persist(miembro);

        Actividad actividad = Actividad.builder()
                .nombre("Actividad Origen " + (sinRequerimiento ? "Directa" : "Req"))
                .descripcion("Actividad de test")
                .duracionMinutos(60)
                .fechaHoraInicio(LocalDateTime.now().plusDays(5))
                .estado(EstadoActividad.Pendiente)
                .planificacion(planificacion)
                .build();
        em.persist(actividad);

        RequerimientoActividad requerimiento = null;
        if (!sinRequerimiento) {
            requerimiento = RequerimientoActividad.builder()
                    .actividad(actividad)
                    .profesion(profesion)
                    .cantidad(1)
                    .descripcion("Requerimiento de test")
                    .build();
            em.persist(requerimiento);
        }

        em.persist(AsignacionActividad.builder()
                .miembro(miembro)
                .actividad(actividad)
                .requerimientoActividad(requerimiento)
                .build());
        em.flush();

        return actividad.getIdActividad();
    }

    private void archivarRequerimientoDeActividad(Long idActividad) {
        RequerimientoActividad requerimiento = em.createQuery("""
                        SELECT r FROM RequerimientoActividad r
                        WHERE r.actividad.idActividad = :idActividad
                        """, RequerimientoActividad.class)
                .setParameter("idActividad", idActividad)
                .getSingleResult();
        requerimiento.setFechaArchivado(LocalDateTime.now());
        em.flush();
        em.clear();
    }
}
