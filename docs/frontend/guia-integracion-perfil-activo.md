# Guía de Integración Frontend: Gestión de Sesión y Perfil Activo

Este documento describe la especificación técnica, endpoints y flujo de interacción necesarios para implementar en el frontend el soporte de **autenticación por cookies HttpOnly**, la **selección de perfil activo ("iniciar sesión en perfil")** y el **cambio en caliente entre perfiles**.

---

## 1. Mecanismo de Autenticación

- **Transporte:** El token JWT se maneja mediante cookies `HttpOnly` denominadas `jwt`.
- **Requisitos de peticiones HTTP en el Frontend:**
  - Todas las peticiones al backend (ej. usando `fetch` o `axios`) **deben incluir credenciales**:
    - Con `fetch`: `{ credentials: 'include' }`
    - Con `axios`: `axios.defaults.withCredentials = true` o `{ withCredentials: true }`
  - El frontend **no necesita leer ni almacenar el JWT manualmente**, el navegador lo enviará y actualizará automáticamente cuando el backend emita los encabezados `Set-Cookie`.

---

## 2. Endpoints Relevantes

### A. Consultar Usuario Actual y Perfil Activo
- **Ruta:** `GET /auth/me`
- **Requiere autenticación:** Sí
- **Respuesta (200 OK):**
```json
{
  "idUsuario": 1,
  "nombre": "Juan",
  "apellido": "Pérez",
  "correo": "juan@example.com",
  "rolGlobal": "Usuario",
  "permisosGlobales": ["VER_PERFILES", "..."],
  "idPerfilActivo": 5, // null si no tiene perfil activo
  "nombreArtisticoActivo": "DJ Juan" // null si no tiene perfil activo
}
```

---

### B. Listar Perfiles del Usuario
- **Ruta:** `GET /usuarios/me/perfiles`
- **Requiere autenticación:** Sí
- **Respuesta (200 OK):** Lista de perfiles pertenecientes al usuario actual.
```json
[
  {
    "idPerfil": 5,
    "nombreArtistico": "DJ Juan",
    "biografia": "Biografía...",
    "fotoPerfil": "https://...",
    "banner": "https://...",
    "estado": "Activo", // "Activo" o "EnBaja"
    "esPrincipal": true,
    "profesiones": [
      {
        "idProfesion": 1,
        "nombre": "DJ",
        "descripcion": "..."
      }
    ],
    "caracteristicasTecnicas": []
  }
]
```

---

### C. Activar / Iniciar Sesión en un Perfil (UC-13)
- **Ruta:** `PATCH /perfiles/{idPerfil}/activar`
- **Requiere autenticación:** Sí
- **Parámetros:** `idPerfil` (número en URL)
- **Cuerpo (body):** Vacío `{}`
- **Respuesta (200 OK):** Devuelve el objeto del perfil activado.
  - **Nota:** El backend emite en este endpoint una nueva cookie `Set-Cookie: jwt=...` con los claims actualizados (`idPerfilActivo`, `nombreArtisticoActivo`).
```json
{
  "idPerfil": 5,
  "nombreArtistico": "DJ Juan",
  "biografia": "Biografía...",
  "fotoPerfil": "https://...",
  "banner": "https://...",
  "estado": "Activo",
  "esPrincipal": true,
  "profesiones": [...],
  "caracteristicasTecnicas": [...]
}
```
- **Errores Posibles:**
  - `404 Not Found`: Si el perfil no existe o no pertenece al usuario autenticado.
  - `400 Bad Request` (`PerfilEnBajaException`): Si el perfil está en proceso de baja o dado de baja (`estado != 'Activo'`).

---

## 3. Flujo de Usuario y Lógica en Frontend

### Flujo 1: Inicio de Sesión o Recarga de Página
1. Al cargar la aplicación o completar el login (`POST /auth/login`):
2. Consumir `GET /auth/me`.
3. Validar `idPerfilActivo`:
   - **Caso A (`idPerfilActivo != null`):**
     - El backend ya asignó un perfil por defecto (el usuario tenía un único perfil activo).
     - Guardar en el estado global (`AuthContext` / `Store`) los datos del usuario y el perfil activo.
     - Redirigir directamente a la pantalla principal o dashboard.
   - **Caso B (`idPerfilActivo == null`):**
     - El usuario tiene 0 perfiles o más de 1 perfil activo.
     - Llamar a `GET /usuarios/me/perfiles`:
       - Si la lista está **vacía**: redirigir a la pantalla de creación de primer perfil (`/crear-perfil`).
       - Si la lista tiene **1 o más perfiles**: mostrar pantalla/modal **"Selecciona tu perfil"**.

---

### Flujo 2: Selección de Perfil ("Iniciar sesión al perfil")
1. En la pantalla o modal de selección de perfiles, el usuario visualiza sus perfiles con su foto, nombre artístico y profesión.
2. Al hacer clic en un perfil:
   - Enviar `PATCH /perfiles/{idPerfil}/activar`.
3. Al recibir respuesta exitosa (200):
   - Actualizar el estado global con el perfil activo seleccionado (`idPerfilActivo`, `nombreArtisticoActivo`).
   - Redirigir a la vista principal.

---

### Flujo 3: Switcher de Perfil en Caliente (Navbar / Avatar)
1. En el Header/Navbar, mostrar un menú desplegable (avatar o nombre del perfil activo).
2. Opciones recomendadas en el menú:
   - Perfil actual (marcado como activo / online).
   - Lista de perfiles alternativos.
   - Opción "Crear nuevo perfil".
   - "Cerrar sesión" (`POST /auth/logout`).
3. Al seleccionar un perfil alternativo:
   - Disparar `PATCH /perfiles/{nuevoIdPerfil}/activar`.
   - Actualizar estado global y recargar la vista/datos contextualmente.
