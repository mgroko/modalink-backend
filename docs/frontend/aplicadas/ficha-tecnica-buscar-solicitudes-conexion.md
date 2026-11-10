# Ficha técnica — Buscar solicitudes de conexión (UC-46)

> **Estado:** backend implementado.
> **Rama:** `modulo-conexiones`.
> **Casos de uso relacionados:** UC-43 (envío/cancelación), UC-45 (gestión de solicitudes).

## 1. Objetivo

Permitir que el perfil activo busque las solicitudes de conexión que recibió o
envió, filtrando por dirección, estado, datos de la contraparte y fecha de
solicitud.

## 2. Endpoint

### `GET /solicitudes-conexion`

Requiere sesión con rol `USUARIO` y perfil activo seleccionado.

### Query params

| Parámetro | Tipo | Obligatorio | Descripción |
|---|---|---|---|
| `direccion` | `RECIBIDA` \| `ENVIADA` | No | Si se omite, incluye ambas |
| `estado` | `PENDIENTE` \| `ACEPTADA` \| `RECHAZADA` | No | Default `PENDIENTE` |
| `nombre` | string | No | Nombre de la contraparte, parcial, case-insensitive y sin acentos |
| `apellido` | string | No | Apellido de la contraparte, parcial, case-insensitive y sin acentos |
| `nombreArtistico` | string | No | Nombre artístico de la contraparte, parcial, case-insensitive y sin acentos |
| `idProfesion` | number | No | Profesión exacta de la contraparte |
| `fechaDesde` | ISO date | No | Inicio inclusivo del rango sobre `fechaEnvio` |
| `fechaHasta` | ISO date | No | Fin inclusivo del rango sobre `fechaEnvio` |
| `page` | number | No | Base 0, default `0` |
| `size` | number | No | Default `20`, máximo `100` |

### Ejemplo

```http
GET /solicitudes-conexion?direccion=RECIBIDA&estado=PENDIENTE&nombre=ana&page=0&size=20
```

### Response `200 OK`

```json
{
  "contenido": [
    {
      "idSolicitudConexion": 100,
      "estado": "PENDIENTE",
      "fechaEnvio": "2026-11-09T10:15:00",
      "fechaRespuesta": null,
      "direccion": "RECIBIDA",
      "idPerfilRemitente": 7,
      "idPerfilDestinatario": 42,
      "perfil": {
        "idPerfil": 7,
        "nombreArtistico": "Ana Moda",
        "nombre": "Ana",
        "apellido": "Pérez",
        "idProfesion": 3,
        "nombreProfesion": "Diseñadora",
        "fotoUrl": "https://..."
      },
      "puedeAceptar": true,
      "puedeRechazar": true,
      "puedeCancelar": false
    }
  ],
  "paginaActual": 0,
  "tamanoPagina": 20,
  "totalElementos": 1,
  "totalPaginas": 1,
  "primera": true,
  "ultima": true
}
```

## 3. Semántica

- `direccion = RECIBIDA`: el perfil activo es el destinatario; `perfil` es el remitente.
- `direccion = ENVIADA`: el perfil activo es el remitente; `perfil` es el destinatario.
- Sin `direccion`: se incluyen ambas.
- Los filtros de texto y profesión se aplican siempre a la **contraparte**.
- `estado` omitido equivale a `PENDIENTE`.
- Orden: `fechaEnvio DESC`, luego `idSolicitudConexion DESC`.
- Sin resultados: `200` con `contenido: []`.

### Flags

| Flag | Condición |
|---|---|
| `puedeAceptar` | `PENDIENTE` y `RECIBIDA` |
| `puedeRechazar` | `PENDIENTE` y `RECIBIDA` |
| `puedeCancelar` | `PENDIENTE` y `ENVIADA` |

## 4. Errores

| HTTP | `codigo` | Cuándo |
|---|---|---|
| `400` | `RANGO_FECHAS_INVALIDO` | `fechaDesde` posterior a `fechaHasta` |
| `400` | — | `direccion`, `estado`, `page` o `size` inválidos |
| `404` | — | No hay perfil activo en la sesión |

## 5. Flujo sugerido en el frontend

1. Al abrir la bandeja, llamar sin filtros para obtener las solicitudes `PENDIENTE` del perfil activo.
2. Ofrecer pestañas o selects para `direccion` y `estado`.
3. Enviar los filtros de texto con debounce.
4. Mostrar los resultados paginados; si `contenido` viene vacío, mostrar “sin resultados”.
5. Para cada fila:
   - si `puedeAceptar`/`puedeRechazar`, mostrar acciones de UC-45;
   - si `puedeCancelar`, mostrar “Cancelar solicitud”;
   - si no hay flags, mostrar solo el estado y los datos de la contraparte.

## 6. Consideraciones

- La búsqueda no incluye solicitudes canceladas porque se eliminan físicamente.
- El filtro de texto es insensible a mayúsculas y acentos; el backend usa `unaccent` de PostgreSQL.
- No se devuelven solicitudes de otros perfiles del mismo usuario: siempre se usa el perfil activo.
- Para consultar el historial, enviar `estado=ACEPTADA` o `estado=RECHAZADA`.
- El tamaño máximo de página es 100.
