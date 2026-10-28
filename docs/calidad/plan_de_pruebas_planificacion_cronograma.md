# Plan de pruebas — Planificación y Cronograma

**Alcance:** tabla de Planificación (UC-52), cronograma (UC-33) y reconexión
de dependencias al cancelar actividades.

---

## 1. Pruebas unitarias

| Clase | Casos principales |
| :--- | :--- |
| `ListarActividadesPlanificacionServiceTest` | Responsables activos, predecesoras, orden topológico, paginación en memoria, canceladas visibles, permiso `VER_PROYECTO`, sin perfil activo. |
| `ObtenerCronogramaServiceTest` | Resumen, makespan/margen, ruta crítica, exclusión de canceladas, predecesoras canceladas filtradas, `finConHolgura`, permiso `VER_PROYECTO`, sin perfil activo. |
| `ModificarActividadServiceTest` | Al cancelar `B` en `A → B → C`, `C` hereda `A`; cancelaciones sin sucesoras/predecesoras. |
| `CronogramaServiceTest` | `estadoDe` con `viable` verdadero/falso/nulo. |
| `EliminarActividadServiceTest` | La reconexión compartida sigue funcionando en la baja física. |

## 2. Pruebas de controlador

`ActividadControllerTest`:

- `GET /proyectos/{id}/actividades` devuelve la tabla liviana con
  `responsables`;
- no expone `critica` ni `holguraMakespan`;
- `404` sin perfil activo.

`CronogramaControllerTest`:

- `GET /proyectos/{id}/cronograma` devuelve `resumen`, `rutaCritica`,
  `enRutaCritica` y `finConHolgura`;
- `404` sin perfil activo.

## 3. Casos de aceptación

- [ ] Un miembro activo puede ver la tabla de Planificación.
- [ ] Un no miembro de un proyecto privado recibe `403`.
- [ ] Una actividad sin asignaciones devuelve `responsables: []`.
- [ ] Los responsables son solo perfiles con membresía activa.
- [ ] Una actividad cancelada aparece en Planificación y no en Cronograma.
- [ ] `enRutaCritica` define el chip "Crítica" y el conteo de la tarjeta.
- [ ] `finConHolgura` dibuja la barra clara; no se usa `rangoEjecucion`.
- [ ] `margenMinutos` se muestra sin recalcular desde fechas.
- [ ] Cancelar una actividad reconecta sus sucesoras a sus predecesoras.
- [ ] El cronograma se refresca después de crear, modificar, eliminar o
      cancelar una actividad.

## 4. Integración pendiente

- Repositorio `findActivasByActividadIds` contra PostgreSQL.
- Servicio de Planificación con specification real y paginación.
- Servicio de Cronograma con CPM real y actividades fijas/canceladas.
