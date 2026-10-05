# Ficha Técnica: Módulo de Perfiles - Frontend

## 1. Visión General
Módulo de gestión de perfiles de usuario en la plataforma. Proporciona funcionalidades para crear, editar, buscar y gestionar perfiles artisticos con características técnicas, profesiones y datos de ubicación. El frontend consumirá los endpoints REST para proporcionar una experiencia completa de gestión de identidad artística.

## 2. Endpoints de API REST

### 2.1 Gestión de Perfiles (`/perfiles`)

| Método | Endpoint | Descripción | Autorización |
|--------|----------|-------------|--------------|
| GET | `/perfiles/buscar` | Buscar perfiles con filtros y paginación | Pública/Usuario |
| POST | `/perfiles` | Crear nuevo perfil | `CREAR_PERFIL` |
| GET | `/usuarios/me/perfiles` | Listar perfiles del usuario autenticado | Requiere auth |
| GET | `/perfiles/{idPerfil}` | Obtener perfil completo (propio o tercero activo) | Requiere auth |
| PUT | `/perfiles/{idPerfil}` | Editar perfil existente | Requiere auth |
| DELETE | `/perfiles/{idPerfil}` | Solicitar baja del perfil (cuenta regresiva 30 días) | Requiere auth |
| POST | `/perfiles/{idPerfil}/reactivar` | Reactivar perfil dentro de los 30 días | Requiere auth |
| PATCH | `/perfiles/{idPerfil}/activar` | Cambiar perfil activo (sesión) | Requiere auth |

### 2.2 Gestión de Fotos (`/perfiles/{idPerfil}/foto`)

| Método | Endpoint | Descripción | Autorización |
|--------|----------|-------------|--------------|
| POST | `/perfiles/{idPerfil}/foto` | Subir foto de perfil | `SUBIR_FOTO` |
| DELETE | `/perfiles/{idPerfil}/foto` | Eliminar foto de perfil | `SUBIR_FOTO` |

### 2.3 Profesiones (`/profesiones`)

| Método | Endpoint | Descripción | Autorización |
|--------|----------|-------------|--------------|
| GET | `/profesiones` | Buscar profesiones (opcional por nombre) | Pública |

### 2.4 Características Técnicas (`/profesiones/{id}/caracteristicas-tecnicas`)

| Método | Endpoint | Descripción | Parámetros |
|--------|----------|-------------|------------|
| GET | `/profesiones/{id}/caracteristicas-tecnicas` | Buscar características por profesión | `idProfesion` (obligatorio), `codigo`, `unidad` (opcionales) |

## 3. Modelos de Datos para Frontend

### 3.1 PerfilResponse (Resumen)
- `idPerfil` (Long)
- `nombreArtistico` (String)
- `biografia` (String)
- `estado` (String: ACTIVO/BAJA/PENDIENTE_BAJA/DESHABILITADO)
- `profesion` (String - nombre de la profesión)
- `fechaSolicitudBaja` (LocalDateTime, opcional - nulo si activo)
- `idImagen` (Long, opcional)
- `fotoUrl` (String, URL de la foto, opcional)
- `caracteristicas` (List<CaracteristicaResponse>)

**CrearPerfilRequest:**
- `nombreArtistico` (String, 2-50 chars, obligatorio)
- `idProfesion` (Long, obligatorio)
- `biografia` (String, máx 500 chars, obligatorio)
- `caracteristicas` (List<CaracteristicaPerfilRequest>, opcional)

**EditarPerfilRequest:**
- `nombreArtistico` (String, 2-50 chars, opcional)
- `biografia` (String, máx 500 chars, opcional)
- `idImagen` (Long, opcional)
- `caracteristicas` (List<CaracteristicaPerfilRequest>, opcional)

### 3.2 PerfilDetalleResponse (Detalle completo)
- `idPerfil` (Long)
- `nombreArtistico` (String)
- `biografia` (String)
- `estado` (String)
- `idProfesion` (Long)
- `profesion` (String - objeto completa)
- `fechaSolicitudBaja` (LocalDateTime, opcional)
- `idImagen` (Long)
- `fotoUrl` (String - URL completa)
- `idUsuario` (Long - usuario dueño del perfil)
- `nombreUsuario` (String)
- `apellidoUsuario` (String)
- `genero` (String)
- `ciudad` (CiudadResponse - objeto ubicación)
- `habilidades` (List<String>)
- `caracteristicas` (List<CaracteristicaResponse>)
- `esPropietario` (boolean)

### 3.3 PerfilBusquedaResponse (Resultados de búsqueda)
Igual estructura a PerfilDetalleResponse pero para listados paginados.

### 3.4 ProfesionResponse
- `idProfesion` (Long)
- `codigo` (String - código único de profesión)
- `nombre` (String - nombre descriptivo)
- `descripcion` (String - descripción opcional)

### 3.5 CaracteristicaResponse
- `idCaracteristica` (Long)
- `codigo` (String - código identificador, ej. "VOZ", "GÉNERO")
- `valor` (String - valor actual del perfil, ej. "SOPRANO", "MASCULINO")
- `idValor` (Long - ID del valor en catálogo)
- `codigoValor` (String - código del valor, ej. "SOPR", "HOMBRE")
- `colorHex` (String - formato #RRGGBB, opcional)

### 3.6 CaracteristicaPerfilRequest
- `idCaracteristica` (Long, obligatorio - ID del catálogo)
- `valor` (String - valor a establecer en el perfil)
- `idValor` (Long, obligatorio - ID del valor catálogo)

### 3.7 BuscarPerfilesFiltro
- `nombreArtistico` (String, filtro texto)
- `nombre` (String, filtro texto - nombre real)
- `apellido` (String, filtro texto - apellido real)
- `idProfesion` (Long, filtro por profesión)
- `profesion` (String, filtro por nombre profesión)
- `idGenero` (Long, filtro por género)
- `genero` (String, filtro por nombre género)
- `idUbicacion` (Long, filtro por ubicación)
- `localidad` (String, filtro por localidad)
- `provincia` (String, filtro por provincia)
- `idsHabilidades` (List<Long>, filtro por IDs de habilidades)
- `idCaracteristica` (Long, filtro por característica catálogo)
- `valorCaracteristica` (String, filtro por valor de característica)
- `idValorCaracteristica` (Long, filtro por ID valor característica)

### 3.8 CiudadResponse (Ubicación)
- `idCiudad` (Long)
- `idExterno` (String - ID en catálogo externo)
- `fuenteApi` (String - origen del catálogo, ej. "GEOREF")
- `nombre` (String - nombre de la ciudad)
- `provincia` (ProvinciaResponse - objeto provincia)

### 3.9 Bloqueo de Estado
- Perfil tiene estados: ACTIVO, BAJA, PENDIENTE_BAJA, DESHABILITADO (por el admin)
- Cuenta regresiva de 30 días desde solicitud de baja
- Una vez vencidos los 30 días, el perfil se elimina permanentemente

## 4. Consideraciones de UI/UX

### 4.1 Formulario de Creación/Edición de Perfil
- **Campos principales:**
  - Nombre artístico (input text, 2-50 chars, requerido)
  - Profesión (select dropdown cargando de `/profesiones`)
  - Biografía (textarea, máx 500 chars, requerido)
  - Foto de perfil (input file, preview en tiempo real)
  
- **Gestión de características:**
  - Grid de características técnicas disponibles
  - Selector de valor para cada característica
  - Color picker opcional para características con colorHex
  - Botón "Añadir característica" para agregar nuevas

### 4.2 Vista de Perfil (Detail page)
- **Encabezado:**
  - Foto de perfil grande (o placeholder)
  - Nombre artístico destacada
  - Botón "Seguir"/"Mensaje" según rol
  
- **Secciones:**
  - Biografía con texto formateado
  - Profesión y género
  - Ubicación (ciudad/Provincia)
  - Habilidades tags
  - Características técnicas con valores y colores

### 4.3 Búsqueda y Filtros
- **Barra de búsqueda global:** Por nombre artístico, nombre, apellido
- **Filtros desplegables:**
  - Profesión (select con todas las opciones)
  - Género (select)
  - Ubicación (ciudad/provincia)
  - Características (multi-select con valores)
- **Paginación:** Default 20 elementos por página

### 4.4 Gestión de Baja y Reactivación
- **Solicitar baja:**
  - Confirmación modal explicando cuenta regresiva 30 días
  - Mostrar fecha de expiración
  - Estado visual: perfil BAJA con tiempo restante
  
- **Reactivar ( dentro de 30 días):**
  - Botón "Reactivar perfil" en perfil BAJA
  - Formulario rápido sin necesidad de reingresar datos principales
  - Confirmación y actualización de estado a ACTIVO

### 4.5 Cambiar Perfil Activo (Sesión)
- **Selector de perfil:** Cuando usuario tiene múltiples perfiles
- **Interfaz:** Dropdown o tarjetitas de perfiles disponibles
- **Efecto:** Cookie de sesión actualizada, vista principal actualizada

## 5. Patrón y Componentes Recomendados

### 5.1 Librerías Sugeridas
- **React Hook Form** + **Yup** para validación de formularios (validaciones ya definidas en backend)
- **Axios** para consumo de APIs (especialmente para upload de archivos multipart)
- **React Dropzone** para upload de fotos
- **Material-UI Select** o **React Select** para dropdowns complejos
- **dayjs** o **date-fns** para manejo de fechas (fechaSolicitudBaja, cuenta regresiva)
- **SweetAlert2** o **Toastify** para modals de confirmación (baja, reactivación)

### 5.2 Componentes por funcionalidad

**PerfilForm:**
- Formulario reactivo con validation schema Yup
- Campos condicionales basados en estado
- Preview de foto antes de upload
- Manejo de características técnicas con add/remove

**PerfilDetail:**
- Header con foto y nombre artístico
- Secciones expandables/collapsable
- Mostrar tiempo restante para baja reactivable

**PerfilSearch:**
- Componente de filtros compuesto
- Panel lateral o header con todos los filtros
- Resultados en grid de tarjetas

**ProfesionSelect:**
- Carga lazy desde `/profesiones`
- Estado controlado o no controlado
- Option con código y nombre

**CaracteristicaGrid:**
- Renderizado dinámico basado en catálogo
- Inputs o selects por tipo de característica
- Validación de valores obligatorios

### 5.3 Flujos de Trabajo Comunes

**Flujo 1: Crear Perfil por Primera Vez**
1. Usuario accede a crear perfil
2. Completa formulario: nombre artístico, profesión, biografía
3. Sube foto de perfil (opcional pero recomendada)
4. Configura características técnicas (si aplica)
5. Envía formulario (POST /perfiles)
6. Redirige a perfil detail o home

**Flujo 2: Editar Perfil Existente**
1. Usuario ve su perfil detail
2. Clic en "Editar" button
3. Formulario pre-cargado con datos actuales
4. Usuario modifica los campos que desee
5. Características pueden ser añadidas/eliminadas/modificadas
6. Guardar cambios (PUT /perfiles/{id})

**Flujo 3: Solicitar Baja Temporal**
1. En perfil detail, clic en "Solicitar Baja"
2. Modal de confirmación con advertencia de 30 días
3. Si confirma, estado cambia a BAJA
4. Mostrar contador regresivo de días restantes
5. Durante los 30 días, botón "Reactivar" disponible

**Flujo 4: Reactivar Perfil**
1. En perfil EN_BAJA dentro del período de 30 días
2. Clic en "Reactivar"
3. Confirmación rápida
4. Estado vuelve a ACTIVO
5. Cookie de sesión actualizada si era el perfil activo

**Flujo 5: Buscar y Descubrir Perfiles**
1. Acceder a vista de búsqueda
2. Aplicar filtros deseados (profesión, género, ubicación, características)
3. Navegar por resultados paginados
4. Clic en perfil para ver detalle

### 5.4 Manejo de Estados y Caching

- **React Query** para fetching de perfiles y profesiones
- Caché de profesiones (raro que cambie frecuentemente)
- Invalidar cache después de crear/editar/eliminar perfil
- Contador regresivo en tiempo real para baja de perfil (calcular días restantes)
- Cache de características por profesión (GET /profesiones/{id}/caracteristicas-tecnicas)

## 6. Rutas y Navegación Sugerida

```
/perfiles/buscar          → Búsqueda y listado de perfiles
/perfiles                 → Crear nuevo perfil (POST)
/usuarios/me/perfiles     → Mis perfiles (usuario autenticado)
/perfiles/{id}            → Ver detalle de perfil (propio o tercero activo)
/perfiles/{id}/editar     → Editar perfil (PUT)
/perfiles/{id}/reactivar  → Reactivar perfil BAJA (POST)
/perfiles/{id}/activar    → Cambiar perfil activo en sesión (PATCH)
/perfiles/{id}/foto       → Subir/eliminar foto (POST/DELETE)
/profesiones              → Listado de profesiones (GET con filtro nombre opcional)
/profesiones/{id}/caracteristicas-tecnicas → Características por profesión
```

## 7. Dependencias Críticas del Backend para Frontend

1. **Validaciones backend:** Los DTOs tienen anotaciones validation (`@NotBlank`, `@Size`, `@NotNull`) que el frontend debe respetar o duplicar en schemas Yup/react-hook-form

2. **Formato fechas:** `LocalDateTime` viaja en formato ISO 8601 (`2024-01-15T14:30:00`) - usar librerías de formateo

3. **URL de fotos:** `fotoUrl` es cadena completa - el backend maneja el storage/cloud, frontend solo muestra

4. **Upload multipart:** Endpoint `/perfiles/{id}/foto` usa `enctype="multipart/form-data"` - requiere FormData en frontend

5. **Cookies JWT:** El endpoint `/perfiles/{idPerfil}/activar` retorna cookie de sesión que debe guardarse en el frontend

6. **Relaciones Anidadas:** Perfil tiene usuario, ciudad, profesión, características - el frontend debe navegar estas relaciones anidadas correctamente