package org.mgroko.backend.repositorio;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.home.servicio.HomeOportunidadesService;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.CaracteristicaTecnica;
import org.mgroko.backend.modelo.Ciudad;
import org.mgroko.backend.modelo.Genero;
import org.mgroko.backend.modelo.Habilidad;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Pais;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.PerfilTyc;
import org.mgroko.backend.modelo.PerfilTycId;
import org.mgroko.backend.modelo.Planificacion;
import org.mgroko.backend.modelo.PostulacionGral;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.Provincia;
import org.mgroko.backend.modelo.RequerimientoGralCaract;
import org.mgroko.backend.modelo.RequerimientoActividad;
import org.mgroko.backend.modelo.RequerimientoGralProyecto;
import org.mgroko.backend.modelo.RolGlobal;
import org.mgroko.backend.modelo.RolProyecto;
import org.mgroko.backend.modelo.Ubicacion;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoActividad;
import org.mgroko.backend.modelo.enums.EstadoParticipacion;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.EstadoSolicitud;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.modelo.enums.Privacidad;
import org.mgroko.backend.requerimientos.dto.BuscarRequerimientosGlobalFiltro;
import org.mgroko.backend.requerimientos.dto.OportunidadesHomeResponse;
import org.mgroko.backend.requerimientos.dto.RequerimientoOportunidadResponse;
import org.mgroko.backend.requerimientos.dto.TipoRequerimiento;
import org.mgroko.backend.requerimientos.servicio.BuscarRequerimientosActividadGlobalService;
import org.mgroko.backend.requerimientos.servicio.BuscarRequerimientosGeneralesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * UC-72/UC-73 contra PostgreSQL real: Specifications globales, visibilidad,
 * cupo, prioridad por profesión, postulaciones previas y bloque de Home.
 */
@SpringBootTest
@Transactional
class BuscarRequerimientoOportunidadIntegrationTest extends AbstractPostgresIntegrationTest {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolGlobalRepository rolGlobalRepository;

    @Autowired
    private GeneroRepository generoRepository;

    @Autowired
    private BuscarRequerimientosGeneralesService buscarGeneralesService;

    @Autowired
    private BuscarRequerimientosActividadGlobalService buscarActividadService;

    @Autowired
    private HomeOportunidadesService homeOportunidadesService;

    @Test
    void generales_priorizaProfesionYRespetaVisibilidad() {
        Perfil perfil = crearPerfil("A", "99400001", "Modelo");
        Profesion otraProfesion = crearProfesion("OTRA_A", "Fotografo");

        Proyecto publicoPropio = crearProyecto(
                "Publico propio A", EstadoProyecto.PUBLICADO, Privacidad.PUBLICO, true);
        RequerimientoGralProyecto propio = crearRequerimientoGeneral(
                publicoPropio, perfil.getProfesion(), 2, "Dos modelos");

        Proyecto publicoAjeno = crearProyecto(
                "Publico ajeno A", EstadoProyecto.PUBLICADO, Privacidad.PUBLICO, true);
        RequerimientoGralProyecto ajeno = crearRequerimientoGeneral(
                publicoAjeno, otraProfesion, 1, "Un fotografo");

        Proyecto privado = crearProyecto(
                "Privado A", EstadoProyecto.PUBLICADO, Privacidad.PRIVADO, true);
        crearRequerimientoGeneral(privado, perfil.getProfesion(), 1, "Privado");

        em.flush();

        PaginaResponse<RequerimientoOportunidadResponse> todos =
                buscarGeneralesService.buscar(
                        perfil.getIdPerfil(), filtroVacio(), 0, 20, false);

        assertThat(todos.contenido())
                .extracting(RequerimientoOportunidadResponse::idRequerimiento)
                .containsExactly(propio.getIdRequerimientoGral(), ajeno.getIdRequerimientoGral());
        assertThat(todos.contenido().getFirst().puedePostularse()).isTrue();
        assertThat(todos.contenido().get(1).puedePostularse()).isFalse();
        assertThat(todos.contenido().get(1).motivoNoPostulable().name())
                .isEqualTo("PROFESION");

        PaginaResponse<RequerimientoOportunidadResponse> soloOtraProfesion =
                buscarGeneralesService.buscar(
                        perfil.getIdPerfil(),
                        new BuscarRequerimientosGlobalFiltro(
                                otraProfesion.getIdProfesion(), null, null, null,
                                null, null, null, null, null, null),
                        0, 20, false);

        assertThat(soloOtraProfesion.contenido())
                .extracting(RequerimientoOportunidadResponse::idRequerimiento)
                .containsExactly(ajeno.getIdRequerimientoGral());

        PaginaResponse<RequerimientoOportunidadResponse> soloPrivado =
                buscarGeneralesService.buscar(
                        perfil.getIdPerfil(),
                        new BuscarRequerimientosGlobalFiltro(
                                null, privado.getIdProyecto(), null, null,
                                null, null, null, null, null, null),
                        0, 20, false);

        assertThat(soloPrivado.contenido()).isEmpty();
    }

    @Test
    void actividades_abiertasYDirectorBloqueado() {
        Perfil perfil = crearPerfil("B", "99400002", "Modelo");
        Proyecto proyecto = crearProyecto(
                "Proyecto actividad B", EstadoProyecto.PUBLICADO, Privacidad.PUBLICO, false);
        Planificacion planificacion = crearPlanificacion(proyecto);
        Actividad actividad = crearActividad(planificacion, "Prueba de vestuario");
        RequerimientoActividad requerimiento = crearRequerimientoActividad(
                actividad, perfil.getProfesion(), 1, "Una modelo");

        em.flush();

        PaginaResponse<RequerimientoOportunidadResponse> abiertas =
                buscarActividadService.buscar(
                        perfil.getIdPerfil(), filtroVacio(), 0, 20, false);

        assertThat(abiertas.contenido()).hasSize(1);
        RequerimientoOportunidadResponse fila = abiertas.contenido().getFirst();
        assertThat(fila.tipo()).isEqualTo(TipoRequerimiento.ACTIVIDAD);
        assertThat(fila.idRequerimiento()).isEqualTo(requerimiento.getIdRequerimiento());
        assertThat(fila.puedePostularse()).isTrue();
        assertThat(fila.requiereVerificacionDisponibilidad()).isTrue();

        crearMiembro(proyecto, perfil, rolDirector(), EstadoParticipacion.ACTIVO);
        em.flush();

        PaginaResponse<RequerimientoOportunidadResponse> comoDirector =
                buscarActividadService.buscar(
                        perfil.getIdPerfil(), filtroVacio(), 0, 20, false);
        assertThat(comoDirector.contenido()).hasSize(1);
        assertThat(comoDirector.contenido().getFirst().puedePostularse()).isFalse();
        assertThat(comoDirector.contenido().getFirst().motivoNoPostulable().name())
                .isEqualTo("DIRECTOR");

        PaginaResponse<RequerimientoOportunidadResponse> soloPostulables =
                buscarActividadService.buscar(
                        perfil.getIdPerfil(),
                        new BuscarRequerimientosGlobalFiltro(
                                null, null, null, null, null, null, null,
                                null, Boolean.TRUE, null),
                        0, 20, false);
        assertThat(soloPostulables.contenido()).isEmpty();
    }

    @Test
    void postulacionPrevia_seMuestraYSePuedeExcluir() {
        Perfil perfil = crearPerfil("C", "99400003", "Modelo");
        Proyecto proyecto = crearProyecto(
                "Proyecto postulacion C", EstadoProyecto.PUBLICADO, Privacidad.PUBLICO, true);
        RequerimientoGralProyecto requerimiento = crearRequerimientoGeneral(
                proyecto, perfil.getProfesion(), 2, "Dos modelos");

        em.persist(PostulacionGral.builder()
                .perfil(perfil)
                .requerimientoGral(requerimiento)
                .estado(EstadoSolicitud.PENDIENTE)
                .build());
        em.flush();

        PaginaResponse<RequerimientoOportunidadResponse> conPostulacion =
                buscarGeneralesService.buscar(
                        perfil.getIdPerfil(), filtroVacio(), 0, 20, false);

        assertThat(conPostulacion.contenido()).hasSize(1);
        assertThat(conPostulacion.contenido().getFirst().yaPostulado()).isTrue();
        assertThat(conPostulacion.contenido().getFirst().estadoPostulacion())
                .isEqualTo(EstadoSolicitud.PENDIENTE);
        assertThat(conPostulacion.contenido().getFirst().puedePostularse()).isTrue();

        PaginaResponse<RequerimientoOportunidadResponse> sinPostulados =
                buscarGeneralesService.buscar(
                        perfil.getIdPerfil(),
                        new BuscarRequerimientosGlobalFiltro(
                                null, null, null, null, null, null, null,
                                null, null, Boolean.TRUE),
                        0, 20, false);

        assertThat(sinPostulados.contenido()).isEmpty();
    }

    @Test
    void generales_filtraPorCaracteristicaYHabilidad() {
        Perfil perfil = crearPerfil("E", "99400005", "Modelo");
        Proyecto proyecto = crearProyecto(
                "Proyecto filtros E", EstadoProyecto.PUBLICADO, Privacidad.PUBLICO, true);
        RequerimientoGralProyecto requerimiento = crearRequerimientoGeneral(
                proyecto, perfil.getProfesion(), 2, "Dos modelos");

        Habilidad habilidad = Habilidad.builder()
                .codigo("HAB_E")
                .nombre("Costura")
                .build();
        em.persist(habilidad);

        CaracteristicaTecnica caracteristica = CaracteristicaTecnica.builder()
                .codigo("CAR_E")
                .nombre("Altura")
                .tipoDato(CaracteristicaTecnica.TIPO_NUMERICO)
                .profesion(perfil.getProfesion())
                .build();
        em.persist(caracteristica);

        em.persist(RequerimientoGralCaract.builder()
                .requerimientoGral(requerimiento)
                .caracteristica(caracteristica)
                .valorMin(BigDecimal.ZERO)
                .valorMax(BigDecimal.TEN)
                .build());

        requerimiento.getHabilidades().add(habilidad);
        em.flush();

        PaginaResponse<RequerimientoOportunidadResponse> porCaracteristica =
                buscarGeneralesService.buscar(
                        perfil.getIdPerfil(),
                        new BuscarRequerimientosGlobalFiltro(
                                null, null, null, null,
                                caracteristica.getIdCaracteristica(), null, null,
                                null, null, null),
                        0, 20, false);

        assertThat(porCaracteristica.contenido()).hasSize(1);

        PaginaResponse<RequerimientoOportunidadResponse> porHabilidad =
                buscarGeneralesService.buscar(
                        perfil.getIdPerfil(),
                        new BuscarRequerimientosGlobalFiltro(
                                null, null, null, null,
                                null, null, List.of(habilidad.getIdHabilidad()),
                                null, null, null),
                        0, 20, false);

        assertThat(porHabilidad.contenido()).hasSize(1);
    }

    @Test
    void home_incluyeSoloOportunidadesAccionablesYCuentaTotales() {
        Perfil perfil = crearPerfil("D", "99400004", "Modelo");
        Profesion otraProfesion = crearProfesion("OTRA_D", "Fotografo");

        Proyecto general = crearProyecto(
                "Proyecto home general D", EstadoProyecto.PUBLICADO, Privacidad.PUBLICO, true);
        RequerimientoGralProyecto requerimientoGeneral = crearRequerimientoGeneral(
                general, perfil.getProfesion(), 2, "Dos modelos");
        em.persist(PostulacionGral.builder()
                .perfil(perfil)
                .requerimientoGral(requerimientoGeneral)
                .estado(EstadoSolicitud.PENDIENTE)
                .build());

        Proyecto generalAjeno = crearProyecto(
                "Proyecto home ajeno D", EstadoProyecto.PUBLICADO, Privacidad.PUBLICO, true);
        crearRequerimientoGeneral(generalAjeno, otraProfesion, 1, "Un fotografo");

        Proyecto actividadProyecto = crearProyecto(
                "Proyecto home actividad D", EstadoProyecto.PUBLICADO, Privacidad.PUBLICO, false);
        Planificacion planificacion = crearPlanificacion(actividadProyecto);
        Actividad actividad = crearActividad(planificacion, "Casting D");
        crearRequerimientoActividad(actividad, perfil.getProfesion(), 1, "Una modelo");

        em.flush();

        OportunidadesHomeResponse home = homeOportunidadesService.obtener(
                perfil.getIdPerfil(), 5);

        assertThat(home.oportunidades()).hasSize(2);
        assertThat(home.totalGenerales()).isEqualTo(1);
        assertThat(home.totalActividades()).isEqualTo(1);
        assertThat(home.oportunidades())
                .extracting(RequerimientoOportunidadResponse::tipo)
                .containsExactly(TipoRequerimiento.ACTIVIDAD, TipoRequerimiento.GENERAL);
        assertThat(home.oportunidades().get(1).yaPostulado()).isTrue();
        assertThat(home.oportunidades().get(0).yaPostulado()).isFalse();
    }

    @Test
    void filtraGeneralesYActividadesPorUbicacionDelProyecto() {
        Perfil perfil = crearPerfil("UBI", "99400020", "Modelo");
        Pais pais = paisArgentina();
        Provincia provincia = crearProvincia("UBI_P", "Provincia Ubi", pais);
        Ciudad ciudad = crearCiudad("UBI_C", "Ciudad Ubi", provincia);
        Ubicacion ubicacion = crearUbicacion(ciudad);

        Proyecto conUbicacion = crearProyecto(
                "Con ubicacion UBI", EstadoProyecto.PUBLICADO, Privacidad.PUBLICO, true);
        conUbicacion.setUbicacion(ubicacion);
        RequerimientoGralProyecto generalConUbicacion = crearRequerimientoGeneral(
                conUbicacion, perfil.getProfesion(), 1, "General UBI");
        Planificacion planificacionConUbicacion = crearPlanificacion(conUbicacion);
        Actividad actividadConUbicacion = crearActividad(
                planificacionConUbicacion, "Actividad UBI");
        RequerimientoActividad requerimientoActividadConUbicacion =
                crearRequerimientoActividad(
                        actividadConUbicacion, perfil.getProfesion(), 1, "Actividad UBI");

        Proyecto sinUbicacion = crearProyecto(
                "Sin ubicacion UBI", EstadoProyecto.PUBLICADO, Privacidad.PUBLICO, true);
        crearRequerimientoGeneral(
                sinUbicacion, perfil.getProfesion(), 1, "General sin UBI");
        Planificacion planificacionSinUbicacion = crearPlanificacion(sinUbicacion);
        Actividad actividadSinUbicacion = crearActividad(
                planificacionSinUbicacion, "Actividad sin UBI");
        crearRequerimientoActividad(
                actividadSinUbicacion, perfil.getProfesion(), 1, "Actividad sin UBI");

        em.flush();

        BuscarRequerimientosGlobalFiltro filtro = new BuscarRequerimientosGlobalFiltro(
                null, null, null, null, null, null, null, null, null, null,
                provincia.getIdExterno(), null);

        PaginaResponse<RequerimientoOportunidadResponse> generales =
                buscarGeneralesService.buscar(
                        perfil.getIdPerfil(), filtro, 0, 20, false);

        assertThat(generales.contenido())
                .extracting(RequerimientoOportunidadResponse::idRequerimiento)
                .containsExactly(generalConUbicacion.getIdRequerimientoGral());
        assertThat(generales.contenido().getFirst().ciudad()).isNotNull();
        assertThat(generales.contenido().getFirst().ciudad().nombre())
                .isEqualTo("Ciudad Ubi");

        PaginaResponse<RequerimientoOportunidadResponse> actividades =
                buscarActividadService.buscar(
                        perfil.getIdPerfil(), filtro, 0, 20, false);

        assertThat(actividades.contenido())
                .extracting(RequerimientoOportunidadResponse::idRequerimiento)
                .containsExactly(requerimientoActividadConUbicacion.getIdRequerimiento());
        assertThat(actividades.contenido().getFirst().ciudad()).isNotNull();
        assertThat(actividades.contenido().getFirst().ciudad().nombre())
                .isEqualTo("Ciudad Ubi");
    }

    private Pais paisArgentina() {
        return em.createQuery("SELECT p FROM Pais p WHERE p.codigoIso = 'AR'", Pais.class)
                .getSingleResult();
    }

    private Provincia crearProvincia(String idExterno, String nombre, Pais pais) {
        Provincia provincia = Provincia.builder()
                .nombre(nombre)
                .idExterno(idExterno)
                .fuenteApi("TEST")
                .activo(true)
                .pais(pais)
                .build();
        em.persist(provincia);
        return provincia;
    }

    private Ciudad crearCiudad(String idExterno, String nombre, Provincia provincia) {
        Ciudad ciudad = Ciudad.builder()
                .nombre(nombre)
                .idExterno(idExterno)
                .fuenteApi("TEST")
                .activo(true)
                .provincia(provincia)
                .build();
        em.persist(ciudad);
        return ciudad;
    }

    private Ubicacion crearUbicacion(Ciudad ciudad) {
        Ubicacion ubicacion = Ubicacion.builder()
                .ciudad(ciudad)
                .build();
        em.persist(ubicacion);
        return ubicacion;
    }

    private BuscarRequerimientosGlobalFiltro filtroVacio() {
        return new BuscarRequerimientosGlobalFiltro(
                null, null, null, null, null, null, null, null, null, null);
    }

    private Perfil crearPerfil(String sufijo, String dni, String nombreProfesion) {
        RolGlobal rol = rolGlobalRepository.findByNombre("Usuario").orElseThrow();
        Genero genero = generoRepository.findByCodigo("mujer").orElseThrow();

        Usuario usuario = usuarioRepository.saveAndFlush(Usuario.builder()
                .nombre("Busqueda").apellido("Oportunidad")
                .dni(dni)
                .fechaNacimiento(LocalDate.now().minusYears(25))
                .correo("busqop" + sufijo + "@example.com")
                .estado(EstadoUsuario.Activo)
                .rolGlobal(rol).genero(genero).build());

        Profesion profesion = crearProfesion("PERFIL_" + sufijo, nombreProfesion);

        Perfil perfil = Perfil.builder()
                .nombreArtistico("Perfil Oportunidad " + sufijo)
                .biografia("Perfil de prueba")
                .estado(EstadoPerfil.Activo)
                .usuario(usuario)
                .profesion(profesion)
                .build();
        em.persist(perfil);

        em.persist(PerfilTyc.builder()
                .id(new PerfilTycId(perfil.getIdPerfil(), 1L))
                .perfil(perfil)
                .descripcion("T&C Base")
                .fechaInicio(LocalDateTime.now())
                .build());

        return perfil;
    }

    private Profesion crearProfesion(String sufijo, String nombre) {
        Profesion profesion = Profesion.builder()
                .codigo("PROF_OP_" + sufijo)
                .nombre(nombre + " " + sufijo)
                .build();
        em.persist(profesion);
        return profesion;
    }

    private Proyecto crearProyecto(
            String nombre,
            EstadoProyecto estado,
            Privacidad privacidad,
            boolean aceptaGeneral) {

        Proyecto proyecto = Proyecto.builder()
                .nombre(nombre)
                .descripcion("Descripcion de test")
                .fechaInicio(LocalDate.now().plusDays(2))
                .estado(estado)
                .privacidad(privacidad)
                .aceptaPostulacionGral(aceptaGeneral)
                .build();
        em.persist(proyecto);
        return proyecto;
    }

    private RequerimientoGralProyecto crearRequerimientoGeneral(
            Proyecto proyecto,
            Profesion profesion,
            int cantidad,
            String descripcion) {

        RequerimientoGralProyecto requerimiento = RequerimientoGralProyecto.builder()
                .cantidad(cantidad)
                .descripcion(descripcion)
                .proyecto(proyecto)
                .profesion(profesion)
                .build();
        em.persist(requerimiento);
        return requerimiento;
    }

    private Planificacion crearPlanificacion(Proyecto proyecto) {
        Planificacion planificacion = Planificacion.builder()
                .proyecto(proyecto)
                .fechaEntrega(LocalDate.now().plusDays(30))
                .build();
        em.persist(planificacion);
        return planificacion;
    }

    private Actividad crearActividad(Planificacion planificacion, String nombre) {
        Actividad actividad = Actividad.builder()
                .nombre(nombre)
                .descripcion("Actividad de prueba")
                .duracionMinutos(120)
                .fechaHoraInicio(LocalDateTime.now().plusDays(5))
                .estado(EstadoActividad.PENDIENTE)
                .planificacion(planificacion)
                .build();
        em.persist(actividad);
        return actividad;
    }

    private RequerimientoActividad crearRequerimientoActividad(
            Actividad actividad,
            Profesion profesion,
            int cantidad,
            String descripcion) {

        RequerimientoActividad requerimiento = RequerimientoActividad.builder()
                .cantidad(cantidad)
                .descripcion(descripcion)
                .actividad(actividad)
                .profesion(profesion)
                .build();
        em.persist(requerimiento);
        return requerimiento;
    }

    private void crearMiembro(
            Proyecto proyecto,
            Perfil perfil,
            RolProyecto rol,
            EstadoParticipacion estado) {

        em.persist(MiembroProyecto.builder()
                .proyecto(proyecto)
                .perfil(perfil)
                .rolProyecto(rol)
                .idTycAceptado(null)
                .estadoParticipacion(estado)
                .build());
    }

    private RolProyecto rolDirector() {
        return em.createQuery("""
                SELECT r FROM RolProyecto r
                WHERE r.codigo = 'DIRECTOR' OR LOWER(r.nombre) LIKE 'director%'
                """, RolProyecto.class)
                .setMaxResults(1)
                .getSingleResult();
    }
}
