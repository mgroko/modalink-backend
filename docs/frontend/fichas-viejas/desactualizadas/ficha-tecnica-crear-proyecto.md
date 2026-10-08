# Ficha Técnica Frontend: Pantalla "Crear Proyecto" (UC-24)

Esta especificación detalla la interacción con la API, validaciones, modelos de datos, manejo de errores y consideraciones de UX/UI para el desarrollo del formulario y flujo de creación de proyectos.

---

## 1. Endpoint y Contexto de Autenticación

* **Método:** `POST`
* **URL:** `/proyectos`
* **Content-Type:** `application/json`
* **Autenticación requerida:** Sí (Bearer Token JWT).
* **Contexto de Perfil Requerido:** 
  * La solicitud debe tener un **perfil activo seleccionado** en el contexto de la sesión (`idPerfilActivo`).
  * Si el usuario no tiene un perfil activo seleccionado o el perfil se encuentra en estado de baja, el backend rechazará la petición con error `403 Forbidden`.

---

## 2. Estructura de Datos (Payloads)

### Request Payload (`POST /proyectos`)

```json
{
  "nombre": "Colección Primavera-Verano",
  "descripcion": "Diseño y confección de prendas sostenibles para la nueva temporada.",
  "privacidad": "Publico",
  "fechaInicio": "2026-10-01",
  "fechaFinEstipulada": "2026-12-15",
  "aceptaPostulacionGral": true,
  "ubicacion": {
    "localidadId": "06049010000",
    "provinciaId": "06"
  },
  "objetivos": [
    {
      "nombre": "Diseño de bocetos iniciales",
      "descripcion": "Elaborar los primeros 10 bocetos de las prendas clave."
    },
    {
      "nombre": "Compra de materia prima",
      "descripcion": "Adquisición de telas recicladas y avíos."
    }
  ]
}
```

---

## 3. Matriz de Campos y Validaciones en Frontend

| Campo | Tipo / Formato | Obligatorio | Reglas / Restricciones Backend | Notas UI/UX sugeridas |
| :--- | :--- | :---: | :--- | :--- |
| `nombre` | `string` | **Sí** | • Máx. 50 caracteres.<br>• No puede estar en blanco (`trim`).<br>• Único para el perfil director activo. | Input de texto con contador de caracteres visibles `(x/50)`. |
| `descripcion` | `string` | **Sí** | • Máx. 200 caracteres.<br>• No puede estar en blanco (`trim`). | Textarea con contador de caracteres `(x/200)`. |
| `privacidad` | `enum (string)` | **Sí** | Valores válidos:<br>• `"Publico"`<br>• `"Privado"`<br>• `"Oculto"` | Selector tipo Segmented Control / Radio Buttons / Select con etiquetas descriptivas. |
| `fechaInicio` | `string (YYYY-MM-DD)` | **Sí** | • Formato ISO `YYYY-MM-DD`.<br>• Campo obligatorio. | Datepicker. Sugerencia: preseleccionar la fecha de hoy o solicitar al usuario. |
| `fechaFinEstipulada` | `string (YYYY-MM-DD)` | No | • Debe ser **igual o posterior** a `fechaInicio`. | Datepicker con validación reactiva (`minDate = fechaInicio`). |
| `aceptaPostulacionGral` | `boolean` | No | • Booleano opcional. Por defecto el backend guarda `false` si es `null`. | Switch / Checkbox: *"¿Aceptar postulaciones abiertas de otros profesionales?"*. |
| `ubicacion` | `object` | No | • Objeto opcional.<br>• Si se envía, `localidadId` no puede estar en blanco. | Buscador / Autocompletado de localidades (catálogo Georef). Si no se define, omitir el campo o enviar `null`. |
| `ubicacion.localidadId` | `string` | Condicional | • Obligatorio si se envía el objeto `ubicacion`. | ID asignado por Georef para la localidad seleccionada. |
| `ubicacion.provinciaId` | `string` | No | • Opcional. | ID asignado por Georef para la provincia. |
| `objetivos` | `array` | No | • Lista dinámica de objetivos. Puede enviarse vacía `[]` o `null`. | Sección dinámica para añadir/eliminar ítems. |
| `objetivos[].nombre` | `string` | **Sí** (en cada ítem) | • Máx. 100 caracteres.<br>• Obligatorio si el objetivo existe. | Input de texto para el título del objetivo. |
| `objetivos[].descripcion` | `string` | No | • Máx. 300 caracteres. | Input/Textarea opcional para detalle del objetivo. |

---

## 4. Respuesta Exitosa (`201 Created`)

El backend devuelve el proyecto creado, asociando al usuario autenticado como **Director** activo y fijando el estado inicial del proyecto en **Borrador**.

```json
{
  "idProyecto": 105,
  "nombre": "Colección Primavera-Verano",
  "descripcion": "Diseño y confección de prendas sostenibles para la nueva temporada.",
  "fechaInicio": "2026-10-01",
  "fechaFinEstipulada": "2026-12-15",
  "estado": "Borrador",
  "privacidad": "Publico",
  "aceptaPostulacionGral": true,
  "ubicacion": {
    "idUbicacion": 12,
    "localidadId": "06049010000",
    "localidad": "Azul",
    "provincia": "Buenos Aires",
    "pais": "Argentina",
    "codigoPostal": "7300",
    "latitud": -36.7766,
    "longitud": -59.8583
  },
  "idDirector": 4,
  "nombreDirector": "Juan Perez",
  "objetivos": [
    {
      "idObjetivo": 210,
      "nombre": "Diseño de bocetos iniciales",
      "descripcion": "Elaborar los primeros 10 bocetos de las prendas clave."
    },
    {
      "idObjetivo": 211,
      "nombre": "Compra de materia prima",
      "descripcion": "Adquisición de telas recicladas y avíos."
    }
  ]
}
```

---

## 5. Manejo de Errores y Excepciones

| Código HTTP | Excepción Backend | Causa / Mensaje de Error | Acción Frontend Recomendada |
| :---: | :--- | :--- | :--- |
| **`400 Bad Request`** | `MethodArgumentNotValidException` | Fallos de validación de formulario (campos vacíos, textos mayores al largo máximo permitido). | Resaltar inputs en rojo y renderizar mensaje inline bajo cada campo afectado. |
| **`400 Bad Request`** | `RangoFechasProyectoInvalidoException` | *"La fecha de finalización o entrega no puede ser anterior a la fecha de inicio."* | Mostrar error en el campo `fechaFinEstipulada` y bloquear el envío si no cumple la condición. |
| **`400 Bad Request`** | `NombreProyectoDuplicadoException` | *"Ya existe un proyecto activo con el nombre '{nombre}' para este perfil director."* | Mostrar error inline en el campo `nombre`. |
| **`403 Forbidden`** | `PerfilActivoNoSeleccionadoException` | El usuario no tiene un perfil activo seleccionado en el contexto de la aplicación. | Redirigir o desplegar modal para que el usuario seleccione el perfil con el que desea operar. |
| **`403 Forbidden`** | `PerfilEnBajaException` | *"El perfil no se encuentra activo."* | Alerta bloqueante indicando que el perfil actual está dado de baja. |
| **`404 Not Found`** | `PerfilNoEncontradoException` | El perfil asociado no existe o no corresponde a la cuenta autenticada. | Forzar sincronización de sesión o redirigir al login/home. |

---

## 6. Recomendaciones de UX y Flujo de Pantalla

1. **Gestión Dinámica de Objetivos:**
   * Permitir agregar múltiples objetivos con botón `+ Agregar Objetivo`.
   * Proveer botón para remover filas de objetivos individuales.
   * Evitar despachar objetivos con nombre vacío; filtrarlos antes de armar el payload.
2. **Validación de Fechas en Tiempo Real:**
   * Al cambiar `fechaInicio`, ajustar automáticamente el `min` del datepicker de `fechaFinEstipulada`.
   * Si `fechaFinEstipulada` ya seleccionada queda anterior a la nueva `fechaInicio`, limpiarla o marcar error inmediatamente.
3. **Payload Limpio de Ubicación:**
   * Si el usuario no interactúa con el selector de ubicación, enviar `ubicacion: null` (no enviar objeto vacío `{}`).
4. **Navegación Post-Creación:**
   * Redirigir al usuario al dashboard o vista de detalle del proyecto creado usando el `idProyecto` retornado en la respuesta (`/proyectos/{idProyecto}`).
