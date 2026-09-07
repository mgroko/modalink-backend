# Guía de Integración Frontend (Vue 3): Perfil Activo y Pantalla Home

Esta guía contiene **exclusivamente las nuevas instrucciones y contratos de API** agregados para dar soporte a la pantalla de inicio (**Home**) y a la consulta directa del **perfil activo**, orientada a una arquitectura modular en **Vue 3**.

---

## 1. Nuevo Endpoint: Consultar Perfil Activo

Permite obtener los datos completos del perfil con el que el usuario está operando en la sesión actual.

- **Método y Ruta:** `GET /perfiles/activo`
- **Autenticación requerida:** Sí (cookie HttpOnly `jwt` con `credentials: 'include'`).
- **Respuesta Exitosa (`200 OK`):**
  Devuelve el objeto [`PerfilResponse`](#ejemplo-perfilresponse) del perfil activo en el token.
- **Códigos de Error:**
  - `404 Not Found`: Si el token no tiene ningún perfil activo seleccionado (por ejemplo, el usuario recién inicia sesión y tiene más de 1 perfil pendiente de elegir).
    ```json
    {
      "message": "No hay un perfil activo seleccionado en la sesión."
    }
    ```
  - `401 Unauthorized`: Si la sesión no es válida o expiró.

### Ejemplo de respuesta `200 OK`:
```json
{
  "idPerfil": 10,
  "nombreArtistico": "Luna Diseños",
  "biografia": "Diseñadora de indumentaria y estilista.",
  "estado": "Activo",
  "profesionPrincipal": "Diseñador",
  "urlFoto": "https://...",
  "caracteristicasTecnicas": [
    {
      "idCaracteristica": 1,
      "nombre": "Especialidad",
      "valor": "Alta costura",
      "descripcion": null,
      "tipoDato": "TEXTO",
      "unidadMedida": null
    }
  ]
}
```

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
    "profesionPrincipal": "Diseñador",
    "urlFoto": "https://...",
    "caracteristicasTecnicas": []
  },
  "proyectosDestacados": [],
  "publicacionesRecientes": []
}
```
*(Nota: Si no hay perfil activo seleccionado, el campo `perfilActivo` se enviará en `null`).*

---

## 3. Flujo Recomendado en Vue 3

### A. Al Activar un Perfil (`PATCH /perfiles/{idPerfil}/activar`)
1. Tras ejecutar con éxito la activación del perfil:
   ```ts
   await apiClient.patch(`/perfiles/${idPerfil}/activar`);
   ```
2. Guardar en el store de Pinia (ej. `useAuthStore` o `useProfileStore`) el perfil devuelto.
3. Redirigir inmediatamente a la vista principal:
   ```ts
   router.push('/home'); // o router.push({ name: 'home' })
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
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import apiClient from '@/api/client';

const authStore = useAuthStore();
const router = useRouter();
const perfilActivo = ref(null);

onMounted(async () => {
  try {
    // Si no está ya en el store de Pinia, se puede consultar directamente:
    const { data } = await apiClient.get('/perfiles/activo');
    perfilActivo.value = data;
    authStore.setPerfilActivo(data);
  } catch (error: any) {
    if (error.response?.status === 404) {
      // No hay perfil activo seleccionado -> redirigir a selección de perfil
      router.push('/perfiles/seleccionar');
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
