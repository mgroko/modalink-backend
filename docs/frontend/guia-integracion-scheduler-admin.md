# Guía de Integración Frontend (Vue 3): Configuración de Scheduler de Deshabilitación (Panel Admin)

Esta guía documenta los contratos de API, permisos, modelos de datos y flujo de integración necesarios para el desarrollo de la pantalla de **Configuración de Tareas Programadas (Scheduler de Deshabilitación)** en el panel de administración.

---

## 1. Contexto y Casos de Uso

El backend incluye una tarea programada periódica (`DeshabilitacionScheduler`) responsable de reactivar automáticamente las cuentas de usuarios cuya deshabilitación temporal (UC-04) haya expirado.

Previamente, esta tarea se ejecutaba en un horario fijo (2:00 AM). Con los cambios introducidos, el administrador puede:
1. **Consultar la configuración horaria actual** del scheduler y su próxima fecha/hora de ejecución.
2. **Reprogramar la hora y minuto diarios** en que se disparará el proceso automático.
3. **Forzar una ejecución manual inmediata** ("Ejecutar Ahora") para procesar reactivaciones pendientes sin esperar al horario programado.

---

## 2. Permisos y Seguridad

- **Rol Requerido:** `Administrador` (o cualquier usuario con el permiso adecuado).
- **Permiso Global Requerido:** `ADMINISTRAR_CONFIGURACION`.
- **Mecanismo de Autenticación:** Cookie `HttpOnly` (`jwt`).
- **Requisito en Peticiones HTTP:** Todas las solicitudes al backend deben enviar credenciales de sesión:
  - En `axios`: `withCredentials: true`
  - En `fetch`: `credentials: 'include'`
- **Control de Acceso Frontend:** Si el usuario no tiene el permiso `ADMINISTRAR_CONFIGURACION` en `authStore.usuario.permisosGlobales`, se debe ocultar la opción en el menú de navegación y proteger la ruta con un navigation guard (`beforeEnter`).

---

## 3. Especificación de Endpoints

**Prefijo base:** `/admin/configuracion/schedulers/deshabilitacion`

### A. Consultar Configuración Actual

Obtiene la hora, minuto, expresión cron activa y el cálculo de la próxima ejecución en el servidor.

- **Método y Ruta:** `GET /admin/configuracion/schedulers/deshabilitacion`
- **Permiso:** `ADMINISTRAR_CONFIGURACION`
- **Respuestas:**
  - `200 OK`: Devuelve los detalles de programación.
  - `401 Unauthorized`: Sesión inválida o expirada.
  - `403 Forbidden`: El usuario no posee el permiso `ADMINISTRAR_CONFIGURACION`.

#### Ejemplo de Respuesta (`200 OK`):

```json
{
  "hora": 2,
  "minuto": 0,
  "cron": "0 0 2 * * *",
  "proximaEjecucion": "2026-09-09T02:00:00"
}
```

#### Descripción de Campos:
| Campo | Tipo | Descripción |
| :--- | :--- | :--- |
| `hora` | `Integer` | Hora del día programada (formato 24h, `0` a `23`). |
| `minuto` | `Integer` | Minuto programado (`0` a `59`). |
| `cron` | `String` | Expresión cron activa evaluada por Spring (ej. `"0 30 3 * * *"`). Solo lectura/informativo. |
| `proximaEjecucion` | `String (ISO-8601)` o `null` | Fecha y hora de la próxima corrida estimada según el huso horario del servidor. |

---

### B. Actualizar Horario del Scheduler

Actualiza la hora y minuto de ejecución diaria. El backend recalcula internamente la expresión cron (ej. `"0 {minuto} {hora} * * *"`) y actualiza la fecha/hora de la próxima ejecución.

- **Método y Ruta:** `POST /admin/configuracion/schedulers/deshabilitacion`
- **Content-Type:** `application/json`
- **Permiso:** `ADMINISTRAR_CONFIGURACION`
- **Respuestas:**
  - `200 OK`: Configuración persistida con éxito (devuelve el objeto actualizado).
  - `400 Bad Request`: Parámetros fuera de rango o ausentes.
  - `401 Unauthorized`: No autenticado.
  - `403 Forbidden`: Sin permisos de configuración.

#### Payload de Solicitud (`Request Body`):

```json
{
  "hora": 3,
  "minuto": 30
}
```

#### Reglas de Validación en el Backend:
- `hora`: Obligatorio (`@NotNull`), valor entero entre `0` y `23` (`@Min(0)`, `@Max(23)`).
- `minuto`: Obligatorio (`@NotNull`), valor entero entre `0` y `59` (`@Min(0)`, `@Max(59)`).

#### Ejemplo de Respuesta (`200 OK`):

```json
{
  "hora": 3,
  "minuto": 30,
  "cron": "0 30 3 * * *",
  "proximaEjecucion": "2026-09-09T03:30:00"
}
```

#### Ejemplo de Error de Validación (`400 Bad Request`):

```json
{
  "message": "Error de validación",
  "errors": {
    "hora": "La hora debe estar entre 0 y 23.",
    "minuto": "El minuto debe estar entre 0 y 59."
  }
}
```

---

### C. Forzar Ejecución Inmediata ("Ejecutar Ahora")

Dispara sincrónicamente el método de expiración de deshabilitaciones en el servidor sin alterar la programación configurada.

- **Método y Ruta:** `POST /admin/configuracion/schedulers/deshabilitacion/ejecutar-ahora`
- **Permiso:** `ADMINISTRAR_CONFIGURACION`
- **Respuestas:**
  - `200 OK`: Ejecución finalizada con el balance de usuarios reactivados.
  - `401 Unauthorized`: No autenticado.
  - `403 Forbidden`: Sin permisos.

#### Ejemplo de Respuesta (`200 OK` - Con usuarios procesados):

```json
{
  "registrosAfectados": 4,
  "ejecutadoEn": "2026-09-08T21:15:30.123456",
  "mensaje": "Se reactivaron con éxito 4 usuario(s)."
}
```

#### Ejemplo de Respuesta (`200 OK` - Sin usuarios pendientes):

```json
{
  "registrosAfectados": 0,
  "ejecutadoEn": "2026-09-08T21:15:30.123456",
  "mensaje": "No se encontraron usuarios con deshabilitación vencida para reactivar."
}
```

---

## 4. Estructura de Datos (JavaScript / Objetos Esperados)

### Estructura de `ConfiguracionSchedulerResponse`
```js
{
  hora: 2,                 // Number (0-23)
  minuto: 0,               // Number (0-59)
  cron: "0 0 2 * * *",     // String
  proximaEjecucion: "2026-09-09T02:00:00" // String ISO-8601 o null
}
```

### Estructura de `ConfigurarSchedulerRequest` (Payload POST)
```js
{
  hora: 3,                 // Number (0-23)
  minuto: 30               // Number (0-59)
}
```

### Estructura de `EjecutarSchedulerResponse`
```js
{
  registrosAfectados: 4,   // Number
  ejecutadoEn: "2026-09-08T21:15:30.123456", // String ISO-8601
  mensaje: "Se reactivaron con éxito 4 usuario(s)." // String
}
```

---

## 5. Servicio de API (Axios en JavaScript)

```js
// src/services/adminConfiguracionService.js
import apiClient from '@/services/apiClient';

const BASE_URL = '/admin/configuracion/schedulers/deshabilitacion';

export const adminConfiguracionService = {
  /**
   * Obtiene la configuración actual del scheduler de deshabilitación.
   * @returns {Promise<{hora: number, minuto: number, cron: string, proximaEjecucion: string|null}>}
   */
  async obtenerConfiguracion() {
    const { data } = await apiClient.get(BASE_URL);
    return data;
  },

  /**
   * Guarda una nueva hora y minuto para la ejecución automática.
   * @param {{hora: number, minuto: number}} payload
   * @returns {Promise<{hora: number, minuto: number, cron: string, proximaEjecucion: string|null}>}
   */
  async actualizarConfiguracion(payload) {
    const { data } = await apiClient.post(BASE_URL, payload);
    return data;
  },

  /**
   * Dispara inmediatamente la tarea de reactivación de cuentas vencidas.
   * @returns {Promise<{registrosAfectados: number, ejecutadoEn: string, mensaje: string}>}
   */
  async ejecutarAhora() {
    const { data } = await apiClient.post(`${BASE_URL}/ejecutar-ahora`);
    return data;
  }
};
```

---

## 6. Integración en Vista Vue 3 (JavaScript)

### Componente de Configuración (`AdminSchedulerConfigView.vue`):

```vue
<template>
  <div class="scheduler-config-card">
    <h2>Programación de Deshabilitaciones</h2>
    <p class="description">
      Configura la hora en que el sistema revisa y reactiva cuentas cuya suspensión temporal finalizó.
    </p>

    <!-- Estado de carga inicial -->
    <div v-if="cargando" class="loading-state">Cargando configuración...</div>

    <form v-else @submit.prevent="guardarConfiguracion" class="config-form">
      <div class="form-row">
        <div class="field-group">
          <label for="hora">Hora (0 - 23):</label>
          <input
            id="hora"
            v-model.number="form.hora"
            type="number"
            min="0"
            max="23"
            required
            class="input-control"
          />
        </div>

        <div class="field-group">
          <label for="minuto">Minuto (0 - 59):</label>
          <input
            id="minuto"
            v-model.number="form.minuto"
            type="number"
            min="0"
            max="59"
            required
            class="input-control"
          />
        </div>
      </div>

      <!-- Información adicional de la tarea -->
      <div class="info-box" v-if="configuracion">
        <p><strong>Expresión Cron:</strong> <code>{{ configuracion.cron }}</code></p>
        <p><strong>Próxima ejecución:</strong> {{ formatearFecha(configuracion.proximaEjecucion) }}</p>
      </div>

      <!-- Acciones -->
      <div class="actions">
        <button type="submit" :disabled="guardando" class="btn-primary">
          {{ guardando ? 'Guardando...' : 'Guardar Horario' }}
        </button>

        <button
          type="button"
          @click="ejecutarManualmente"
          :disabled="ejecutando"
          class="btn-secondary"
        >
          {{ ejecutando ? 'Ejecutando...' : 'Ejecutar Ahora' }}
        </button>
      </div>

      <!-- Mensajes de feedback -->
      <p v-if="feedbackMensaje" :class="['feedback', feedbackTipo]">
        {{ feedbackMensaje }}
      </p>
    </form>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { adminConfiguracionService } from '@/services/adminConfiguracionService';

const cargando = ref(true);
const guardando = ref(false);
const ejecutando = ref(false);
const configuracion = ref(null);

const form = ref({
  hora: 2,
  minuto: 0
});

const feedbackMensaje = ref('');
const feedbackTipo = ref('success');

const cargarDatos = async () => {
  try {
    cargando.value = true;
    const data = await adminConfiguracionService.obtenerConfiguracion();
    configuracion.value = data;
    form.value.hora = data.hora;
    form.value.minuto = data.minuto;
  } catch (error) {
    mostrarFeedback('Error al cargar la configuración.', 'error');
  } finally {
    cargando.value = false;
  }
};

const guardarConfiguracion = async () => {
  try {
    guardando.value = true;
    const data = await adminConfiguracionService.actualizarConfiguracion({
      hora: form.value.hora,
      minuto: form.value.minuto
    });
    configuracion.value = data;
    mostrarFeedback('Horario actualizado con éxito.', 'success');
  } catch (error) {
    const errorMsg = error.response?.data?.message || 'Error al guardar los cambios.';
    mostrarFeedback(errorMsg, 'error');
  } finally {
    guardando.value = false;
  }
};

const ejecutarManualmente = async () => {
  if (!confirm('¿Desea forzar la revisión de suspensiones vencidas ahora mismo?')) {
    return;
  }

  try {
    ejecutando.value = true;
    const res = await adminConfiguracionService.ejecutarAhora();
    mostrarFeedback(res.mensaje, 'success');
  } catch (error) {
    mostrarFeedback('No se pudo ejecutar la tarea manual.', 'error');
  } finally {
    ejecutando.value = false;
  }
};

const mostrarFeedback = (mensaje, tipo = 'success') => {
  feedbackMensaje.value = mensaje;
  feedbackTipo.value = tipo;
  setTimeout(() => {
    feedbackMensaje.value = '';
  }, 5000);
};

const formatearFecha = (iso) => {
  if (!iso) return 'No calculada';
  return new Date(iso).toLocaleString();
};

onMounted(() => {
  cargarDatos();
});
</script>
```

---

## 7. Buenas Prácticas y Puntos de Atención

1. **Huso Horario:** El campo `proximaEjecucion` es devuelto en formato ISO sin zona (`LocalDateTime` según el huso horario del servidor). Al formatearlo en frontend con `new Date(...)`, validar si se requiere sincronizar el huso de referencia o mostrarlo con un helper de formato local.
2. **Validación en Cliente:** Proporcionar validación temprana para `0 <= hora <= 23` y `0 <= minuto <= 59` antes de enviar el POST, para una mejor experiencia de usuario.
3. **Feedback en Ejecución Manual:** Al presionar "Ejecutar Ahora", la respuesta informa directamente la cantidad de cuentas procesadas (`registrosAfectados`), ideal para mostrarlo en un Toast o notificación de éxito.
