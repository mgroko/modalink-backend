# Ficha técnica — Presencia por perfil (Mi red)

> **Estado:** backend implementado.
> **Rama:** `modulo-conexiones`.
> **Casos relacionados:** UC-43, UC-44, UC-44 bis, UC-45, UC-46.
> **Extensión del mockup:** no forma parte del catálogo numerado de UC.

## 1. Objetivo

Mostrar en “Mi red” si **un perfil** está en línea o desconectado. La presencia es
por perfil, no por usuario: sólo el perfil activo de cada sesión recibe heartbeats.
Los demás perfiles del mismo usuario se muestran desconectados, salvo que estén
activos en otro dispositivo.

El contrato expone únicamente `enLinea: boolean`. **No se expone la última
actividad** ni un “hace X”: la UI muestra “En línea” o “Desconectado”.

## 2. Modelo

- Columna: `perfil.ultima_actividad` (`timestamp`).
- Fuente de verdad: el último heartbeat del perfil.
- Umbral: 120 segundos por defecto (`app.presencia.umbral-en-linea`).
- Heartbeat esperado: cada 30 segundos desde el frontend.
- Un perfil está en línea si:
  - el perfil está `ACTIVO`;
  - su usuario está `ACTIVO`;
  - `ultima_actividad` no es nula y está dentro del umbral.
- El logout no marca offline; el perfil vence por el umbral.
- Cerrar una pestaña no es un evento confiable; también vence por el umbral.

## 3. Endpoints

### `POST /presencia/heartbeat`

Requiere sesión con rol `USUARIO` y perfil activo. No recibe body. Devuelve `204`.

```http
POST /presencia/heartbeat
```

El frontend debe enviar el header CSRF (`X-XSRF-TOKEN`) porque es `POST`.

### `GET /presencia?idsPerfil=12,18,25`

Snapshot para hasta 200 perfiles. No requiere perfil activo, sólo sesión autenticada.

```json
[
  { "idPerfil": 12, "enLinea": true },
  { "idPerfil": 18, "enLinea": false }
]
```

- IDs inexistentes o inválidos devuelven `enLinea: false` (los inválidos, `400`).
- Más de 200 IDs: `400 IDS_PERFIL_INVALIDOS`.
- `Cache-Control: no-store`.

### `GET /presencia/stream?idsPerfil=12,18,25`

Stream SSE. Requiere al menos un ID y hasta 200.

- `Content-Type: text/event-stream`.
- Evento inicial `snapshot` con el mismo formato que `GET /presencia`.
- Eventos posteriores `presencia`:

```text
event: presencia
data: {"idPerfil":12,"enLinea":false}
```

- Ping cada 20 segundos.
- Timeout del stream: 30 minutos.
- Si se corta, el cliente reconecta y recibe un snapshot nuevo.

## 4. `enLinea` en las respuestas existentes

| Endpoint | Campo | Uso en el mockup |
|---|---|---|
| `GET /perfiles/{idPerfil}` | `enLinea` | Detalle/modal de perfil |
| `GET /perfiles/buscar` | `enLinea` | Resultados de búsqueda |
| `GET /conexiones` | `enLinea` | Tarjetas de la pestaña Conexiones |
| `GET /solicitudes-conexion` | `enLinea` | Filas de Recibidas y Enviadas |

`enLinea` siempre corresponde a la contraparte del ítem:
- en conexiones, la contraparte de la conexión;
- en solicitudes, el remitente si es recibida o el destinatario si es enviada;
- en detalle, el perfil consultado.

## 5. Filtro y resumen de Conexiones

### `GET /conexiones?estadoPresencia=EN_LINEA`

Valores: `EN_LINEA`, `DESCONECTADO`. Si se omite, devuelve todas.

```http
GET /conexiones?estadoPresencia=EN_LINEA&page=0&size=20
```

El filtro se aplica a la contraparte y combina con `texto`, `nombre`, `apellido`,
`nombreArtistico` e `idProfesion`.

### `GET /conexiones/resumen`

```json
{
  "totalConexiones": 6,
  "totalEnLinea": 4,
  "profesiones": [
    { "idProfesion": 2, "nombre": "Fotógrafo", "cantidad": 3 },
    { "idProfesion": 5, "nombre": "Modelo", "cantidad": 2 }
  ]
}
```

Sirve para:
- el hero “X conexiones · Y en línea ahora”;
- los chips de profesión con sus contadores.

Para el riel “En línea ahora”, usar
`GET /conexiones?estadoPresencia=EN_LINEA&size=20`; `totalElementos` es la cantidad
total en línea y `contenido` son las tarjetas.

## 6. Integración sugerida en el frontend

### Heartbeat

- Enviar al montar la app, cada 30 segundos y al recuperar foco/visibilidad.
- Usar `setTimeout` recursivo, no `setInterval`, para evitar solapamientos.
- Detener el heartbeat al logout o ante un `401`.
- El perfil activo se muestra en línea localmente mientras la app está abierta.

### SSE

- Abrir un `EventSource` con `{ withCredentials: true }` si el frontend y el backend
  están en orígenes distintos.
- Suscribirse al conjunto de IDs visibles: conexiones, solicitudes de la pestaña
  activa y el perfil abierto en el detalle/modal.
- Al cambiar de pantalla, cerrar y reabrir con el nuevo conjunto (con debounce).
- Aplicar el `snapshot` inicial y luego los eventos `presencia`.
- Fallback: si el stream falla, consultar `GET /presencia?idsPerfil=...` cada 30 s.
- Una conexión por pestaña; evitar abrir una por cada tarjeta.

### UI

- Punto verde y texto “En línea” sólo si `enLinea === true`.
- Texto “Desconectado” sin “hace X”.
- En el detalle de perfil, usar el `enLinea` de `GET /perfiles/{idPerfil}` y
  actualizarlo con el stream mientras el detalle está abierto.
- En la pestaña Conexiones:
  - “Todos”: mostrar la página y ordenar en el cliente las conexiones en línea primero;
  - “En línea”: `estadoPresencia=EN_LINEA`;
  - “Desconectados”: `estadoPresencia=DESCONECTADO`.
- En Recibidas/Enviadas: usar `enLinea` de cada fila.

### Contadores del mockup

- Conexiones totales: `totalElementos` de `GET /conexiones?size=1`.
- En línea: `totalElementos` de
  `GET /conexiones?estadoPresencia=EN_LINEA&size=1`, o `totalEnLinea` de
  `GET /conexiones/resumen`.
- Solicitudes por estado: `GET /solicitudes-conexion?direccion=...&size=1`
  (con `estado=PENDIENTE` o `todosLosEstados=true`) y leer `totalElementos`.
- Badge de Recibidas: `GET /solicitudes-conexion?direccion=RECIBIDA&estado=PENDIENTE&size=1`.
- El perfil activo del header se puede mostrar en línea localmente mientras la
  app está abierta; no requiere un campo extra del backend.

## 7. Errores

| HTTP | `codigo` | Cuándo |
|---|---|---|
| `400` | `IDS_PERFIL_INVALIDOS` | Más de 200 IDs, IDs negativos o stream sin IDs |
| `403` | `CSRF_INVALIDO` | Heartbeat sin token CSRF |
| `404` | — | Heartbeat sin perfil activo |
| `401` | `NO_AUTENTICADO` | Sin sesión |

## 8. Consideraciones

- La presencia es **best-effort**: mide actividad del cliente, no presencia humana.
- Un script o una pestaña abierta puede mantener un perfil en línea.
- No usar `enLinea` para autorización ni como dato sensible.
- Con varias instancias del backend, el bus en memoria no comparte eventos; hace
  falta Redis Pub/Sub o PostgreSQL `LISTEN/NOTIFY`. La lectura por base de datos
  sigue siendo correcta, pero los eventos SSE no llegan a todos los nodos.
- La migración asociada es
  `docs/3_diseño/migraciones/2026-11-09_presencia_perfiles.sql`.
