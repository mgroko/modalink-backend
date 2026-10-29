package org.mgroko.backend.repositorio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.AsignacionActividad;
import org.mgroko.backend.modelo.Genero;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Planificacion;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RequerimientoGralProyecto;
import org.mgroko.backend.modelo.RolGlobal;
import org.mgroko.backend.modelo.RolProyecto;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoActividad;
import org.mgroko.backend.modelo.enums.EstadoParticipacion;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.OrigenInicio;
import org.mgroko.backend.modelo.enums.Privacidad;
import org.mgroko.backend.proyectos.dto.ConfirmarProyectoResponse;
import org.mgroko.backend.proyectos.dto.PrevisualizacionConfirmacionResponse;
import org.mgroko.backend.proyectos.exception.ProyectoNoConfirmableException;
import org.mgroko.backend.proyectos.servicio.ConfirmarProyectoService;
import org.mgroko.backend.proyectos.servicio.PrevisualizarConfirmacionService;
import org.mgroko.backend.repositorio.GeneroRepository;
import org.mgroko.backend.repositorio.RolGlobalRepository;
import org.mgroko.backend.repositorio.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Integración de UC-60 contra PostgreSQL real.
 *
 * <p>Usa una actividad sin requerimientos y una asignación directa para
 * aislar el flujo de confirmación (estado, fechaInicio, CPM, planificador y
 * persistencia de horarios) de UC-26/UC-34.</p>
 */
@SpringBootTest
@Transactional
class ConfirmarProyectoIntegrationTest extends AbstractPostgresIntegrationTest {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private ConfirmarProyectoService confirmarProyectoService;

    @Autowired
    private PrevisualizarConfirmacionService previsualizarConfirmacionService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolGlobalRepository rolGlobalRepository;

    @Autowired
    private GeneroRepository generoRepository;

    @Test
    void confirmar_caminoFeliz_persisteEstadoFechaYHorario() {
        Datos datos = crearProyectoConActividadAsignada(false);

        PrevisualizacionConfirmacionResponse preview =
                previsualizarConfirmacionService.previsualizar(
                        datos.director().getIdUsuario(),
                        datos.perfilDirector().getIdPerfil(),
                        datos.proyecto().getIdProyecto());

        assertThat(preview.puedeConfirmar()).isTrue();
        assertThat(preview.propuestaHorarios()).hasSize(1);

        ConfirmarProyectoResponse response = confirmarProyectoService.confirmar(
                datos.director().getIdUsuario(),
                datos.perfilDirector().getIdPerfil(),
                datos.proyecto().getIdProyecto());

        em.flush();
        em.clear();

        Proyecto confirmado = em.find(Proyecto.class, datos.proyecto().getIdProyecto());
        Actividad actividad = em.find(Actividad.class, datos.actividad().getIdActividad());

        assertThat(confirmado.getEstado()).isEqualTo(EstadoProyecto.CONFIRMADO);
        assertThat(confirmado.getFechaInicio()).isNotNull();
        assertThat(actividad.getFechaHoraInicio()).isNotNull();
        assertThat(actividad.getOrigenInicio()).isEqualTo(OrigenInicio.CONFIRMADA);
        assertThat(response.proyecto().estado()).isEqualTo("Confirmado");
        assertThat(response.cronograma().resumen().tentativo()).isFalse();
    }

    @Test
    void confirmar_conRequerimientoGeneralIncompleto_noCambiaElProyecto() {
        Datos datos = crearProyectoConActividadAsignada(true);

        assertThatThrownBy(() -> confirmarProyectoService.confirmar(
                datos.director().getIdUsuario(),
                datos.perfilDirector().getIdPerfil(),
                datos.proyecto().getIdProyecto()))
                .isInstanceOf(ProyectoNoConfirmableException.class)
                .extracting("codigo")
                .isEqualTo("REQUERIMIENTOS_INCOMPLETOS");

        em.flush();
        em.clear();

        Proyecto proyecto = em.find(Proyecto.class, datos.proyecto().getIdProyecto());
        Actividad actividad = em.find(Actividad.class, datos.actividad().getIdActividad());

        assertThat(proyecto.getEstado()).isEqualTo(EstadoProyecto.PUBLICADO);
        assertThat(proyecto.getFechaInicio()).isNull();
        assertThat(actividad.getFechaHoraInicio()).isNull();
    }

    private Datos crearProyectoConActividadAsignada(boolean requerimientoIncompleto) {
        Profesion profesion = Profesion.builder()
                .codigo("PROF_UC60_" + System.nanoTime())
                .nombre("Profesional UC60")
                .build();
        em.persist(profesion);

        Usuario director = guardarUsuario("uc60.dir." + System.nanoTime() + "@example.com", "88990001");
        Usuario miembro = guardarUsuario("uc60.miem." + System.nanoTime() + "@example.com", "88990002");

        Perfil perfilDirector = perfil("Directora UC60", director, profesion);
        Perfil perfilMiembro = perfil("Miembro UC60", miembro, profesion);

        Proyecto proyecto = Proyecto.builder()
                .nombre("Proyecto UC60 " + System.nanoTime())
                .descripcion("Proyecto de integración UC-60")
                .estado(EstadoProyecto.PUBLICADO)
                .privacidad(Privacidad.PUBLICO)
                .build();
        em.persist(proyecto);

        Planificacion planificacion = Planificacion.builder()
                .proyecto(proyecto)
                .fechaEntrega(LocalDate.now().plusDays(7))
                .build();
        em.persist(planificacion);

        em.persist(MiembroProyecto.builder()
                .proyecto(proyecto)
                .perfil(perfilDirector)
                .rolProyecto(rol("DIRECTOR"))
                .estadoParticipacion(EstadoParticipacion.ACTIVO)
                .build());

        MiembroProyecto miembroProyecto = MiembroProyecto.builder()
                .proyecto(proyecto)
                .perfil(perfilMiembro)
                .rolProyecto(rol("MIEMBRO"))
                .estadoParticipacion(EstadoParticipacion.ACTIVO)
                .build();
        em.persist(miembroProyecto);

        Actividad actividad = Actividad.builder()
                .nombre("Actividad UC60")
                .duracionMinutos(60)
                .estado(EstadoActividad.PENDIENTE)
                .planificacion(planificacion)
                .build();
        em.persist(actividad);

        em.persist(AsignacionActividad.builder()
                .miembro(miembroProyecto)
                .actividad(actividad)
                .build());

        if (requerimientoIncompleto) {
            em.persist(RequerimientoGralProyecto.builder()
                    .cantidad(2)
                    .descripcion("Personal faltante")
                    .profesion(profesion)
                    .proyecto(proyecto)
                    .build());
        }

        em.flush();
        return new Datos(proyecto, actividad, director, perfilDirector);
    }

    private Usuario guardarUsuario(String correo, String dni) {
        RolGlobal rol = rolGlobalRepository.findByNombre("Usuario").orElseThrow();
        Genero genero = generoRepository.findByCodigo("mujer").orElseThrow();
        return usuarioRepository.saveAndFlush(Usuario.builder()
                .nombre("Ana")
                .apellido("Gomez")
                .dni(dni)
                .fechaNacimiento(LocalDate.now().minusYears(22))
                .correo(correo)
                .rolGlobal(rol)
                .genero(genero)
                .build());
    }

    private Perfil perfil(String nombre, Usuario usuario, Profesion profesion) {
        Perfil perfil = Perfil.builder()
                .nombreArtistico(nombre)
                .biografia("Perfil de integración UC-60")
                .estado(EstadoPerfil.Activo)
                .usuario(usuario)
                .profesion(profesion)
                .build();
        em.persist(perfil);
        return perfil;
    }

    private RolProyecto rol(String codigo) {
        return em.createQuery(
                "SELECT r FROM RolProyecto r WHERE r.codigo = :codigo",
                RolProyecto.class)
                .setParameter("codigo", codigo)
                .getSingleResult();
    }

    private record Datos(
            Proyecto proyecto,
            Actividad actividad,
            Usuario director,
            Perfil perfilDirector) {
    }
}
