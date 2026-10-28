# Ficha Técnica Frontend: Ver Proyecto (Dashboard)

Especificación de la API y de la UI para la vista de detalle/dashboard del proyecto
(ruta frontend `/proyectos/:id`, componente `DashboardProyectoView`). Incluye el
**contexto del consultante** (rol, permisos y elegibilidad de postulación) para que
el frontend condicioné la UI sin llamadas adicionales.

---

## 1. Endpoint y Contexto de Autenticación

* **Método:** `GET`
* **URL:** `/proyectos/{idProyecto}`
* **Path param:** `idProyecto` (`long`, obligatorio)
* **Query params:** ninguno
* **Content-Type:** `application/json` (respuesta)
* **Autenticación requerida:** Sí (JWT en cookie HTTP-only `jwt` + header `X-XSRF-TOKEN`).
* **Permisos:** cualquier sesión válida. **Requiere perfil activo seleccionado** (se resuelve desde la sesión; el frontend no envía el id).
* **Ruta frontend que la consume:** `/proyectos/:id` (name `dashboard-proyecto`).

---

## 2. Visibilidad (regla del detalle)

| Proyecto | Miembro activo del proyecto | No miembro |
| :--- | :---: | :---: |
| **Borrador** (cualquier privacidad) | ✅ ve todo | ❌ **403** |
| No borrador + privacidad **Público** | ✅ ve todo | ✅ ve todo |
| No borrador + privacidad **Privado** | ✅ ve todo | ❌ **403** |

Notas:

* A diferencia del listado (UC-29, que **omite** los invisibles), el detalle de un
  proyecto inaccesible responde **403** con mensaje.
* El **Borrador** es visible para **cualquier miembro activo** (no solo el director):
  permite los flujos de colaboración antes de publicar.
* *Comportamiento interino de privacidad (aprobado por producto):* el requerimiento
  final hará visible el proyecto Privado también a la red de contactos del director
  cuando exista el módulo de contactos (UC-43..46, segunda iteración). No usar textos
  de "solo contactos" en la UI hasta entonces.

---

## 3. Respuesta Exitosa (`200 OK`)

Ejemplo completo (proyecto visto por su **director**):

```json
{
  "idProyecto": 105,
  "nombre": "Colección Madoka",
  "descripcion": "Colección cápsula de verano inspirada en tonos tierra y texturas naturales. Incluye desfile y sesión de fotos de catálogo.",
  "fechaInicio": "2026-06-01",
  "fechaEntregaEstipulada": "2027-03-15",
  "estado": "Borrador",
  "privacidad": "PRIVADO",
  "aceptaPostulacionGral": true,
  "ubicacion": {
    "idUbicacion": 50,
    "direccion": "Av. Mitre 1234",
    "codigoPostal": "3300",
    "latitud": -27.3676,
    "longitud": -55.8989,
    "ciudad": {
      "idCiudad": 12,
      "idExterno": "3300801",
      "fuenteApi": "GEOREF",
      "nombre": "Posadas",
      "provincia": {
        "idProvincia": 3,
        "idExterno": "54",
        "fuenteApi": "GEOREF",
        "nombre": "Misiones",
        "pais": { "idPais": 1, "codigoIso": "AR", "nombre": "Argentina" }
      }
    }
  },
  "idDirector": 4,
  "nombreDirector": "Luna Diseños",
  "objetivos": [
    {
      "idObjetivo": 1,
      "nombre": "Presentar la colección en pasarela",
      "descripcion": "Desfile de cierre con 12 looks."
    },
    {
      "idObjetivo": 2,
      "nombre": "Producir el catálogo fotográfico",
      "descripcion": "Sesión en exteriores con luz natural."
    }
  ],
  "requerimientosGral": [
    {
      "idRequerimientoGral": 10,
      "cantidad": 2,
      "descripcion": "Vacante para desfile y sesión de fotos.",
      "idProfesion": 2,
      "nombreProfesion": "Modelo",
      "caracteristicas": [
        {
          "idCaracteristica": 5,
          "codigo": "ALTURA",
          "tipoDato": "NUMERICO",
          "valorMin": 170,
          "valorMax": null,
          "valores": []
        },
        {
          "idCaracteristica": 8,
          "codigo": "COLOR_OJOS",
          "tipoDato": "ENUMERADO",
          "valorMin": null,
          "valorMax": null,
          "valores": [1, 3]
        }
      ],
      "habilidades": [1, 4],
      "cubiertos": 1,
      "faltantes": 1,
      "satisfecho": false,
      "puedePostularse": false
    }
  ],
  "moodboard": {
    "idMoodboard": 77,
    "descripcion": "Paleta cálida de arena y terracota, texturas de lino y madera clara.",
    "fechaCreacion": "2026-06-01T10:00:00",
    "imagenes": [
      { "idImagen": 501, "url": "/uploads/perfiles/moodboard_abc.webp" },
      { "idImagen": 502, "url": "/uploads/perfiles/moodboard_def.webp" }
    ]
  },
  "rolUsuario": "DIRECTOR",
  "esMiembro": true,
  "permisos": [
    "ASIGNAR_ACTIVIDAD",
    "CANCELAR_PROYECTO",
    "CONFIRMAR_PROYECTO",
    "CREAR_ACTIVIDAD",
    "ELIMINAR_ACTIVIDAD",
    "ELIMINAR_INTEGRANTE",
    "ELIMINAR_INVITACION",
    "FINALIZAR_PROYECTO",
    "GESTIONAR_POSTULACIONES",
    "INVITAR_MIEMBRO",
    "MODIFICAR_ACTIVIDAD",
    "MODIFICAR_PROYECTO",
    "PUBLICAR_PROYECTO",
    "VER_PROYECTO"
  ],
  "elegibleParaPostularse": false
}
```

Ejemplo del bloque de contexto visto por un **no miembro** (proyecto Público
publicado que acepta postulaciones y cuya profesión coincide con la del perfil):

```json
{
  "...": "mismos campos de siempre",
  "aceptaPostulacionGral": true,
  "rolUsuario": null,
  "esMiembro": false,
  "permisos": [],
  "elegibleParaPostularse": true
}
```

### 3.1 Campos del contexto del consultante (contrato de esta iteración)

| Campo | Tipo | Descripción |
| :--- | :--- | :--- |
| `rolUsuario` | `"DIRECTOR"` \| `"MIEMBRO"` \| `null` | Rol del perfil activo en el proyecto. **`null` = no es miembro.** Coincide con los valores de `ROLES_PROYECTO` en `proyectoConstants.js`. |
| `esMiembro` | `boolean` | `true` si el perfil es **miembro activo** del proyecto (incluye al director). |
| `permisos` | `string[]` | Permisos del rol en este proyecto (nombres del enum del backend, p. ej. `MODIFICAR_PROYECTO`). **Ordenados alfabéticamente. Vacío si no es miembro.** |
| `elegibleParaPostularse` | `boolean` | **Resumen global histórico, deprecado para el gating.** `true` solo si **no es miembro**, el proyecto tiene `aceptaPostulacionGral = true` y la **profesión del perfil activo** coincide con la de algún requerimiento general. Para miembros siempre es `false`. No evalúa cupo ni reemplaza a `requerimientosGral[].puedePostularse`. |

### 3.2 Campos del proyecto (ya existentes, record de formatos)

| Campo | Tipo | Notas |
| :--- | :--- | :--- |
| `estado` | `string` | Forma **legible**: `Borrador`, `Publicado`, `Confirmado`, `Finalizado`, `Cancelado`. Para comparar usar `normalizarEstadoProyecto()` de `proyectoConstants.js`. |
| `privacidad` | `string` | Enum: `PUBLICO` \| `PRIVADO`. Etiquetar con `ETIQUETAS_PRIVACIDAD`. |
| `aceptaPostulacionGral` | `boolean` | El proyecto acepta postulaciones abiertas. |
| `fechaInicio` / `fechaEntregaEstipulada` | `string` (ISO date) \| `null` | Formatear con `formatearFechaCorta()` de `fechas.js`. |
| `ubicacion` | objeto \| `null` | Anidada: `ciudad.provincia.pais`. Para mostrar: `ubicacion.ciudad.nombre` + `ubicacion.provincia.nombre`. |
| `idDirector` / `nombreDirector` | `long` \| `string` \| `null` | `null` si el proyecto no tiene director activo. |
| `objetivos` | `ObjetivoResponse[]` | `[]` si no hay. `moodboard` puede ser `null` si nunca se subieron imágenes ni descripción. |
| `requerimientosGral[].cubiertos` | `int` | Miembros/asignaciones activas vinculadas al requerimiento. |
| `requerimientosGral[].faltantes` | `int` | `max(0, cantidad - cubiertos)`. |
| `requerimientosGral[].satisfecho` | `boolean` | `true` si el cupo está completo. |
| `requerimientosGral[].puedePostularse` | `boolean` | **Fuente de verdad por requerimiento.** Usar `=== true` para mostrar el botón. Incluye estado del proyecto, membresía, profesión y cupo. |
| `requerimientosGral[].caracteristicas[].valores` | `long[]` | **Ids** de valores enumerados; para mostrar etiquetas, reutilizar el catálogo que ya consume `RequerimientoForm.vue`. |
| `requerimientosGral[].habilidades` | `long[]` | **Ids** de habilidades; traducir con `habilidadService` (mismo catálogo del paso 3 de creación). |
| `moodboard.imagenes[].url` | `string` | Ruta relativa a `API_BASE_URL` (servida por `/uploads/**`). |

---

## 4. Matriz de UI por rol (corazón del dashboard)

| Elemento | Director (`rolUsuario === "DIRECTOR"`) | Miembro (`"MIEMBRO"`) | No miembro (`esMiembro === false`) |
| :--- | :--- | :--- | :--- |
| Hero: nombre + `EstadoProyectoBadge` + descripción | ✅ | ✅ | ✅ |
| Subtítulo de rol | ✅ ver textos §4.1 | ✅ ver textos §4.1 | ✅ "Vista pública del proyecto" |
| Botón **Editar** | ✅ **deshabilitado**, tooltip "Próximamente" | ❌ oculto | ❌ oculto |
| Botón **Publicar** | ✅ **activo** (solo si estado Borrador y `PUBLICAR_PROYECTO`) | ❌ oculto | ❌ oculto |
| Botón **Postularme** | ❌ | ❌ | ✅ **deshabilitado**, etiqueta "Próximamente..." (si cumple §4.2) |
| Secciones: Datos generales / Objetivos / Personal requerido / Moodboard | ✅ | ✅ | ✅ |
| Bloque "Próximamente" (miembros, actividades, postulaciones) | ✅ placeholder | ✅ placeholder | ❌ oculto |

### 4.1 Textos del subtítulo de rol

| Escenario | Texto sugerido |
| :--- | :--- |
| Director, proyecto en **Borrador** | `Tu rol: Director de proyecto · Solo vos podés ver el borrador hasta publicarlo` |
| Director, otro estado | `Tu rol: Director de proyecto` |
| Miembro, proyecto en **Borrador** | `Tu rol: Miembro de proyecto · Este proyecto está en borrador: aún no es visible para otros perfiles` |
| Miembro, otro estado | `Tu rol: Miembro de proyecto` |
| No miembro | `Vista pública del proyecto` |

Usar `etiquetaRolProyecto(rolUsuario)` de `proyectoConstants.js` para el nombre del rol.

### 4.2 Reglas de los botones

* **Editar** — visible solo si `permisos.includes("MODIFICAR_PROYECTO")`
  (en la práctica: solo el director). **`disabled`** con tooltip *"Próximamente"*:
  el backend aún no implementa Modificar proyecto (UC-36). No disparar ninguna llamada.
* **Publicar** — visible solo si `permisos.includes("PUBLICAR_PROYECTO")` **y**
  `normalizarEstadoProyecto(estado) === "BORRADOR"`. **Activo**: llama a
  `POST /proyectos/{idProyecto}/publicar` tras confirmación del usuario (modal de
  confirmación del UC-25, paso 3). Si la respuesta es `400` (información básica
  incompleta) mostrar el detalle de campos faltantes; si es `409` (estado ≠ Borrador),
  refrescar los datos del proyecto. En `200`, reemplazar los datos del proyecto con la
  respuesta (el estado pasa a `Publicado` y el botón desaparece).
* **Postularme** — un botón por requerimiento, visible solo si
  `requerimiento.puedePostularse === true` (UC-26 implementado). No usar
  `elegibleParaPostularse` para el gating. Si `puedePostularse = false` por
  profesión y no por cupo, no mostrar “Cupo completo”; usar un mensaje genérico
  como “No disponible para tu perfil”. El `409` de duplicado o disponibilidad
  sigue siendo posible.
* Acciones mutantes con endpoint: **Publicar** (UC-25), **Postularme** (UC-26)
  y las acciones de gestión de postulaciones del director (UC-34). El resto de
  la vista es de solo lectura según permisos.

---

## 5. Estructura de la vista (mockup "Ver proyecto")

1. **Hero**
   * Nombre del proyecto + `EstadoProyectoBadge` (reutilizar componente).
   * Descripción (párrafo).
   * Subtítulo de rol (§4.1) y, a la derecha, acciones por rol (§4.2).
2. **Datos generales**
   * `descripcion` del proyecto (párrafo completo).
   * Campos en grilla: **Privacidad** (`ETIQUETAS_PRIVACIDAD[privacidad]` +
     hint "Visible solo para tus perfiles miembros" cuando `PRIVADO`), **Ubicación**
     (`ciudad.nombre, provincia.nombre` o "—"), **Fecha de entrega**
     (`fechaEntregaEstipulada`), **Fecha de inicio**, **Director**
     (`nombreDirector` o "—").
   * `aceptaPostulacionGral`: badge opcional "Acepta postulaciones".
3. **Objetivos** — lista real (no conteo): por cada objetivo, `nombre` (título) +
   `descripcion`. Estado vacío: "Este proyecto no tiene objetivos cargados."
4. **Personal requerido** — por cada elemento de `requerimientosGral`:
   * Encabezado: **cantidad** + `nombreProfesion` (p. ej. `2 · Modelo`).
   * **Características técnicas** como chips: según `tipoDato` usar
     `esNumerico()` → chip `valorMin`–`valorMax`; `esEnumerado()` → un chip por
     cada id de `valores` traducido con el catálogo.
   * **Habilidades** como chips (traducir ids con `habilidadService`).
   * `descripcion` del requerimiento.
   * **Solo lectura** (hoy solo existe el form editable `RequerimientoForm.vue`;
     renderizar inline o en componente nuevo de lectura).
5. **Moodboard** — si `moodboard` es `null`, ocultar la sección. Si no:
   * Collage de `moodboard.imagenes` (reutilizar `CollageMoodboard.vue`).
   * Contador `${moodboard.imagenes.length} de ${MOODBOARD_IMAGENES.MAX_IMAGENES} imágenes`.
   * `moodboard.descripcion`.
   * Visor ampliado al hacer clic (reutilizar `ModalGaleriaMoodboard.vue`).
6. **Bloque "Próximamente"** (solo miembros): nota de que miembros del equipo,
   cronograma/actividades y postulaciones se habilitarán en una iteración siguiente.

---

## 6. Manejo de Errores

| Código HTTP | Excepción Backend | Mensaje | Acción Frontend |
| :---: | :--- | :--- | :--- |
| **404** | `PerfilActivoNoSeleccionadoException` | *"No hay un perfil activo seleccionado en la sesión."* | Invitar a elegir perfil activo (mismo flujo que el resto del módulo). |
| **404** | `ProyectoNoEncontradoException` | *"El proyecto con id {id} no existe."* | Estado "proyecto no encontrado" + botón volver a Proyectos. |
| **403** | `AccesoDenegadoProyectoException` | *"No tienes acceso a este proyecto."* | Aviso: el proyecto es privado o está en borrador y no sos miembro. **No** confundir con 401 (que el interceptor `http.js` ya redirige a login). |
| **409** | `PerfilEnBajaException` | *"El perfil no se encuentra activo."* | Aviso de perfil en baja (patrón ya usado en creación). |
| **401** | Seguridad | — | Redirigir a login (ya lo hace `http.js`). |
| **≥ 500** | — | — | Mostrar error genérico con botón **Reintentar**. |

---

## 7. Consideraciones de UX (patrones existentes del frontend)

1. **Nuevo método de servicio** en `src/services/proyectoService.js`:

   ```js
   obtener(idProyecto, signal) {
     return http.get(`/proyectos/${idProyecto}`, { signal });
   }
   ```

   Actualizar el `@typedef` con los campos de §3.1.
2. **Carga:** la vista **siempre** debe llamar a la API en `created`/`mounted`.
   La precarga opcional desde `history.state` (como hace hoy `CrearProyectoView`)
   puede usarse para pintar de inmediato, pero **no como única fuente**: al navegar
   desde `ProyectosInicioView.irAProyecto` o `BuscarPerfilView.irAProyecto` no llega
   `state` y hoy la vista queda en un placeholder vacío.
3. **Patrón loading/error/retry:** copiar el de `ProyectosInicioView.vue`
   (`cargando`, `mensajeError`, `puedeReintentar`, `BaseAlert`, toast `danger`,
   retry en respuestas ≥ 500, `AbortController` con guardas `ERR_CANCELED` y
   `abort()` en `beforeUnmount`).
4. **Constantes/utilidades a reusar** (`src/utils/`):
   `EstadoProyectoBadge.vue`, `ETIQUETAS_PRIVACIDAD`, `ESTADOS_PROYECTO`,
   `coloresEstadoProyecto()`, `normalizarEstadoProyecto()`, `ROLES_PROYECTO`,
   `etiquetaRolProyecto()`, `coloresRolProyecto()`, `MOODBOARD_IMAGENES`,
   `formatearFechaCorta` (`fechas.js`), `esNumerico`/`esEnumerado`.
5. **Comparar `permisos`** con `includes("MODIFICAR_PROYECTO")` /
   `includes("PUBLICAR_PROYECTO")` (strings exactos, case-sensitive del backend).
6. **Quitar** el aviso *"…cuando el backend exponga GET /proyectos/{id}"* y el
   placeholder genérico de la vista actual: el endpoint está implementado.
7. El backend de esta ficha ya está implementado y testeado (`mvn test` verde);
   el contrato de §3.1 es **aditivo** y no rompe a los listados UC-29 / UC-29 bis.
