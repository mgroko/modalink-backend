package org.mgroko.backend.repositorio;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.modelo.Contrato;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoContrato;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.usuario.servicio.BajaDefinitivaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class BajaDefinitivaContratoIntegrationTest
        extends AbstractFirmaContratoIntegrationTest {

    @Autowired
    private BajaDefinitivaService bajaDefinitivaService;

    @Autowired
    private PerfilRepository perfilRepository;

    @Test
    void bajaDeCuenta_cancelaContratoPendienteDeFirma() {
        Datos datos = crearProyectoConContrato();

        Usuario miembro = usuarioRepository.findById(datos.miembro().getIdUsuario())
                .orElseThrow();
        miembro.setEstado(EstadoUsuario.PendienteBaja);
        miembro.setFechaSolicitudBaja(LocalDateTime.now().minusDays(31));
        usuarioRepository.saveAndFlush(miembro);

        Perfil perfil = perfilRepository.findById(datos.perfilMiembro().getIdPerfil())
                .orElseThrow();
        perfil.setEstado(EstadoPerfil.PendienteBaja);
        perfil.setFechaSolicitudBaja(miembro.getFechaSolicitudBaja());
        perfilRepository.saveAndFlush(perfil);

        em.flush();
        em.clear();

        bajaDefinitivaService.expirarCuentasVencidas(30);
        em.flush();
        em.clear();

        Contrato contrato = contrato(datos.idContrato());
        assertThat(contrato.getEstado()).isEqualTo(EstadoContrato.CANCELADO);
        assertThat(contrato.getMotivoCancelacion()).isNotBlank();
        assertThat(contrato.getFechaCancelacion()).isNotNull();

        assertThat(eventos(datos.idContrato()))
                .extracting(evento -> evento.getTipoEvento().name())
                .contains("FIRMA_CANCELADA", "MIEMBRO_BAJA", "CANCELADO");
    }
}
