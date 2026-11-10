# Ficha técnica — Listar red de contactos (UC-44 bis)

> **Estado:** backend implementado.
> **Rama:** `modulo-conexiones`.
> **Casos de uso relacionados:** UC-43 (envío/cancelación), UC-44 (eliminar conexión), UC-45 (gestión de solicitudes), UC-46 (buscar solicitudes).

## 1. Objetivo

Permitir que el perfil activo consulte su red de contactos: las conexiones
`ACTIVA` que mantiene con otros perfiles, con los datos necesarios para
mostrarlas en la pestaña **Conexiones** de “Mi red”.

Este caso es derivado del mockup y no forma parte del catálogo numerado de
requisitos.

## 2. Endpoint

### `GET /conexiones`

Requiere sesión con rol `USUARIO` y perfil activo seleccionado.

### Query params

| Parámetro | Tipo | Obligatorio | Descripción |
|---|---|---|---|
| `texto` | string | No | Busca en nombre, apellido, nombre artístico y profesión de la contraparte |
| `nombre` | string | No | Nombre de la contraparte, parcial, case-insensitive y sin acentos |
| `apellido` | string | No | Apellido de la contraparte, parcial, case-insensitive y sin acentos |
| `nombreArtistico` | string | No | Nombre artístico de la contraparte, parcial, case-insensitive y sin acentos |
| `idProfesion` | number | No | Profesión exacta de la contraparte |
| `page` | number | No | Base 0, default `0` |
| `size` | number | No | Default `20`, máximo `100` |

Los filtros son combinables con `AND`.

### Ejemplo

```http
GET /conexiones?texto=bruno&page=0&size=20
```

### Response `200 OK`

```json
{
  "contenido": [
    {
      "idConexion": 55,
      "fechaInicioConexion": "2026-11-01T10:00:00",
      "idPerfil1": 7,
      "idPerfil2": 42,
      "perfil": {
        "idPerfil": 42,
        "nombreArtistico": "Bruno Estilo",
        "nombre": "Bruno",
        "apellido": "Gómez",
        "idProfesion": 2,
        "nombreProfesion": "Fotógrafo",
        "fotoUrl": "https://...",
        "ciudad": {
          "idCiudad": 1,
          "idExterno": "3430000",
          "fuenteApi": "GEOREF",
          "nombre": "Posadas",
          "provincia": {
            "idProvincia": 1,
            "nombre": "Misiones",
            "pais": {
              "idPais": 1,
              "nombre": "Argentina"
            }
          }
        }
      }
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

- Sólo se devuelven conexiones en estado `ACTIVA` donde participa el perfil activo.
- `perfil` es siempre la **contraparte**; si el perfil activo es `idPerfil2`, se
  devuelve `idPerfil1` y viceversa.
- Las conexiones `ELIMINADA` no aparecen en el listado.
- Orden: `fechaInicioConexion DESC`, luego `idConexion DESC`.
- Sin resultados: `200` con `contenido: []`.
- `fechaInicioConexion` permite mostrar “conectados desde”.

## 4. Errores

| HTTP | `codigo` | Cuándo |
|---|---|---|
| `400` | — | `page` o `size` inválidos |
| `404` | — | No hay perfil activo en la sesión |

## 5. Flujo sugerido en el frontend

1. Al abrir la pestaña **Conexiones**, llamar a `GET /conexiones`.
2. Enviar el buscador con debounce usando `texto`.
3. Mostrar la tarjeta con foto, nombre artístico, ciudad y profesión de `perfil`.
4. Ofrecer la acción “Eliminar conexión” con `idConexion` (UC-44).
5. Paginar con `page`/`size`; si `contenido` viene vacío, mostrar el estado vacío
   de la red.

## 6. Consideraciones

- No se listan solicitudes pendientes ni conexiones eliminadas; esos datos se
  consultan con UC-46.
- El filtro de texto usa `unaccent` de PostgreSQL.
- La consulta se pagina en base de datos y las asociaciones de la página se
  cargan con `@EntityGraph` para evitar N+1.
- El tamaño máximo de página es 100 y los valores menores o iguales a cero se
  normalizan al default.

## 7. Integración de las pestañas “Mi red”

El mockup de “Mi red” usa tres pestañas. La fuente de datos de cada una es:

| Pestaña | Endpoint | Parámetros |
|---|---|---|
| Conexiones | `GET /conexiones` | filtros de UC-44 bis |
| Recibidas | `GET /solicitudes-conexion` | `direccion=RECIBIDA&todosLosEstados=true` |
| Enviadas | `GET /solicitudes-conexion` | `direccion=ENVIADA&todosLosEstados=true` |

Para las pestañas de solicitudes se usa `todosLosEstados=true` porque el default
de UC-46 es `PENDIENTE`. `todosLosEstados` no se puede combinar con `estado`;
esa combinación responde `400 FILTRO_INVALIDO`. Si se quiere acotar una pestaña a
un estado, enviar `estado=...` sin `todosLosEstados`.

Los `link` de las notificaciones de conexiones apuntan a:

| Notificación | Link |
|---|---|
| `SOLICITUD_CONEXION_RECIBIDA` | `/mi-red?tab=recibidas` |
| `SOLICITUD_CONEXION_ACEPTADA` | `/mi-red?tab=conexiones&idConexion={idConexion}` |
| `SOLICITUD_CONEXION_RECHAZADA` | `/mi-red?tab=enviadas&estado=RECHAZADA` |
| `CONEXION_ELIMINADA` | `/mi-red?tab=conexiones` |
