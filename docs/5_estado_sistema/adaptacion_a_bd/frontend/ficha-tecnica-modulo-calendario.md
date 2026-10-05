# Ficha Técnica: Módulo de Calendario - Frontend

## 1. Visión General
Módulo de backend para gestión de calendarios personales y bloques de tiempo. El frontend consumirá los endpoints REST para proporcionar visualización y configuración de jornadas laborales, bloqueos manuales y bloques derivados de actividades de proyectos.

## 2. Endpoints de API REST

### 2.1 Obtener Calendario del Usuario (`/calendario`)

| Método | Endpoint | Descripción | Parámetros |
|--------|----------|-------------|------------|
| GET | `/calendario` | Obtener calendario completo del usuario autenticado | `Authentication` (idUsuario del principal) |

**Respuesta CalendarioResponse:**
```json
{
  "jornada": {
    "margenActividadMinutos": 30,
    "dias": [
      {
        "diaSemana": 1,
        "horaInicioManana": "08:00:00",
        "horaFinManana": "13:00:00",
        "horaInicioTarde": null,
        "horaFinTarde": "18:00:00"
      }
    ]
  },
  "bloqueosManuales": [
    {
      "idBloqueo": 1,
      "fechaHoraInicio": "2024-01-15T09:00:00",
      "fechaHoraFin": "2024-01-15T11:00:00",
      "motivo": "Reunión con cliente"
    }
  ],
  "actividades": [
    {
      "idActividad": 5,
      "nombre": "Diseño UI",
      "fechaHoraInicio": "2024-01-15T09:00:00",
      "fechaHoraFin": "2024-01-15T11:00:00"
    }
  ]
}
```

### 2.2 Obtener Calendario por Perfil (`/calendario/perfil/{id}`)

| Método | Endpoint | Descripción | Parámetros |
|--------|----------|-------------|------------|
| GET | `/calendario/perfil/{id}` | Obtener calendario público de un usuario por ID | `id` (Long) |

- Endpoint público (sin autenticación requerida del usuario actual)
- Muestra la configuración de jornada y bloqueos visibles para el público

### 2.3 Configurar Jornada Laboral (`/calendario/jornada`)

| Método | Endpoint | Descripción | Autorización |
|--------|----------|-------------|--------------|
| PUT | `/calendario/jornada` | Configurar jornada laboral completa | Requiere auth |

**ConfigJornadaRequest:**
```json
{
  "margenActividadMinutos": 30,
  "dias": [
    {
      "diaSemana": 1,
      "horaInicioManana": "08:00:00",
      "horaFinManana": "13:00:00",
      "horaInicioTarde": null,
      "horaFinTarde": "18:00:00"
    },
    {
      "diaSemana": 2,
      "horaInicioManana": "09:00:00",
      "horaFinManana": "14:00:00",
      "horaInicioTarde": "15:00:00",
      "horaFinTarde": "19:00:00"
    }
  ]
}
```

**JornadaDiaRequest:**
- `diaSemana`: Integer (1-7, Lunes-domingo, ISO 8601)
- `horaInicioManana`: LocalTime (formato 24h)
- `horaFinManana`: LocalTime (formato 24h)
- `horaInicioTarde`: LocalTime (opcional, para jornada partida)
- `horaFinTarde`: LocalTime (formato 24h)

**ConfigJornadaResponse:**
```json
{
  "margenActividadMinutos": 30,
  "dias": [...]
}
```

### 2.4 Marcar Bloque como No Disponible (`/calendario/bloqueos`)

| Método | Endpoint | Descripción | Autorización |
|--------|----------|-------------|--------------|
| POST | `/calendario/bloqueos` | Marcar período como no disponible | Requiere auth |

**MarcarNoDisponibleRequest:**
```json
{
  "fechaHoraInicio": "2024-01-15T09:00:00",
  "fechaHoraFin": "2024-01-15T11:00:00",
  "motivo": "Capacitación interna"
}
```

**BloqueoResponse:**
```json
{
  "idBloqueo": 1,
  "fechaHoraInicio": "2024-01-15T09:00:00",
  "fechaHoraFin": "2024-01-15T11:00:00",
  "motivo": "Capacitación interna"
}
```

### 2.5 Quitar Bloqueo (`/calendario/bloqueos/{idBloqueo}`)

| Método | Endpoint | Descripción | Autorización |
|--------|----------|-------------|--------------|
| DELETE | `/calendario/bloqueos/{idBloqueo}` | Marcar bloqueo como disponible nuevamente | Requiere auth |

- `idBloqueo`: ID del bloqueo a eliminar

## 3. Modelos de Datos para Frontend

### 3.1 CalendarioResponse
- `jornada` (ConfigJornadaResponse)
- `bloqueosManuales` (List<BloqueoResponse>)
- `actividades` (List<BloqueoActividadResponse>)

### 3.2 ConfigJornadaResponse
- `margenActividadMinutos` (Integer) - margen en minutos entre actividades
- `dias` (List<JornadaDiaResponse>) - configuración por día de la semana

### 3.3 ConfigJornadaRequest
- `margenActividadMinutos` (Integer, ≥0, obligatorio)
- `dias` (List<JornadaDiaRequest>, obligatorio, no vacío)

### 3.4 JornadaDiaRequest / JornadaDiaResponse
- `diaSemana` (Integer, 1-7, ISO 8601: Lunes=1, Domingo=7)
- `horaInicioManana` (LocalTime, formato HH:mm)
- `horaFinManana` (LocalTime, formato HH:mm)
- `horaInicioTarde` (LocalTime, opcional - null para jornada corrida)
- `horaFinTarde` (LocalTime, formato HH:mm)

**Notas de la jornada:**
- **Jornada corrida**: Solo `horaInicioManana` y `horaFinTarde` son utilizados; `horaFinManana` y `horaInicioTarde` son null
- **Jornada partida**: Las cuatro horas están presentes en orden estricto

### 3.5 BloqueoResponse
- `idBloqueo` (Long)
- `fechaHoraInicio` (LocalDateTime)
- `fechaHoraFin` (LocalDateTime)
- `motivo` (String, máximo 200 caracteres)

### 3.6 BloqueoActividadResponse
- `idActividad` (Long)
- `nombre` (String) - nombre de la actividad del proyecto
- `fechaHoraInicio` (LocalDateTime)
- `fechaHoraFin` (LocalDateTime)
- *Nota*: Bloques calculados derivados del cronograma y margen de actividad; no se persisten directamente

## 4. Consideraciones de UI/UX

### 4.1 Visualización de Calendario
- **Vista mes/semana**: Similar a Google Calendar
- **Bloques de tiempo**: Representación visual con colores diferenciados
- **Jornada laboral**: Resaltar horas de inicio/fin en la vista

### 4.2 Configuración de Jornada
- **Selector de día**: Picker con días Lunes-Domingo (o numérico 1-7)
- **Horarios**: Time picker para horaInicioManana, horaFinManana, horaInicioTarde, horaFinTarde
- **Tipo de jornada**: Alternar entre " corrida " y " partida "
- **Margen actividad**: Input numérico (minutos, mínimo 0)

### 4.3 Bloqueos Manuales
- **Agregar bloqueo**: Modal con:
  - DateTime picker para fecha/hora inicio
  - DateTime picker para fecha/hora fin
  - Input de texto para motivo (máx 200 chars)
  - Botón "Marcar como no disponible"
- **Listado de bloqueos**: Tabla con:
  - Fecha y hora inicio/fin
  - Motivo
  - Botón "Disponibilizar" (eliminar bloqueo)

### 4.4 Actividades de Proyecto
- **Mostrar bloques de actividad**: Mostrados como bloques semi-transparentes
- **Nombre actividad**: Etiqueta identificadora
- **Superposición**: Los bloques de actividad consideran el margen por actividad configurado

## 5. Patrón y Componentes Recomendados

### 5.1 Librerías Sugeridas
- **Day.js** o **date-fns** para manejo de fechas/horas
- **React Big Calendar** o **FullCalendar** para vista de calendario
- **React Hook Form** + **Yup** para validación de formularios
- **Material-UI** o **Bootstrap** para inputs de time picker y date picker

### 5.2 Componentes por funcionalidad

**CalendarioPrincipal:**
- Display de la vista de calendario mensual/semanal
- Integración con eventos de bloqueos y actividades
- Consumo de `/calendario` endpoint

**ConfigJornadaForm:**
- Formulario con campos por día (Lunes-Sábado-Domingo)
- Time pickers para cada hora
- Toggle para jornada corrida/partida
- Input para margen actividad

**BloqueoModal:**
- Formulario con date-time pickers
- Validadación de que fin > inicio
- Input motivo (requerido, límite 200 chars)
- Acciones: Marcar/Quitar disponible

**ActividadesList:**
- Grid o lista de bloques de actividades
- Coloreado por tipo de actividad
- Considerar margen de configuración

## 6. Rutas y Navegación Sugerida

```
/calendario           → Vista principal del calendario del usuario usuario autenticado
/calendario/perfil/{id} → Calendario público de otro usuario (view-only)
```

## 7. Flujos de Trabajo Comunes

### Flujo 1: Configurar Jornada Laboral
1. Usuario accede a `/calendario`
2. Abre configuración de jornada
3. Selecciona tipo (corrida/partida)
4. Define horarios por día (Lunes-Domingo)
5. Establece margen por actividad (ej. 30 min)
6. Guardar configuración (PUT /calendario/jornada)

### Flujo 2: Agregar Bloqueo Personal
1. En la vista del calendario, hace clic en "Bloquear tiempo"
2. Completa modal con fecha/hora inicio y fin
3. Ingresa motivo (obligatorio)
4. Confirma acción
5. Bloque aparece en calendario y lista de bloqueos manuales

### Flujo 3: Gestionar Actividades de Proyecto
1. El sistema muestra bloques automáticos basados en cronograma de proyectos
2. Los bloques consideran el margen por actividad configurado
3. El usuario puede ver superposiciones y conflictos
4. Los bloques son de solo lectura (derivados, no editables directamente)

## 8. Manejo de Estados y Caching

Considerar:
- **React Query** para fetching de calendario (cache automático)
- Invalidar cache después de configurar jornada o agregar quitar bloqueos
- Estado local para formulario abierto/cerrado
- Persistencia en localStorage de preferencias de vista (mes vs semana)