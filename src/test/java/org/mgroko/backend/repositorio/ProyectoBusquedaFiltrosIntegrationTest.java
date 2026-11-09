package org.mgroko.backend.repositorio;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.Ciudad;
import org.mgroko.backend.modelo.Pais;
import org.mgroko.backend.modelo.Planificacion;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.Provincia;
import org.mgroko.backend.modelo.RequerimientoActividad;
import org.mgroko.backend.modelo.RequerimientoGralProyecto;
import org.mgroko.backend.modelo.Ubicacion;
import org.mgroko.backend.modelo.enums.EstadoActividad;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
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
 * Integración de los filtros de profesión y ubicación de la búsqueda global
 * de proyectos (UC-29).
 *
 * <p>Profesión: el proyecto necesita esa profesión si tiene un requerimiento
 * general abierto o un requerimiento de actividad no archivado en una
 * actividad Pendiente o En curso.</p>
 *
 * <p>Ubicación: se filtra por los ids externos Georef de la provincia y la
 * ciudad del proyecto.</p>
 */
@SpringBootTest
@Transactional
class ProyectoBusquedaFiltrosIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final Long PERFIL_INEXISTENTE = 999999L;

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private ProyectoRepository proyectoRepository;

    @Test
    void filtroProfesion_incluyeGeneralesYActividadesPendientesOEnCurso() {
        Profesion profesion = crearProfesion("FILTRO_A");
        Profesion otraProfesion = crearProfesion("FILTRO_B");

        Proyecto conRequerimientoGeneral = crearProyecto("Proy general A", null);
        crearRequerimientoGeneral(conRequerimientoGeneral, profesion, null);

        Proyecto conActividadEnCurso = crearProyecto("Proy actividad en curso A", null);
        crearRequerimientoActividad(conActividadEnCurso, profesion, EstadoActividad.EN_CURSO);

        Proyecto conActividadPendiente = crearProyecto("Proy actividad pendiente A", null);
        crearRequerimientoActividad(conActividadPendiente, profesion, EstadoActividad.PENDIENTE);

        Proyecto conActividadFinalizada = crearProyecto("Proy actividad finalizada A", null);
        crearRequerimientoActividad(conActividadFinalizada, profesion, EstadoActividad.FINALIZADA);

        Proyecto conRequerimientoArchivado = crearProyecto("Proy archivado A", null);
        crearRequerimientoGeneral(conRequerimientoArchivado, profesion, LocalDateTime.now());

        Proyecto conOtraProfesion = crearProyecto("Proy otra profesion B", null);
        crearRequerimientoGeneral(conOtraProfesion, otraProfesion, null);

        em.flush();

        List<String> nombres = nombresVisibles(new BuscarProyectosFiltro(
                null, null, null, profesion.getIdProfesion(), null, null));

        assertThat(nombres).containsExactlyInAnyOrder(
                conRequerimientoGeneral.getNombre(),
                conActividadEnCurso.getNombre(),
                conActividadPendiente.getNombre());
        assertThat(nombres).doesNotContain(
                conActividadFinalizada.getNombre(),
                conRequerimientoArchivado.getNombre(),
                conOtraProfesion.getNombre());
    }

    @Test
    void filtroUbicacion_porProvinciaYLocalidad() {
        Pais pais = paisArgentina();

        Provincia provinciaUno = crearProvincia("TEST_P1", "Provincia Test Uno", pais);
        Ciudad ciudadUno = crearCiudad("TEST_C1", "Ciudad Test Uno", provinciaUno);

        Provincia provinciaDos = crearProvincia("TEST_P2", "Provincia Test Dos", pais);
        Ciudad ciudadDos = crearCiudad("TEST_C2", "Ciudad Test Dos", provinciaDos);

        Proyecto proyectoUno = crearProyecto("Proy ubicado uno", crearUbicacion(ciudadUno));
        Proyecto proyectoDos = crearProyecto("Proy ubicado dos", crearUbicacion(ciudadDos));
        Proyecto sinUbicacion = crearProyecto("Proy sin ubicacion", null);

        em.flush();

        List<String> porProvincia = nombresVisibles(new BuscarProyectosFiltro(
                null, null, null, null, provinciaUno.getIdExterno(), null));

        assertThat(porProvincia)
                .contains(proyectoUno.getNombre())
                .doesNotContain(proyectoDos.getNombre(), sinUbicacion.getNombre());

        List<String> porLocalidad = nombresVisibles(new BuscarProyectosFiltro(
                null, null, null, null, null, ciudadDos.getIdExterno()));

        assertThat(porLocalidad)
                .contains(proyectoDos.getNombre())
                .doesNotContain(proyectoUno.getNombre(), sinUbicacion.getNombre());

        List<String> porProvinciaYLocalidad = nombresVisibles(new BuscarProyectosFiltro(
                null, null, null, null,
                provinciaUno.getIdExterno(), ciudadUno.getIdExterno()));

        assertThat(porProvinciaYLocalidad).containsExactly(proyectoUno.getNombre());

        List<String> combinacionIncoherente = nombresVisibles(new BuscarProyectosFiltro(
                null, null, null, null,
                provinciaUno.getIdExterno(), ciudadDos.getIdExterno()));

        assertThat(combinacionIncoherente).isEmpty();
    }

    private List<String> nombresVisibles(BuscarProyectosFiltro filtro) {
        Page<Proyecto> resultados = proyectoRepository.findAll(
                ProyectoSpecifications.conFiltros(filtro, PERFIL_INEXISTENTE),
                PageRequest.of(0, 50, Sort.by(Sort.Direction.ASC, "nombre")));
        return resultados.map(Proyecto::getNombre).getContent();
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

    private Proyecto crearProyecto(String nombre, Ubicacion ubicacion) {
        Proyecto proyecto = Proyecto.builder()
                .nombre(nombre)
                .descripcion("Descripcion de test")
                .fechaInicio(LocalDate.now())
                .estado(EstadoProyecto.PUBLICADO)
                .privacidad(Privacidad.PUBLICO)
                .aceptaPostulacionGral(true)
                .ubicacion(ubicacion)
                .build();
        em.persist(proyecto);
        return proyecto;
    }

    private Profesion crearProfesion(String codigo) {
        Profesion profesion = Profesion.builder()
                .codigo(codigo)
                .nombre(codigo + " nombre")
                .build();
        em.persist(profesion);
        return profesion;
    }

    private RequerimientoGralProyecto crearRequerimientoGeneral(
            Proyecto proyecto,
            Profesion profesion,
            LocalDateTime fechaArchivado) {

        RequerimientoGralProyecto requerimiento = RequerimientoGralProyecto.builder()
                .cantidad(1)
                .descripcion("Requerimiento de prueba")
                .proyecto(proyecto)
                .profesion(profesion)
                .fechaArchivado(fechaArchivado)
                .build();
        em.persist(requerimiento);
        return requerimiento;
    }

    private RequerimientoActividad crearRequerimientoActividad(
            Proyecto proyecto,
            Profesion profesion,
            EstadoActividad estadoActividad) {

        Planificacion planificacion = Planificacion.builder()
                .proyecto(proyecto)
                .fechaEntrega(LocalDate.now().plusDays(30))
                .build();
        em.persist(planificacion);

        Actividad actividad = Actividad.builder()
                .nombre("Actividad de prueba")
                .descripcion("Actividad de prueba")
                .duracionMinutos(120)
                .estado(estadoActividad)
                .planificacion(planificacion)
                .build();
        em.persist(actividad);

        RequerimientoActividad requerimiento = RequerimientoActividad.builder()
                .cantidad(1)
                .descripcion("Requerimiento de prueba")
                .actividad(actividad)
                .profesion(profesion)
                .build();
        em.persist(requerimiento);
        return requerimiento;
    }
}
