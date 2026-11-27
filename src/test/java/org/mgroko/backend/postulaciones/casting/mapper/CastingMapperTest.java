package org.mgroko.backend.postulaciones.casting.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.modelo.GrupoPolaroid;
import org.mgroko.backend.modelo.Imagen;
import org.mgroko.backend.modelo.PolaroidPerfil;
import org.mgroko.backend.modelo.TipoPolaroid;
import org.mgroko.backend.postulaciones.casting.dto.PolaroidCastingResponse;

class CastingMapperTest {

    @Test
    void ordenaPrimeroFrentePerfilYCuerpoEnteroFrente() {
        List<PolaroidCastingResponse> respuesta = CastingMapper.toPolaroids(List.of(
                polaroid(1L, 5, "CUERPO_COMPLETO_PERFIL"),
                polaroid(2L, 3, "PRIMER_PLANO_PERFIL"),
                polaroid(3L, 1, "PRIMER_PLANO_FRENTE"),
                polaroid(4L, 4, "CUERPO_COMPLETO_FRENTE"),
                polaroid(5L, 2, "PRIMER_PLANO_ESPALDAS")));

        assertThat(respuesta)
                .extracting(polaroid -> polaroid.tipo().codigo())
                .containsExactly(
                        "PRIMER_PLANO_FRENTE",
                        "PRIMER_PLANO_PERFIL",
                        "CUERPO_COMPLETO_FRENTE",
                        "PRIMER_PLANO_ESPALDAS",
                        "CUERPO_COMPLETO_PERFIL");
        assertThat(respuesta)
                .extracting(PolaroidCastingResponse::prioridadCasting)
                .containsExactly(1, 2, 3, 4, 5);
    }

    @Test
    void siNoHayCuerpoEnteroFrenteUsaLaPrimeraCuerpoCompleto() {
        List<PolaroidCastingResponse> respuesta = CastingMapper.toPolaroids(List.of(
                polaroid(1L, 1, "PRIMER_PLANO_FRENTE"),
                polaroid(2L, 2, "PRIMER_PLANO_PERFIL"),
                polaroid(3L, 3, "CUERPO_COMPLETO_ESPALDAS"),
                polaroid(4L, 4, "PLANO_AMERICANO_FRENTE")));

        assertThat(respuesta)
                .extracting(polaroid -> polaroid.tipo().codigo())
                .containsExactly(
                        "PRIMER_PLANO_FRENTE",
                        "PRIMER_PLANO_PERFIL",
                        "CUERPO_COMPLETO_ESPALDAS",
                        "PLANO_AMERICANO_FRENTE");
    }

    @Test
    void sinPolaroidsDevuelveListaVacia() {
        assertThat(CastingMapper.toPolaroids(List.of())).isEmpty();
        assertThat(CastingMapper.toPolaroids(null)).isEmpty();
    }

    private PolaroidPerfil polaroid(Long id, int orden, String codigo) {
        return PolaroidPerfil.builder()
                .idPolaroid(id)
                .orden(orden)
                .imagen(Imagen.builder()
                        .idImagen(id + 100)
                        .url("/uploads/polaroid-" + id + ".webp")
                        .build())
                .tipoPolaroid(tipo(codigo))
                .build();
    }

    private TipoPolaroid tipo(String codigo) {
        String grupoCodigo = codigo.substring(0, codigo.lastIndexOf('_'));
        return TipoPolaroid.builder()
                .idTipoPolaroid(1L)
                .codigo(codigo)
                .nombre("De frente")
                .grupo(GrupoPolaroid.builder()
                        .idGrupoPolaroid(1L)
                        .codigo(grupoCodigo)
                        .nombre("Grupo")
                        .build())
                .build();
    }
}
