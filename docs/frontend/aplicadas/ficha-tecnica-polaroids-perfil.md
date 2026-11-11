# Ficha técnica - Polaroids de perfil de modelo

> **Estado:** backend implementado y testeado. Frontend pendiente.
> **Casos de uso relacionados:** UC-10, UC-11, UC-14 (extensión de perfil de modelo).
> **Endpoints principales:** `/polaroids/tipos` y `/perfiles/{idPerfil}/polaroids`

## 1. Objetivo

Permitir que un perfil con profesión `MODELO` gestione hasta 8 polaroids
propias, cada una con una imagen, un tipo de plano/ángulo y un orden de
visualización.

El catálogo de tipos es fijo y vive en base de datos. No se hardcodea en Java
ni en el frontend y no tiene ABM en esta iteración.

Cada perfil puede tener:

- mínimo: **0 polaroids**;
- máximo: **8** por defecto, configurable con
  `app.polaroids.max-por-perfil`.

## 2. Catálogo de tipos

```http
GET /polaroids/tipos
```

Requiere sesión con rol `USUARIO`.

### Respuesta

```json
[
  {
    "idGrupoPolaroid": 1,
    "codigo": "PRIMER_PLANO",
    "nombre": "Primer plano",
    "tipos": [
      {
        "idTipoPolaroid": 1,
        "codigo": "PRIMER_PLANO_FRENTE",
        "nombre": "De frente",
        "nombreCompleto": "Primer plano · De frente"
      },
      {
        "idTipoPolaroid": 2,
        "codigo": "PRIMER_PLANO_PERFIL",
        "nombre": "De perfil",
        "nombreCompleto": "Primer plano · De perfil"
      },
      {
        "idTipoPolaroid": 3,
        "codigo": "PRIMER_PLANO_ESPALDAS",
        "nombre": "De espaldas",
        "nombreCompleto": "Primer plano · De espaldas"
      }
    ]
  }
]
```

El catálogo tiene 3 grupos y 9 tipos:

| Grupo | Tipos |
|---|---|
| Primer plano | De frente, De perfil, De espaldas |
| Plano americano | De frente, De perfil, De espaldas |
| Cuerpo completo | De frente, De perfil, De espaldas |

- No hay campo `descripcion`.
- No hay campo `orden` en el catálogo.
- El orden de presentación queda determinado por el `id` de la semilla.
- `nombreCompleto` es el texto recomendado para mostrar en el selector, la
  grilla y el modal.

## 3. Listar polaroids de un perfil

```http
GET /perfiles/{idPerfil}/polaroids
```

### Respuesta

```json
{
  "idPerfil": 7,
  "maximo": 8,
  "total": 5,
  "polaroids": [
    {
      "idPolaroid": 10,
      "idImagen": 88,
      "url": "/uploads/polaroids/polaroid_....webp",
      "orden": 1,
      "fechaRegistro": "2026-11-10T14:30:00",
      "tipo": {
        "idTipoPolaroid": 2,
        "codigo": "PRIMER_PLANO_PERFIL",
        "nombre": "De perfil",
        "nombreCompleto": "Primer plano · De perfil"
      }
    }
  ]
}
```

Reglas:

- Devuelve las polaroids ordenadas por `orden ASC`.
- Un perfil que no es `MODELO` devuelve `200` con `total = 0` y lista vacía.
- La visibilidad del perfil sigue las reglas de UC-14.
- El frontend puede usar `maximo` para mostrar “N de 8” sin hardcodear el tope.

## 4. Alta de polaroid

```http
POST /perfiles/{idPerfil}/polaroids
Content-Type: multipart/form-data
```

| Campo | Obligatorio | Descripción |
|---|---|---|
| `archivo` | Sí | Imagen JPEG, PNG o WebP de hasta 10 MB. |
| `idTipoPolaroid` | Sí | Tipo seleccionado del catálogo. |

Respuesta `201 Created`: `PolaroidResponse`.

Reglas:

- Solo el dueño del perfil puede crear polaroids.
- El perfil debe ser `MODELO`.
- El perfil no puede estar en `BAJA`.
- El alta es en un solo paso: imagen + tipo.
- La polaroid se agrega al final del orden.
- Se permite repetir el mismo tipo.
- La imagen se guarda en `uploads/polaroids/` y se sirve por `/uploads/**`.
- Si se alcanzó el máximo, responde `409`.
- Sin polaroids el perfil sigue siendo válido y usable.

## 5. Modificar polaroid

```http
PUT /perfiles/{idPerfil}/polaroids/{idPolaroid}
Content-Type: multipart/form-data
```

| Campo | Obligatorio | Descripción |
|---|---|---|
| `archivo` | No | Nueva imagen. |
| `idTipoPolaroid` | No | Nuevo tipo del catálogo. |

Debe enviarse **al menos uno** de los dos campos.

Comportamiento:

- Si se envía `archivo`:
  - reemplaza la imagen;
  - actualiza `fechaRegistro` a la fecha actual;
  - elimina la imagen anterior y programa el borrado del archivo.
- Si se envía solo `idTipoPolaroid`:
  - cambia el tipo;
  - **no** actualiza `fechaRegistro`.
- Si se envían ambos, se actualizan imagen, fecha y tipo en una transacción.
- El `orden` no cambia; para eso existe el endpoint de reordenamiento.

Respuesta `200 OK`: `PolaroidResponse` actualizado.

## 6. Reordenar polaroids

```http
PUT /perfiles/{idPerfil}/polaroids/orden
Content-Type: application/json
```

```json
{
  "idsPolaroid": [12, 10, 11]
}
```

Reglas:

- La lista debe contener **exactamente todos** los IDs del perfil.
- No se permiten IDs repetidos, faltantes ni ajenos.
- Se reasigna el orden `1..N`.
- La operación es atómica.
- Respuesta `200 OK`: `PolaroidsPerfilResponse` actualizado.

## 7. Eliminar polaroid

```http
DELETE /perfiles/{idPerfil}/polaroids/{idPolaroid}
```

Reglas:

- Solo el dueño puede eliminar.
- La polaroid debe pertenecer a ese perfil.
- Se elimina la polaroid, su fila de `imagen` y el archivo físico.
- El resto de las polaroids se compacta a `1..N`.
- Respuesta `204 No Content`.
- Después de eliminar, el frontend debe refrescar
  `GET /perfiles/{idPerfil}/polaroids`.

## 8. Errores

| HTTP | Caso |
|---|---|
| `400` | Archivo vacío, formato inválido, dimensiones excedidas, modificación sin cambios o reordenamiento inválido. |
| `401` | Sin sesión. |
| `403` | Sin rol `USUARIO`. |
| `404` | Perfil no visible, perfil ajeno, polaroid inexistente o tipo inexistente. |
| `409` | Perfil que no es `MODELO`, perfil en baja o máximo de polaroids alcanzado. |
| `500` | Error interno o de almacenamiento. |

## 9. Seguridad

- `/polaroids/**` está habilitado para rol `USUARIO` en `SecurityConfig`.
- Los endpoints bajo `/perfiles/**` ya estaban habilitados para ese rol.
- La lectura de polaroids respeta la visibilidad del perfil.
- La escritura requiere que el `idPerfil` de la ruta sea el perfil activo del
  usuario autenticado.

## 10. Frontend

### Grilla

- Mostrar la sección “Polaroids”.
- Mostrar el contador usando `total` y `maximo`: `"5 de 8"`.
- Cada tarjeta debe mostrar:
  - imagen (`url`);
  - etiqueta con `tipo.nombreCompleto`.
- Al hacer clic en una tarjeta, abrir el modal/visor con:
  - imagen en grande;
  - `tipo.nombreCompleto`;
  - “Subida hace…” calculado desde `fechaRegistro`.
- No mostrar descripción: el catálogo no tiene ese campo.
- Navegación `Anterior` / `Siguiente` dentro del modal sobre la misma lista,
  sin endpoint adicional.

### Gestión

- Mostrar el botón “Gestionar polaroids” solo si el perfil es propio.
- El alta debe pedir primero la imagen y luego el tipo del catálogo.
- Usar `GET /polaroids/tipos` para el selector; no hardcodear los 9 tipos.
- Deshabilitar u ocultar “Agregar foto” cuando `total >= maximo`.
- “Reemplazar” usa `PUT` con solo `archivo`.
- “Cambiar tipo” usa `PUT` con solo `idTipoPolaroid`.
- “Eliminar” usa `DELETE` y luego refresca la lista.
- El reordenamiento por drag-and-drop debe enviar la lista completa de IDs en
  el nuevo orden.

### Estado vacío

- Si `total = 0`:
  - el perfil sigue siendo válido;
  - mostrar un estado vacío;
  - si el perfil es propio, mostrar “Agregar foto”;
  - si es de terceros, no mostrar acciones de gestión.

## 11. Notas de implementación

- Entidades: `GrupoPolaroid`, `TipoPolaroid`, `PolaroidPerfil`.
- Tablas: `grupo_polaroid`, `tipo_polaroid`, `polaroid_perfil`.
- Semilla: 3 grupos y 9 tipos en
  `docs/3_diseño/migraciones/2026-11-10_polaroids_perfil_y_fecha_cancelacion.sql`.
- Servicios principales:
  - `PolaroidCatalogoService`
  - `ListarPolaroidsPerfilService`
  - `CrearPolaroidPerfilService`
  - `ModificarPolaroidPerfilService`
  - `ReordenarPolaroidsPerfilService`
  - `EliminarPolaroidPerfilService`
- El catálogo no tiene ABM: los tipos se agregan o modifican por migración.
- El máximo de 8 es configurable. Si se decide permitir los 9 tipos
  simultáneamente, cambiar `app.polaroids.max-por-perfil=9`.
