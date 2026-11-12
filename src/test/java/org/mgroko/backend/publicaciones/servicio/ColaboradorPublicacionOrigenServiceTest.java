package org.mgroko.backend.publicaciones.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.modelo.ColaboradorPublicacionOrigen;
import org.mgroko.backend.modelo.enums.OrigenColaboracion;
import org.mgroko.backend.repositorio.ColaboradorPublicacionOrigenRepository;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ColaboradorPublicacionOrigenServiceTest {

    private static final Clock RELOJ = Clock.fixed(
            Instant.parse("2026-11-11T12:00:00Z"), ZoneOffset.UTC);

    @Mock
    private ColaboradorPublicacionOrigenRepository repository;

    private ColaboradorPublicacionOrigenService servicio;

    @BeforeEach
    void setUp() {
        servicio = new ColaboradorPublicacionOrigenService(repository, RELOJ);
        when(repository.save(any()))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    @Test
    void registrarManual_creaOrigenManual() {
        when(repository.findByIdPublicacionAndIdPerfil(10L, 7L))
                .thenReturn(Optional.empty());

        ColaboradorPublicacionOrigen origen = servicio.registrarManual(10L, 7L);

        assertEquals(OrigenColaboracion.MANUAL, origen.getOrigen());
    }

    @Test
    void registrarAutomatica_sobreManual_loConvierteEnAmbos() {
        ColaboradorPublicacionOrigen existente = ColaboradorPublicacionOrigen.builder()
                .idPublicacion(10L)
                .idPerfil(7L)
                .origen(OrigenColaboracion.MANUAL)
                .build();
        when(repository.findByIdPublicacionAndIdPerfil(10L, 7L))
                .thenReturn(Optional.of(existente));

        ColaboradorPublicacionOrigen origen = servicio.registrarAutomatica(10L, 7L, 50L);

        assertEquals(OrigenColaboracion.AMBOS, origen.getOrigen());
        assertEquals(50L, origen.getIdColaboracionAutomatica());
    }
}
