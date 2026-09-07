package org.mgroko.backend.perfiles.servicio;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.auth.exception.UsuarioNoEncontradoException;
import org.mgroko.backend.modelo.CaracteristicaPerfil;
import org.mgroko.backend.modelo.CaracteristicaTecnica;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.ValorCaracteristica;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.perfiles.dto.CaracteristicaPerfilRequest;
import org.mgroko.backend.perfiles.dto.CrearPerfilRequest;
import org.mgroko.backend.perfiles.dto.PerfilResponse;
import org.mgroko.backend.perfiles.exception.CaracteristicaDuplicateException;
import org.mgroko.backend.perfiles.exception.CaracteristicaNoEncontradaException;
import org.mgroko.backend.perfiles.exception.CaracteristicaProfesionNoCoincideException;
import org.mgroko.backend.perfiles.exception.IdValorObligatorioException;
import org.mgroko.backend.perfiles.exception.PerfilDuplicadoException;
import org.mgroko.backend.perfiles.exception.ProfesionNoEncontradaException;
import org.mgroko.backend.perfiles.exception.ValorCaracteristicaNoEncontradoException;
import org.mgroko.backend.perfiles.exception.ValorNumericoNegativoException;
import org.mgroko.backend.perfiles.exception.ValorObligatorioException;
import org.mgroko.backend.repositorio.CaracteristicaTecnicaRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.ProfesionRepository;
import org.mgroko.backend.repositorio.UsuarioRepository;
import org.mgroko.backend.repositorio.ValorCaracteristicaRepository;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CrearPerfilServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ProfesionRepository profesionRepository;

    @Mock
    private CaracteristicaTecnicaRepository caracteristicaTecnicaRepository;

    @Mock
    private ValorCaracteristicaRepository valorCaracteristicaRepository;

    @Mock
    private PerfilRepository perfilRepository;

    @InjectMocks
    private CrearPerfilService crearPerfilService;

    private Usuario usuarioActivo() {
        return Usuario.builder()
                .idUsuario(1L)
                .nombre("Maria")
                .apellido("Flores")
                .estado(EstadoUsuario.Activo)
                .build();
    }

    private Profesion profesionModelo() {
        return Profesion.builder().idProfesion(2L).nombre("modelo").build();
    }

    private CaracteristicaTecnica caracteristicaAltura(Profesion profesion) {
        return CaracteristicaTecnica.builder()
                .idCaracteristica(11L)
                .codigo("altura")
                .unidad("cm")
                .tipoDato(CaracteristicaTecnica.TIPO_TEXTO)
                .profesion(profesion)
                .build();
    }

    private CaracteristicaTecnica caracteristicaColorOjos(Profesion profesion) {
        return CaracteristicaTecnica.builder()
                .idCaracteristica(12L)
                .codigo("color_ojos")
                .tipoDato(CaracteristicaTecnica.TIPO_ENUMERADO)
                .profesion(profesion)
                .build();
    }

    private CaracteristicaTecnica caracteristicaMedida(Profesion profesion) {
        return CaracteristicaTecnica.builder()
                .idCaracteristica(13L)
                .codigo("medida_pecho")
                .unidad("cm")
                .tipoDato(CaracteristicaTecnica.TIPO_NUMERICO)
                .profesion(profesion)
                .build();
    }

    private CrearPerfilRequest requestValido() {
        return new CrearPerfilRequest(
                "Luna", 2L, "Modelo profesional con 5 años de experiencia.",
                List.of(new CaracteristicaPerfilRequest(11L, "175", null)));
    }

    private void configurarFlujoFeliz() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioActivo()));
        when(profesionRepository.findById(2L)).thenReturn(Optional.of(profesionModelo()));
        when(perfilRepository.existsByUsuarioIdUsuarioAndProfesionIdProfesionAndEstadoNot(
                anyLong(), anyLong(), any(EstadoPerfil.class))).thenReturn(false);
        when(caracteristicaTecnicaRepository.findById(11L))
                .thenReturn(Optional.of(caracteristicaAltura(profesionModelo())));
        when(perfilRepository.save(any(Perfil.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void crear_datosValidos_devuelveResponse() {
        configurarFlujoFeliz();

        PerfilResponse response = crearPerfilService.crear(1L, requestValido());

        assertEquals("Luna", response.nombreArtistico());
        assertEquals("modelo", response.profesion());
        assertEquals("Activo", response.estado());
        assertEquals("Modelo profesional con 5 años de experiencia.", response.biografia());
        assertEquals(1, response.caracteristicas().size());
        assertEquals("altura", response.caracteristicas().get(0).codigo());
        assertEquals("175", response.caracteristicas().get(0).valor());

        ArgumentCaptor<Perfil> captor = ArgumentCaptor.forClass(Perfil.class);
        verify(perfilRepository).save(captor.capture());
        Perfil guardado = captor.getValue();
        assertEquals("Luna", guardado.getNombreArtistico());
        assertEquals(EstadoPerfil.Activo, guardado.getEstado());
        assertEquals(1L, guardado.getUsuario().getIdUsuario());
        assertEquals(1, guardado.getCaracteristicas().size());
        CaracteristicaPerfil cp = guardado.getCaracteristicas().iterator().next();
        assertEquals("175", cp.getValor());
        assertNotNull(cp.getFechaRegistro());
    }

    @Test
    void crear_sinCaracteristicas_devuelveResponse() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioActivo()));
        when(profesionRepository.findById(2L)).thenReturn(Optional.of(profesionModelo()));
        when(perfilRepository.existsByUsuarioIdUsuarioAndProfesionIdProfesionAndEstadoNot(
                anyLong(), anyLong(), any(EstadoPerfil.class))).thenReturn(false);
        when(perfilRepository.save(any(Perfil.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CrearPerfilRequest requestSinCaracteristicas = new CrearPerfilRequest(
                "Luna", 2L, "Modelo profesional.", List.of());

        PerfilResponse response = crearPerfilService.crear(1L, requestSinCaracteristicas);

        assertTrue(response.caracteristicas().isEmpty());
    }

    @Test
    void crear_caracteristicaTextoValorNulo_lanzaExcepcion() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioActivo()));
        when(profesionRepository.findById(2L)).thenReturn(Optional.of(profesionModelo()));
        when(perfilRepository.existsByUsuarioIdUsuarioAndProfesionIdProfesionAndEstadoNot(
                anyLong(), anyLong(), any(EstadoPerfil.class))).thenReturn(false);
        when(caracteristicaTecnicaRepository.findById(11L))
                .thenReturn(Optional.of(caracteristicaAltura(profesionModelo())));

        CrearPerfilRequest request = new CrearPerfilRequest(
                "Luna", 2L, "Modelo profesional.",
                List.of(new CaracteristicaPerfilRequest(11L, null, null)));

        assertThrows(ValorObligatorioException.class,
                () -> crearPerfilService.crear(1L, request));
    }

    @Test
    void crear_caracteristicaEnumeradoSinIdValor_lanzaExcepcion() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioActivo()));
        when(profesionRepository.findById(2L)).thenReturn(Optional.of(profesionModelo()));
        when(perfilRepository.existsByUsuarioIdUsuarioAndProfesionIdProfesionAndEstadoNot(
                anyLong(), anyLong(), any(EstadoPerfil.class))).thenReturn(false);
        when(caracteristicaTecnicaRepository.findById(12L))
                .thenReturn(Optional.of(caracteristicaColorOjos(profesionModelo())));

        CrearPerfilRequest request = new CrearPerfilRequest(
                "Luna", 2L, "Modelo profesional.",
                List.of(new CaracteristicaPerfilRequest(12L, null, null)));

        assertThrows(IdValorObligatorioException.class,
                () -> crearPerfilService.crear(1L, request));
    }

    @Test
    void crear_caracteristicaEnumeradoConIdValor_devuelveResponse() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioActivo()));
        when(profesionRepository.findById(2L)).thenReturn(Optional.of(profesionModelo()));
        when(perfilRepository.existsByUsuarioIdUsuarioAndProfesionIdProfesionAndEstadoNot(
                anyLong(), anyLong(), any(EstadoPerfil.class))).thenReturn(false);
        when(caracteristicaTecnicaRepository.findById(12L))
                .thenReturn(Optional.of(caracteristicaColorOjos(profesionModelo())));

        ValorCaracteristica valorMarron = ValorCaracteristica.builder()
                .idValor(20L)
                .codigo("marron")
                .colorHex("#6B4226")
                .caracteristicaTecnica(caracteristicaColorOjos(profesionModelo()))
                .build();
        when(valorCaracteristicaRepository.findById(20L)).thenReturn(Optional.of(valorMarron));
        when(perfilRepository.save(any(Perfil.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CrearPerfilRequest request = new CrearPerfilRequest(
                "Luna", 2L, "Modelo profesional.",
                List.of(new CaracteristicaPerfilRequest(12L, null, 20L)));

        ArgumentCaptor<Perfil> captor = ArgumentCaptor.forClass(Perfil.class);
        PerfilResponse response = crearPerfilService.crear(1L, request);

        verify(perfilRepository).save(captor.capture());
        CaracteristicaPerfil cp = captor.getValue().getCaracteristicas().iterator().next();
        assertNotNull(cp.getValorCaracteristica());
        assertEquals(20L, cp.getValorCaracteristica().getIdValor());
        assertEquals("marron", response.caracteristicas().get(0).codigoValor());
        assertEquals("#6B4226", response.caracteristicas().get(0).colorHex());
    }

    @Test
    void crear_caracteristicaEnumeradoIdValorInexistente_lanzaExcepcion() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioActivo()));
        when(profesionRepository.findById(2L)).thenReturn(Optional.of(profesionModelo()));
        when(perfilRepository.existsByUsuarioIdUsuarioAndProfesionIdProfesionAndEstadoNot(
                anyLong(), anyLong(), any(EstadoPerfil.class))).thenReturn(false);
        when(caracteristicaTecnicaRepository.findById(12L))
                .thenReturn(Optional.of(caracteristicaColorOjos(profesionModelo())));
        when(valorCaracteristicaRepository.findById(99L)).thenReturn(Optional.empty());

        CrearPerfilRequest request = new CrearPerfilRequest(
                "Luna", 2L, "Modelo profesional.",
                List.of(new CaracteristicaPerfilRequest(12L, null, 99L)));

        assertThrows(ValorCaracteristicaNoEncontradoException.class,
                () -> crearPerfilService.crear(1L, request));
    }

    @Test
    void crear_usuarioNoExiste_lanzaExcepcion() {
        when(usuarioRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(UsuarioNoEncontradoException.class,
                () -> crearPerfilService.crear(999L, requestValido()));

        verify(perfilRepository, never()).save(any());
    }

    @Test
    void crear_usuarioNoActivo_lanzaExcepcion() {
        Usuario usuarioDeshabilitado = Usuario.builder()
                .idUsuario(1L)
                .estado(EstadoUsuario.Deshabilitado)
                .build();
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioDeshabilitado));

        assertThrows(UsuarioNoEncontradoException.class,
                () -> crearPerfilService.crear(1L, requestValido()));

        verify(perfilRepository, never()).save(any());
    }

    @Test
    void crear_profesionNoExiste_lanzaExcepcion() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioActivo()));
        when(profesionRepository.findById(2L)).thenReturn(Optional.empty());

        assertThrows(ProfesionNoEncontradaException.class,
                () -> crearPerfilService.crear(1L, requestValido()));

        verify(perfilRepository, never()).save(any());
    }

    @Test
    void crear_perfilDuplicado_lanzaExcepcion() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioActivo()));
        when(profesionRepository.findById(2L)).thenReturn(Optional.of(profesionModelo()));
        when(perfilRepository.existsByUsuarioIdUsuarioAndProfesionIdProfesionAndEstadoNot(
                anyLong(), anyLong(), any(EstadoPerfil.class))).thenReturn(true);

        assertThrows(PerfilDuplicadoException.class,
                () -> crearPerfilService.crear(1L, requestValido()));

        verify(perfilRepository, never()).save(any());
    }

    @Test
    void crear_caracteristicaNoExiste_lanzaExcepcion() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioActivo()));
        when(profesionRepository.findById(2L)).thenReturn(Optional.of(profesionModelo()));
        when(perfilRepository.existsByUsuarioIdUsuarioAndProfesionIdProfesionAndEstadoNot(
                anyLong(), anyLong(), any(EstadoPerfil.class))).thenReturn(false);
        when(caracteristicaTecnicaRepository.findById(11L)).thenReturn(Optional.empty());

        assertThrows(CaracteristicaNoEncontradaException.class,
                () -> crearPerfilService.crear(1L, requestValido()));

        verify(perfilRepository, never()).save(any());
    }

    @Test
    void crear_caracteristicaDeOtraProfesion_lanzaExcepcion() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioActivo()));
        when(profesionRepository.findById(2L)).thenReturn(Optional.of(profesionModelo()));
        when(perfilRepository.existsByUsuarioIdUsuarioAndProfesionIdProfesionAndEstadoNot(
                anyLong(), anyLong(), any(EstadoPerfil.class))).thenReturn(false);

        Profesion otraProfesion = Profesion.builder().idProfesion(3L).nombre("fotografo").build();
        when(caracteristicaTecnicaRepository.findById(11L))
                .thenReturn(Optional.of(caracteristicaAltura(otraProfesion)));

        assertThrows(CaracteristicaProfesionNoCoincideException.class,
                () -> crearPerfilService.crear(1L, requestValido()));

        verify(perfilRepository, never()).save(any());
    }

    @Test
    void crear_caracteristicaDuplicada_lanzaExcepcion() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioActivo()));
        when(profesionRepository.findById(2L)).thenReturn(Optional.of(profesionModelo()));
        when(perfilRepository.existsByUsuarioIdUsuarioAndProfesionIdProfesionAndEstadoNot(
                anyLong(), anyLong(), any(EstadoPerfil.class))).thenReturn(false);
        when(caracteristicaTecnicaRepository.findById(11L))
                .thenReturn(Optional.of(caracteristicaAltura(profesionModelo())));

        CrearPerfilRequest request = new CrearPerfilRequest(
                "Luna", 2L, "Modelo profesional.",
                List.of(new CaracteristicaPerfilRequest(11L, "175", null),
                        new CaracteristicaPerfilRequest(11L, "176", null)));

        assertThrows(CaracteristicaDuplicateException.class,
                () -> crearPerfilService.crear(1L, request));

        verify(perfilRepository, never()).save(any());
    }

    private void configurarFlujoParaCaracteristica(CaracteristicaTecnica caracteristica) {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioActivo()));
        when(profesionRepository.findById(2L)).thenReturn(Optional.of(profesionModelo()));
        when(perfilRepository.existsByUsuarioIdUsuarioAndProfesionIdProfesionAndEstadoNot(
                anyLong(), anyLong(), any(EstadoPerfil.class))).thenReturn(false);
        when(caracteristicaTecnicaRepository.findById(caracteristica.getIdCaracteristica()))
                .thenReturn(Optional.of(caracteristica));
    }

    @Test
    void crear_caracteristicaNumericoValorNegativo_lanzaExcepcion() {
        configurarFlujoParaCaracteristica(caracteristicaMedida(profesionModelo()));

        CrearPerfilRequest request = new CrearPerfilRequest(
                "Luna", 2L, "Modelo profesional.",
                List.of(new CaracteristicaPerfilRequest(13L, "-84", null)));

        assertThrows(ValorNumericoNegativoException.class,
                () -> crearPerfilService.crear(1L, request));

        verify(perfilRepository, never()).save(any());
    }

    @Test
    void crear_caracteristicaNumericoNoEsNumero_lanzaExcepcion() {
        configurarFlujoParaCaracteristica(caracteristicaMedida(profesionModelo()));

        CrearPerfilRequest request = new CrearPerfilRequest(
                "Luna", 2L, "Modelo profesional.",
                List.of(new CaracteristicaPerfilRequest(13L, "abc", null)));

        assertThrows(ValorNumericoNegativoException.class,
                () -> crearPerfilService.crear(1L, request));

        verify(perfilRepository, never()).save(any());
    }

    @Test
    void crear_caracteristicaNumericoValorValido_devuelveResponse() {
        configurarFlujoParaCaracteristica(caracteristicaMedida(profesionModelo()));
        when(perfilRepository.save(any(Perfil.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CrearPerfilRequest request = new CrearPerfilRequest(
                "Luna", 2L, "Modelo profesional.",
                List.of(new CaracteristicaPerfilRequest(13L, "84", null)));

        ArgumentCaptor<Perfil> captor = ArgumentCaptor.forClass(Perfil.class);
        PerfilResponse response = crearPerfilService.crear(1L, request);

        verify(perfilRepository).save(captor.capture());
        CaracteristicaPerfil cp = captor.getValue().getCaracteristicas().iterator().next();
        assertEquals("84", cp.getValor());
        assertEquals("84", response.caracteristicas().get(0).valor());
    }

    @Test
    void crear_caracteristicaTextoValorNegativo_noLanzaExcepcion() {
        configurarFlujoParaCaracteristica(caracteristicaAltura(profesionModelo()));
        when(perfilRepository.save(any(Perfil.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CrearPerfilRequest request = new CrearPerfilRequest(
                "Luna", 2L, "Modelo profesional.",
                List.of(new CaracteristicaPerfilRequest(11L, "nota-personal", null)));

        ArgumentCaptor<Perfil> captor = ArgumentCaptor.forClass(Perfil.class);
        crearPerfilService.crear(1L, request);

        verify(perfilRepository).save(captor.capture());
        assertEquals("nota-personal",
                captor.getValue().getCaracteristicas().iterator().next().getValor());
    }
}