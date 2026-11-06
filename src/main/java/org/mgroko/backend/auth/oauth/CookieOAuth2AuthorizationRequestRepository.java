package org.mgroko.backend.auth.oauth;

import java.time.Duration;
import java.util.Map;
import java.util.Set;

import org.mgroko.backend.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Guarda la solicitud de autorización OAuth2 en una cookie firmada en lugar de
 * la sesión HTTP. Así el resto de la aplicación sigue siendo stateless.
 */
@Component
public class CookieOAuth2AuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    public static final String COOKIE_NAME = "oauth2_auth_request";
    private static final long MAX_AGE_SECONDS = 300;

    private final JwtService jwtService;
    private final ObjectMapper objectMapper;
    private final boolean secure;
    private final String sameSite;

    public CookieOAuth2AuthorizationRequestRepository(
            JwtService jwtService,
            ObjectMapper objectMapper,
            @Value("${app.cookie.secure:false}") boolean secure,
            @Value("${app.cookie.same-site:Lax}") String sameSite) {
        this.jwtService = jwtService;
        this.objectMapper = objectMapper;
        this.secure = secure;
        this.sameSite = sameSite;
    }

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        String valor = leerCookie(request);
        if (valor == null || valor.isBlank()) {
            return null;
        }

        try {
            Claims claims = jwtService.validarYObtenerClaims(valor);
            String json = claims.get("solicitud", String.class);
            if (json == null || json.isBlank()) {
                return null;
            }
            return objectMapper.readValue(json, SolicitudSerializada.class).aAuthorizationRequest();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void saveAuthorizationRequest(
            OAuth2AuthorizationRequest authorizationRequest,
            HttpServletRequest request,
            HttpServletResponse response) {

        if (authorizationRequest == null) {
            eliminarCookie(response);
            return;
        }

        try {
            String json = objectMapper.writeValueAsString(
                    SolicitudSerializada.desde(authorizationRequest));
            String ticket = jwtService.generarTokenConExpiracion(
                    "oauth2-auth-request",
                    Map.of("solicitud", json),
                    MAX_AGE_SECONDS * 1000L);

            ResponseCookie cookie = base(COOKIE_NAME, ticket)
                    .maxAge(Duration.ofSeconds(MAX_AGE_SECONDS))
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo guardar la solicitud de autorización OAuth2.", e);
        }
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(
            HttpServletRequest request,
            HttpServletResponse response) {
        OAuth2AuthorizationRequest authorizationRequest = loadAuthorizationRequest(request);
        eliminarCookie(response);
        return authorizationRequest;
    }

    private String leerCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if (COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private void eliminarCookie(HttpServletResponse response) {
        ResponseCookie cookie = base(COOKIE_NAME, "")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private ResponseCookie.ResponseCookieBuilder base(String nombre, String valor) {
        return ResponseCookie.from(nombre, valor)
                .httpOnly(true)
                .secure(secure)
                .path("/")
                .sameSite(sameSite);
    }

    /**
     * Representación serializable de {@link OAuth2AuthorizationRequest}. No se
     * serializa la clase de Spring directamente para no depender de su forma
     * interna.
     */
    record SolicitudSerializada(
            String authorizationUri,
            String clientId,
            String redirectUri,
            Set<String> scopes,
            String state,
            Map<String, Object> additionalParameters,
            String authorizationRequestUri,
            Map<String, Object> attributes) {

        static SolicitudSerializada desde(OAuth2AuthorizationRequest request) {
            return new SolicitudSerializada(
                    request.getAuthorizationUri(),
                    request.getClientId(),
                    request.getRedirectUri(),
                    request.getScopes(),
                    request.getState(),
                    request.getAdditionalParameters(),
                    request.getAuthorizationRequestUri(),
                    request.getAttributes());
        }

        OAuth2AuthorizationRequest aAuthorizationRequest() {
            OAuth2AuthorizationRequest.Builder builder = OAuth2AuthorizationRequest.authorizationCode()
                    .authorizationUri(authorizationUri)
                    .clientId(clientId)
                    .redirectUri(redirectUri)
                    .scopes(scopes)
                    .state(state)
                    .additionalParameters(additionalParameters != null ? additionalParameters : Map.of())
                    .authorizationRequestUri(authorizationRequestUri);

            if (attributes != null && !attributes.isEmpty()) {
                builder.attributes(attributes);
            }

            return builder.build();
        }
    }
}
