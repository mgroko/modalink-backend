package org.mgroko.backend.proyectos.servicio;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.modelo.Ciudad;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Objetivo;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Planificacion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RolProyecto;
import org.mgroko.backend.modelo.Ubicacion;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoParticipacion;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.Privacidad;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.perfiles.exception.PerfilNoEncontradoException;
import org.mgroko.backend.proyectos.dto.CrearObjetivoRequest;
import org.mgroko.backend.proyectos.dto.CrearProyectoRequest;
import org.mgroko.backend.proyectos.dto.ProyectoResponse;
import org.mgroko.backend.proyectos.exception.NombreProyectoDuplicadoException;
import org.mgroko.backend.proyectos.exception.RangoFechasProyectoInvalidoException;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
import org.mgroko.backend.repositorio.ObjetivoRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.PlanificacionRepository;
import org.mgroko.backend.repositorio.ProyectoRepository;
import org.mgroko.backend.repositorio.RolProyectoRepository;
import org.mgroko.backend.ubicacion.dto.UbicacionRequest;
import org.mgroko.backend.ubicacion.servicio.UbicacionService;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CrearProyectoServiceTest {

    @Mock
    private ProyectoRepository proyectoRepository;

    @Mock
    private PerfilRepository perfilRepository;

    @Mock
    private RolProyectoRepository rolProyectoRepository;

    @Mock
    private MiembroProyectoRepository miembroProyectoRepository;

    @Mock
    private PlanificacionRepository planificacionRepository;

    @Mock
    private ObjetivoRepository objetivoRepository;

    @Mock
    private UbicacionService ubicacionService;

    @Mock
    private org.mgroko.backend.repositorio.ProfesionRepository profesionRepository;

    @Mock
    private org.mgroko.backend.repositorio.CaracteristicaTecnicaRepository caracteristicaTecnicaRepository;

    @Mock
    private org.mgroko.backend.repositorio.ValorCaracteristicaRepository valorCaracteristicaRepository;

    @Mock
    private org.mgroko.backend.repositorio.HabilidadRepository habilidadRepository;

    @Mock
    private org.mgroko.backend.repositorio.RequerimientoGralProyectoRepository requerimientoGralProyectoRepository;

    @Mock
    private org.mgroko.backend.repositorio.RequerimientoGralCaractValorRepository requerimientoGralCaractValorRepository;

    @Mock
    private org.mgroko.backend.repositorio.MoodboardRepository moodboardRepository;

    @InjectMocks
    private CrearProyectoService crearProyectoService;

    private Perfil perfil;
    private RolProyecto rolDirector;

    @BeforeEach
    void setUp() {
        Usuario usuario = Usuario.builder().idUsuario(1L).build();
        perfil = Perfil.builder()
                .idPerfil(10L)
                .nombreArtistico("Luna Diseños")
                .estado(EstadoPerfil.Activo)
                .usuario(usuario)
                .build();

        rolDirector = RolProyecto.builder()
                .idRolProyecto(1L)
                .nombre("Director")
                .build();
    }

    @Test
    void crear_datosValidos_creaProyectoConDirectorYPlanificacion() {
        CrearProyectoRequest request = new CrearProyectoRequest(
                "Desfile 2026",
                "Pasarela invierno",
                Privacidad.Publico,
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 15),
                true,
                new UbicacionRequest("12345", "06"),
                List.of(new CrearObjetivoRequest("Conseguir sponsors", "Contactar marcas")),
                null,
                null
        );

        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(10L, 1L)).thenReturn(Optional.of(perfil));
        when(proyectoRepository.existeProyectoConNombreParaPerfil("Desfile 2026", 10L, "Director", EstadoParticipacion.Activo))
                .thenReturn(false);
        when(rolProyectoRepository.findByNombre("Director", "Director")).thenReturn(Optional.of(rolDirector));

        Ubicacion ubicacion = Ubicacion.builder()
                .idUbicacion(50L)
                .ciudad(Ciudad.builder().nombre("La Plata").build())
                .build();
        when(ubicacionService.obtenerOCrear("12345", "06")).thenReturn(ubicacion);

        when(proyectoRepository.save(any(Proyecto.class))).thenAnswer(invocation -> {
            Proyecto p = invocation.getArgument(0);
            p.setIdProyecto(100L);
            return p;
        });

        when(objetivoRepository.save(any(Objetivo.class))).thenAnswer(invocation -> {
            Objetivo obj = invocation.getArgument(0);
            obj.setIdObjetivo(1L);
            return obj;
        });

        ProyectoResponse response = crearProyectoService.crear(1L, 10L, request);

        assertThat(response).isNotNull();
        assertThat(response.idProyecto()).isEqualTo(100L);
        assertThat(response.nombre()).isEqualTo("Desfile 2026");
        assertThat(response.estado()).isEqualTo("Borrador");
        assertThat(response.idDirector()).isEqualTo(10L);
        assertThat(response.nombreDirector()).isEqualTo("Luna Diseños");
        assertThat(response.fechaFinEstipulada()).isEqualTo(LocalDate.of(2026, 6, 15));
        assertThat(response.objetivos()).hasSize(1);
        assertThat(response.objetivos().get(0).nombre()).isEqualTo("Conseguir sponsors");

        verify(miembroProyectoRepository).save(any(MiembroProyecto.class));
        verify(planificacionRepository).save(any(Planificacion.class));
    }

    @Test
    void crear_sinPerfilActivo_lanzaExcepcion() {
        CrearProyectoRequest request = new CrearProyectoRequest(
                "Proyecto", "Desc", Privacidad.Publico, LocalDate.now(), null, false, null, null, null, null
        );

        assertThatThrownBy(() -> crearProyectoService.crear(1L, null, request))
                .isInstanceOf(PerfilActivoNoSeleccionadoException.class);
    }

    @Test
    void crear_perfilNoExiste_lanzaExcepcion() {
        CrearProyectoRequest request = new CrearProyectoRequest(
                "Proyecto", "Desc", Privacidad.Publico, LocalDate.now(), null, false, null, null, null, null
        );
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> crearProyectoService.crear(1L, 99L, request))
                .isInstanceOf(PerfilNoEncontradoException.class);
    }

    @Test
    void crear_perfilEnBaja_lanzaExcepcion() {
        perfil.setEstado(EstadoPerfil.PendienteBaja);
        CrearProyectoRequest request = new CrearProyectoRequest(
                "Proyecto", "Desc", Privacidad.Publico, LocalDate.now(), null, false, null, null, null, null
        );
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(10L, 1L)).thenReturn(Optional.of(perfil));

        assertThatThrownBy(() -> crearProyectoService.crear(1L, 10L, request))
                .isInstanceOf(PerfilEnBajaException.class);
    }

    @Test
    void crear_fechaFinMenorAFechaInicio_lanzaExcepcion() {
        CrearProyectoRequest request = new CrearProyectoRequest(
                "Proyecto", "Desc", Privacidad.Publico,
                LocalDate.of(2026, 6, 10),
                LocalDate.of(2026, 6, 5),
                false, null, null, null, null
        );
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(10L, 1L)).thenReturn(Optional.of(perfil));

        assertThatThrownBy(() -> crearProyectoService.crear(1L, 10L, request))
                .isInstanceOf(RangoFechasProyectoInvalidoException.class)
                .hasMessageContaining("no puede ser anterior a la fecha de inicio");
    }

    @Test
    void crear_nombreDuplicadoParaMismoDirector_lanzaExcepcion() {
        CrearProyectoRequest request = new CrearProyectoRequest(
                "Proyecto Existente", "Desc", Privacidad.Publico, LocalDate.now(), null, false, null, null, null, null
        );
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(10L, 1L)).thenReturn(Optional.of(perfil));
        when(proyectoRepository.existeProyectoConNombreParaPerfil("Proyecto Existente", 10L, "Director", EstadoParticipacion.Activo))
                .thenReturn(true);

        assertThatThrownBy(() -> crearProyectoService.crear(1L, 10L, request))
                .isInstanceOf(NombreProyectoDuplicadoException.class)
                .hasMessageContaining("Ya tienes un proyecto con el nombre 'Proyecto Existente'");
    }

    // ---------------------------------------------------------------------
    // Requerimientos de personal y moodboard
    // ---------------------------------------------------------------------

    private void prepararCaminoFelizRequerimiento() {
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(10L, 1L)).thenReturn(Optional.of(perfil));
        when(proyectoRepository.existeProyectoConNombreParaPerfil(anyString(), eq(10L), eq("Director"), eq(EstadoParticipacion.Activo)))
                .thenReturn(false);
        when(rolProyectoRepository.findByNombre("Director", "Director")).thenReturn(Optional.of(rolDirector));
        when(proyectoRepository.save(any(Proyecto.class))).thenAnswer(invocation -> {
            Proyecto p = invocation.getArgument(0);
            p.setIdProyecto(100L);
            return p;
        });
    }

    @Test
    void crear_requerimientoNumericoConRangoValido_persiste() {
        prepararCaminoFelizRequerimiento();

        org.mgroko.backend.modelo.Profesion profesion = org.mgroko.backend.modelo.Profesion.builder()
                .idProfesion(2L).nombre("Modelo").build();
        when(profesionRepository.findById(2L)).thenReturn(Optional.of(profesion));

        org.mgroko.backend.modelo.Profesion profesionCaract = org.mgroko.backend.modelo.Profesion.builder()
                .idProfesion(2L).build();
        org.mgroko.backend.modelo.CaracteristicaTecnica caract = org.mgroko.backend.modelo.CaracteristicaTecnica.builder()
                .idCaracteristica(1L)
                .codigo("ALTURA")
                .tipoDato("NUMERICO")
                .profesion(profesionCaract)
                .build();
        when(caracteristicaTecnicaRepository.findById(1L)).thenReturn(Optional.of(caract));
        when(requerimientoGralProyectoRepository.save(any(org.mgroko.backend.modelo.RequerimientoGralProyecto.class)))
                .thenAnswer(invocation -> {
                    org.mgroko.backend.modelo.RequerimientoGralProyecto r = invocation.getArgument(0);
                    r.setIdRequerimientoGral(10L);
                    if (!r.getCaracteristicas().isEmpty()) {
                        r.getCaracteristicas().get(0).setIdReqGralCaract(55L);
                    }
                    return r;
                });

        CrearProyectoRequest request = new CrearProyectoRequest(
                "Desfile Rango", "Desc", Privacidad.Publico, LocalDate.now(), null, false, null, null,
                List.of(new org.mgroko.backend.proyectos.dto.CrearRequerimientoGralRequest(
                        2, 2L, "Dos modelos",
                        List.of(new org.mgroko.backend.proyectos.dto.CrearRequerimientoCaractRequest(
                                1L, java.math.BigDecimal.valueOf(150), java.math.BigDecimal.valueOf(190), null)),
                        null)),
                null
        );

        ProyectoResponse response = crearProyectoService.crear(1L, 10L, request);

        assertThat(response.requerimientosGral()).hasSize(1);
        assertThat(response.requerimientosGral().get(0).cantidad()).isEqualTo(2);
        assertThat(response.requerimientosGral().get(0).idProfesion()).isEqualTo(2L);
        verify(requerimientoGralProyectoRepository).save(any(org.mgroko.backend.modelo.RequerimientoGralProyecto.class));
    }

    @Test
    void crear_requerimientoEnumeradoConValores_persiste() {
        prepararCaminoFelizRequerimiento();

        org.mgroko.backend.modelo.Profesion profesion = org.mgroko.backend.modelo.Profesion.builder()
                .idProfesion(2L).nombre("Modelo").build();
        when(profesionRepository.findById(2L)).thenReturn(Optional.of(profesion));

        org.mgroko.backend.modelo.Profesion profesionCaract = org.mgroko.backend.modelo.Profesion.builder()
                .idProfesion(2L).build();
        org.mgroko.backend.modelo.CaracteristicaTecnica caract = org.mgroko.backend.modelo.CaracteristicaTecnica.builder()
                .idCaracteristica(5L)
                .codigo("COLOR_OJOS")
                .tipoDato("ENUMERADO")
                .profesion(profesionCaract)
                .build();
        when(caracteristicaTecnicaRepository.findById(5L)).thenReturn(Optional.of(caract));
        org.mgroko.backend.modelo.ValorCaracteristicaId valorId =
                new org.mgroko.backend.modelo.ValorCaracteristicaId(1L, 5L);
        when(valorCaracteristicaRepository.findById(valorId)).thenReturn(Optional.of(
                org.mgroko.backend.modelo.ValorCaracteristica.builder().id(valorId).build()));
        when(requerimientoGralProyectoRepository.save(any(org.mgroko.backend.modelo.RequerimientoGralProyecto.class)))
                .thenAnswer(invocation -> {
                    org.mgroko.backend.modelo.RequerimientoGralProyecto r = invocation.getArgument(0);
                    r.setIdRequerimientoGral(11L);
                    if (!r.getCaracteristicas().isEmpty()) {
                        r.getCaracteristicas().get(0).setIdReqGralCaract(56L);
                    }
                    return r;
                });
        when(requerimientoGralCaractValorRepository.save(any(org.mgroko.backend.modelo.RequerimientoGralCaractValor.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CrearProyectoRequest request = new CrearProyectoRequest(
                "Desfile Enumerado", "Desc", Privacidad.Publico, LocalDate.now(), null, false, null, null,
                List.of(new org.mgroko.backend.proyectos.dto.CrearRequerimientoGralRequest(
                        1, 2L, null,
                        List.of(new org.mgroko.backend.proyectos.dto.CrearRequerimientoCaractRequest(
                                5L, null, null, List.of(1L))),
                        null)),
                null
        );

        ProyectoResponse response = crearProyectoService.crear(1L, 10L, request);

        assertThat(response.requerimientosGral()).hasSize(1);
        assertThat(response.requerimientosGral().get(0).caracteristicas()).hasSize(1);
        assertThat(response.requerimientosGral().get(0).caracteristicas().get(0).valores()).containsExactly(1L);
        verify(requerimientoGralCaractValorRepository).save(any(org.mgroko.backend.modelo.RequerimientoGralCaractValor.class));
    }

    @Test
    void crear_requerimientoNumericoSinRango_lanzaExcepcion() {
        prepararCaminoFelizRequerimiento();

        org.mgroko.backend.modelo.Profesion profesion = org.mgroko.backend.modelo.Profesion.builder()
                .idProfesion(2L).build();
        when(profesionRepository.findById(2L)).thenReturn(Optional.of(profesion));

        org.mgroko.backend.modelo.Profesion profesionCaract = org.mgroko.backend.modelo.Profesion.builder()
                .idProfesion(2L).build();
        org.mgroko.backend.modelo.CaracteristicaTecnica caract = org.mgroko.backend.modelo.CaracteristicaTecnica.builder()
                .idCaracteristica(1L).codigo("ALTURA").tipoDato("NUMERICO").profesion(profesionCaract).build();
        when(caracteristicaTecnicaRepository.findById(1L)).thenReturn(Optional.of(caract));

        CrearProyectoRequest request = new CrearProyectoRequest(
                "Req Sin Rango", "Desc", Privacidad.Publico, LocalDate.now(), null, false, null, null,
                List.of(new org.mgroko.backend.proyectos.dto.CrearRequerimientoGralRequest(
                        1, 2L, null,
                        List.of(new org.mgroko.backend.proyectos.dto.CrearRequerimientoCaractRequest(1L, null, null, null)),
                        null)),
                null
        );

        assertThatThrownBy(() -> crearProyectoService.crear(1L, 10L, request))
                .isInstanceOf(org.mgroko.backend.proyectos.exception.RequerimientoInvalidoException.class)
                .hasMessageContaining("exige valorMin y valorMax");
    }

    @Test
    void crear_requerimientoEnumeradoSinValores_lanzaExcepcion() {
        prepararCaminoFelizRequerimiento();

        org.mgroko.backend.modelo.Profesion profesion = org.mgroko.backend.modelo.Profesion.builder()
                .idProfesion(2L).build();
        when(profesionRepository.findById(2L)).thenReturn(Optional.of(profesion));

        org.mgroko.backend.modelo.Profesion profesionCaract = org.mgroko.backend.modelo.Profesion.builder()
                .idProfesion(2L).build();
        org.mgroko.backend.modelo.CaracteristicaTecnica caract = org.mgroko.backend.modelo.CaracteristicaTecnica.builder()
                .idCaracteristica(5L).codigo("COLOR_OJOS").tipoDato("ENUMERADO").profesion(profesionCaract).build();
        when(caracteristicaTecnicaRepository.findById(5L)).thenReturn(Optional.of(caract));

        CrearProyectoRequest request = new CrearProyectoRequest(
                "Req Sin Valores", "Desc", Privacidad.Publico, LocalDate.now(), null, false, null, null,
                List.of(new org.mgroko.backend.proyectos.dto.CrearRequerimientoGralRequest(
                        1, 2L, null,
                        List.of(new org.mgroko.backend.proyectos.dto.CrearRequerimientoCaractRequest(5L, null, null, null)),
                        null)),
                null
        );

        assertThatThrownBy(() -> crearProyectoService.crear(1L, 10L, request))
                .isInstanceOf(org.mgroko.backend.proyectos.exception.RequerimientoInvalidoException.class)
                .hasMessageContaining("exige al menos un valor");
    }

    @Test
    void crear_requerimientoCaracteristicaDuplicada_lanzaExcepcion() {
        prepararCaminoFelizRequerimiento();

        org.mgroko.backend.modelo.Profesion profesion = org.mgroko.backend.modelo.Profesion.builder()
                .idProfesion(2L).build();
        when(profesionRepository.findById(2L)).thenReturn(Optional.of(profesion));

        org.mgroko.backend.modelo.Profesion profesionCaract = org.mgroko.backend.modelo.Profesion.builder()
                .idProfesion(2L).build();
        org.mgroko.backend.modelo.CaracteristicaTecnica caract = org.mgroko.backend.modelo.CaracteristicaTecnica.builder()
                .idCaracteristica(1L).codigo("ALTURA").tipoDato("NUMERICO").profesion(profesionCaract).build();
        when(caracteristicaTecnicaRepository.findById(1L)).thenReturn(Optional.of(caract));

        CrearProyectoRequest request = new CrearProyectoRequest(
                "Req Duplicada", "Desc", Privacidad.Publico, LocalDate.now(), null, false, null, null,
                List.of(new org.mgroko.backend.proyectos.dto.CrearRequerimientoGralRequest(
                        1, 2L, null,
                        List.of(
                                new org.mgroko.backend.proyectos.dto.CrearRequerimientoCaractRequest(
                                        1L, java.math.BigDecimal.ONE, java.math.BigDecimal.TEN, null),
                                new org.mgroko.backend.proyectos.dto.CrearRequerimientoCaractRequest(
                                        1L, java.math.BigDecimal.ONE, java.math.BigDecimal.TEN, null)),
                        null)),
                null
        );

        assertThatThrownBy(() -> crearProyectoService.crear(1L, 10L, request))
                .isInstanceOf(org.mgroko.backend.proyectos.exception.RequerimientoInvalidoException.class)
                .hasMessageContaining("duplicada");
    }

    @Test
    void crear_requerimientoRangoInvertido_lanzaExcepcion() {
        prepararCaminoFelizRequerimiento();

        org.mgroko.backend.modelo.Profesion profesion = org.mgroko.backend.modelo.Profesion.builder()
                .idProfesion(2L).build();
        when(profesionRepository.findById(2L)).thenReturn(Optional.of(profesion));

        org.mgroko.backend.modelo.Profesion profesionCaract = org.mgroko.backend.modelo.Profesion.builder()
                .idProfesion(2L).build();
        org.mgroko.backend.modelo.CaracteristicaTecnica caract = org.mgroko.backend.modelo.CaracteristicaTecnica.builder()
                .idCaracteristica(1L).codigo("ALTURA").tipoDato("NUMERICO").profesion(profesionCaract).build();
        when(caracteristicaTecnicaRepository.findById(1L)).thenReturn(Optional.of(caract));

        CrearProyectoRequest request = new CrearProyectoRequest(
                "Req Invertido", "Desc", Privacidad.Publico, LocalDate.now(), null, false, null, null,
                List.of(new org.mgroko.backend.proyectos.dto.CrearRequerimientoGralRequest(
                        1, 2L, null,
                        List.of(new org.mgroko.backend.proyectos.dto.CrearRequerimientoCaractRequest(
                                1L, java.math.BigDecimal.TEN, java.math.BigDecimal.ONE, null)),
                        null)),
                null
        );

        assertThatThrownBy(() -> crearProyectoService.crear(1L, 10L, request))
                .isInstanceOf(org.mgroko.backend.proyectos.exception.RequerimientoInvalidoException.class)
                .hasMessageContaining("valorMin no puede ser mayor");
    }

    @Test
    void crear_habilidadInexistente_lanzaExcepcion() {
        prepararCaminoFelizRequerimiento();

        org.mgroko.backend.modelo.Profesion profesion = org.mgroko.backend.modelo.Profesion.builder()
                .idProfesion(2L).build();
        when(profesionRepository.findById(2L)).thenReturn(Optional.of(profesion));
        when(habilidadRepository.findById(999L)).thenReturn(Optional.empty());

        CrearProyectoRequest request = new CrearProyectoRequest(
                "Req Habilidad", "Desc", Privacidad.Publico, LocalDate.now(), null, false, null, null,
                List.of(new org.mgroko.backend.proyectos.dto.CrearRequerimientoGralRequest(
                        1, 2L, null, null, List.of(999L))),
                null
        );

        assertThatThrownBy(() -> crearProyectoService.crear(1L, 10L, request))
                .isInstanceOf(org.mgroko.backend.proyectos.exception.HabilidadNoEncontradaException.class);
    }

    @Test
    void crear_moodboardEnRequest_persisteYSeReflejaEnResponse() {
        prepararCaminoFelizRequerimiento();

        when(moodboardRepository.save(any(org.mgroko.backend.modelo.Moodboard.class)))
                .thenAnswer(invocation -> {
                    org.mgroko.backend.modelo.Moodboard m = invocation.getArgument(0);
                    m.setIdMoodboard(77L);
                    m.setFechaCreacion(java.time.LocalDateTime.of(2026, 6, 1, 10, 0));
                    return m;
                });

        CrearProyectoRequest request = new CrearProyectoRequest(
                "Proyecto Moodboard", "Desc", Privacidad.Publico, LocalDate.now(), null, false, null, null,
                null,
                new org.mgroko.backend.proyectos.dto.CrearMoodboardRequest("Paleta fría, telas de lino")
        );

        ProyectoResponse response = crearProyectoService.crear(1L, 10L, request);

        assertThat(response.moodboard()).isNotNull();
        assertThat(response.moodboard().idMoodboard()).isEqualTo(77L);
        assertThat(response.moodboard().descripcion()).isEqualTo("Paleta fría, telas de lino");
        verify(moodboardRepository).save(any(org.mgroko.backend.modelo.Moodboard.class));
    }

    @Test
    void crear_requerimientosYNullNoRompe_respuestaVacia() {
        prepararCaminoFelizRequerimiento();

        CrearProyectoRequest request = new CrearProyectoRequest(
                "Proyecto Regresion", "Desc", Privacidad.Publico, LocalDate.now(), null, false, null, null,
                null, null
        );

        ProyectoResponse response = crearProyectoService.crear(1L, 10L, request);

        assertThat(response.requerimientosGral()).isEmpty();
        assertThat(response.moodboard()).isNull();
    }
}
