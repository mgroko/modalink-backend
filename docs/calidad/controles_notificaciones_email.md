# Controles — Notificaciones por email

## Alcance

Esta extensión envía por email las notificaciones in-app de prioridad `ALTA`.
En desarrollo/testing el SMTP apunta a Mailpit; no se entregan correos reales.
No hay preferencias por usuario en esta versión: la política es global.

## Base de datos

- `notificacion_tipo.prioridad`: `ACTIVIDAD_ASIGNADA` y `CONTRATO_FIRMADO` pasan
  a `ALTA`; `PUBLICACION_COMENTARIO` y `PUBLICACION_MEGUSTA` quedan en `MEDIA`.
- `notificacion_entrega`: outbox con `canal`, `estado`, `intentos`,
  `proximo_intento`, `fecha_ultimo_intento`, `fecha_envio` y `error`.
  Estados: `PENDIENTE`, `ENVIANDO`, `ENVIADO`, `FALLIDO`, `CANCELADO`.
- `token_acceso_email`: token de un solo uso para auto-login. Solo se guarda el
  hash SHA-256; el valor crudo viaja en el email.
- Triggers:
  - `trg_encolar_entrega_email_notificacion` crea una entrega `PENDIENTE` por
    cada notificación `ALTA`.
  - Las entregas se eliminan por cascada si se elimina la notificación.

## Scheduler

- Bean condicionado a `app.notificaciones.email.habilitado=true`.
- Reclama lotes con `FOR UPDATE SKIP LOCKED` y estado `ENVIANDO`.
- Envía fuera de la transacción que reclama la fila.
- Reintenta con backoff exponencial hasta `NOTIFICACION_EMAIL_REINTENTOS_MAX`.
- Al superar el máximo, la entrega queda `CANCELADO` con el error.
- Las entregas que quedan `ENVIANDO` por una caída se recuperan a los 10 minutos.
- Usuarios `BAJA` o `DESHABILITADO` no reciben emails; la entrega se cancela.
- No se envían notificaciones históricas: el trigger solo aplica a inserts nuevos.

## Token de auto-login

- 32 bytes aleatorios, Base64 URL-safe.
- Hash SHA-256 persistido; nunca se guarda el valor crudo.
- Vencimiento: `NOTIFICACION_EMAIL_TOKEN_TTL_MINUTOS` (60 por defecto).
- Un solo uso; se marca `usado_en` al validar.
- Se vincula a la entrega, al usuario y al perfil contextual de la notificación.
- El endpoint `POST /auth/email/validar` es público y está exento de CSRF.
- Si el perfil contextual no pertenece al usuario o no está activo, se usa el
  perfil activo por defecto.

## Seguridad

- El link usa el fragmento `#token=...` del frontend: no se envía al servidor.
- `Cache-Control: no-store` y `Referrer-Policy: no-referrer` en la respuesta.
- El `redirect` se valida en el frontend como ruta interna.
- El email escapa todo contenido dinámico.
- No se loguean tokens.
- Mailpit no debe exponerse en producción.

## Configuración

Claves en `configuracion_sistema`:

- `NOTIFICACION_EMAIL_REINTENTOS_MAX` (existente)
- `NOTIFICACION_EMAIL_BACKOFF_MINUTOS` (existente)
- `NOTIFICACION_EMAIL_LOTE`
- `NOTIFICACION_EMAIL_INTERVALO_SEGUNDOS`
- `NOTIFICACION_EMAIL_TOKEN_TTL_MINUTOS`
- `NOTIFICACION_EMAIL_TOKEN_LIMPIEZA_DIAS`

Properties:

- `app.notificaciones.email.habilitado`
- `app.notificaciones.email.remitente`
- `spring.mail.host`
- `spring.mail.port`

## Tests

- Unitarios de builder, resolver de links, política, tokens y scheduler.
- Integración de outbox con PostgreSQL y `ModaLinkBD.sql`.
- Integración de envío con Mailpit Testcontainer.
- La suite existente no se ve afectada porque el scheduler está deshabilitado
  por defecto.
