package org.mgroko.backend.polaroids.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.modelo.GrupoPolaroid;
import org.mgroko.backend.modelo.TipoPolaroid;
import org.mgroko.backend.polaroids.dto.GrupoPolaroidResponse;
import org.mgroko.backend.polaroids.exception.TipoPolaroidNoEncontradoException;
import org.mgroko.backend.repositorio.GrupoPolaroidRepository;
import org.mgroko.backend.repositorio.TipoPolaroidRepository;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PolaroidCatalogoServiceTest {

    @Mock
    private GrupoPolaroidRepository grupoPolaroidRepository;

    @Mock
    private TipoPolaroidRepository tipoPolaroidRepository;

    @Test
    void agrupaLosTiposYArmaElNombreCompleto() {
        GrupoPolaroid grupo = GrupoPolaroid.builder()
                .idGrupoPolaroid(1L)
                .codigo("PRIMER_PLANO")
                .nombre("Primer plano")
                .build();
        TipoPolaroid tipo = TipoPolaroid.builder()
                .idTipoPolaroid(2L)
                .codigo("PRIMER_PLANO_PERFIL")
                .nombre("De perfil")
                .grupo(grupo)
                .build();

        when(grupoPolaroidRepository.findAllByOrderByIdGrupoPolaroidAsc())
                .thenReturn(List.of(grupo));
        when(tipoPolaroidRepository.findAllByOrderByIdTipoPolaroidAsc())
                .thenReturn(List.of(tipo));

        PolaroidCatalogoService servicio = new PolaroidCatalogoService(
                grupoPolaroidRepository, tipoPolaroidRepository);

        List<GrupoPolaroidResponse> grupos = servicio.listar();

        assertEquals(1, grupos.size());
        assertEquals("Primer plano", grupos.get(0).nombre());
        assertEquals(1, grupos.get(0).tipos().size());
        assertEquals("Primer plano · De perfil", grupos.get(0).tipos().get(0).nombreCompleto());
    }

    @Test
    void fallaSiElTipoNoExiste() {
        when(tipoPolaroidRepository.findById(99L)).thenReturn(Optional.empty());

        PolaroidCatalogoService servicio = new PolaroidCatalogoService(
                grupoPolaroidRepository, tipoPolaroidRepository);

        assertThrows(TipoPolaroidNoEncontradoException.class, () -> servicio.obtenerTipo(99L));
    }
}
