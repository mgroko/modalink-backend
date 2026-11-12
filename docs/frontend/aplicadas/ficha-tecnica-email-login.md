# Ficha técnica — Auto-login desde email

## Objetivo

Permitir que el link de una notificación por email abra una sesión en ModaLink
sin pedir credenciales, respetando el perfil contextual de la notificación.

## Formato del link

El backend arma el link contra el frontend:

```text
{FRONTEND_URL}/auth/email#token={token}&redirect={rutaDestino}
```

Ejemplo:

```text
http://localhost:5173/auth/email#token=abc123&redirect=%2Fproyectos%2F12
```

El token viaja en el fragmento (`#`), que no se envía al servidor. Así no queda
en logs de acceso ni en el `Referer`.

## Frontend

Ruta nueva:

```text
/auth/email
```

Vista: `src/views/auth/EmailLoginView.vue`.

Flujo:

1. Lee `token` y `redirect` de `window.location.hash`.
2. Limpia el hash con `history.replaceState`.
3. Llama `POST /auth/email/validar` con `{ token }`.
4. Si responde `200`:
   - `marcarSesionRestaurada(usuario)`
   - `refrescarSesion()`
   - navega al `redirect` validado o a home/dashboard.
5. Si falla, redirige a `/login` con `emailError` y conserva `redirect`.

El `redirect` se valida con `src/utils/redirect.js`: solo rutas internas que
empiecen con `/`, sin `//`, sin `/login` y sin `/auth/email`.

## Backend

Endpoint:

```http
POST /auth/email/validar
Content-Type: application/json

{ "token": "..." }
```

Respuesta exitosa:

- `200 OK`
- `Set-Cookie: jwt=...` (HTTP-only)
- Body: `AuthResponse` con `usuario`

Errores:

| Código | HTTP | Situación |
|---|---|---|
| `TOKEN_REQUERIDO` | 400 | Falta token |
| `TOKEN_INVALIDO` | 401 | No existe |
| `TOKEN_EXPIRADO` | 410 | Venció |
| `TOKEN_USADO` | 409 | Ya se usó |
| `USUARIO_NO_ACTIVO` | 403 | Cuenta inhabilitada |

El endpoint es público y está exento de CSRF. El token es la credencial.

## Perfil contextual

La notificación puede tener `id_perfil_contexto`. El token lo guarda y, al
validar, el backend usa ese perfil si pertenece al usuario y está `ACTIVO`.
Si no, usa el perfil activo por defecto. Para administradores no se activa
ningún perfil.

## Seguridad

- Token de un solo uso, 60 minutos, hash SHA-256 en base.
- El token se elimina por cascada si se elimina la notificación/entrega.
- No se loguea el token.
- El mail muestra el correo de la cuenta para detectar confusiones.
- Mailpit es solo para desarrollo/testing.
