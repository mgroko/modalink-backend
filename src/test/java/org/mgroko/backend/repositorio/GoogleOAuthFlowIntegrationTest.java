package org.mgroko.backend.repositorio;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import jakarta.servlet.http.Cookie;

/**
 * Verifica el wiring real de OAuth2 con Google: endpoint de entrada, redirect
 * al proveedor y persistencia del state en la cookie firmada.
 */
@SpringBootTest
@AutoConfigureMockMvc
class GoogleOAuthFlowIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void iniciarGoogle_redirigeAlEndpointDeAutorizacion() throws Exception {
        mockMvc.perform(get("/auth/google"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "/oauth2/authorization/google"));
    }

    @Test
    void authorizationEndpoint_redirigeAGoogleYGuardaStateEnCookie() throws Exception {
        MvcResult resultado = mockMvc.perform(get("/oauth2/authorization/google"))
                .andExpect(status().isFound())
                .andReturn();

        String location = resultado.getResponse().getHeader("Location");
        assertNotNull(location);
        assertTrue(location.contains("accounts.google.com"));
        assertTrue(location.contains("client_id=dev-google-client-id"));
        assertTrue(location.contains("state="));

        Cookie cookie = resultado.getResponse().getCookie("oauth2_auth_request");
        assertNotNull(cookie, "La solicitud de autorización debe viajar en una cookie firmada");
        assertTrue(cookie.isHttpOnly());
    }
}
