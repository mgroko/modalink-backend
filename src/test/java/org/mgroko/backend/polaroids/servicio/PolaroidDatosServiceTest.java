package org.mgroko.backend.polaroids.servicio;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.perfiles.exception.PerfilNoEncontradoException;
import org.mgroko.backend.perfiles.servicio.VerPerfilService;
import org.mgroko.backend.polaroids.exception.LimitePolaroidsExcedidoException;
import org.mgroko.backend.polaroids.exception.PolaroidsSoloParaModelosException;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PolaroidDatosServiceTest {

    @Mock
    private PerfilRepository perfilRepository;

    @Mock
    private VerPerfilService verPerfilService;

    @Mock
    private PolaroidCatalogoService polaroidCatalogoService;

    private PolaroidDatosService servicio;

    @BeforeEach
    void setUp() {
        servicio = new PolaroidDatosService(
                perfilRepository, verPerfilService, polaroidCatalogoService, 8);
    }

    @Test
    void bloqueaPerfilPropioDeModelo() {
        Perfil perfil = perfil("MODELO", EstadoPerfil.Activo);
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuarioForUpdate(7L, 1L))
                .thenReturn(Optional.of(perfil));

        assertSame(perfil, servicio.bloquearPerfilPropioModelo(1L, 7L, 7L));
    }

    @Test
    void rechazaPerfilQueNoEsModelo() {
        Perfil perfil = perfil("FOTOGRAFO", EstadoPerfil.Activo);
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuarioForUpdate(7L, 1L))
                .thenReturn(Optional.of(perfil));

        assertThrows(PolaroidsSoloParaModelosException.class,
                () -> servicio.bloquearPerfilPropioModelo(1L, 7L, 7L));
    }

    @Test
    void rechazaPerfilActivoNoSeleccionado() {
        assertThrows(PerfilActivoNoSeleccionadoException.class,
                () -> servicio.bloquearPerfilPropioModelo(1L, 7L, null));
    }

    @Test
    void rechazaPerfilAjenoComoNoEncontrado() {
        assertThrows(PerfilNoEncontradoException.class,
                () -> servicio.bloquearPerfilPropioModelo(1L, 7L, 8L));
    }

    @Test
    void rechazaPerfilDadoDeBaja() {
        Perfil perfil = perfil("MODELO", EstadoPerfil.Baja);
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuarioForUpdate(7L, 1L))
                .thenReturn(Optional.of(perfil));

        assertThrows(PerfilEnBajaException.class,
                () -> servicio.bloquearPerfilPropioModelo(1L, 7L, 7L));
    }

    @Test
    void validaElMaximoConfigurado() {
        assertDoesNotThrow(() -> servicio.validarLimite(7));
        assertThrows(LimitePolaroidsExcedidoException.class,
                () -> servicio.validarLimite(8));
    }

    private Perfil perfil(String codigoProfesion, EstadoPerfil estado) {
        return Perfil.builder()
                .idPerfil(7L)
                .estado(estado)
                .profesion(Profesion.builder().codigo(codigoProfesion).nombre(codigoProfesion).build())
                .build();
    }
}
