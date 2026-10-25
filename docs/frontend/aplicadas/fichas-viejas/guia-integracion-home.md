# Guía de Integración Frontend (Vue 3): Perfil Activo y Pantalla Home

Esta guía contiene **exclusivamente las nuevas instrucciones y contratos de API** agregados para dar soporte a la pantalla de inicio (**Home**) y a la consulta directa del **perfil activo**, orientada a una arquitectura modular en **Vue 3**.

---

## 1. Nuevo Endpoint: Consultar Perfil Activo

Permite obtener los datos completos del perfil con el que el usuario está operando en la sesión actual.

- **Método y Ruta:** `GET /perfiles/activo`
- **Autenticación requerida:** Sí (cookie HttpOnly `jwt` con `credentials: 'include'`).
- **Respuesta Exitosa (`200 OK`):**
  Devuelve el objeto `PerfilResponse` del perfil activo en el token.
- **Códigos de Error:**
  - `404 Not Found`: Si el token no tiene ningún perfil activo seleccionado (por ejemplo, el usuario recién inicia sesión y tiene más de 1 perfil pendiente de elegir).
    ```json
    { "message": "No hay un perfil activo seleccionado en la sesión.", "httpStatus": 404, "timestamp": 1788000000000 }
    ```
  - `401 Unauthorized`: Si la sesión no es válida o expiró.

### Ejemplo de respuesta `200 OK` (`PerfilResponse`):

```json
{
  "idPerfil": 10,
  "nombreArtistico": "Luna Diseños",
  "biografia": "Diseñadora de indumentaria y estilista.",
  "estado": "Activo",
  "profesion": "Diseñador",
  "fechaSolicitudBaja": null,
  "idImagen": 42,
  "fotoUrl": "/uploads/perfiles/perfil_abc.jpg",
  "caracteristicas": [
    {
      "idCaracteristica": 1,
      "codigo": "ESPECIALIDAD",
      "valor": "Alta costura",
      "idValor": null,
      "codigoValor": null,
      "colorHex": null
    },
    {
      "idCaracteristica": 2,
      "codigo": "TIPO_CONTRATO",
      "valor": null,
      "idValor": 7,
      "codigoValor": "Por proyecto",
      "colorHex": "#1565c0"
    }
  ]
}
```

| Campo | Detalle |
| :--- | :--- |
| `estado` | Nombre display: `"Activo"`, `"PendienteBaja"`, `"Deshabilitado"`, `"Baja"`. |
| `profesion` | Nombre de la profesión (string, no objeto). |
| `fechaSolicitudBaja` | ISO 8601 si el perfil está en `PendienteBaja`; `null` en caso contrario. |
| `fotoUrl` | URL relativa de la foto, o `null` si no tiene. |
| `caracteristicas[].valor` | Valor de texto, o `null` si es un valor predefinido (`idValor`/`codigoValor`). |
| `caracteristicas[].codigoValor` | Etiqueta visible del valor predefinido (no un código corto). |

---

## 2. Endpoint Agregador Base: Resumen de Home (Opcional)

Si la pantalla de Home prefiere cargar su estado inicial en una sola llamada:

- **Método y Ruta:** `GET /home/resumen`
- **Autenticación requerida:** Sí (`credentials: 'include'`).
- **Respuesta Exitosa (`200 OK`):**

```json
{
  "perfilActivo": {
    "idPerfil": 10,
    "nombreArtistico": "Luna Diseños",
    "biografia": "Diseñadora de indumentaria y estilista.",
    "estado": "Activo",
    "profesion": "Diseñador",
    "fechaSolicitudBaja": null,
    "idImagen": 42,
    "fotoUrl": "/uploads/perfiles/perfil_abc.jpg",
    "caracteristicas": []
  },
  "proyectosDestacados": [],
  "publicacionesRecientes": []
}
```

- `perfilActivo`: mismo objeto `PerfilResponse` de §1. Si no hay perfil activo seleccionado, el campo se envía en **`null`** (el endpoint **no** responde 404 en este caso).
- `proyectosDestacados` / `publicacionesRecientes`: **hoy el backend siempre responde `[]`** (stub sin datos reales). No depender de su contenido por ahora.

---

## 3. Flujo Recomendado en Vue 3

### A. Al Activar un Perfil (`PATCH /perfiles/{idPerfil}/activar`)

1. Tras ejecutar con éxito la activación del perfil:
   ```ts
   await apiClient.patch(`/perfiles/${idPerfil}/activar`);
   ```
   La respuesta trae el `PerfilResponse` actualizado **y una `Set-Cookie`** con el nuevo JWT (el perfil activo queda embebido en el token). Usar `credentials: 'include'` para que el navegador la guarde.
2. Guardar en el store de Pinia (ej. `useAuthStore` o `useProfileStore`) el perfil devuelto.
3. Redirigir inmediatamente a la vista principal:
   ```ts
   router.push("/home"); // o router.push({ name: 'home' })
   ```

### B. En la Vista `HomeView.vue` (Enfoque Modular por Recursos)

En `HomeView.vue`, orquestar la vista mediante componentes independientes:

```vue
<template>
  <div class="home-layout">
    <!-- Barra superior o lateral: Datos del perfil activo -->
    <ProfileWidget :perfil="perfilActivo" />

    <main class="feed-container">
      <!-- Módulo de proyectos (se conectará a endpoints de proyectos) -->
      <ProyectosSection />

      <!-- Módulo de publicaciones / feed (se conectará a endpoints de publicaciones) -->
      <PublicacionesFeed />
    </main>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { useAuthStore } from "@/stores/auth";
import apiClient from "@/api/client";

const authStore = useAuthStore();
const router = useRouter();
const perfilActivo = ref(null);

onMounted(async () => {
  try {
    // Si no está ya en el store de Pinia, se puede consultar directamente:
    const { data } = await apiClient.get("/perfiles/activo");
    perfilActivo.value = data;
    authStore.setPerfilActivo(data);
  } catch (error: any) {
    if (error.response?.status === 404) {
      // No hay perfil activo seleccionado -> redirigir a selección de perfil
      router.push("/perfiles/seleccionar");
    }
  }
});
</script>
```

### C. Manejo de Rutas Protegidas en Vue Router

En la guardia de navegación global (`router.beforeEach`):

- Si la ruta requiere un perfil activo (`meta: { requiresActiveProfile: true }`):
  - Verificar si existe `idPerfilActivo` en el store.
  - Si no existe o es `null`, redirigir al usuario a la vista de selección de perfil antes de permitirle entrar a `/home` o `/proyectos`.

---

## 4. Gestión de Proyectos: Crear Proyecto (UC-24)

Cuando el usuario con un perfil activo crea un proyecto, se convierte automáticamente en el **Director** del mismo:

- **Método y Ruta:** `POST /proyectos`
- **Autenticación requerida:** Sí (`credentials: 'include'`). Requiere que la sesión posea un perfil activo.
- **Cuerpo (JSON Request):**

```json
{
  "nombre": "Campaña Urbana 2026",
  "descripcion": "Producción fotográfica y estilismo para indumentaria de calle.",
  "privacidad": "PUBLICO",
  "fechaInicio": "2026-06-01",
  "fechaFinEstipulada": "2026-06-15",
  "aceptaPostulacionGral": true,
  "ubicacion": {
    "localidadId": "06441010000",
    "provinciaId": "06"
  },
  "objetivos": [
    {
      "nombre": "Conseguir marcas patrocinadoras",
      "descripcion": "Contactar al menos 3 marcas locales"
    }
  ]
}
```

| Campo | Obligatorio | Reglas |
| :--- | :---: | :--- |
| `nombre` | Sí | `@NotBlank`, máx. 50 caracteres. |
| `descripcion` | Sí | `@NotBlank`, máx. 200 caracteres. |
| `privacidad` | Sí | **Enum exacto (case-sensitive): `PUBLICO`, `PRIVADO`, `OCULTO`.** Otro valor ⇒ 400. |
| `fechaInicio` | Sí | Fecha ISO (`yyyy-MM-dd`). |
| `fechaFinEstipulada` | No | Si se envía, debe ser `>= fechaInicio`. |
| `aceptaPostulacionGral` | No | Default `false` si se omite o `null`. |
| `ubicacion` | No | Objeto `{ localidadId, provinciaId }`. Si se envía el objeto, `localidadId` es **obligatorio** (`@NotBlank`); `provinciaId` opcional pero **no puede ir sin `localidadId`**. Ambos son IDs del catálogo Georef (strings). |
| `objetivos` | No | Array de `{ nombre (obligatorio, máx. 100), descripcion (opcional, máx. 300) }`. |

- **Respuesta Exitosa (`201 Created`):**

```json
{
  "idProyecto": 100,
  "nombre": "Campaña Urbana 2026",
  "descripcion": "Producción fotográfica y estilismo para indumentaria de calle.",
  "fechaInicio": "2026-06-01",
  "fechaFinEstipulada": "2026-06-15",
  "estado": "Borrador",
  "privacidad": "PUBLICO",
  "aceptaPostulacionGral": true,
  "ubicacion": {
    "idUbicacion": 50,
    "direccion": null,
    "codigoPostal": null,
    "latitud": -34.9214,
    "longitud": -57.9545,
    "ciudad": {
      "idCiudad": 12,
      "idExterno": "06441010000",
      "fuenteApi": "GEOREF",
      "nombre": "La Plata",
      "provincia": {
        "idProvincia": 3,
        "idExterno": "06",
        "fuenteApi": "GEOREF",
        "nombre": "Buenos Aires",
        "pais": { "idPais": 1, "idExterno": "AR", "fuenteApi": "GEOREF", "nombre": "Argentina" }
      }
    }
  },
  "idDirector": 10,
  "nombreDirector": "Luna Diseños",
  "objetivos": [
    {
      "idObjetivo": 1,
      "nombre": "Conseguir marcas patrocinadoras",
      "descripcion": "Contactar al menos 3 marcas locales"
    }
  ]
}
```

Notas sobre la respuesta:

| Campo | Detalle |
| :--- | :--- |
| `estado` | Nombre display: `"Borrador"` (estado inicial del proyecto). |
| `privacidad` | **Nombre del enum:** `"PUBLICO"` / `"PRIVADO"` / `"OCULTO"` (no `"Publico"`). |
| `ubicacion` | Objeto anidado `UbicacionResponse` (no forma plana). `null` si no se envió ubicación. **No** expone `localidadId` de Georef; para reeditar, el dato de catálogo vive en `ciudad.idExterno` / `provincia.idExterno`. |
| `idDirector` | **ID del perfil** director (no de usuario) y `nombreDirector` es su nombre artístico. |

- **Errores Posibles** (cuerpo: `{ "message", "httpStatus", "timestamp" }`):

| Código | Escenario | `message` |
| :---: | :--- | :--- |
| `400` | Bean Validation: campos obligatorios vacíos o excedidos (`nombre` >50, `descripcion` >200, `objetivos[].nombre` >100, etc.). | Objeto `errores` por campo: `{ "message": "Validación fallida", "errores": { "nombre": "..." } }`. |
| `400` | `fechaFinEstipulada` < `fechaInicio`. | `La fecha de finalización o entrega no puede ser anterior a la fecha de inicio.` |
| `400` | `ubicacion` enviada con `provinciaId` sin `localidadId`. | `No se puede enviar provincia sin localidad.` |
| `400` | `privacidad` con valor fuera del enum (ej: `"Publico"`). | Error de deserialización de Jackson (no usar el valor literal de la respuesta para reenviarlo… usar siempre mayúsculas). |
| `404` | Sesión sin perfil activo seleccionado. | `No hay un perfil activo seleccionado en la sesión.` |
| `404` | El perfil activo no existe o no pertenece al usuario. | `Perfil no encontrado.` |
| `409` | El perfil activo ya tiene un proyecto con ese mismo nombre. | `Ya tienes un proyecto con el nombre '...'.` |
| `409` | El perfil activo no está en estado `Activo` (está en baja). | `El perfil no se encuentra activo.` |
| `403` | Sesión sin autorización / token inválido (fallos de seguridad). | — |

---

## 5. Referencias de Implementación Backend

| Aspecto | Archivo |
| :--- | :--- |
| Perfil activo | `perfiles/controlador/PerfilController.java` (`obtenerPerfilActivo`) |
| Resumen Home | `home/controlador/HomeController.java` + `home/dto/HomeResumenResponse.java` |
| DTO del perfil | `perfiles/dto/PerfilResponse.java` |
| Activar perfil (cookie JWT) | `perfiles/controlador/PerfilController.java` (`activar`) |
| Crear proyecto | `proyectos/controlador/ProyectoController.java` + `proyectos/servicio/CrearProyectoService.java` |
| Request/Response de proyecto | `proyectos/dto/CrearProyectoRequest.java`, `ProyectoResponse.java` |
| Enum privacidad | `modelo/enums/Privacidad.java` (`PUBLICO`/`PRIVADO`/`OCULTO`) |
| Ubicación (request/response) | `ubicacion/dto/UbicacionRequest.java`, `UbicacionResponse.java` |
| Errores (400/404/409) | `common/exception/GlobalExceptionHandler.java` |
