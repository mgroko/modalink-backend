package org.mgroko.backend.repositorio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.actividades.dto.ActividadResponse;
import org.mgroko.backend.actividades.dto.ModificarActividadRequest;
import org.mgroko.backend.actividades.dto.ModificarRequerimientoActividadRequest;
import org.mgroko.backend.actividades.enums.UnidadDuracion;
import org.mgroko.backend.actividades.exception.CicloActividadException;
import org.mgroko.backend.actividades.exception.RequerimientoConVinculosException;
import org.mgroko.backend.actividades.servicio.ModificarActividadService;
import org.mgroko.backend.actividades.servicio.ObtenerActividadService;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.AsignacionActividad;
import org.mgroko.backend.modelo.Ciudad;
import org.mgroko.backend.modelo.Genero;
import org.mgroko.backend.modelo.InvitacionActividad;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Pais;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.PerfilTyc;
import org.mgroko.backend.modelo.Planificacion;
import org.mgroko.backend.modelo.PostulacionActividad;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@SpringBootTest
@Transactional
class ModificarActividadIntegrationTest extends AbstractPostgresIntegrationTest {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private ModificarActividadService modificarActividadService;

    @Autowired
    private ObtenerActividadService obtenerActividadService;

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

    @Test
    void modificar_persisteDatosYLimpiaUbicacion() {
        Escenario escenario = crearEscenario();

        ModificarActividadRequest request = new ModificarActividadRequest(
                "Montaje final",
                "Descripcion actualizada",
                2,
                UnidadDuracion.HORAS,
                null,
                List.of(),
                List.of(new ModificarRequerimientoActividadRequest(
                        escenario.idRequerimiento(),
                        3,
                        escenario.idProfesion(),
                        "Requerimiento actualizado",
                        List.of(),
                        List.of())),
                EstadoActividad.EN_CURSO);

        ActividadResponse response = modificarActividadService.modificar(
                escenario.idUsuario(),
                escenario.idPerfil(),
                escenario.idProyecto(),
                escenario.idActividad(),
                request);

        assertThat(response.nombre()).isEqualTo("Montaje final");
        assertThat(response.descripcion()).isEqualTo("Descripcion actualizada");
        assertThat(response.duracionMinutos()).isEqualTo(120);
        assertThat(response.estado()).isEqualTo("EN_CURSO");
        assertThat(response.estadoCronograma()).isEqualTo("NORMAL");
        assertThat(response.ubicacion()).isNull();
        assertThat(response.requerimientos()).singleElement().satisfies(requerimiento -> {
            assertThat(requerimiento.cantidad()).isEqualTo(3);
            assertThat(requerimiento.descripcion()).isEqualTo("Requerimiento actualizado");
        });

        em.flush();
        em.clear();

        Actividad actualizada = em.find(Actividad.class, escenario.idActividad());
        assertThat(actualizada.getNombre()).isEqualTo("Montaje final");
        assertThat(actualizada.getDescripcion()).isEqualTo("Descripcion actualizada");
        assertThat(actualizada.getDuracionMinutos()).isEqualTo(120);
        assertThat(actualizada.getEstado()).isEqualTo(EstadoActividad.EN_CURSO);
        assertThat(actualizada.getUbicacion()).isNull();

        RequerimientoActividad requerimiento = em.find(
                RequerimientoActividad.class, escenario.idRequerimiento());
        assertThat(requerimiento.getCantidad()).isEqualTo(3);
        assertThat(requerimiento.getDescripcion()).isEqualTo("Requerimiento actualizado");
    }

    @Test
    void modificar_superaFechaEntrega_persisteCambiosYDevuelveAlerta() {
        Escenario escenario = crearEscenario();

        ModificarActividadRequest request = new ModificarActividadRequest(
                "Montaje final",
                "Duracion que desborda la entrega",
                60,
                UnidadDuracion.DIAS,
                null,
                List.of(),
                List.of(new ModificarRequerimientoActividadRequest(
                        escenario.idRequerimiento(),
                        1,
                        escenario.idProfesion(),
                        "Requerimiento actualizado",
                        List.of(),
                        List.of())),
                EstadoActividad.PENDIENTE);

        ActividadResponse response = modificarActividadService.modificar(
                escenario.idUsuario(),
                escenario.idPerfil(),
                escenario.idProyecto(),
                escenario.idActividad(),
                request);

        assertThat(response.nombre()).isEqualTo("Montaje final");
        assertThat(response.estadoCronograma()).isEqualTo("ALERTA_DESBORDE");

        em.flush();
        em.clear();

        Actividad actualizada = em.find(Actividad.class, escenario.idActividad());
        assertThat(actualizada.getNombre()).isEqualTo("Montaje final");
        assertThat(actualizada.getDuracionMinutos()).isEqualTo(60 * 1440);

        ActividadResponse detalle = obtenerActividadService.obtener(
                escenario.idUsuario(),
                escenario.idPerfil(),
                escenario.idProyecto(),
                escenario.idActividad());
        assertThat(detalle.estadoCronograma()).isEqualTo("ALERTA_DESBORDE");
    }

    @Test
    void modificar_conCicloDePredecesoras_lanzaExcepcionYNoPersiste() {
        Escenario escenario = crearEscenario();
        Actividad actual = em.find(Actividad.class, escenario.idActividad());

        Actividad sucesora = Actividad.builder()
                .nombre("Sucesora")
                .descripcion("Depende de la actividad editada")
                .duracionMinutos(60)
                .fechaHoraInicio(LocalDateTime.now().plusDays(4))
                .estado(EstadoActividad.PENDIENTE)
                .planificacion(actual.getPlanificacion())
                .predecesoras(Set.of(actual))
                .build();
        em.persist(sucesora);
        em.flush();

        ModificarActividadRequest request = new ModificarActividadRequest(
                "Montaje final",
                null,
                2,
                UnidadDuracion.HORAS,
                null,
                List.of(sucesora.getIdActividad()),
                List.of(),
                EstadoActividad.PENDIENTE);

        assertThatThrownBy(() -> modificarActividadService.modificar(
                escenario.idUsuario(),
                escenario.idPerfil(),
                escenario.idProyecto(),
                escenario.idActividad(),
                request))
                .isInstanceOf(CicloActividadException.class);

        em.clear();
        Actividad recargada = em.find(Actividad.class, escenario.idActividad());
        assertThat(recargada.getPredecesoras()).isEmpty();
    }

    @Test
    void modificar_requerimientoConPostulacion_lanzaExcepcionYNoElimina() {
        Escenario escenario = crearEscenario();
        Perfil perfil = em.find(Perfil.class, escenario.idPerfil());
        RequerimientoActividad requerimiento = em.find(
                RequerimientoActividad.class, escenario.idRequerimiento());

        em.persist(PostulacionActividad.builder()
                .perfil(perfil)
                .requerimiento(requerimiento)
                .build());
        em.flush();

        ModificarActividadRequest request = new ModificarActividadRequest(
                "Montaje final",
                null,
                1,
                UnidadDuracion.HORAS,
                null,
                List.of(),
                List.of(),
                EstadoActividad.PENDIENTE);

        assertThatThrownBy(() -> modificarActividadService.modificar(
                escenario.idUsuario(),
                escenario.idPerfil(),
                escenario.idProyecto(),
                escenario.idActividad(),
                request))
                .isInstanceOf(RequerimientoConVinculosException.class)
                .hasMessageContaining("requerimiento " + escenario.idRequerimiento());

        em.clear();
        assertThat(em.find(RequerimientoActividad.class, escenario.idRequerimiento()))
                .isNotNull();
    }

    @Test
    void modificar_requerimientoConInvitacion_lanzaExcepcionYNoElimina() {
        Escenario escenario = crearEscenario();
        Perfil remitente = em.find(Perfil.class, escenario.idPerfil());
        RequerimientoActividad requerimiento = em.find(
                RequerimientoActividad.class, escenario.idRequerimiento());

        Profesion profesionDestinatario = Profesion.builder()
                .codigo("PROF_UC50_INV")
                .nombre("Profesion UC50 Invitacion")
                .build();
        em.persist(profesionDestinatario);

        Perfil destinatario = Perfil.builder()
                .nombreArtistico("Sol UC50")
                .biografia("Perfil destinatario UC50")
                .estado(EstadoPerfil.Activo)
                .usuario(remitente.getUsuario())
                .profesion(profesionDestinatario)
                .build();
        em.persist(destinatario);

        em.persist(InvitacionActividad.builder()
                .perfilRemitente(remitente)
                .perfilDestinatario(destinatario)
                .requerimiento(requerimiento)
                .build());
        em.flush();

        ModificarActividadRequest request = new ModificarActividadRequest(
                "Montaje final",
                null,
                1,
                UnidadDuracion.HORAS,
                null,
                List.of(),
                List.of(),
                EstadoActividad.PENDIENTE);

        assertThatThrownBy(() -> modificarActividadService.modificar(
                escenario.idUsuario(),
                escenario.idPerfil(),
                escenario.idProyecto(),
                escenario.idActividad(),
                request))
                .isInstanceOf(RequerimientoConVinculosException.class)
                .hasMessageContaining("requerimiento " + escenario.idRequerimiento());

        em.clear();
        assertThat(em.find(RequerimientoActividad.class, escenario.idRequerimiento()))
                .isNotNull();
    }

    @Test
    void obtener_devuelveDetalleCompletoConProfesionalesAsignados() {
        Escenario escenario = crearEscenario();

        ActividadResponse response = obtenerActividadService.obtener(
                escenario.idUsuario(),
                escenario.idPerfil(),
                escenario.idProyecto(),
                escenario.idActividad());

        assertThat(response.idActividad()).isEqualTo(escenario.idActividad());
        assertThat(response.ubicacion()).isNotNull();
        assertThat(response.requerimientos()).singleElement().satisfies(requerimiento -> {
            assertThat(requerimiento.idRequerimiento()).isEqualTo(escenario.idRequerimiento());
            assertThat(requerimiento.cantidad()).isEqualTo(1);
        });
        assertThat(response.profesionalesAsignados()).singleElement().satisfies(profesional -> {
            assertThat(profesional.idPerfil()).isEqualTo(escenario.idPerfil());
            assertThat(profesional.nombreArtistico()).isEqualTo("Luna UC50");
            assertThat(profesional.nombreProfesion()).isEqualTo("Profesion UC50");
        });
    }

    private Escenario crearEscenario() {
        RolGlobal rol = rolGlobalRepository.findByNombre("Usuario").orElseThrow();
        Genero genero = generoRepository.findByCodigo("mujer").orElseThrow();

        Usuario usuario = usuarioRepository.saveAndFlush(Usuario.builder()
                .nombre("Ana")
                .apellido("Gomez")
                .dni("50505001")
                .fechaNacimiento(LocalDate.now().minusYears(22))
                .correo("uc50.modificar@example.com")
                .rolGlobal(rol)
                .genero(genero)
                .build());

        Profesion profesion = Profesion.builder()
                .codigo("PROF_UC50")
                .nombre("Profesion UC50")
                .build();
        em.persist(profesion);

        Perfil perfil = Perfil.builder()
                .nombreArtistico("Luna UC50")
                .biografia("Perfil de test UC50")
                .estado(EstadoPerfil.Activo)
                .usuario(usuario)
                .profesion(profesion)
                .build();
        em.persist(perfil);

        PerfilTyc tyc = PerfilTyc.builder()
                .id(new org.mgroko.backend.modelo.PerfilTycId(perfil.getIdPerfil(), 1L))
                .perfil(perfil)
                .descripcion("T&C Base")
                .fechaInicio(LocalDateTime.now())
                .build();
        em.persist(tyc);

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
                            .idExterno("uc50")
                            .fuenteApi("uc50")
                            .build();
                    em.persist(nueva);
                    return nueva;
                });

        Ciudad ciudad = Ciudad.builder()
                .nombre("CABA UC50")
                .activo(true)
                .provincia(provincia)
                .idExterno("uc50")
                .fuenteApi("uc50")
                .build();
        em.persist(ciudad);

        Ubicacion ubicacion = Ubicacion.builder().ciudad(ciudad).build();
        em.persist(ubicacion);

        Proyecto proyecto = Proyecto.builder()
                .nombre("Proyecto UC50")
                .descripcion("Proyecto de test UC50")
                .fechaInicio(LocalDate.now())
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
                actividad.getIdActividad(),
                requerimiento.getIdRequerimiento(),
                profesion.getIdProfesion());
    }

    private record Escenario(
            Long idUsuario,
            Long idPerfil,
            Long idProyecto,
            Long idActividad,
            Long idRequerimiento,
            Long idProfesion) {
    }
}
