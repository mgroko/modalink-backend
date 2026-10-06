# Ficha Técnica: Módulo de Ubicación y Asignación de Ubicación al Usuario — Frontend

> **Estado:** vigente sobre la rama `adaptacion-de-bd` (commit `8a33e57`).
> **Objetivo:** adaptar el frontend a la **nueva BD normalizada** (`pais` ← `provincia` ← `ciudad` ← `ubicacion`) que reemplazó a la vieja tabla plana `ubicacion` con `localidad`/`provincia`/`pais` como strings.
> **Audencia:** agente/ equipo de frontend.

---

## 0. TL;DR — Qué cambió y qué debe hacer el front

| Aspecto | Versión vieja (actual en el front) | Versión nueva (lo que devuelve el backend HOY) |
|---|---|---|
| Modelo de BD | 1 tabla `ubicacion` con columnas `localidad`, `provincia`, `pais` (strings) | 4 tablas normalizadas: `pais`, `provincia`, `ciudad`, `ubicacion` (FK encadenadas) |
| `UbicacionResponse` | `{ idUbicacion, localidadId, localidad, provincia, pais, codigoPostal, latitud, longitud }` | `{ idUbicacion, direccion, codigoPostal, latitud, longitud, ciudad: { ... , provincia: { ..., pais: { ... } } } }` |
| `UbicacionRequest` | `{ localidadId }` | `{ localidadId, provinciaId? }` (ambos **String**, ids de Georef) |
| `DatosPersonalesResponse.ubicacion` | `String` `"localidad, provincia"` | objeto `UbicacionResponse` anidado **o `null`** |
| `PerfilBusquedaResponse` / `PerfilDetalleResponse` | `localidad: "..."`, `provincia: "..."` (2 strings planos) | `ciudad: CiudadResponse` (1 objeto anidado) |
| Endpoints de catálogo | no existían / otros | `GET /ubicaciones/provincias`, `GET /ubicaciones/localidades` |
| Endpoint dedicado de usuario | — | `GET/PUT/DELETE /usuario/ubicacion` |

**Acciones mínimas del front:**

1. Dejar de leer `ubicacion.localidad`, `ubicacion.provincia`, `ubicacion.pais` → leer `ubicacion.ciudad.nombre`, `ubicacion.ciudad.provincia.nombre`, `ubicacion.ciudad.provincia.pais.nombre`.
2. Dejar de leer `perfil.localidad` / `perfil.provincia` → leer `perfil.ciudad.nombre` y `perfil.ciudad.provincia.nombre`.
3. Cambiar el selector de ubicación a **cascada País→Provincia→Localidad** usando `GET /ubicaciones/provincias` y `GET /ubicaciones/localidades?provinciaId=...&nombre=...`.
4. Enviar siempre `localidadId` como **string** (nunca número) en `PUT /usuario/ubicacion` y en `PUT /usuario/datos-personales`.
5. Manejar `ubicacion: null` = "Sin ubicación" (es un estado válido, responde `200`).

---

## 1. Visión general del modelo

### 1.1 Cadena jerárquica (BD)

```
pais (id_pais, codigo_iso, nombre, activo)
  └── provincia (id_provincia, nombre, id_externo, fuente_api, activo, id_pais FK)
        └── ciudad (id_ciudad, nombre, codigo_postal, id_externo, fuente_api, activo,
                    latitud_defecto, longitud_defecto, id_provincia FK)
              └── ubicacion (id_ubicacion, direccion, codigo_postal, latitud, longitud, id_ciudad FK NOT NULL)
```

- `usuario.id_ubicacion` → FK **nullable**. Usuario **puede no tener ubicación**.
- La misma fila `ubicacion` puede ser compartida por varios usuarios/proyectos/actividades (el backend la reutiliza y nunca duplica por ciudad).
- Las coordenadas de `ubicacion` se heredan automáticamente de `ciudad.latitud_defecto/longitud_defecto` por un trigger cuando llegan `null` → por eso las respuestas traen `latitud`/`longitud` poblados con `direccion: null` y `codigoPostal: null`.

### 1.2 Catálogo Georef (fuente de los ids)

- El catálogo es **argentino**: 24 provincias + ~1.000 localidades, cargado desde archivos estáticos `classpath:georef/*.json` (sin llamadas externas en runtime).
- Los ids que viajan por la API son **strings de Georef**:
  - provincia: `"02"`, `"06"`, `"10"`…
  - localidad: `"0208401002"`…
- El país se resuelve por configuración (`CATALOGO_GEOREF_PAIS_ISO = AR`); **no hay endpoint de países** y el front no debe listarlos.

---

## 2. Endpoints de API REST

> **Autenticación:** todos requieren `Authorization: Bearer <JWT>` (no hay rutas públicas en este módulo). No hay `context-path`; base `http://localhost:8080`.

### 2.1 Catálogo de provincias

| Método | Endpoint | Auth | Descripción |
|--------|----------|------|-------------|
| GET | `/ubicaciones/provincias` | JWT | Lista las 24 provincias del catálogo |

**Response `200`:**
```json
[
  { "id": "02", "nombre": "Ciudad Autónoma de Buenos Aires" },
  { "id": "06", "nombre": "Buenos Aires" },
  { "id": "10", "nombre": "Catamarca" }
]
```

- `id` es **String** (id Georef) y es el valor que luego se envía como `provinciaId`.
- Orden alfabético.

### 2.2 Búsqueda de localidades

| Método | Endpoint | Auth | Query params |
|--------|----------|------|--------------|
| GET | `/ubicaciones/localidades` | JWT | `provinciaId?`, `nombre?` (ambos opcionales) |

Ejemplos:
```
GET /ubicaciones/localidades                     → todas (≈1.000), orden alfabético
GET /ubicaciones/localidades?provinciaId=02      → solo CABA
GET /ubicaciones/localidades?nombre=saavedra     → filtrado por nombre (contains, case-insensitive)
GET /ubicaciones/localidades?provinciaId=02&nombre=saavedra  → combinado
```

**Response `200`:**
```json
[
  {
    "id": "0208401002",
    "nombre": "Saavedra",
    "provinciaId": "02",
    "provinciaNombre": "Ciudad Autónoma de Buenos Aires",
    "latitud": -34.5548978526608,
    "longitud": -58.4863271154338
  }
]
```

- `id` es el valor a enviar como `localidadId`.
- `latitud`/`longitud` del catálogo son **solo informativas** (preview en mapa); las coordenadas finales las calcula el backend.

### 2.3 Ubicación del usuario autenticado

| Método | Endpoint | Auth | Descripción |
|--------|----------|------|-------------|
| GET | `/usuario/ubicacion` | JWT | Devuelve la ubicación del usuario logueado |
| PUT | `/usuario/ubicacion` | JWT | Asigna/actualiza la ubicación del usuario logueado |
| DELETE | `/usuario/ubicacion` | JWT | Quita la ubicación del usuario |

No recibe `idUsuario` por path: lo obtiene del JWT (`authentication.getPrincipal()`).

#### GET `/usuario/ubicacion`

**Response `200` con ubicación:**
```json
{
  "idUbicacion": 10,
  "direccion": null,
  "codigoPostal": null,
  "latitud": -34.5548978526608,
  "longitud": -58.4863271154338,
  "ciudad": {
    "idCiudad": 100,
    "idExterno": "0208401002",
    "fuenteApi": "GEOREF",
    "nombre": "Saavedra",
    "provincia": {
      "idProvincia": 10,
      "idExterno": "02",
      "fuenteApi": "GEOREF",
      "nombre": "Ciudad Autónoma de Buenos Aires",
      "pais": { "idPais": 1, "codigoIso": "AR", "nombre": "Argentina" }
    }
  }
}
```

**Response `200` SIN ubicación:** `200 OK` con **cuerpo vacío** (no `404`, no `null` en JSON). El front debe tratar `status 200 + body vacío` como "sin ubicación".

#### PUT `/usuario/ubicacion`

**Request:**
```json
{ "localidadId": "0208401002", "provinciaId": "02" }
```

- `localidadId`: **String, obligatorio** (`@NotBlank`). Es el `id` devuelto por `GET /ubicaciones/localidades`.
- `provinciaId`: **String, opcional**. Si se envía, el backend valida que la localidad pertenezca a esa provincia.
  - Variante mínima válida: `{ "localidadId": "0208401002" }`.

**Response `200`:** mismo `UbicacionResponse` anidado del GET.

**Errores:**

| Código | Causa | Body |
|--------|-------|------|
| `400` | `localidadId` vacío o en blanco | `{"message": "La localidad es obligatoria."}` |
| `400` | `provinciaId` enviado sin `localidadId` | `{"message": "No se puede enviar provincia sin localidad."}` |
| `400` | localidad inexistente en el catálogo | `{"message": "Localidad no encontrada con id: 999"}` |
| `400` | la localidad no pertenece a la provincia indicada | `{"message": "La localidad X pertenece a la provincia Y, pero se indicó la provincia: Z"}` ⚠️ ver §6 |
| `401` | usuario inexistente o no activo | `{"message": "Usuario no encontrado."}` |

#### DELETE `/usuario/ubicacion`

- **Response `204 No Content`** (no hay body).

### 2.4 Datos personales (incluye ubicación)

| Método | Endpoint | Auth | Descripción |
|--------|----------|------|-------------|
| PUT | `/usuario/datos-personales` | JWT | Actualiza nombre, apellido, fecha, género **y** ubicación |

**Request:**
```json
{
  "nombre": "Maria",
  "apellido": "Flores",
  "fechaNacimiento": "1990-06-15",
  "genero": "MUJER",
  "localidadId": "0208401002"
}
```

Sin ubicación (borra la actual):
```json
{
  "nombre": "Maria",
  "apellido": "Flores",
  "fechaNacimiento": "1990-06-15",
  "genero": "MUJER",
  "localidadId": null
}
```

- `localidadId`: **String, opcional, sin `@NotBlank`**. `null` o `""` ⇒ `ubicacion = null` (borra).
- **No acepta `provinciaId`** en este DTO (se resuelve solo con `localidadId`).
- `genero` debe ser un código válido de la tabla `genero`: `MUJER`, `HOMBRE`, `NO_BINARIO`, `NO_DECIRLO`.

**Response `200`:**
```json
{
  "idUsuario": 1,
  "nombre": "Maria",
  "apellido": "Flores",
  "fechaNacimiento": "1990-06-15",
  "genero": "MUJER",
  "ubicacion": {
    "idUbicacion": 10,
    "direccion": null,
    "codigoPostal": null,
    "latitud": -34.5548978526608,
    "longitud": -58.4863271154338,
    "ciudad": { "idCiudad": 100, "idExterno": "0208401002", "fuenteApi": "GEOREF",
                "nombre": "Saavedra",
                "provincia": {
                  "idProvincia": 10, "idExterno": "02", "fuenteApi": "GEOREF",
                  "nombre": "Ciudad Autónoma de Buenos Aires",
                  "pais": { "idPais": 1, "codigoIso": "AR", "nombre": "Argentina" } } }
  }
}
```

Sin ubicación → `"ubicacion": null`.

**Errores adicionales:** `400` edad < 18 años; `400` género inexistente.

### 2.5 Otros endpoints que exponen ubicación

| Método | Endpoint | Campo de ubicación en la respuesta |
|--------|----------|------------------------------------|
| GET | `/perfiles/buscar?idUbicacion&localidad&provincia&page&size` | `ciudad: CiudadResponse` en cada item |
| GET | `/perfiles/{idPerfil}` | `ciudad: CiudadResponse` |
| POST | `/proyectos` | request: `"ubicacion": { "localidadId": "...", "provinciaId": "..." }`; response: `ubicacion: UbicacionResponse` |

**Detalle de los filtros de búsqueda de perfiles** (query string, todos opcionales):

| Param | Tipo | Qué filtra realmente |
|-------|------|----------------------|
| `idUbicacion` | number/long | **filtra por `ciudad.idCiudad`** (no por `ubicacion.idUbicacion`) ⚠️ trampa de nombres |
| `localidad` | string | contains sobre `ciudad.nombre` (case-insensitive) |
| `provincia` | string | contains sobre `provincia.nombre` (case-insensitive) |

**Fragmento de respuesta de `/perfiles/buscar`:**
```json
{
  "contenido": [
    {
      "idPerfil": 7,
      "nombreArtistico": "Luna",
      "genero": "MUJER",
      "ciudad": {
        "idCiudad": 100,
        "idExterno": "0208401002",
        "fuenteApi": "GEOREF",
        "nombre": "Saavedra",
        "provincia": {
          "idProvincia": 10, "idExterno": "02", "fuenteApi": "GEOREF",
          "nombre": "Ciudad Autónoma de Buenos Aires",
          "pais": { "idPais": 1, "codigoIso": "AR", "nombre": "Argentina" }
        }
      },
      "habilidades": [],
      "caracteristicas": []
    }
  ],
  "pagina": 0, "tamanio": 20, "totalElementos": 1, "totalPaginas": 1
}
```

---

## 3. Modelos de datos (DTOs) para frontend

### 3.1 `UbicacionResponse` — SALIDA
| Campo | Tipo | Notas |
|-------|------|-------|
| `idUbicacion` | Long | id de la fila; **no** es el id de ciudad |
| `direccion` | String \| null | texto libre; hoy siempre `null` en altas desde el front |
| `codigoPostal` | String \| null | hoy siempre `null` en altas desde el front |
| `latitud` | BigDecimal \| null | heredada de la ciudad por trigger |
| `longitud` | BigDecimal \| null | heredada de la ciudad por trigger |
| `ciudad` | CiudadResponse \| null | objeto anidado |

### 3.2 `CiudadResponse`
| Campo | Tipo | Notas |
|-------|------|-------|
| `idCiudad` | Long | id interno BD |
| `idExterno` | String | **id Georef** = `localidadId` enviado |
| `fuenteApi` | String | `"GEOREF"` |
| `nombre` | String | nombre de la localidad |
| `provincia` | ProvinciaResponse | anidado |

### 3.3 `ProvinciaResponse`
| Campo | Tipo | Notas |
|-------|------|-------|
| `idProvincia` | Long | id interno BD |
| `idExterno` | String | **id Georef** = `provinciaId` |
| `fuenteApi` | String | `"GEOREF"` |
| `nombre` | String | |
| `pais` | PaisResponse | anidado |

### 3.4 `PaisResponse`
| Campo | Tipo | Notas |
|-------|------|-------|
| `idPais` | Long | |
| `codigoIso` | String | `"AR"` |
| `nombre` | String | `"Argentina"` |

### 3.5 `UbicacionRequest` — ENTRADA
| Campo | Tipo | Obligatorio | Validación |
|-------|------|-------------|------------|
| `localidadId` | String | **Sí** | `@NotBlank` → 400 si null/vacío |
| `provinciaId` | String | No | si llega sin `localidadId` → 400; si llega, valida coherencia |

### 3.6 `ProvinciaCatalogoResponse` (catálogo)
`{ "id": "02", "nombre": "Ciudad Autónoma de Buenos Aires" }` — ambos `String`.

### 3.7 `LocalidadResponse` (catálogo)
| Campo | Tipo |
|-------|------|
| `id` | String (id Georef) |
| `nombre` | String |
| `provinciaId` | String |
| `provinciaNombre` | String |
| `latitud` | BigDecimal |
| `longitud` | BigDecimal |

### 3.8 `DatosPersonalesRequest` — ENTRADA
| Campo | Tipo | Validación |
|-------|------|------------|
| `nombre` | String | `@NotBlank @Size(2..50)` |
| `apellido` | String | `@NotBlank @Size(2..50)` |
| `fechaNacimiento` | LocalDate (`yyyy-MM-dd`) | `@NotNull @Past` |
| `genero` | String | `@NotBlank`, código de tabla `genero` |
| `localidadId` | String | opcional; `null`/`""` borra ubicación |

### 3.9 `DatosPersonalesResponse` — SALIDA
`{ idUsuario, nombre, apellido, fechaNacimiento, genero, ubicacion: UbicacionResponse|null }`

---

## 4. Guion de adaptación (cambios concretos en el código del front)

### 4.1 Lectura de una ubicación — reemplazos directos

| ❌ Antes (API vieja) | ✅ Ahora |
|---|---|
| `ubicacion.localidad` | `ubicacion.ciudad.nombre` |
| `ubicacion.provincia` | `ubicacion.ciudad.provincia.nombre` |
| `ubicacion.pais` | `ubicacion.ciudad.provincia.pais.nombre` |
| `ubicacion.localidadId` | `ubicacion.ciudad.idExterno` |
| `perfil.localidad` | `perfil.ciudad?.nombre` |
| `perfil.provincia` | `perfil.ciudad?.provincia?.nombre` |
| `ubicacion` (string `"X, Y"`) en datos personales | `ubicacion.ciudad` (objeto) o `null` |

Helper sugerido (muestra segura ante `null`):

```ts
export function formatUbicacion(u: UbicacionResponse | null | undefined): string | null {
  if (!u?.ciudad) return null;
  const partes = [u.ciudad.nombre, u.ciudad.provincia?.nombre, u.ciudad.provincia?.pais?.nombre];
  return partes.filter(Boolean).join(', ');
}
```

### 4.2 Escritura — reemplazos directos

| ❌ Antes | ✅ Ahora |
|---|---|
| `PUT /usuario/ubicacion` body `{ "localidadId": 5 }` (número) | `{ "localidadId": "0208401002" }` (**string**) |
| `PUT /usuario/datos-personales` con `ubicacion: "Saavedra, CABA"` | con `localidadId: "0208401002"` o `null` |
| Autocompletado por nombre de ciudad | Selector **cascada** Provincia → Localidad contra `/ubicaciones/*` |

### 4.3 Flujo de UI recomendado: selector en cascada

```
1. GET /ubicaciones/provincias          → populate <select> Provincia   (cacheable: catálogo estático)
2. usuario elige provincia (o escribe texto)
3. GET /ubicaciones/localidades?provinciaId={id}&nombre={texto}
                                              → dropdown/combobox con resultados
4. usuario elige localidad → se guarda el `id` como string
5. PUT /usuario/ubicacion  { localidadId, provinciaId }   → 200 con UbicacionResponse
   (o PUT /usuario/datos-personales { ..., localidadId })
6. refrescar UI con la respuesta (no re-GET)
```

Consideraciones:
- `GET /ubicaciones/localidades` sin parámetros devuelve ~1.000 registros → **siempre** enviar `nombre` (debounce 300 ms) o al menos `provinciaId`.
- Ambos catálogos son estáticos (archivos embebidos) → cachear de por vida de la sesión.
- Campo de apertura de localidades puede quedar vacío ⇒ elegir "Sin ubicación" ⇒ enviar `localidadId: null` (datos personales) o `DELETE /usuario/ubicacion`.
- El backend **crea sola** la fila de `ubicacion` (y `ciudad`/`provincia` si no existen): el front **nunca** envía `idUbicacion`, `idCiudad`, `direccion`, `codigoPostal`, `latitud` ni `longitud`.

---

## 5. Estados, códigos y errores (GlobalExceptionHandler)

| Código | Cuándo | Body |
|--------|--------|------|
| `200` | GET con ubicación | `UbicacionResponse` |
| `200` | GET **sin** ubicación | cuerpo **vacío** |
| `200` | PUT ok | `UbicacionResponse` |
| `204` | DELETE ok | vacío |
| `400` | `localidadId` en blanco | `{"message":"La localidad es obligatoria."}` |
| `400` | `provinciaId` sin `localidadId` | `{"message":"No se puede enviar provincia sin localidad."}` |
| `400` | localidad inexistente | `{"message":"Localidad no encontrada con id: 999"}` |
| `400` | localidad sin provincia en catálogo | mensaje de `LocalidadSinProvinciaException` |
| `401` | usuario inexistente o estado ≠ Activo | `{"message":"Usuario no encontrado."}` |
| `401` | sin JWT / inválido | Spring Security (estándar) |
| `500` | país no configurado en BD | `{"message": ...}` de `PaisNoConfiguradoException` (falla de despliegue) |

Formato de error estándar (vía `GlobalExceptionHandler`):
```json
{
  "timestamp": "2026-10-06T12:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Localidad no encontrada con id: 999",
  "path": "/usuario/ubicacion"
}
```

> ⚠️ **Known issue backend:** `ProvinciaLocalidadIncoherenteException` (localidad que no pertenece a la provincia enviada) **no tiene `@ExceptionHandler`** ⇒ hoy devuelve un `500` con el formato genérico de Spring y no el cuerpo `{"message": ...}` estándar. El front debe mostrar el mensaje si existe y, si no, un mensaje genérico de validación. *(Deuda pendiente del backend.)*

---

## 6. Checklist de QA para el front

- [ ] `GET /usuario/ubicacion` sin ubicación → se muestra "Sin ubicación" (200 + body vacío), **no** error.
- [ ] Asignar ubicación con `PUT /usuario/ubicacion` → la respuesta anida `ciudad.provincia.pais`.
- [ ] Reenviar la misma `localidadId` → no duplica (idempotente), devuelve `idUbicacion` igual.
- [ ] Enviar `provinciaId` incorrecta → 400/500 con mensaje, y no se guarda.
- [ ] `DELETE /usuario/ubicacion` → 204 y UI en "Sin ubicación".
- [ ] `PUT /usuario/datos-personales` con `localidadId: null` → `ubicacion: null` en la respuesta.
- [ ] `localidadId` se envía como **string** (un `"02"` numérico perdería ceros a la izquierda y fallaría).
- [ ] Perfiles: se renderiza `ciudad.nombre` / `ciudad.provincia.nombre` (antes `localidad`/`provincia` planos).
- [ ] Filtro `idUbicacion` de `/perfiles/buscar` se envía con el valor de **`ciudad.idCiudad`** (ver §2.5).
- [ ] Errores mostrados desde `message` del body.

---

## 7. Preguntas frecuentes

**¿Por qué `direccion` y `codigoPostal` vienen `null`?**
El alta desde el front solo envía `localidadId`; la tabla `ubicacion` no tiene esos datos y el trigger solo rellena coordenadas. No hay UI para editarlos todavía (el modelo los soporta, el backend no expone endpoint para escribirlos).

**¿Por qué latitud/longitud existen si yo no las envío?**
Trigger `trg_heredar_coordenadas_ciudad`: si llegan `null`, las copia de `ciudad.latitud_defecto/longitud_defecto`. Sirven para mapas sin llamadas extra.

**¿Puedo enviar solo `localidadId`?**
Sí. `provinciaId` es opcional y solo agrega validación de coherencia. En `PUT /usuario/datos-personales` ni siquiera existe.

**¿Cómo borro la ubicación?**
- Endpoint dedicado: `DELETE /usuario/ubicacion` → 204.
- Vía datos personales: enviar `localidadId: null`.

**¿Hay endpoint de países?**
No. El país se deriva de la provincia (`AR` por configuración) y aparece solo como dato de lectura en `ciudad.provincia.pais`.

**¿Los ids de Georef son numéricos?**
No, son strings con ceros a la izquierda (`"02"`, `"0208401002"`). **Siempre** tiparlos como string en el front.

**¿`idUbicacion` de la respuesta es la ciudad?**
No. `idUbicacion` es el id de la fila `ubicacion`; el id de ciudad es `ciudad.idCiudad` y el id Georef de la localidad es `ciudad.idExterno`. Ojo con el filtro `idUbicacion` de `/perfiles/buscar`, que filtra por `ciudad.idCiudad`.

---

## 8. Referencias de código (backend)

| Qué | Archivo |
|---|---|
| Controlador usuario | `src/main/java/org/mgroko/backend/ubicacion/controlador/UbicacionUsuarioController.java` |
| Controlador catálogo | `src/main/java/org/mgroko/backend/ubicacion/controlador/UbicacionCatalogoController.java` |
| DTO entrada | `src/main/java/org/mgroko/backend/ubicacion/dto/UbicacionRequest.java` |
| DTO salida | `src/main/java/org/mgroko/backend/ubicacion/dto/UbicacionResponse.java` |
| Mappers (anidamiento) | `src/main/java/org/mgroko/backend/ubicacion/mapper/UbicacionMapper.java` |
| Servicio de asignación | `src/main/java/org/mgroko/backend/ubicacion/servicio/UbicacionUsuarioService.java` |
| Motor alta/idempotencia | `src/main/java/org/mgroko/backend/ubicacion/servicio/UbicacionService.java` |
| Datos personales | `src/main/java/org/mgroko/backend/usuario/controlador/DatosPersonalesController.java` |
| Entidades | `src/main/java/org/mgroko/backend/modelo/{Ubicacion,Ciudad,Provincia,Pais,Usuario}.java` |
| Esquema BD | `docs/3_diseño/ModaLinkBD.sql` |
| Tests de referencia | `src/test/java/org/mgroko/backend/ubicacion/**` |

---

*Ficha técnica generada el 06/10/2026 a partir del análisis de `org.mgroko.backend.ubicacion` y `org.mgroko.backend.usuario`, esquema `ModaLinkBD.sql` y rama `adaptacion-de-bd`.*
