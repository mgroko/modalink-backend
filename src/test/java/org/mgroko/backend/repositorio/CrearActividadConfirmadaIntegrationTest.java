package org.mgroko.backend.repositorio;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.actividades.dto.ActividadResponse;
import org.mgroko.backend.actividades.dto.CrearActividadRequest;
import org.mgroko.backend.actividades.dto.CronogramaResponse;
import org.mgroko.backend.actividades.enums.UnidadDuracion;
import org.mgroko.backend.actividades.servicio.CrearActividadService;
import org.mgroko.backend.actividades.servicio.ObtenerCronogramaService;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.AsignacionActividad;
import org.mgroko.backend.modelo.Genero;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Planificacion;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RolGlobal;
import org.mgroko.backend.modelo.RolProyecto;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoActividad;
import org.mgroko.backend.modelo.enums.EstadoParticipacion;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.OrigenInicio;
import org.mgroko.backend.modelo.enums.Privacidad;
import org.mgroko.backend.proyectos.servicio.ConfirmarProyectoService;
import org.mgroko.backend.repositorio.GeneroRepository;
import org.mgroko.backend.repositorio.RolGlobalRepository;
import org.mgroko.backend.repositorio.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Integración del alta de una actividad en un proyecto confirmado.
 *
 * <p>El proyecto tiene tres actividades ya confirmadas. La actividad nueva
 * depende de las tres y debe quedar agendada después de ellas, sin generar
 * conflictos CPM.</p>
 */
@SpringBootTest
@Transactional
class CrearActividadConfirmadaIntegrationTest extends AbstractPostgresIntegrationTest {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolGlobalRepository rolGlobalRepository;

    @Autowired
    private GeneroRepository generoRepository;

    @Autowired
    private ConfirmarProyectoService confirmarProyectoService;

    @Autowired
    private CrearActividadService crearActividadService;

    @Autowired
    private ObtenerCronogramaService obtenerCronogramaService;

    @Test
    void crearActividadConTresPredecesorasEnProyectoConfirmado_noGeneraConflictos() {
        Datos datos = crearProyectoConfirmadoConTresActividades();

        CrearActividadRequest request = new CrearActividadRequest(
                "Actividad nueva",
                "Depende de las tres actividades existentes",
                1,
                UnidadDuracion.HORAS,
                null,
                datos.idsActividades(),
                null);

        ActividadResponse creada = crearActividadService.crear(
                datos.director().getIdUsuario(),
                datos.perfilDirector().getIdPerfil(),
                datos.proyecto().getIdProyecto(),
                request);

        em.flush();
        em.clear();

        Actividad nueva = em.find(Actividad.class, creada.idActividad());
        assertThat(nueva).isNotNull();
        assertThat(nueva.getEstado()).isEqualTo(EstadoActividad.PENDIENTE);
        assertThat(nueva.getOrigenInicio()).isEqualTo(OrigenInicio.PROYECTADA);
        assertThat(nueva.getFechaHoraInicio()).isNotNull();

        LocalDateTime finMaximoPredecesoras = LocalDateTime.MIN;
        for (Long idPredecesora : datos.idsActividades()) {
            Actividad predecesora = em.find(Actividad.class, idPredecesora);
            assertThat(predecesora).isNotNull();
            assertThat(predecesora.getFechaHoraFin()).isNotNull();
            if (predecesora.getFechaHoraFin().isAfter(finMaximoPredecesoras)) {
                finMaximoPredecesoras = predecesora.getFechaHoraFin();
            }
        }

        assertThat(nueva.getFechaHoraInicio())
                .isAfterOrEqualTo(finMaximoPredecesoras);

        CronogramaResponse cronograma = obtenerCronogramaService.obtener(
                datos.director().getIdUsuario(),
                datos.perfilDirector().getIdPerfil(),
                datos.proyecto().getIdProyecto());

        assertThat(cronograma.conflictos()).isEmpty();
        assertThat(cronograma.resumen().estadoCronograma())
                .isNotEqualTo("CONFLICTO");

        var filaNueva = cronograma.actividades().stream()
                .filter(actividad -> actividad.idActividad().equals(nueva.getIdActividad()))
                .findFirst()
                .orElseThrow();

        assertThat(filaNueva.conflicto()).isFalse();
        assertThat(filaNueva.motivoConflicto()).isNull();
    }

    private Datos crearProyectoConfirmadoConTresActividades() {
        Profesion profesion = Profesion.builder()
                .codigo("PROF_UC60_TRES_" + System.nanoTime())
                .nombre("Profesional UC60 Tres")
                .build();
        em.persist(profesion);

        Usuario director = guardarUsuario(
                "uc60.tres.dir." + System.nanoTime() + "@example.com", "88990101");
        Usuario miembro = guardarUsuario(
                "uc60.tres.miem." + System.nanoTime() + "@example.com", "88990102");

        Perfil perfilDirector = perfil("Directora UC60 Tres", director, profesion);
        Perfil perfilMiembro = perfil("Miembro UC60 Tres", miembro, profesion);

        Proyecto proyecto = Proyecto.builder()
                .nombre("Proyecto UC60 Tres " + System.nanoTime())
                .descripcion("Proyecto de integración con tres actividades")
                .estado(EstadoProyecto.PUBLICADO)
                .privacidad(Privacidad.PUBLICO)
                .build();
        em.persist(proyecto);

        Planificacion planificacion = Planificacion.builder()
                .proyecto(proyecto)
                .fechaEntrega(LocalDate.now().plusDays(10))
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

        List<Long> idsActividades = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            Actividad actividad = Actividad.builder()
                    .nombre("Actividad " + i)
                    .duracionMinutos(60)
                    .estado(EstadoActividad.PENDIENTE)
                    .planificacion(planificacion)
                    .build();
            em.persist(actividad);
            em.flush();
            idsActividades.add(actividad.getIdActividad());

            em.persist(AsignacionActividad.builder()
                    .miembro(miembroProyecto)
                    .actividad(actividad)
                    .build());
        }

        em.flush();

        confirmarProyectoService.confirmar(
                director.getIdUsuario(),
                perfilDirector.getIdPerfil(),
                proyecto.getIdProyecto());

        em.flush();
        em.clear();

        return new Datos(
                proyecto,
                director,
                perfilDirector,
                miembro,
                idsActividades);
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
                .biografia("Perfil de integración")
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
            Usuario director,
            Perfil perfilDirector,
            Usuario miembro,
            List<Long> idsActividades) {
    }
}
