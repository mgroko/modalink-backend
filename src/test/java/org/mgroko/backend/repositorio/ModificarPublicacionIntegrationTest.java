package org.mgroko.backend.repositorio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.modelo.Genero;
import org.mgroko.backend.modelo.Imagen;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.Publicacion;
import org.mgroko.backend.modelo.RolGlobal;
import org.mgroko.backend.modelo.RolProyecto;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoParticipacion;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.Privacidad;
import org.mgroko.backend.publicaciones.dto.ModificarPublicacionRequest;
import org.mgroko.backend.publicaciones.dto.PublicacionResponse;
import org.mgroko.backend.publicaciones.exception.PublicacionNoEncontradaException;
import org.mgroko.backend.publicaciones.servicio.ModificarPublicacionService;
import org.mgroko.backend.publicaciones.servicio.ObtenerPublicacionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@SpringBootTest
@Transactional
class ModificarPublicacionIntegrationTest extends AbstractPostgresIntegrationTest {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private ModificarPublicacionService modificarPublicacionService;

    @Autowired
    private ObtenerPublicacionService obtenerPublicacionService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolGlobalRepository rolGlobalRepository;

    @Autowired
    private GeneroRepository generoRepository;

    @Test
    void modificar_actualizaDatosYReconciliaImagenes() {
        Escenario escenario = crearEscenario();
        Publicacion publicacion = crearPublicacion(escenario.perfil(), escenario.proyecto(), 2);
        Long idImagenConservada = publicacion.getImagenes().get(0).getIdImagen();
        Long idImagenEliminada = publicacion.getImagenes().get(1).getIdImagen();
        em.flush();

        ModificarPublicacionRequest request = request(
                "  Editorial Primavera  ", "Descripción nueva", List.of(idImagenConservada));

        PublicacionResponse response = modificarPublicacionService.modificar(
                escenario.idUsuario(), escenario.idPerfil(), publicacion.getIdPublicacion(), request);

        assertThat(response.titulo()).isEqualTo("Editorial Primavera");
        assertThat(response.descripcion()).isEqualTo("Descripción nueva");
        assertThat(response.imagenes()).singleElement()
                .satisfies(imagen -> assertThat(imagen.idImagen()).isEqualTo(idImagenConservada));

        em.flush();
        em.clear();

        Publicacion actualizada = em.find(Publicacion.class, publicacion.getIdPublicacion());
        assertThat(actualizada.getTitulo()).isEqualTo("Editorial Primavera");
        assertThat(actualizada.getDescripcion()).isEqualTo("Descripción nueva");
        assertThat(actualizada.getImagenes()).extracting(Imagen::getIdImagen)
                .containsExactly(idImagenConservada);
        assertThat(em.find(Imagen.class, idImagenEliminada)).isNull();
    }

    @Test
    void modificar_desvinculaProyecto() {
        Escenario escenario = crearEscenario();
        Publicacion publicacion = crearPublicacion(escenario.perfil(), escenario.proyecto(), 1);
        em.flush();

        PublicacionResponse response = modificarPublicacionService.modificar(
                escenario.idUsuario(),
                escenario.idPerfil(),
                publicacion.getIdPublicacion(),
                request("Editorial", "Descripción", ids(publicacion)));

        assertThat(response.proyecto()).isNull();
        assertThat(publicacion.getProyecto()).isNull();
    }

    @Test
    void modificar_asociaUnProyectoNuevo() {
        Escenario escenario = crearEscenario();
        Publicacion publicacion = crearPublicacion(escenario.perfil(), null, 1);
        Proyecto nuevoProyecto = crearProyecto(escenario.perfil(), "Proyecto nuevo");
        em.flush();

        ModificarPublicacionRequest request = request("Editorial", "Descripción", ids(publicacion));
        request.setIdProyecto(nuevoProyecto.getIdProyecto());

        PublicacionResponse response = modificarPublicacionService.modificar(
                escenario.idUsuario(), escenario.idPerfil(), publicacion.getIdPublicacion(), request);

        assertThat(response.proyecto()).isNotNull();
        assertThat(response.proyecto().idProyecto()).isEqualTo(nuevoProyecto.getIdProyecto());
        assertThat(publicacion.getProyecto().getIdProyecto()).isEqualTo(nuevoProyecto.getIdProyecto());
    }

    @Test
    void modificar_mismoProyectoNoRevalidaMembresia() {
        Escenario escenario = crearEscenario();
        Proyecto proyectoAjeno = crearProyectoSinMembresia("Proyecto sin membresía");
        Publicacion publicacion = crearPublicacion(escenario.perfil(), proyectoAjeno, 1);
        em.flush();

        ModificarPublicacionRequest request = request("Editorial", "Descripción", ids(publicacion));
        request.setIdProyecto(proyectoAjeno.getIdProyecto());

        PublicacionResponse response = modificarPublicacionService.modificar(
                escenario.idUsuario(), escenario.idPerfil(), publicacion.getIdPublicacion(), request);

        assertThat(response.proyecto()).isNotNull();
        assertThat(response.proyecto().idProyecto()).isEqualTo(proyectoAjeno.getIdProyecto());
    }

    @Test
    void obtener_incluyeColaboradoresRegistrados() {
        Escenario escenario = crearEscenario();
        Publicacion publicacion = crearPublicacion(escenario.perfil(), null, 1);
        Perfil colaborador = crearPerfilColaborador(escenario.usuario());
        em.flush();

        em.createNativeQuery("""
                INSERT INTO colaborador_publicacion (id_publicacion, id_perfil)
                VALUES (:idPublicacion, :idPerfil)
                """)
                .setParameter("idPublicacion", publicacion.getIdPublicacion())
                .setParameter("idPerfil", colaborador.getIdPerfil())
                .executeUpdate();
        em.flush();
        em.clear();

        PublicacionResponse response = obtenerPublicacionService.obtener(
                escenario.idUsuario(), escenario.idPerfil(), publicacion.getIdPublicacion());

        assertThat(response.colaboradores()).singleElement()
                .satisfies(autor -> assertThat(autor.idPerfil()).isEqualTo(colaborador.getIdPerfil()));
    }

    @Test
    void obtener_publicacionDeOtroPerfil_lanzaNoEncontrada() {
        Escenario escenario = crearEscenario();
        Publicacion publicacion = crearPublicacion(escenario.perfil(), null, 1);
        Perfil otroPerfil = crearPerfilColaborador(escenario.usuario());
        em.flush();

        assertThatThrownBy(() -> obtenerPublicacionService.obtener(
                escenario.idUsuario(), otroPerfil.getIdPerfil(), publicacion.getIdPublicacion()))
                .isInstanceOf(PublicacionNoEncontradaException.class);
    }

    private ModificarPublicacionRequest request(
            String titulo, String descripcion, List<Long> imagenesConservadas) {
        ModificarPublicacionRequest request = new ModificarPublicacionRequest();
        request.setTitulo(titulo);
        request.setDescripcion(descripcion);
        request.setImagenesConservadas(imagenesConservadas);
        request.setArchivos(List.of());
        return request;
    }

    private List<Long> ids(Publicacion publicacion) {
        return publicacion.getImagenes().stream().map(Imagen::getIdImagen).toList();
    }

    private Escenario crearEscenario() {
        RolGlobal rol = rolGlobalRepository.findByNombre("Usuario").orElseThrow();
        Genero genero = generoRepository.findByCodigo("mujer").orElseThrow();

        Usuario usuario = usuarioRepository.saveAndFlush(Usuario.builder()
                .nombre("Ana")
                .apellido("Gomez")
                .dni("60606001")
                .fechaNacimiento(LocalDate.now().minusYears(22))
                .correo("uc21.modificar@example.com")
                .rolGlobal(rol)
                .genero(genero)
                .build());

        Profesion profesion = Profesion.builder()
                .codigo("PROF_UC21")
                .nombre("Profesion UC21")
                .build();
        em.persist(profesion);

        Perfil perfil = Perfil.builder()
                .nombreArtistico("Luna UC21")
                .biografia("Perfil de test UC21")
                .estado(EstadoPerfil.Activo)
                .usuario(usuario)
                .profesion(profesion)
                .build();
        em.persist(perfil);

        Proyecto proyecto = crearProyecto(perfil, "Proyecto UC21");
        em.flush();

        return new Escenario(usuario, perfil, proyecto);
    }

    private Proyecto crearProyecto(Perfil perfil, String nombre) {
        Proyecto proyecto = Proyecto.builder()
                .nombre(nombre)
                .descripcion("Proyecto de test UC21")
                .estado(EstadoProyecto.CONFIRMADO)
                .privacidad(Privacidad.Publico)
                .fechaInicio(LocalDateTime.now().minusDays(1))
                .build();
        em.persist(proyecto);

        RolProyecto rolDirector = em.createQuery(
                "SELECT r FROM RolProyecto r WHERE r.codigo = 'DIRECTOR'", RolProyecto.class)
                .getSingleResult();

        em.persist(MiembroProyecto.builder()
                .proyecto(proyecto)
                .perfil(perfil)
                .rolProyecto(rolDirector)
                .estadoParticipacion(EstadoParticipacion.Activo)
                .build());

        return proyecto;
    }

    private Proyecto crearProyectoSinMembresia(String nombre) {
        Proyecto proyecto = Proyecto.builder()
                .nombre(nombre)
                .descripcion("Proyecto de test UC21")
                .estado(EstadoProyecto.CONFIRMADO)
                .privacidad(Privacidad.Privado)
                .fechaInicio(LocalDateTime.now().minusDays(1))
                .build();
        em.persist(proyecto);
        return proyecto;
    }

    private Perfil crearPerfilColaborador(Usuario usuario) {
        Profesion profesion = Profesion.builder()
                .codigo("PROF_UC21_COLAB")
                .nombre("Profesion colaborador UC21")
                .build();
        em.persist(profesion);

        Perfil perfil = Perfil.builder()
                .nombreArtistico("Sol UC21")
                .biografia("Perfil colaborador UC21")
                .estado(EstadoPerfil.Activo)
                .usuario(usuario)
                .profesion(profesion)
                .build();
        em.persist(perfil);
        return perfil;
    }

    private Publicacion crearPublicacion(Perfil perfil, Proyecto proyecto, int cantidadImagenes) {
        Publicacion publicacion = Publicacion.builder()
                .titulo("Editorial Otoño")
                .descripcion("Descripción original")
                .fechaPublicacion(LocalDateTime.now())
                .perfil(perfil)
                .proyecto(proyecto)
                .imagenes(new ArrayList<>())
                .build();
        em.persist(publicacion);

        for (int i = 0; i < cantidadImagenes; i++) {
            Imagen imagen = Imagen.builder()
                    .nombreArchivo("uc21_" + i + "_" + System.nanoTime() + ".webp")
                    .url("/uploads/publicaciones/uc21_" + i + ".webp")
                    .tipoImagen("image/webp")
                    .tamanoBytes(100)
                    .build();
            em.persist(imagen);
            publicacion.getImagenes().add(imagen);
        }

        return publicacion;
    }

    private record Escenario(Usuario usuario, Perfil perfil, Proyecto proyecto) {

        Long idUsuario() {
            return usuario.getIdUsuario();
        }

        Long idPerfil() {
            return perfil.getIdPerfil();
        }
    }
}
