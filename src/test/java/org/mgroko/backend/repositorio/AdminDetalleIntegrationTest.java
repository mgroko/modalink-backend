package org.mgroko.backend.repositorio;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.admin.dto.AdminContratoDetalleResponse;
import org.mgroko.backend.admin.dto.AdminPerfilDetalleResponse;
import org.mgroko.backend.admin.dto.AdminProyectoDetalleResponse;
import org.mgroko.backend.admin.dto.BuscarContratosAdminFiltro;
import org.mgroko.backend.admin.dto.BuscarPerfilesAdminFiltro;
import org.mgroko.backend.admin.servicio.AdminContratoService;
import org.mgroko.backend.admin.servicio.AdminPerfilService;
import org.mgroko.backend.admin.servicio.AdminProyectoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verifica las consultas administrativas de detalle contra PostgreSQL real.
 */
@SpringBootTest
@Transactional
class AdminDetalleIntegrationTest extends AbstractFirmaContratoIntegrationTest {

    @Autowired
    private AdminPerfilService adminPerfilService;

    @Autowired
    private AdminProyectoService adminProyectoService;

    @Autowired
    private AdminContratoService adminContratoService;

    @Test
    void perfil_incluyeUsuarioYListasRelacionadas() {
        Datos datos = crearProyectoConContrato();

        AdminPerfilDetalleResponse detalle = adminPerfilService.obtenerDetalle(
                datos.perfilDirector().getIdPerfil());

        assertThat(detalle.idPerfil()).isEqualTo(datos.perfilDirector().getIdPerfil());
        assertThat(detalle.usuario()).isNotNull();
        assertThat(detalle.usuario().correo()).isEqualTo(datos.director().getCorreo());
        assertThat(detalle.proyectos()).isNotEmpty();
        assertThat(detalle.contratos()).isNotEmpty();
        assertThat(detalle.resumenRelacionados().totalProyectos()).isGreaterThanOrEqualTo(1);
        assertThat(detalle.resumenRelacionados().totalContratos()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void proyecto_incluyeMiembrosYContratos() {
        Datos datos = crearProyectoConContrato();

        AdminProyectoDetalleResponse detalle = adminProyectoService.obtenerDetalle(
                datos.proyecto().getIdProyecto());

        assertThat(detalle.idProyecto()).isEqualTo(datos.proyecto().getIdProyecto());
        assertThat(detalle.director()).isNotNull();
        assertThat(detalle.director().idPerfil())
                .isEqualTo(datos.perfilDirector().getIdPerfil());
        assertThat(detalle.miembros()).isNotEmpty();
        assertThat(detalle.contratos()).hasSize(1);
        assertThat(detalle.totalContratos()).isEqualTo(1);
    }

    @Test
    void contrato_incluyeTextoPartesYEventos() {
        Datos datos = crearProyectoConContrato();

        AdminContratoDetalleResponse detalle = adminContratoService.obtenerDetalle(
                datos.idContrato());

        assertThat(detalle.idContrato()).isEqualTo(datos.idContrato());
        assertThat(detalle.texto()).isNotBlank();
        assertThat(detalle.partes()).hasSize(2);
        assertThat(detalle.firmados() + 1).isGreaterThanOrEqualTo(1);
        assertThat(adminContratoService.listarEventos(datos.idContrato())).isNotEmpty();
    }

    @Test
    void listadosAdministrativos_funcionanConSpecificationsYEntityGraph() {
        crearProyectoConContrato();

        var perfiles = adminPerfilService.buscar(
                new BuscarPerfilesAdminFiltro(null, null, null, null, null, null, null),
                0, 20, false);
        var proyectos = adminProyectoService.listarProyectos(null, 0, 20);
        var contratos = adminContratoService.buscar(
                new BuscarContratosAdminFiltro(
                        null, null, null, null, null, null, null, null, null, null, null),
                0, 20, false);

        assertThat(perfiles.contenido()).isNotEmpty();
        assertThat(proyectos.contenido()).isNotEmpty();
        assertThat(contratos.contenido()).isNotEmpty();
    }
}
