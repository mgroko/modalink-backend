# Ficha Técnica Frontend: Pantalla "Modificar Proyecto" (UC-36)

Esta especificación detalla la interacción con la API para editar un proyecto existente: payload, reglas de negocio, semántica de campos opcionales, respuesta, errores y consideraciones de UX/UI.

---

## 1. Endpoint y Contexto de Autenticación

* **Método:** `PUT`
* **URL:** `/proyectos/{idProyecto}`
* **Content-Type:** `application/json`
* **Autenticación requerida:** Sí (JWT en cookie HTTP-only `jwt` + header `X-XSRF-TOKEN`).
* **Contexto de Perfil Requerido:**
  * Debe haber un **perfil activo seleccionado** (`idPerfilActivo`) en la sesión.
  * El perfil debe ser **miembro activo** del proyecto y tener el permiso `MODIFICAR_PROYECTO`.
* **Respuesta exitosa:** `200 OK` con el proyecto completo (mismo contrato que `GET /proyectos/{idProyecto}`).

### Estados del proyecto

| Estado | ¿Se puede modificar? |
| :--- | :---: |
| `Borrador` | Sí |
| `Publicado` | Sí |
| `Confirmado` | Sí |
| `Finalizado` | No → `409` |
| `Cancelado` | No → `409` |

> El **nombre del proyecto es inmutable**: no forma parte del request.

---

## 2. Estructura de Datos (Request)

### Payload (`PUT /proyectos/{idProyecto}`)

```json
{
  "descripcion": "Diseño y confección de prendas sostenibles para la nueva temporada.",
  "privacidad": "PUBLICO",
  "aceptaPostulacionGral": true,
  "fechaEntregaEstipulada": "2026-12-15",
  "ubicacion": {
    "localidadId": "06049010000",
    "provinciaId": "06"
  },
  "objetivos": [
    {
      "nombre": "Diseño de bocetos iniciales",
      "descripcion": "Elaborar los primeros 10 bocetos de las prendas clave."
    }
  ],
  "requerimientosGral": [
    {
      "idRequerimientoGral": 10,
      "cantidad": 2,
      "idProfesion": 2,
      "descripcion": "Modelos para pasarela",
      "caracteristicas": [
        { "idCaracteristica": 1, "valorMin": 160, "valorMax": 180 },
        { "idCaracteristica": 5, "valores": [1, 4] }
      ],
      "habilidades": [1, 4]
    }
  ],
  "moodboard": {
    "descripcion": "Paleta fría, telas de lino, inspiración nórdica."
  }
}
```

---

## 3. Matriz de Campos y Validaciones

| Campo | Tipo / Formato | Obligatorio | Reglas / Restricciones Backend | Notas UI/UX sugeridas |
| :--- | :--- | :---: | :--- | :--- |
| `descripcion` | `string` | **Sí** | Máx. 200 caracteres. No blanco. | Textarea con contador `(x/200)`. |
| `privacidad` | `enum` | **Sí** | `PUBLICO` \| `PRIVADO` (mayúsculas). | Segmented Control / Radio. |
| `aceptaPostulacionGral` | `boolean` | No | Si es `null` o se omite, **no cambia**. | Switch. |
| `fechaEntregaEstipulada` | `YYYY-MM-DD` | No | Fecha ISO. Si se envía, no puede ser anterior a hoy. Si es `null`/se omite, no cambia. | Datepicker con `min = hoy`. |
| `ubicacion` | `object` | No | Si se envía, `localidadId` no puede estar en blanco. `null` solo permitido en `Borrador`. | Autocompletado Georef. |
| `objetivos[]` | `array` | No | Si es `null`/se omite, **no se tocan**. Si se envía, **reemplaza todos** los objetivos. | Sección +/- objetivos. |
| `objetivos[].nombre` | `string` | Sí (por ítem) | Máx. 100 caracteres. No blanco. | Input. |
| `objetivos[].descripcion` | `string` | No | Máx. 300 caracteres. | Textarea. |
| `requerimientosGral[]` | `array` | No | Ver sección de reglas de requerimientos. | Sección +/- requerimientos. |
| `requerimientosGral[].idRequerimientoGral` | `long` | No | Si se envía, debe pertenecer al proyecto y **no estar satisfecho**. | Se incluye solo para requerimientos existentes editables. |
| `requerimientosGral[].cantidad` | `int` | **Sí** | > 0 (`@Positive`). | Number input. |
| `requerimientosGral[].idProfesion` | `long` | **Sí** | Debe existir. | Select de profesiones. |
| `requerimientosGral[].descripcion` | `string` | No | Máx. 200 caracteres. | Input. |
| `requerimientosGral[].caracteristicas[]` | `array` | No | Sin duplicados por `idCaracteristica`. La característica debe pertenecer a la profesión del requerimiento. | Selector filtrado por profesión. |
| `requerimientosGral[].caracteristicas[].idCaracteristica` | `long` | **Sí** | Debe existir. | Select. |
| `requerimientosGral[].caracteristicas[].valorMin` / `valorMax` | `number` | Condicional | **NUMERICO**: ambos obligatorios, ≥ 0, `min ≤ max`. **No-NUMERICO**: deben ser `null`. | Rango solo visible si tipo = NUMERICO. |
| `requerimientosGral[].caracteristicas[].valores[]` | `array<long>` | Condicional | **ENUMERADO**: ≥ 1 valor existente y perteneciente a la característica. **No-ENUMERADO**: debe ser vacío/`null`. | Checklist de valores solo si tipo = ENUMERADO. |
| `requerimientosGral[].habilidades[]` | `array<long>` | No | Cada habilidad debe existir. | Multi-select. |
| `moodboard` | `object` | No | Si es `null`/se omite, no se toca. Si se envía, actualiza (o crea) el moodboard. | — |
| `moodboard.descripcion` | `string` | No | Máx. 200 caracteres. `null` borra la descripción. | Textarea con contador `(x/200)`. |

---

## 4. Semántica de `null` / Ausente vs. Lista Vacía

| Campo | Ausente o `null` | Lista vacía `[]` |
| :--- | :--- | :--- |
| `objetivos` | No se tocan | Se eliminan todos |
| `requerimientosGral` | No se tocan (se conservan todos, incluidos los satisfechos) | Se eliminan los no satisfechos; los satisfechos se conservan |
| `aceptaPostulacionGral` | No cambia | — |
| `fechaEntregaEstipulada` | No cambia | — |
| `moodboard` | No se toca | — |
| `ubicacion` | Ver regla por estado (abajo) | — |

> **Importante:** el front puede hacer actualizaciones parciales enviando `null`/omitiendo los campos que no se modificaron.

---

## 5. Reglas de Negocio

### 5.1 Ubicación

| Estado del proyecto | `ubicacion: null` / omitida |
| :--- | :--- |
| `Borrador` | Borra la ubicación del proyecto |
| `Publicado` / `Confirmado` | **Error `400`** (`UbicacionObligatoriaException`) |

> En `Publicado`/`Confirmado` el front **debe reenviar siempre la ubicación**, aunque no haya cambiado.

### 5.2 Requerimientos de personal

Un requerimiento está **satisfecho** cuando tiene al menos una postulación en estado `ACEPTADA`.

* **No se pueden enviar requerimientos satisfechos** en `requerimientosGral` → `409` (`RequerimientoSatisfechoException`).
* Si un requerimiento satisfecho **se omite del payload, se conserva automáticamente**.
* Los requerimientos **no satisfechos** que se envían se **eliminan y recrean** (el `id` solo se usa para validar pertenencia y estado).
* Un `idRequerimientoGral` que no pertenece al proyecto → `400` (`RequerimientoInvalidoException`).
* Si `requerimientosGral` es `null`/ausente, no se modifica ningún requerimiento.

### 5.3 Objetivos

* Si se envía el array, **reemplaza por completo** los objetivos actuales.
* Si se envía `[]`, se eliminan todos.

### 5.4 Moodboard

* Solo se modifica la **descripción**.
* Si el proyecto no tenía moodboard, se crea.
* Las imágenes **no** se modifican desde este endpoint.

### 5.5 Información básica (solo `Publicado` / `Confirmado`)

Después de aplicar los cambios, el proyecto debe conservar:

1. Ubicación.
2. Fecha de entrega.
3. Moodboard.
4. Al menos un requerimiento de personal.

Si falta alguno → `400` (`InformacionBasicaIncompletaException`), con un mensaje que enumera los faltantes:

```
El proyecto tiene información básica incompleta. Faltan: ubicación, moodboard.
```

---

## 6. Respuesta Exitosa (`200 OK`)

Se devuelve el proyecto completo, con el mismo contrato que `GET /proyectos/{idProyecto}`:

```json
{
  "idProyecto": 100,
  "nombre": "Colección Primavera-Verano",
  "descripcion": "Diseño y confección de prendas sostenibles para la nueva temporada.",
  "fechaInicio": "2026-06-01",
  "fechaEntregaEstipulada": "2026-12-15",
  "estado": "Publicado",
  "privacidad": "PUBLICO",
  "aceptaPostulacionGral": true,
  "ubicacion": {
    "idUbicacion": 50,
    "direccion": null,
    "codigoPostal": null,
    "latitud": -34.9214,
    "longitud": -57.9545,
    "ciudad": {
      "idCiudad": 3,
      "idExterno": "06049010000",
      "fuenteApi": "GEOREF",
      "nombre": "La Plata",
      "provincia": {
        "idProvincia": 1,
        "idExterno": "06",
        "fuenteApi": "GEOREF",
        "nombre": "Buenos Aires",
        "pais": {
          "idPais": 1,
          "codigoIso": "AR",
          "nombre": "Argentina"
        }
      }
    }
  },
  "idDirector": 10,
  "nombreDirector": "Luna Diseños",
  "objetivos": [
    {
      "idObjetivo": 1,
      "nombre": "Diseño de bocetos iniciales",
      "descripcion": "Elaborar los primeros 10 bocetos de las prendas clave."
    }
  ],
  "requerimientosGral": [
    {
      "idRequerimientoGral": 10,
      "cantidad": 2,
      "descripcion": "Modelos para pasarela",
      "idProfesion": 2,
      "nombreProfesion": "Modelo",
      "caracteristicas": [
        {
          "idCaracteristica": 1,
          "codigo": "ALTURA",
          "tipoDato": "NUMERICO",
          "valorMin": 160,
          "valorMax": 180,
          "valores": []
        }
      ],
      "habilidades": [1, 4],
      "satisfecho": false
    }
  ],
  "moodboard": {
    "idMoodboard": 77,
    "descripcion": "Paleta fría, telas de lino, inspiración nórdica.",
    "fechaCreacion": "2026-06-01T10:00:00",
    "imagenes": []
  },
  "rolUsuario": "DIRECTOR",
  "esMiembro": true,
  "permisos": ["MODIFICAR_PROYECTO", "PUBLICAR_PROYECTO", "VER_PROYECTO"],
  "elegibleParaPostularse": false
}
```

> `estado` se devuelve con nombre visible (`Borrador`, `Publicado`, `Confirmado`, `Finalizado`, `Cancelado`). `privacidad` se devuelve en mayúsculas (`PUBLICO` / `PRIVADO`).

---

## 7. Cambio Transversal: Campo `satisfecho`

Se agregó el campo `satisfecho: boolean` a cada elemento de `requerimientosGral[]`. Está presente en:

* `PUT /proyectos/{idProyecto}`
* `GET /proyectos/{idProyecto}`
* `POST /proyectos`
* `POST /proyectos/{idProyecto}/publicar`

### Uso en el front

* `satisfecho === true`: el requerimiento **no se puede editar ni eliminar**.
* `satisfecho === false`: se puede editar/eliminar.
* Al construir el payload de modificación, **excluir los requerimientos satisfechos**.

---

## 8. Manejo de Errores y Excepciones

### Formato general

```json
{
  "message": "No se puede modificar un proyecto en estado Finalizado.",
  "httpStatus": 409,
  "timestamp": 1760000000000
}
```

### Errores de validación (`400`)

```json
{
  "message": "Validación fallida",
  "errores": {
    "descripcion": "La descripción del proyecto es obligatoria.",
    "requerimientosGral[0].cantidad": "La cantidad de profesionales debe ser mayor a cero."
  },
  "httpStatus": 400,
  "timestamp": 1760000000000
}
```

### Tabla de errores

| HTTP | Caso | Origen / Mensaje |
| :---: | :--- | :--- |
| `400` | Ubicación quitada en `Publicado`/`Confirmado` | `La ubicación es obligatoria si el proyecto está en estado publicado o confirmado.` |
| `400` | Fecha de entrega anterior a hoy | `La fecha de entrega estipulada no puede ser anterior a la fecha actual.` |
| `400` | Información básica incompleta | `El proyecto tiene información básica incompleta. Faltan: ...` |
| `400` | Requerimiento inválido o ajeno al proyecto | `El requerimiento de personal {id} no pertenece a este proyecto.` |
| `400` | Profesión / característica / valor inválido | Excepciones de catálogo |
| `400` | Característica duplicada, rango inválido, valores incorrectos | `RequerimientoInvalidoException` |
| `403` | Sin permiso `MODIFICAR_PROYECTO` o no es miembro activo | `AccesoDenegadoProyectoException` |
| `404` | Sin perfil activo | `No hay un perfil activo seleccionado en la sesión.` |
| `404` | Perfil no encontrado | `PerfilNoEncontradoException` |
| `404` | Proyecto inexistente | `El proyecto con id {id} no existe.` |
| `404` | Habilidad inexistente | `HabilidadNoEncontradaException` |
| `409` | Proyecto `Finalizado` / `Cancelado` | `No se puede modificar un proyecto en estado {estado}.` |
| `409` | Se intentó enviar un requerimiento satisfecho | `No se puede modificar ni eliminar el requerimiento {id} porque ya tiene postulaciones aceptadas.` |
| `409` | Perfil en baja | `El perfil no se encuentra activo.` |

---

## 9. Consideraciones de UX

1. **Reutilizar el formulario de creación**, pero:
   * El campo `nombre` se muestra como **solo lectura**.
   * Precargar todos los datos desde `GET /proyectos/{idProyecto}`.
2. **Actualizar el estado local con la respuesta del `PUT`** (evita un `GET` adicional).
3. En `Publicado`/`Confirmado`:
   * La ubicación es **obligatoria** y no debe permitirse vaciarla.
   * No permitir guardar si falta fecha de entrega, moodboard o al menos un requerimiento.
4. **Requerimientos satisfechos:**
   * Mostrar badge/estado “Satisfecho”.
   * Deshabilitar edición y eliminación.
   * Excluirlos del payload.
5. **Requerimientos editables:**
   * Enviar `idRequerimientoGral` si ya existían.
   * Omitir el `id` para los nuevos.
6. **Actualizaciones parciales:**
   * Enviar `null`/omitir `objetivos` y `requerimientosGral` si no se tocaron esas secciones.
   * Enviar el array completo si se editó (reemplazo total).
7. **Fechas:** enviar en formato `YYYY-MM-DD`.
8. **Manejo de errores:**
   * Mostrar `message` en errores `403`, `404`, `409`.
   * Mapear `errores` por campo en errores `400` de validación.
9. **Confirmación:** al modificar un proyecto publicado, mostrar un diálogo de confirmación antes de guardar.

---

## 10. Ejemplos de Casos de Uso

### 10.1 Modificar solo la descripción (Borrador)

```json
{
  "descripcion": "Nueva descripción",
  "privacidad": "PUBLICO"
}
```

### 10.2 Publicado: mantener ubicación y quitar un requerimiento no satisfecho

```json
{
  "descripcion": "Descripción actualizada",
  "privacidad": "PUBLICO",
  "ubicacion": {
    "localidadId": "06049010000",
    "provinciaId": "06"
  },
  "requerimientosGral": [
    {
      "idRequerimientoGral": 11,
      "cantidad": 1,
      "idProfesion": 3,
      "descripcion": "Un estilista"
    }
  ]
}
```

> El requerimiento `10` (no satisfecho) se elimina porque no viaja en el array. El requerimiento satisfecho (si existe) se conserva aunque no viaje.

### 10.3 Borrador: quitar la ubicación

```json
{
  "descripcion": "Proyecto sin ubicación aún",
  "privacidad": "PRIVADO",
  "ubicacion": null
}
```
