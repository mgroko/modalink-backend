package org.mgroko.backend.repositorio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.PerfilTyc;
import org.mgroko.backend.modelo.PerfilTycId;
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
import org.mgroko.backend.proyectos.dto.BuscarProyectosFiltro;
import org.mgroko.backend.proyectos.especificacion.ProyectoSpecifications;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Integración de la regla de visibilidad de la búsqueda global (UC-29)
 * contra PostgreSQL real: el Borrador y el Privado solo son visibles para
 * miembros activos; lo Público no-borrador es visible para cualquier perfil.
 */
@SpringBootTest
@Transactional
class ProyectoBusquedaIntegrationTest extends AbstractPostgresIntegrationTest {

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

    private record Escenario(
            Perfil perfilPropio,
            Perfil perfilAjeno,
            Proyecto borradorPropio,
            Proyecto privadoPropio,
            Proyecto publicoPropio,
            Proyecto borradorAjeno,
            Proyecto privadoAjeno,
            Proyecto publicoAjeno) {}

    private Escenario crearEscenario(String sufijo, String dniPropio, String dniAjeno) {
        RolGlobal rol = rolGlobalRepository.findByNombre("Usuario").orElseThrow();
        var genero = generoRepository.findByCodigo("mujer").orElseThrow();

        Usuario usuarioPropio = usuarioRepository.saveAndFlush(Usuario.builder()
                .nombre("Busqueda").apellido("Propia").dni(dniPropio)
                .fechaNacimiento(LocalDate.now().minusYears(25))
                .correo("busqprop" + sufijo + "@example.com")
                .estado(EstadoUsuario.Activo)
                .rolGlobal(rol).genero(genero).build());

        Profesion profesion = Profesion.builder()
                .codigo("PROF_BQP" + sufijo).nombre("Profesion Busqueda Propia").build();
        em.persist(profesion);

        Perfil perfilPropio = Perfil.builder()
                .nombreArtistico("Perfil Propio " + sufijo).biografia("Perfil propio")
                .estado(EstadoPerfil.Activo)
                .usuario(usuarioPropio).profesion(profesion).build();
        em.persist(perfilPropio);

        PerfilTyc tyc = PerfilTyc.builder()
                .id(new PerfilTycId(perfilPropio.getIdPerfil(), 1L))
                .perfil(perfilPropio)
                .descripcion("T&C Base")
                .fechaInicio(LocalDateTime.now())
                .build();
        em.persist(tyc);

        Usuario usuarioAjeno = usuarioRepository.saveAndFlush(Usuario.builder()
                .nombre("Busqueda").apellido("Ajena").dni(dniAjeno)
                .fechaNacimiento(LocalDate.now().minusYears(30))
                .correo("busqaju" + sufijo + "@example.com")
                .estado(EstadoUsuario.Activo)
                .rolGlobal(rol).genero(genero).build());

        Profesion profesionAjena = Profesion.builder()
                .codigo("PROF_BQA" + sufijo).nombre("Profesion Busqueda Ajena").build();
        em.persist(profesionAjena);

        Perfil perfilAjeno = Perfil.builder()
                .nombreArtistico("Perfil Ajeno " + sufijo).biografia("Perfil ajeno")
                .estado(EstadoPerfil.Activo)
                .usuario(usuarioAjeno).profesion(profesionAjena).build();
        em.persist(perfilAjeno);

        Proyecto borradorPropio = crearProyecto("Borrador Propio " + sufijo,
                EstadoProyecto.BORRADOR, Privacidad.PUBLICO);
        Proyecto privadoPropio = crearProyecto("Privado Propio " + sufijo,
                EstadoProyecto.PUBLICADO, Privacidad.PRIVADO);
        Proyecto publicoPropio = crearProyecto("Publico Propio " + sufijo,
                EstadoProyecto.PUBLICADO, Privacidad.PUBLICO);

        Proyecto borradorAjeno = crearProyecto("Borrador Ajeno " + sufijo,
                EstadoProyecto.BORRADOR, Privacidad.PUBLICO);
        Proyecto privadoAjeno = crearProyecto("Privado Ajeno " + sufijo,
                EstadoProyecto.PUBLICADO, Privacidad.PRIVADO);
        Proyecto publicoAjeno = crearProyecto("Publico Ajeno " + sufijo,
                EstadoProyecto.PUBLICADO, Privacidad.PUBLICO);

        crearMiembro(borradorPropio, perfilPropio);
        crearMiembro(privadoPropio, perfilPropio);
        crearMiembro(publicoPropio, perfilPropio);

        em.flush();
        return new Escenario(perfilPropio, perfilAjeno, borradorPropio, privadoPropio,
                publicoPropio, borradorAjeno, privadoAjeno, publicoAjeno);
    }

    private Proyecto crearProyecto(String nombre, EstadoProyecto estado, Privacidad privacidad) {
        Proyecto proyecto = Proyecto.builder()
                .nombre(nombre).descripcion("Descripción de test")
                .fechaInicio(LocalDate.now())
                .estado(estado).privacidad(privacidad).build();
        em.persist(proyecto);
        return proyecto;
    }

    private void crearMiembro(Proyecto proyecto, Perfil perfil) {
        em.persist(MiembroProyecto.builder()
                .proyecto(proyecto).perfil(perfil).rolProyecto(rolMiembro())
                .idTycAceptado(1L)
                .estadoParticipacion(EstadoParticipacion.ACTIVO).build());
    }

    private RolProyecto rolMiembro() {
        return em.createQuery("""
                SELECT r FROM RolProyecto r
                WHERE r.codigo = 'MIEMBRO' OR LOWER(r.nombre) LIKE 'miembro%'
                """, RolProyecto.class)
                .setMaxResults(1)
                .getSingleResult();
    }

    private List<String> nombresVisibles(Long idPerfil, BuscarProyectosFiltro filtro) {
        Page<Proyecto> resultados = proyectoRepository.findAll(
                ProyectoSpecifications.conFiltros(filtro, idPerfil),
                PageRequest.of(0, 50, Sort.by(Sort.Direction.ASC, "nombre")));
        return resultados.map(Proyecto::getNombre).getContent();
    }

    @Test
    void comoMiembro_veSusBorradoresYPrivadosYLoPublicoAjeno() {
        Escenario e = crearEscenario("A", "99200001", "99200002");

        List<String> nombres = nombresVisibles(e.perfilPropio().getIdPerfil(), null);

        assertThat(nombres).containsExactlyInAnyOrder(
                e.borradorPropio().getNombre(),
                e.privadoPropio().getNombre(),
                e.publicoPropio().getNombre(),
                e.publicoAjeno().getNombre());
        assertThat(nombres).doesNotContain(
                e.borradorAjeno().getNombre(),
                e.privadoAjeno().getNombre());
    }

    @Test
    void comoNoMiembro_noVeBorradoresNiPrivadosAjenos() {
        Escenario e = crearEscenario("B", "99200003", "99200004");

        List<String> nombres = nombresVisibles(e.perfilAjeno().getIdPerfil(), null);

        assertThat(nombres).containsExactlyInAnyOrder(
                e.publicoAjeno().getNombre(),
                e.publicoPropio().getNombre());
        assertThat(nombres).doesNotContain(
                e.borradorPropio().getNombre(),
                e.privadoPropio().getNombre(),
                e.borradorAjeno().getNombre(),
                e.privadoAjeno().getNombre());
    }

    @Test
    void filtroEstadoBorrador_soloRetornaElPropio() {
        Escenario e = crearEscenario("C", "99200005", "99200006");

        List<String> nombres = nombresVisibles(e.perfilPropio().getIdPerfil(),
                new BuscarProyectosFiltro(null, EstadoProyecto.BORRADOR, null));

        assertThat(nombres).containsExactly(e.borradorPropio().getNombre());
    }

    @Test
    void filtroPorNombre_noFiltraLaVisibilidadBase() {
        Escenario e = crearEscenario("D", "99200007", "99200008");

        List<String> ajenoPublico = nombresVisibles(e.perfilAjeno().getIdPerfil(),
                new BuscarProyectosFiltro("Publico Ajeno", null, null));
        assertThat(ajenoPublico).containsExactly(e.publicoAjeno().getNombre());

        List<String> ajenoPrivado = nombresVisibles(e.perfilAjeno().getIdPerfil(),
                new BuscarProyectosFiltro("Privado Ajeno", null, null));
        assertThat(ajenoPrivado).isEmpty();

        List<String> propioBorrador = nombresVisibles(e.perfilAjeno().getIdPerfil(),
                new BuscarProyectosFiltro("Borrador Propio", null, null));
        assertThat(propioBorrador).isEmpty();
    }

    @Test
    void comoDirectorDelProyectoBorrador_seSigueViendo() {
        Escenario e = crearEscenario("E", "99200009", "99200010");

        var miembro = em.createQuery("""
                SELECT mp FROM MiembroProyecto mp
                WHERE mp.proyecto.idProyecto = :idProyecto AND mp.perfil.idPerfil = :idPerfil
                """, MiembroProyecto.class)
                .setParameter("idProyecto", e.borradorPropio().getIdProyecto())
                .setParameter("idPerfil", e.perfilPropio().getIdPerfil())
                .getSingleResult();
        miembro.setRolProyecto(em.createQuery("""
                SELECT r FROM RolProyecto r
                WHERE r.codigo = 'DIRECTOR' OR LOWER(r.nombre) LIKE 'director%'
                """, RolProyecto.class)
                .setMaxResults(1)
                .getSingleResult());
        em.flush();

        List<String> porDirector = nombresVisibles(e.perfilPropio().getIdPerfil(),
                new BuscarProyectosFiltro(null, null, "Perfil Propio"));

        assertThat(porDirector).contains(e.borradorPropio().getNombre());
    }
}
