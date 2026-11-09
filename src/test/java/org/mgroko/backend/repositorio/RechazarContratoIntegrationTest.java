package org.mgroko.backend.repositorio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.contratos.dto.ContratoDetalleResponse;
import org.mgroko.backend.contratos.dto.RechazarContratoRequest;
import org.mgroko.backend.contratos.exception.ContratoNoModificableException;
import org.mgroko.backend.contratos.servicio.FirmarContratoService;
import org.mgroko.backend.contratos.servicio.RechazarContratoService;
import org.mgroko.backend.modelo.Contrato;
import org.mgroko.backend.modelo.FirmaContrato;
import org.mgroko.backend.modelo.Notificacion;
import org.mgroko.backend.modelo.enums.EstadoContrato;
import org.mgroko.backend.modelo.enums.EstadoFirmaContrato;
import org.mgroko.backend.notificaciones.servicio.NotificacionService;
import org.mgroko.backend.proyectos.exception.AccesoDenegadoProyectoException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integración de UC-55: rechazar contrato con motivo.
 */
@SpringBootTest
@Transactional
class RechazarContratoIntegrationTest
        extends AbstractFirmaContratoIntegrationTest {

    @Autowired
    private RechazarContratoService rechazarContratoService;

    @Autowired
    private FirmarContratoService firmarContratoService;

    @Test
    void rechazar_conMotivo_rechazaCancelaPendientesYNotifica() {
        Datos datos = crearProyectoConContrato();

        ContratoDetalleResponse response = rechazar(
                datos.miembro().getIdUsuario(),
                datos.perfilMiembro().getIdPerfil(),
                datos,
                "No acepto la cláusula de exclusividad.");

        assertThat(response.estado()).isEqualTo("RECHAZADO_POR_PARTES");
        assertThat(response.miEstadoFirma()).isEqualTo("RECHAZADO");
        assertThat(response.puedeFirmar()).isFalse();
        assertThat(response.puedeRechazar()).isFalse();

        Contrato contrato = contrato(datos.idContrato());
        assertThat(contrato.getEstado())
                .isEqualTo(EstadoContrato.RECHAZADO_POR_PARTES);

        FirmaContrato rechazada = firma(
                datos.idContrato(), datos.miembroProyecto().getIdMiembro());
        assertThat(rechazada.getEstadoFirma())
                .isEqualTo(EstadoFirmaContrato.RECHAZADO);
        assertThat(rechazada.getMotivoRechazo())
                .isEqualTo("No acepto la cláusula de exclusividad.");
        assertThat(rechazada.getFechaRechazo()).isNotNull();

        FirmaContrato pendiente = firma(
                datos.idContrato(), datos.emisor().getIdMiembro());
        assertThat(pendiente.getEstadoFirma())
                .isEqualTo(EstadoFirmaContrato.CANCELADA);

        List<Notificacion> rechazo = notificaciones(datos.idContrato()).stream()
                .filter(n -> NotificacionService.TIPO_CONTRATO_RECHAZADO
                        .equals(n.getTipo()))
                .toList();

        assertThat(rechazo).hasSize(2);
        assertThat(rechazo)
                .allMatch(n -> datos.proyecto().getIdProyecto()
                        .equals(n.getIdProyecto()));
    }

    @Test
    void rechazar_sinMotivo_devuelveErrorDeNegocio() {
        Datos datos = crearProyectoConContrato();

        assertThatThrownBy(() -> rechazar(
                datos.miembro().getIdUsuario(),
                datos.perfilMiembro().getIdPerfil(),
                datos,
                "   "))
                .isInstanceOf(ContratoNoModificableException.class)
                .extracting("codigo")
                .isEqualTo("MOTIVO_RECHAZO_OBLIGATORIO");
    }

    @Test
    void rechazarDespuesDeFirmar_devuelveContratoYaFirmado() {
        Datos datos = crearProyectoConContrato();

        firmarContratoService.firmar(
                datos.miembro().getIdUsuario(),
                datos.perfilMiembro().getIdPerfil(),
                datos.proyecto().getIdProyecto(),
                datos.idContrato());

        assertThatThrownBy(() -> rechazar(
                datos.miembro().getIdUsuario(),
                datos.perfilMiembro().getIdPerfil(),
                datos,
                "Ya no quiero firmar."))
                .isInstanceOf(ContratoNoModificableException.class)
                .extracting("codigo")
                .isEqualTo("CONTRATO_YA_FIRMADO");
    }

    @Test
    void rechazarSinSerParte_devuelveAccesoDenegado() {
        Datos datos = crearProyectoConContrato();

        assertThatThrownBy(() -> rechazar(
                datos.noParte().getIdUsuario(),
                datos.perfilNoParte().getIdPerfil(),
                datos,
                "No soy parte."))
                .isInstanceOf(AccesoDenegadoProyectoException.class);
    }

    @Test
    void rechazarContratoYaRechazado_devuelveContratoNoPendiente() {
        Datos datos = crearProyectoConContrato();

        rechazar(datos.miembro().getIdUsuario(),
                datos.perfilMiembro().getIdPerfil(),
                datos,
                "Rechazo inicial.");

        assertThatThrownBy(() -> rechazar(
                datos.director().getIdUsuario(),
                datos.perfilDirector().getIdPerfil(),
                datos,
                "Rechazo posterior."))
                .isInstanceOf(ContratoNoModificableException.class)
                .extracting("codigo")
                .isEqualTo("CONTRATO_NO_PENDIENTE_FIRMA");
    }

    private ContratoDetalleResponse rechazar(
            Long idUsuario,
            Long idPerfil,
            Datos datos,
            String motivo) {

        return rechazarContratoService.rechazar(
                idUsuario,
                idPerfil,
                datos.proyecto().getIdProyecto(),
                datos.idContrato(),
                new RechazarContratoRequest(motivo));
    }
}
