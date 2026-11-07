# Ficha técnica — Reseñas de perfil

Extensión de UC-37 para mostrar las reseñas recibidas por un perfil. El alta de
reseñas sigue perteneciendo al flujo de finalización de proyecto; esta ficha
cubre únicamente las pantallas de lectura.

## 1. Alcance

- Tarjeta lateral “Reseñas” en perfiles ajenos: promedio, total y últimas dos.
- Listado completo con promedio, distribución 5→1 y paginación “Cargar más”.
- Detalle de una reseña.
- Disponible tanto en el perfil propio como en perfiles ajenos.
- Sin “Escribir reseña” y sin “Reportar reseña”.

## 2. Endpoints

### 2.1. Listado paginado

```http
GET /perfiles/{idPerfil}/calificaciones?page=0&size=10
```

- `page`: 0-indexado, default `0`.
- `size`: default `10`, máximo `50`.
- Orden: `fechaRegistro DESC`, luego `idCalificacion DESC`.

Respuesta `200`:

```json
{
  "idPerfil": 5,
  "resumen": {
    "promedio": 4.67,
    "total": 3,
    "distribucion": [
      { "valoracion": 5, "cantidad": 2 },
      { "valoracion": 4, "cantidad": 1 },
      { "valoracion": 3, "cantidad": 0 },
      { "valoracion": 2, "cantidad": 0 },
      { "valoracion": 1, "cantidad": 0 }
    ]
  },
  "calificaciones": {
    "contenido": [
      {
        "idCalificacion": 1,
        "valoracion": 5,
        "resenia": "Excelente predisposición.",
        "fechaRegistro": "2026-10-12T15:30:00",
        "emisor": {
          "idPerfil": 10,
          "nombreArtistico": "Lucía Benítez",
          "profesion": "Diseñadora de indumentaria",
          "fotoUrl": "/uploads/perfiles/lucia.jpg"
        },
        "proyecto": {
          "idProyecto": 100,
          "nombre": "Editorial Primavera 2026"
        }
      }
    ],
    "paginaActual": 0,
    "tamanoPagina": 10,
    "totalElementos": 3,
    "totalPaginas": 1,
    "primera": true,
    "ultima": true
  }
}
```

La tarjeta lateral usa `size=2` y reutiliza el mismo contrato.

### 2.2. Detalle

```http
GET /perfiles/{idPerfil}/calificaciones/{idCalificacion}
```

Respuesta `200`: el mismo objeto de `contenido`. Responde `404` si la reseña no
existe o no pertenece al perfil consultado.

### 2.3. Visibilidad

Se reutilizan las reglas de `GET /perfiles/{idPerfil}`:

- Perfil propio: visible salvo estado `Baja`.
- Perfil ajeno: perfil `Activo` y usuario dueño `Activo`; si no, `404`.
- Administrador: `403`.
- Las reseñas de autores que hoy están en `Baja` se muestran igual porque son
  históricas.

## 3. Backend

| Archivo | Rol |
|---|---|
| `perfiles/controlador/PerfilResenaController.java` | Endpoints de listado y detalle. |
| `perfiles/servicio/ConsultarResenasPerfilService.java` | Paginación, resumen y detalle. |
| `perfiles/mapper/ResenaPerfilMapper.java` | Entidad → DTO. |
| `perfiles/dto/ResenasPerfilResponse.java` | Respuesta del listado. |
| `perfiles/dto/ResumenResenasResponse.java` | Promedio, total y distribución. |
| `perfiles/dto/ResenaPerfilResponse.java` | Item/detalle. |
| `perfiles/dto/AutorResenaResponse.java` | Datos públicos del emisor. |
| `perfiles/dto/ProyectoResenaResponse.java` | Proyecto asociado. |
| `repositorio/CalificacionPerfilRepository.java` | Consultas por receptor y agregado. |
| `perfiles/exception/ResenaNoEncontradaException.java` | 404 de reseña. |
| `perfiles/servicio/VerPerfilService.java` | `obtenerPerfilVisible` reutilizable. |

No se modifica el modelo `CalificacionPerfil` ni el flujo de escritura de UC-37.
Se agrega el índice `IDX_calificacion_receptor_fecha` para el orden por fecha.

## 4. Frontend

| Archivo | Rol |
|---|---|
| `services/calificacionPerfilService.js` | `listar` y `obtener`. |
| `utils/resenasConstants.js` | Normalización, nombres, iniciales, porcentajes. |
| `utils/fechas.js` | `formatearFechaResena`. |
| `components/perfil/ResenasCard.vue` | Tarjeta lateral con promedio y últimas 2. |
| `components/perfil/ResenasListaPanel.vue` | Listado, distribución y “Cargar más”. |
| `components/perfil/ResenaDetallePanel.vue` | Detalle de la reseña. |
| `views/perfil/InicioPerfilView.vue` | Integración de sección, tarjeta y navegación. |
| `router/index.js` | Rutas de detalle de reseña. |

### 4.1. Rutas

- `/perfiles/:id/resenas/:idCalificacion` → `ver-perfil-resena`
- `/inicio-perfil/resenas/:idCalificacion` → `inicioperfil-resena`

La sección de listado sigue usando las rutas existentes:

- `/perfiles/:id/resenas` → `ver-perfil-seccion`
- `/inicio-perfil/resenas` → `inicioperfil-seccion`

### 4.2. Navegación

- Click en el nombre del autor → perfil del autor. Si es el perfil activo, va a
  `/inicio-perfil`; si no, a `/perfiles/{idPerfilAutor}`.
- Click en una reseña → detalle.
- “Ver todas” → listado.
- “Volver al perfil” → ficha del perfil consultado.
- “Volver al listado” → listado de reseñas.
- El chip del proyecto es solo visual, sin link.

## 5. Tests

Backend:

- `ConsultarResenasPerfilServiceTest`: resumen, paginación, límites de `size`,
  visibilidad y detalle.
- `PerfilResenaControllerTest`: 200, defaults de paginación y 404.
- `ResenasPerfilIntegrationTest`: consulta real contra PostgreSQL, EntityGraph,
  resumen agregado, segunda página y reglas de visibilidad.

Frontend:

- `test/resenasConstants.test.js`: normalización, nombres, iniciales,
  porcentajes y formateo.
