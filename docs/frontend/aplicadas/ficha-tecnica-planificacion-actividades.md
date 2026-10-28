# Ficha técnica — Pestaña Planificación (actividades)

**Endpoint:** `GET /proyectos/{idProyecto}/actividades`  
**Acceso:** `VER_PROYECTO` (miembros activos del proyecto).  
**Uso:** tabla de gestión de actividades. No calcula CPM.

---

## 1. Contrato

```http
GET /proyectos/{idProyecto}/actividades?nombre=&estado=&page=0&size=20&todos=false
```

Filtros disponibles: `nombre`, `descripcion`, `requerimiento`, `duracionMin`,
`duracionMax`, `estado`. La paginación y el orden se resuelven en servidor.

```json
{
  "contenido": [
    {
      "idActividad": 1,
      "nombre": "Bocetos y selección de telas",
      "descripcion": "Bocetos iniciales",
      "duracionMinutos": 11520,
      "estado": "PENDIENTE",
      "fechaHoraInicio": null,
      "fechaHoraFin": null,
      "predecesoras": [
        { "idActividad": 0, "nombre": "Actividad previa" }
      ],
      "responsables": [
        {
          "idPerfil": 10,
          "idUsuario": 1,
          "nombreArtistico": "Luna",
          "idProfesion": 2,
          "nombreProfesion": "Modelo"
        }
      ]
    }
  ],
  "paginaActual": 0,
  "tamanoPagina": 20,
  "totalElementos": 8,
  "totalPaginas": 1,
  "primera": true,
  "ultima": true
}
```

---

## 2. Reglas

- **Sin CPM:** no hay `critica`, `holgura`, `inicioProyectado` ni
  `finProyectado`. Las fechas son las persistidas.
- Actividades `PENDIENTE` sin fecha muestran `null`; el front las presenta
  como "-". Las proyecciones se ven en la pestaña Cronograma.
- **Responsables:** perfiles activos en `asignacion_actividad`. Si no hay
  asignaciones, `responsables: []` y el front muestra "-".
- **Canceladas:** se listan con `estado = "CANCELADA"`. No se calculan.
- **Orden:** topológico por dependencias con desempate por `idActividad`.
  El front numera las filas 1..N en el orden recibido.
- **Predecesoras:** se devuelven con `idActividad` y `nombre`. Si una
  predecesora no está en el resultado filtrado, el front puede mostrar su
  nombre en lugar del número de fila.
- La búsqueda de candidatos **no** es este endpoint; es
  `GET /proyectos/{idProyecto}/actividades/buscar`.

---

## 3. Errores

| Código | Caso |
| :--- | :--- |
| `400` | Rango de duración inválido o estado fuera del enum |
| `403` | El perfil no es miembro activo del proyecto |
| `404` | Proyecto inexistente o sin perfil activo seleccionado |

---

## 4. UI

- Director: puede crear, editar, eliminar y (a futuro) asignar.
- Miembro: solo lectura.
- Las acciones se ocultan con `rolUsuario`/`permisos` de `ProyectoResponse`.
- La columna "Responsables" muestra los `nombreArtistico`; "Vos" es una
  decisión de presentación del front comparando `idUsuario`/`idPerfil`.
