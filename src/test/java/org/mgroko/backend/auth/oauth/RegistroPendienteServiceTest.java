package org.mgroko.backend.auth.oauth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mgroko.backend.auth.exception.RegistroPendienteInvalidoException;
import org.mgroko.backend.security.JwtService;

class RegistroPendienteServiceTest {

    private static final String SECRETO = "secreto-de-prueba-para-jwt-con-longitud-suficiente-123456";

    private RegistroPendienteService registroPendienteService;

    @BeforeEach
    void setUp() {
        JwtService jwtService = new JwtService(SECRETO, 3_600_000L);
        registroPendienteService = new RegistroPendienteService(jwtService);
    }

    @Test
    void crearYLeerTicket_conservaLosDatosDeGoogle() {
        GoogleUserData datos = new GoogleUserData(
                "google-sub-123",
                "Persona@Example.com",
                true,
                "Ana",
                "Gómez",
                null);

        String ticket = registroPendienteService.crearTicket(datos);
        GoogleUserData leidos = registroPendienteService.leerTicket(ticket);

        assertEquals(datos.sub(), leidos.sub());
        assertEquals(datos.correo(), leidos.correo());
        assertTrue(leidos.emailVerificado());
        assertEquals(datos.nombre(), leidos.nombre());
        assertEquals(datos.apellido(), leidos.apellido());
    }

    @Test
    void leerTicket_nuloOLaBasura_lanzaExcepcion() {
        assertThrows(RegistroPendienteInvalidoException.class,
                () -> registroPendienteService.leerTicket(null));
        assertThrows(RegistroPendienteInvalidoException.class,
                () -> registroPendienteService.leerTicket("no-es-un-jwt"));
    }

    @Test
    void leerTicket_conFirmaDeOtroSecreto_lanzaExcepcion() {
        JwtService otroJwt = new JwtService(
                "otro-secreto-de-prueba-con-longitud-suficiente-1234567890", 3_600_000L);
        String ticket = otroJwt.generarTokenConExpiracion(
                "google-registro",
                java.util.Map.of(
                        "proposito", "google_registro",
                        "sub", "sub-1",
                        "correo", "a@b.com",
                        "emailVerificado", true),
                60_000L);

        assertThrows(RegistroPendienteInvalidoException.class,
                () -> registroPendienteService.leerTicket(ticket));
    }
}
