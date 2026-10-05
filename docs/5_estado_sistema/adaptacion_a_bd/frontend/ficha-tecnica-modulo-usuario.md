# Ficha Técnica: Módulo de Usuario - Frontend

## 1. Visión General
Módulo de gestión de datos personales y estado de cuenta de usuario. Proporciona funcionalidades para actualizar datos personales, solicitar baja temporal de cuenta y reactivar cuenta. El frontend consumirá los endpoints REST para proporcionar gestión del perfil de usuario y control de estado de cuenta.

## 2. Endpoints de API REST

### 2.1 Datos Personales (`/usuario/datos-personales`)

| Método | Endpoint | Descripción | Autorización |
|--------|----------|-------------|--------------|
| PUT | `/usuario/datos-personales` | Actualizar datos personales del usuario | Requiere auth |

**DatosPersonalesRequest:**
```json
{
  "nombre": "Juan",
  "apellido": "Pérez",
  "fechaNacimiento": "1990-05-15",
  "genero": "MASCULINO",
  "localidadId": 5
}
```

**Javat constraints:**
- `nombre`: `@NotBlank @Size(min = 2, max = 50)` - obligatorio
- `apellido`: `@NotBlank @Size(min = 2, max = 50)` - obligatorio
- `fechaNacimiento`: `@NotNull @Past` - fecha pasada, obligatoria
- `genero`: `@NotBlank` - código de género, obligatorio
- `localidadId`: String - ID localidad catálogo Georef; null/vacio = sin ubicación

**DatosPersonalesResponse:**
```json
{
  "idUsuario": 1,
  "nombre": "Juan",
  "apellido": "Pérez",
  "fechaNacimiento": "1990-05-15",
  "genero": "MASCULINO",
  "ubicacion": {
    "idUbicacion": 10,
    "direccion": "Calle Falsa 123",
    "codigoPostal": "28001",
    "latitud": 40.4168,
    "longitud": -3.7038,
    "ciudad": {
      "idCiudad": 1,
      "idExterno": "GEOREF-123",
      "fuenteApi": "GEOREF",
      "nombre": "Madrid",
      "provincia": {...}
    }
  }
}
```

### 2. Solicitar Baja de Cuenta (`/usuario/solicitar-baja`)

| Método | Endpoint | Descripción | Autorización |
|--------|----------|-------------|--------------|
| POST | `/usuario/solicitar-baja` | Solicitar baja temporal de cuenta | Requiere auth |

**SolicitudBajaResponse:**
```json
{
  "mensaje": "Se ha solicitado la baja de su cuenta. Tiene 30 días para reactivarla.",
  "fechaLimite": "2024-01-20T10:30:00"
}
```

- **Proceso:** Usuario solicita baja → cuenta pasa a estado PENDIENTE_BAJA → cuenta regresiva de 30 días
- **fechaLimite:** Fecha exacta cuando la cuenta se eliminará permanentemente si no se reactiva
- **Mensaje:** Informa al usuario sobre el período de 30 días

### 3. Reactivar Cuenta (`/usuario/reactivar-cuenta`)

| Método | Endpoint | Descripción | Autorización |
|--------|----------|-------------|--------------|
| POST | `/usuario/reactivar-cuenta` | Reactivar cuenta dada de baja | Requiere auth |

**ReactivarCuentaResponse:**
```json
{
  "mensaje": "Su cuenta ha sido reactivada exitosamente."
}
```

- **Proceso:** Dentro de los 30 días de solicitar baja → cuenta vuelve a estado ACTIVO
- **Limitación:** Después de 30 días, la cuenta se elimina permanentemente y no se puede reactivar

### 2.4 Inicio de sesión con cuenta pendiente de baja (`POST /auth/login`)

| Método | Endpoint | Descripción | Autorización |
|--------|----------|-------------|--------------|
| POST | `/auth/login` | Login; devuelve **403** si la cuenta está PENDIENTE_BAJA | Público |

Cuando las credenciales son válidas pero la cuenta está en estado `PENDIENTE_BAJA`, **no se emite sesión** (no hay header `Set-Cookie`) y se responde:

| Código | Causa |
|--------|-------|
| `403` | Dentro del plazo: se ofrece la reactivación (payload abajo) |
| `409` | Plazo vencido: `"El plazo de 30 días para recuperar la cuenta ha expirado."` |
| `401` | Credenciales inválidas |
| `403` | Usuario `DESHABILITADO` |

**Respuesta 403:**
```json
{
  "message": "Solicitaste la baja de tu cuenta. Reactívala antes del 20/01/2024 10:30 para poder iniciar sesión.",
  "codigo": "CUENTA_PENDIENTE_BAJA",
  "fechaSolicitudBaja": "2023-12-21T10:30:00",
  "fechaLimite": "2024-01-20T10:30:00",
  "diasRestantes": 25,
  "httpStatus": 403,
  "timestamp": 1703500000000
}
```

- **Front:** ante `403` + `codigo: "CUENTA_PENDIENTE_BAJA"` mostrar el modal [Cancelar / Reactivar cuenta] con el contador basado en `diasRestantes` (o `fechaLimite`).
- **Requiere contraseña correcta:** el estado del cuenta no se revela a credenciales inválidas (se responde `401` genérico).

### 2.5 Reactivar cuenta desde el login (`POST /auth/reactivar-cuenta`)

| Método | Endpoint | Descripción | Autorización |
|--------|----------|-------------|--------------|
| POST | `/auth/reactivar-cuenta` | Reactiva la cuenta **y** abre sesión en el mismo paso | Público (body: credenciales) |

**Request:** mismo body que `/auth/login` (`correo`, `password`).

**Respuesta `200`:** igual a un login normal (`AuthResponse` + header `Set-Cookie` con el JWT), por lo que el front queda dentro de la aplicación sin necesitar un paso extra.

| Código | Causa |
|--------|-------|
| `200` | Cuenta reactivada y sesión emitida |
| `401` | Credenciales inválidas |
| `409` | No hay solicitud activa o el plazo ya venció |

> `POST /usuario/reactivar-cuenta` (§2.3, requiere auth) **sigue existiendo** para reactivar desde una sesión ya iniciada; `/auth/reactivar-cuenta` es el camino a usar cuando el login fue bloqueado (no hay sesión).

## 3. Modelos de Datos para Frontend

### 3.1 DatosPersonalesRequest
- `nombre` (String, 2-50 chars, obligatorio)
- `apellido` (String, 2-50 chars, obligatorio)
- `fechaNacimiento` (LocalDate, fecha pasada ≥ hoy, obligatoria)
- `genero` (String, código obligatorio, ej: "MUJER","HOMBRE", "NO_BINARIO", "NO_DECIRLO")
- `localidadId` (String, opcional - ID catálogo Georef; null/vacio = sin ubicación)

### 3.2 DatosPersonalesResponse
- `idUsuario` (Long)
- `nombre` (String)
- `apellido` (String)
- `fechaNacimiento` (LocalDate - formato dd-MM-aaaa)
- `genero` (String - código)
- `ubicacion` (UbicacionResponse, opcional - null si no tiene ubicación)

### 3.3 UbicacionResponse (anidada en DatosPersonalesResponse)
- `idUbicacion` (Long)
- `direccion` (String - dirección en texto libre)
- `codigoPostal` (String - código postal)
- `latitud` (BigDecimal - coordenadas geográficas)
- `longitud` (BigDecimal - coordenadas geográficas)
- `ciudad` (CiudadResponse - objeto anidado)

### 3.4 CiudadResponse (anidada en UbicacionResponse)
- `idCiudad` (Long)
- `idExterno` (String - ID en catálogo origen, ej. "GEOREF-123")
- `fuenteApi` (String - origen catálogo, ej. "GEOREF")
- `nombre` (String - nombre ciudad)
- `provincia` (provincia anidada - estructura completa)

### 3.5 SolicitudBajaResponse
- `mensaje` (String - mensaje explicativo al usuario)
- `fechaLimite` (LocalDateTime - cuándo se elimina permanentemente)

### 3.6 ReactivarCuentaResponse
- `mensaje` (String - confirmación de reactivación)

## 4. Consideraciones de UI/UX

### 4.1 Formulario de Datos Personales
- **Campos del formulario:**
  - Nombre (input text, 2-50 chars, requerido, validación backend)
  - Apellido (input text, 2-50 chars, requerido, validación backend)
  - Fecha de nacimiento (date picker, fecha debe ser pasada)
  - Género (select dropdown con códigos (para el display utilizar el nombre asociado a los códigos): "MUJER","HOMBRE", "NO_BINARIO", "NO_DECIRLO")
  - Localidad/Ubicación (input opcional o search picker catálogo Georef)

- **Validaciones frontend (acorde a backend):**
  - Nombre y apellido mín. 2 máx. 50 caracteres
  - Fecha nacimiento: no puede ser futura
  - Género: campo requerido con opciones definidas

- **Ubicación:**
  - Si el usuario ya tiene ubicación, mostrar datos completos (ciudad, dirección, CP)
  - Input para seleccionar/changear localidad del catálogo Georef
  - Opción "Sin ubicación" (dejar campo vacío/null)

### 4.2 Flujo de Solicitud de Baja
1. Usuario accede a configuración de cuenta
2. Clic en "Solicitar Baja" o "Cerrar Cuenta"
3. Modal de confirmación explicando:
   - La cuenta pasará a estado PENDIENTE_BAJA
   - Período de 30 días para reactivar
   - Después de 30 días, eliminación permanente
4. Confirmar acción
5. Mostrar respuesta: mensaje + fecha límite
6. Actualizar UI: botón de "Reactivar" aparece en perfil

### 4.3 Flujo de Reactivación

**A) Desde el login (sin sesión) — camino principal:**
1. Usuario ingresa correo y contraseña y confirma el login
2. Backend responde `403` con `codigo: "CUENTA_PENDIENTE_BAJA"` (no se emite cookie)
3. Modal: "Solicitaste la baja de tu cuenta…" con contador de días (`diasRestantes`) y botones [Cancelar / Reactivar cuenta]
4. **Reactivar cuenta:** `POST /auth/reactivar-cuenta` con las mismas credenciales → `200` + `Set-Cookie` + `AuthResponse` → entrar directo a la app
5. **Cancelar:** cerrar el modal y volver al formulario de login (no existe sesión que limpiar)
6. Si el backend responde `409`, el plazo venció: mostrar el mensaje y redirigir al login (la cuenta pasará a `BAJA` con el scheduler)

**B) Desde una sesión iniciada:**
1. Usuario con cuenta PENDIENTE_BAJA (dentro de los 30 días) con sesión vigente (la sesión abierta antes de solicitar la baja **no** se cierra)
2. Accede a perfil o configuración
3. Clic en "Reactivar Cuenta" → `POST /usuario/reactivar-cuenta`
4. Estado del usuario y de sus perfiles cambia a `ACTIVO`
5. Refrescar los datos del usuario con `GET /auth/me` (la respuesta de reactivación solo trae `mensaje`; la cookie no se modifica)

### 4.4 Visualización de Estado de Cuenta
- **Perfil de usuario:** Mostrar tiempo restante para reactivación
- **Formato:** "Cuenta en baja hasta el 20 de enero de 2024" o "30 días restantes"
- **Clases CSS/estilos:** Diferente color según proximidad al límite (verde > 15 días, amarillo 8-15 días, rojo < 8 días)

## 5. Patrón y Componentes Recomendados

### 5.1 Librerías Sugeridas
- **React Hook Form** + **Yup** para validación de formulario datos personales
- **Axios** para consumo de APIs
- **Day.js** o **date-fns** para manejo de fechas (validar fecha pasada, calcular días restantes)
- **React Select** o **Material-UI Select** para dropdowns de género y búsqueda de localidades
- **SweetAlert2** para modals de confirmación (baja, reactivación)
- **Toastify** o **notifications** para mensajes temporales

### 5.2 Componentes por funcionalidad

**DatosPersonalesForm:**
- Formulario reactivo con schema Yup validación
- Date picker para fecha nacimiento
- Select género con opciones codificadas
- Input/search localidad catálogo Georef
- Preview de ubicación actual si existe
- Botón guardar que hace PUT /usuario/datos-personales

**BajaCuentaModal:**
- Confirmación con detalles del proceso de 30 días
- Mostrar mensaje y fechaLimite de SolicitudBajaResponse
- Botón "Solicitar Baja" principal
- Estilo de advertencia/alert

**ReactivarCuentaModal:**
- Disponible solo cuando cuenta está PENDIENTE_BAJA y dentro del período
- Confirmación rápida "¿Desea reactivar su cuenta?"
- POST /usuario/reactivar-cuenta al confirmar
- Éxito: mensaje confirmación + redirección/actualización estado

**CuentaEstadoBanner:**
- Componente que muestra cuándo cuenta está PENDIENTE_BAJA
- Contador regresivo de días restantes
- Formato: "30 días para reactivar" o "X días restantes"
- Acceso rápido a botón reactivar

### 5.3 Integración con Módulo Perfiles
- Los datos personales se reflejan en el perfil artístico
- `idUsuario` en DatosPersonalesResponse corresponde a `idUsuario` en PerfilResponse
- Ubicación geográfica puede mostrarse en perfil público
- Género puede influir en ciertas características o búsquedas

### 5.4 Manejo de Estados y Caching

- **React Query** para fetching de datos personales
- Cache de localidad/catálogo Georef (puede ser estático)
- Calculadora en tiempo real de días restantes para baja:
  - Al hacer login bloqueado el backend ya devuelve `diasRestantes` (§2.4)
  - Si se tiene `fechaLimite`: `diasRestantes = fechaLimite - fechaActual`
  - Actualizar display cada 24h o cada hora
- Invalidar cache después de actualizar datos personales
- Estado local para modals abiertos/cerrados

## 6. Rutas y Navegación Sugerida

```
/usuario/datos-personales → Formulario actualizar datos personales (PUT)
/usuario/solicitar-baja → Solicitar baja temporal (POST)
/usuario/reactivar-cuenta → Reactivar cuenta dada de baja (POST)
```

## 7. Consideraciones Importantes del Backend

1. **Validaciones stricto sensu:** Las anotaciones `@NotBlank`, `@Size`, `@NotNull`, `@Past` en backend deben ser respetadas en validación frontend (Yup schema debe ser compatible)

2. **Formato fechas:**
   - `fechaNacimiento`: `LocalDate` → formato `yyyy-MM-dd` en input date
   - `fechaLimite`: `LocalDateTime` → formato `yyyy-MM-ddTHH:mm:ss` para display

3. **BigDecimal para coordenadas:** latitud/longitud vienen como BigDecimal - usar librería compatible en frontend (dayjs plugins, decimal.js)

4. **Catálogo Georef localidadId:** Es un String que representa ID externo en catálogo origen (puede ser null/vacio). El frontend debe manejar caso "sin ubicación" (campo vacío).

5. **Autenticación Spring Security:** Todos los endpoints requieren `Authentication` con `getPrincipal()` que contiene el idUsuario como String

6. **Respuestas estandarizadas:** `SolicitudBajaResponse` y `ReactivarCuentaResponse` solo contienen `mensaje` + `fechaLimite` (o solo mensaje) - frontend debe mostrar estos mensajes tal cual del backend

## 8. Flujos de Trabajo Comunes

### Flujo 1: Actualizar Datos Personales
1. Usuario accede a configuración → "Editar datos personales"
2. Formulario pre-cargado con datos actuales
3. Usuario modifica los campos que desee (nombre, apellido, fecha nacimiento, género)
4. Si tiene ubicación actual, se muestra y puede ser cambiada
5. Clic en "Guardar" (hace PUT /usuario/datos-personales)
6. Éxito: mensaje toast "Datos actualizados correctamente"
7. UI actualizada con nuevos datos

### Flujo 2: Solicitar Baja Temporal
1. En configuración de cuenta o perfil, clic en "Solicitar Baja"
2. Modal de confirmación con explicación del proceso
3. Usuario confirma solicitud
4. Response: mensaje + fechaLimite mostrados en pantalla
5. Estado cuenta cambia a PENDIENTE_BAJA visualmente
6. Mostrar contador regresivo: "30 días para reactivar"
7. Durante los 30 días, opción "Reactivar cuenta" está disponible

### Flujo 3: Reactivar Cuenta (dentro de 30 días)

**3a. El intento de login fue bloqueado (sin sesión):**
1. Usuario intenta iniciar sesión → `403` con `codigo: "CUENTA_PENDIENTE_BAJA"` y `diasRestantes`
2. Modal "Solicitaste la baja de tu cuenta…" con [Cancelar / Reactivar cuenta]
3. Reactivar: `POST /auth/reactivar-cuenta` con las mismas credenciales → `200` + cookie + `AuthResponse` → entrar a la app
4. Cancelar: volver al login (no hay sesión que limpiar)
5. `409` → plazo vencido: mostrar mensaje y quedarse en el login

**3b. Sesión iniciada (banner en perfil/configuración):**
1. Usuario ve contador regresivo o banner de "cuenta en baja"
2. Clic en "Reactivar Cuenta" → confirmación rápida
3. `POST /usuario/reactivar-cuenta` → `200` con `mensaje`
4. Refrescar datos con `GET /auth/me` (el estado pasa a `ACTIVO`; la cookie no cambia)
5. Banner/contador desaparece; acceso completo de nuevo

### Flujo 4: Gestionar Ubicación Geográfica
1. Usuario ve su ubicación actual en el formulario de datos personales
2. Clic en "Cambiar ubicación" o lápiz edit
3. Search picker o input para nueva localidad ID Georef
4. Seleccionar nueva localidad del catálogo
5. Guardar cambios → actualiza UbicacionResponse con nueva ciudad/coordenadas
6. Si deja vacío → ubicacion becomes null, se muestra "Sin ubicación" en UI

---
*Ficha técnica generada basada en el análisis de endpoints y DTOs del módulo usuario en C:\Users\HP\Desktop\modalink-backend\src\main\java\org\mgroko\backend\usuario*