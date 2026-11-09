package org.mgroko.backend.repositorio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.contratos.dto.HistorialContratoEventoResponse;
import org.mgroko.backend.contratos.servicio.ListarHistorialContratosProyectoService;
import org.mgroko.backend.proyectos.exception.AccesoDenegadoProyectoException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * Visibilidad y contenido del historial de contratos por proyecto.
 */
@SpringBootTest
@Transactional
class HistorialContratosIntegrationTest
        extends AbstractFirmaContratoIntegrationTest {

    @Autowired
    private ListarHistorialContratosProyectoService historialContratosService;

    @Test
    void directorVeTodosLosEventosDelProyecto() {
        Datos datos = crearProyectoConContrato();

        PaginaResponse<HistorialContratoEventoResponse> pagina =
                historialContratosService.listar(
                        datos.director().getIdUsuario(),
                        datos.perfilDirector().getIdPerfil(),
                        datos.proyecto().getIdProyecto(),
                        null,
                        null,
                        0,
                        20);

        assertThat(pagina.totalElementos()).isEqualTo(2);
        assertThat(pagina.contenido())
                .extracting(HistorialContratoEventoResponse::tipoEvento)
                .containsExactlyInAnyOrder("CREADO", "EMITIDO");
        assertThat(pagina.contenido())
                .allMatch(evento -> evento.idContrato().equals(datos.idContrato()));
    }

    @Test
    void miembroVeSoloLosEventosDeSusContratos() {
        Datos datos = crearProyectoConContrato();

        PaginaResponse<HistorialContratoEventoResponse> delMiembro =
                historialContratosService.listar(
                        datos.miembro().getIdUsuario(),
                        datos.perfilMiembro().getIdPerfil(),
                        datos.proyecto().getIdProyecto(),
                        null,
                        null,
                        0,
                        20);

        assertThat(delMiembro.totalElementos()).isEqualTo(2);

        PaginaResponse<HistorialContratoEventoResponse> delNoParte =
                historialContratosService.listar(
                        datos.noParte().getIdUsuario(),
                        datos.perfilNoParte().getIdPerfil(),
                        datos.proyecto().getIdProyecto(),
                        null,
                        null,
                        0,
                        20);

        assertThat(delNoParte.totalElementos()).isZero();
    }

    @Test
    void usuarioAjenoNoPuedeConsultarElHistorial() {
        Datos datos = crearProyectoConContrato();

        assertThatThrownBy(() -> historialContratosService.listar(
                datos.ajeno().getIdUsuario(),
                datos.perfilAjeno().getIdPerfil(),
                datos.proyecto().getIdProyecto(),
                null,
                null,
                0,
                20))
                .isInstanceOf(AccesoDenegadoProyectoException.class);
    }

    @Test
    void contratoEliminadoApareceEnElHistorial() {
        Datos datos = crearProyectoConContrato();

        em.createNativeQuery("""
                UPDATE contrato
                   SET estado = 'ELIMINADO'
                 WHERE id_contrato = :idContrato
                """)
                .setParameter("idContrato", datos.idContrato())
                .executeUpdate();

        em.flush();
        em.clear();

        PaginaResponse<HistorialContratoEventoResponse> pagina =
                historialContratosService.listar(
                        datos.director().getIdUsuario(),
                        datos.perfilDirector().getIdPerfil(),
                        datos.proyecto().getIdProyecto(),
                        "ELIMINADO",
                        null,
                        0,
                        20);

        assertThat(pagina.totalElementos()).isEqualTo(1);
        assertThat(pagina.contenido().get(0).tipoEvento()).isEqualTo("ELIMINADO");
        assertThat(pagina.contenido().get(0).estadoContrato()).isEqualTo("ELIMINADO");
        assertThat(pagina.contenido().get(0).nombreContrato()).isNotBlank();
        assertThat(pagina.contenido().get(0).fechaBaja()).isNotNull();
    }

    @Test
    void miembroVeElEventoEliminadoDeSuPropioContrato() {
        Datos datos = crearProyectoConContrato();

        em.createNativeQuery("""
                UPDATE contrato
                   SET estado = 'ELIMINADO'
                 WHERE id_contrato = :idContrato
                """)
                .setParameter("idContrato", datos.idContrato())
                .executeUpdate();

        em.flush();
        em.clear();

        PaginaResponse<HistorialContratoEventoResponse> pagina =
                historialContratosService.listar(
                        datos.miembro().getIdUsuario(),
                        datos.perfilMiembro().getIdPerfil(),
                        datos.proyecto().getIdProyecto(),
                        "ELIMINADO",
                        null,
                        0,
                        20);

        assertThat(pagina.totalElementos()).isEqualTo(1);
        assertThat(pagina.contenido().get(0).tipoEvento()).isEqualTo("ELIMINADO");
        assertThat(pagina.contenido().get(0).estadoContrato()).isEqualTo("ELIMINADO");
    }

    @Test
    void filtroPorNombreDeContratoEsParcialYCaseInsensitive() {
        Datos datos = crearProyectoConContrato();

        PaginaResponse<HistorialContratoEventoResponse> coincidencias =
                historialContratosService.listar(
                        datos.director().getIdUsuario(),
                        datos.perfilDirector().getIdPerfil(),
                        datos.proyecto().getIdProyecto(),
                        null,
                        "uc55",
                        0,
                        20);

        assertThat(coincidencias.totalElementos()).isEqualTo(2);

        PaginaResponse<HistorialContratoEventoResponse> sinCoincidencias =
                historialContratosService.listar(
                        datos.director().getIdUsuario(),
                        datos.perfilDirector().getIdPerfil(),
                        datos.proyecto().getIdProyecto(),
                        null,
                        "no-existe",
                        0,
                        20);

        assertThat(sinCoincidencias.totalElementos()).isZero();
    }

    @Test
    void filtrosInvalidosDevuelvenErrorDeParametro() {
        Datos datos = crearProyectoConContrato();

        assertThatThrownBy(() -> historialContratosService.listar(
                datos.director().getIdUsuario(),
                datos.perfilDirector().getIdPerfil(),
                datos.proyecto().getIdProyecto(),
                "NO_EXISTE",
                null,
                0,
                20))
                .isInstanceOf(
                        org.mgroko.backend.proyectos.exception
                                .HistorialParametroInvalidoException.class);
    }
}
