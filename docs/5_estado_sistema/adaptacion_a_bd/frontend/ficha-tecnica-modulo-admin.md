# Ficha Técnica: Módulo de Administrador - Frontend

## 1. Visión General
Módulo de backend para gestión administrativa del sistema. El frontend necesitará consumir los endpoints REST expuestos por este módulo para proporcionar funcionalidades de administración de usuarios, perfiles, configuraciones y características técnicas.

## 2. Endpoints de API REST

### 2.1 Gestión de Usuarios (`/admin/usuarios`)

| Método | Endpoint | Descripción | Autorización |
|--------|----------|-------------|--------------|
| GET | `/admin/usuarios` | Listar todos los usuarios | `VER_USUARIOS` |
| GET | `/admin/usuarios/buscar` | Buscar usuarios con filtros y paginación | `VER_USUARIOS` |
| GET | `/admin/usuarios/{id}` | Obtener detalle de usuario | `VER_USUARIOS` |
| PATCH | `/admin/usuarios/{id}/habilitar` | Habilitar usuario | `HABILITAR_USUARIO` |
| PATCH | `/admin/usuarios/{id}/deshabilitar` | Deshabilitar usuario | `DESHABILITAR_USUARIO` |

**Respuesta GET /admin/usuarios:**
```json
[
  {
    "idUsuario": 1,
    "nombre": "Juan",
    "apellido": "Pérez",
    "correo": "juan@example.com",
    "estado": "ACTIVO",
    "rolGlobal": "ADMIN",
    "fechaNacimiento": "1990-05-15",
    "dni": "12345678A",
    "fechaSolicitudBaja": null,
    "motivoDeshabilitacion": null,
    "fechaHastaDeshabilitacion": null,
    "genero": "MASCULINO"
  }
]
```

**Filtros en `/admin/usuarios/buscar` (BuscarUsuariosAdminFiltro):**
- `nombre`, `apellido`, `correo`: filtros de texto
- `estado`: enum `EstadoUsuario`
- `idProfesion`: Long
- `nombreProfesion`: String
- `nombreArtisticoPerfil`: String

### 2.2 Perfiles de Usuario (`/admin/usuarios/{id}/perfiles`)

| Método | Endpoint | Descripción | Autorización |
|--------|----------|-------------|--------------|
| GET | `/admin/usuarios/{id}/perfiles` | Listar perfiles de un usuario | `VER_USUARIOS` |

**Respuesta:**
```json
[
  {
    "idPerfil": 1,
    "nombreArtistico": "Artista X",
    "biografia": "Músico y compositor",
    "estado": "ACTIVO",
    "profesion": "Músico",
    "fechaSolicitudBaja": null
  }
]
```

### 2.3 Configuración del Sistema (`/admin/configuracion/schedulers/deshabilitacion`)

| Método | Endpoint | Descripción | Autorización |
|--------|----------|-------------|--------------|
| GET | `/admin/configuracion/schedulers/deshabilitacion` | Obtener configuración de deshabilitación | `ADMINISTRAR_CONFIGURACION` |
| POST | `/admin/configuracion/schedulers/deshabilitacion` | Actualizar configuración de deshabilitación | `ADMINISTRAR_CONFIGURACION` |
| POST | `/admin/configuracion/schedulers/deshabilitacion/ejecutar-ahora` | Ejecutar deshabilitación manual | `ADMINISTRAR_CONFIGURACION` |

**ConfiguracionSchedulerResponse:**
- Configuración relacionada con el scheduler de deshabilitación automática de usuarios

### 2.4 Unidades de Medida (`/admin/unidades-medida`)

| Método | Endpoint | Descripción | Autorización |
|--------|----------|-------------|--------------|
| GET | `/admin/unidades-medida` | Listar unidades de medida | `VER_CARACTERISTICAS` |
| GET | `/admin/unidades-medida?tipoDato=` | Filtrar por tipo de dato | `VER_CARACTERISTICAS` |

**UnidadMedidaResponse:**
```json
{
  "idUnidad": 1,
  "nombre": "Kilogramos",
  "simbolo": "kg",
  "tipoDatoPermitido": "NUMERICO"
}
```

### 2.5 Características Técnicas (`/admin/caracteristicas-tecnicas`)

| Método | Endpoint | Descripción | Autorización |
|--------|----------|-------------|--------------|
| GET | `/admin/caracteristicas-tecnicas` | Listar todas las características técnicas | `VER_CARACTERISTICAS` |
| POST | `/admin/caracteristicas-tecnicas` | Crear nueva característica técnica | `CREAR_CARACTERISTICA` |
| PUT | `/admin/caracteristicas-tecnicas/{id}` | Actualizar característica técnica | `MODIFICAR_CARACTERISTICA` |
| DELETE | `/admin/caracteristicas-tecnicas/{id}` | Eliminar característica técnica | `ELIMINAR_CARACTERISTICA` |
| POST | `/admin/caracteristicas-tecnicas/{id}/valores` | Agregar valor a característica | `CREAR_CARACTERISTICA` |
| PUT | `/admin/caracteristicas-tecnicas/{id}/valores/{idValor}` | Actualizar valor de característica | `MODIFICAR_CARACTERISTICA` |
| DELETE | `/admin/caracteristicas-tecnicas/{id}/valores/{idValor}` | Eliminar valor de característica | `ELIMINAR_CARACTERISTICA` |

**AdminCaracteristicaTecnicaRequest:**
```json
{
  "codigo": "CARACT-001",
  "nombre": "Tipo de voz",
  "idUnidad": 1,
  "idProfesion": 5,
  "tipoDato": "TEXTO",
  "valores": [
    {
      "idValor": 1,
      "codigo": "SOPRANO",
      "colorHex": "#FF0000"
    }
  ]
}
```

**AdminValorCaracteristicaRequest:**
```json
{
  "idValor": 1,
  "codigo": "SOPRANO",
  "colorHex": "#FF0000"
}
```
- `colorHex` debe tener formato `#RRGGBB` (validación regex: `^#[0-9A-Fa-f]{6}$`)

## 3. Modelos de Datos para Frontend

### 3.1 AdminUsuarioResponse
- `idUsuario` (Long)
- `nombre` (String)
- `apellido` (String)
- `correo` (String)
- `estado` (String: ACTIVO/DESHABILITADO)
- `rolGlobal` (String)
- `fechaNacimiento` (LocalDate)
- `dni` (String)
- `fechaSolicitudBaja` (LocalDateTime, opcional)
- `motivoDeshabilitacion` (String, opcional)
- `fechaHastaDeshabilitacion` (LocalDateTime, opcional)
- `genero` (String: MASCULINO/FEMENINO/OTRO)

### 3.2 AdminPerfilResponse
- `idPerfil` (Long)
- `nombreArtistico` (String)
- `biografia` (String)
- `estado` (String)
- `profesion` (String)
- `fechaSolicitudBaja` (LocalDateTime, opcional)

### 3.3 UnidadMedidaResponse
- `idUnidad` (Long)
- `nombre` (String)
- `simbolo` (String)
- `tipoDatoPermitido` (String)

### 3.4 Característica Técnica (desde CaracteristicaTecnicaResponse de perfiles)
- `id` (Long)
- `codigo` (String)
- `nombre` (String)
- `idUnidad` (Long)
- `idProfesion` (Long)
- `tipoDato` (String)
- `valores` (lista de ValorCaracteristicaResponse)

### 3.5 Valor de Característica
- `idValor` (Long)
- `codigo` (String)
- `colorHex` (String format #RRGGBB)

## 4. Consideraciones de Autenticación y Autorización

- Todos los endpoints requieren autenticación Spring Security
- Autorizaciones basadas en authorities/roles:
  - `VER_USUARIOS`: para ver listados y detalles de usuarios
  - `HABILITAR_USUARIO`: para habilitar usuarios
  - `DESHABILITAR_USUARIO`: para deshabilitar usuarios
  - `ADMINISTRAR_CONFIGURACION`: para configuraciones del sistema
  - `VER_CARACTERISTICAS`: para ver características y unidades
  - `CREAR_CARACTERISTICA`: para crear/agregar valores
  - `MODIFICAR_CARACTERISTICA`: para actualizar
  - `ELIMINAR_CARACTERISTICA`: para eliminar

## 5. Componentes y Patrón UI Recomendado

### 5.1 Gestión de Usuarios
- **Lista de usuarios**: Tabla con paginación, filtros por nombre/email/estado
- **Formulario de usuario**: Modal para crear/editar usuario con campos: nombre, apellido, correo, fecha nacimiento, DNI, género, estado
- **Acciones por usuario**: Botones de habilitar/deshabilitar, ver detalle, gestionar perfiles

### 5.2 Perfiles de Usuario
- **Lista de perfiles**: Tabla asociada a un usuario específico
- **Formulario de perfil**: Modal con campos: nombre artístico, biografía, profesión, estado

### 5.3 Configuración
- **Configuración scheduler**: Formulario con campos de configuración de deshabilitación automática
- **Ejecutar manual**: Botón para ejecución inmediata

### 5.4 Unidades de Medida
- **Lista desplegable**: Para selección en formularios de características técnicas

### 5.5 Características Técnicas
- **Cuadro de características**: Grid con lista de características
- **Formulario de creación**: Campos para código, nombre, tipo dato, unidad, profesión, y gestión de valores
- **Gestión de valores**: Tabla inline o modal para agregar/editar/eliminar valores con selector de color (colorHex)

## 6. Gestión de Estado

Considerar usar:
- **React Query** o **TanStack Query** para fetching y caching de datos
- **Formik** o **React Hook Form** para manejo de formularios
- **Context API** o **Redux** para estado global de permisos y configuraciones

## 7. Dependencias Sugeridas

- Axios o Fetch API para consumo de endpoints
- React Router para navegación
- SweetAlert2 o similar para mensajes de confirmación en acciones críticas (deshabilitar usuario, eliminar características)
- Datepicker para campos de fecha
- Color picker para campos colorHex

## 8. Rutas Sugeridas del Frontend

```
/admin/usuarios           → Lista de usuarios con búsqueda
/admin/usuarios/{id}      → Detalle de usuario
/admin/usuarios/{id}/perfiles → Perfiles del usuario
/admin/configuracion      → Configuración del sistema
/admin/unidades-medida    → Lista de unidades (select)
/admin/caracteristicas    → Gestión de características técnicas
```