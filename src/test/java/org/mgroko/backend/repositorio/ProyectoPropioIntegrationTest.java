package org.mgroko.backend.repositorio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.AsignacionActividad;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.PerfilTyc;
import org.mgroko.backend.modelo.PerfilTycId;
import org.mgroko.backend.modelo.Planificacion;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RolGlobal;
import org.mgroko.backend.modelo.RolProyecto;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoParticipacion;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.modelo.enums.Privacidad;
import org.mgroko.backend.modelo.enums.RolProyectoUsuario;
import org.mgroko.backend.proyectos.dto.BuscarProyectosPropiosFiltro;
import org.mgroko.backend.proyectos.especificacion.ProyectoPropioSpecifications;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Integración de la especificación de proyectos propios (UC-29 bis) y de
 * {@link ActividadRepository#findProximasActividadesAsignadas} contra
 * PostgreSQL real: valida el alcance por membresía, los filtros y los
 * patrones de rol sobre la semilla de rol_proyecto.
 */
@SpringBootTest
@Transactional
class ProyectoPropioIntegrationTest extends AbstractPostgresIntegrationTest {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolGlobalRepository rolGlobalRepository;

    @Autowired
    private GeneroRepository generoRepository;

    @Autowired
    private ProyectoRepository proyectoRepository;

    @Autowired
    private ActividadRepository actividadRepository;

    private Escenario crearEscenario(String sufijo, String dni) {
        RolGlobal rol = rolGlobalRepository.findByNombre("Usuario").orElseThrow();
        var genero = generoRepository.findByCodigo("mujer").orElseThrow();

        Usuario usuario = usuarioRepository.saveAndFlush(Usuario.builder()
                .nombre("Propia").apellido("Prueba").dni(dni)
                .fechaNacimiento(LocalDate.now().minusYears(25))
                .correo("propios" + sufijo + "@example.com")
                .estado(EstadoUsuario.Activo)
                .rolGlobal(rol).genero(genero).build());

        Profesion profesion = Profesion.builder()
                .codigo("PROF_PROP" + sufijo).nombre("Profesion Propios").build();
        em.persist(profesion);

        Perfil perfil = Perfil.builder()
                .nombreArtistico("Director " + sufijo).biografia("Perfil de test")
                .estado(EstadoPerfil.Activo)
                .usuario(usuario).profesion(profesion).build();
        em.persist(perfil);

        PerfilTyc tyc = PerfilTyc.builder()
                .id(new PerfilTycId(perfil.getIdPerfil(), 1L))
                .perfil(perfil)
                .descripcion("T&C Base")
                .fechaInicio(LocalDateTime.now())
                .build();
        em.persist(tyc);

        // El trigger chk_perfil_unico_por_profesion no permite dos perfiles activos
        // del mismo usuario para la misma profesión, así que el ajeno usa otra.
        Profesion profesionAjena = Profesion.builder()
                .codigo("PROF_AJU" + sufijo).nombre("Profesion Ajena").build();
        em.persist(profesionAjena);

        Perfil perfilAjeno = Perfil.builder()
                .nombreArtistico("Ajeno " + sufijo).biografia("Perfil ajeno")
                .estado(EstadoPerfil.Activo)
                .usuario(usuario).profesion(profesionAjena).build();
        em.persist(perfilAjeno);

        PerfilTyc tycAjeno = PerfilTyc.builder()
                .id(new PerfilTycId(perfilAjeno.getIdPerfil(), 1L))
                .perfil(perfilAjeno)
                .descripcion("T&C Base")
                .fechaInicio(LocalDateTime.now())
                .build();
        em.persist(tycAjeno);

        Proyecto proyectoDirector = crearProyecto("Propio Alfa " + sufijo, EstadoProyecto.Publicado);
        Proyecto proyectoMiembro = crearProyecto("Propio Beta " + sufijo, EstadoProyecto.Borrador);
        Proyecto proyectoAjeno = crearProyecto("Ajeno Gamma " + sufijo, EstadoProyecto.Publicado);

        crearMiembro(proyectoDirector, perfil, rolDirector());
        crearMiembro(proyectoMiembro, perfil, rolMiembro());
        crearMiembro(proyectoAjeno, perfilAjeno, rolDirector());

        em.flush();
        return new Escenario(perfil, proyectoDirector, proyectoMiembro, proyectoAjeno);
    }

    private Proyecto crearProyecto(String nombre, EstadoProyecto estado) {
        Proyecto proyecto = Proyecto.builder()
                .nombre(nombre).descripcion("Descripción de test")
                .fechaInicio(LocalDate.now())
                .estado(estado).privacidad(Privacidad.Publico).build();
        em.persist(proyecto);
        return proyecto;
    }

    private void crearMiembro(Proyecto proyecto, Perfil perfil, RolProyecto rol) {
        em.persist(MiembroProyecto.builder()
                .proyecto(proyecto).perfil(perfil).rolProyecto(rol)
                .idTycAceptado(1L)
                .estadoParticipacion(EstadoParticipacion.Activo).build());
    }

    private RolProyecto rolDirector() {
        return em.createQuery("""
                SELECT r FROM RolProyecto r
                WHERE r.codigo = 'DIRECTOR' OR LOWER(r.nombre) LIKE 'director%'
                """, RolProyecto.class)
                .setMaxResults(1)
                .getSingleResult();
    }

    private RolProyecto rolMiembro() {
        return em.createQuery("""
                SELECT r FROM RolProyecto r
                WHERE r.codigo = 'MIEMBRO' OR LOWER(r.nombre) LIKE 'miembro%'
                """, RolProyecto.class)
                .setMaxResults(1)
                .getSingleResult();
    }

    private List<String> nombresDe(Long idPerfil, BuscarProyectosPropiosFiltro filtro) {
        return proyectoRepository
                .findAll(ProyectoPropioSpecifications.conFiltros(idPerfil, filtro),
                        PageRequest.of(0, 50, Sort.by(Sort.Direction.ASC, "nombre")))
                .map(Proyecto::getNombre)
                .getContent();
    }

    @Test
    void sinFiltros_retornaSoloProyectosDelPerfil() {
        Escenario e = crearEscenario("A", "99100001");

        List<String> nombres = nombresDe(e.perfil().getIdPerfil(),
                new BuscarProyectosPropiosFiltro(null, null, null));

        assertThat(nombres).containsExactlyInAnyOrder(
                e.proyectoDirector().getNombre(), e.proyectoMiembro().getNombre());
    }

    @Test
    void conRolDirector_filtraPorRol() {
        Escenario e = crearEscenario("B", "99100002");

        List<String> nombres = nombresDe(e.perfil().getIdPerfil(),
                new BuscarProyectosPropiosFiltro(null, null, RolProyectoUsuario.DIRECTOR));

        assertThat(nombres).containsExactly(e.proyectoDirector().getNombre());
    }

    @Test
    void conRolMiembro_filtraPorRol() {
        Escenario e = crearEscenario("C", "99100003");

        List<String> nombres = nombresDe(e.perfil().getIdPerfil(),
                new BuscarProyectosPropiosFiltro(null, null, RolProyectoUsuario.MIEMBRO));

        assertThat(nombres).containsExactly(e.proyectoMiembro().getNombre());
    }

    @Test
    void conNombreYEstado_filtraParcialPorAmbos() {
        Escenario e = crearEscenario("D", "99100004");

        List<String> porNombre = nombresDe(e.perfil().getIdPerfil(),
                new BuscarProyectosPropiosFiltro("alfa", null, null));
        assertThat(porNombre).containsExactly(e.proyectoDirector().getNombre());

        List<String> porEstado = nombresDe(e.perfil().getIdPerfil(),
                new BuscarProyectosPropiosFiltro(null, EstadoProyecto.Borrador, null));
        assertThat(porEstado).containsExactly(e.proyectoMiembro().getNombre());
    }

    @Test
    void proximasActividades_retornaSoloFuturasDelPerfilActivo() {
        Escenario e = crearEscenario("E", "99100005");

        MiembroProyecto miembroPropio = em.createQuery("""
                SELECT mp FROM MiembroProyecto mp
                WHERE mp.proyecto.idProyecto = :idProyecto AND mp.perfil.idPerfil = :idPerfil
                """, MiembroProyecto.class)
                .setParameter("idProyecto", e.proyectoDirector().getIdProyecto())
                .setParameter("idPerfil", e.perfil().getIdPerfil())
                .getSingleResult();

        Planificacion planificacion = Planificacion.builder()
                .proyecto(e.proyectoDirector())
                .fechaEntrega(LocalDate.now().plusDays(30))
                .build();
        em.persist(planificacion);

        Actividad futura = crearActividad(planificacion, "Sesion futura",
                LocalDateTime.now().plusDays(1).withHour(15).withMinute(0));
        Actividad pasada = crearActividad(planificacion, "Sesion pasada",
                LocalDateTime.now().minusDays(2));
        em.persist(AsignacionActividad.builder().miembro(miembroPropio).actividad(futura).build());
        em.persist(AsignacionActividad.builder().miembro(miembroPropio).actividad(pasada).build());
        em.flush();

        List<String> nombres = actividadRepository
                .findProximasActividadesAsignadas(
                        List.of(e.proyectoDirector().getIdProyecto()),
                        e.perfil().getIdPerfil(),
                        LocalDateTime.now())
                .stream()
                .map(Actividad::getNombre)
                .toList();

        assertThat(nombres).containsExactly("Sesion futura");
    }

    private Actividad crearActividad(Planificacion planificacion, String nombre, LocalDateTime inicio) {
        Actividad actividad = Actividad.builder()
                .nombre(nombre)
                .duracionMinutos(120)
                .fechaHoraInicio(inicio)
                .planificacion(planificacion)
                .build();
        em.persist(actividad);
        return actividad;
    }

    private record Escenario(
            Perfil perfil,
            Proyecto proyectoDirector,
            Proyecto proyectoMiembro,
            Proyecto proyectoAjeno) {}
}
