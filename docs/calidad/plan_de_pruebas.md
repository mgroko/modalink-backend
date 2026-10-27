# Plan de pruebas — UC-34 Gestionar postulaciones y UC-74 Buscar postulaciones

**Alcance:** aceptar/rechazar postulaciones generales y de actividad, búsqueda
de postulaciones por proyecto, cupo por miembros/asignaciones activos y ajustes
de UC-26.

---

## 1. Pruebas unitarias

| Clase | Casos principales |
| :--- | :--- |
| `CupoRequerimientoServiceTest` | Cupo parcial, completo, con exceso, sin requerimiento y actividad. |
| `CrearPostulacionServiceTest` | General en `PUBLICADO`/`CONFIRMADO`, miembro `MIEMBRO` en actividad, director bloqueado, cupo por activos, duplicados y disponibilidad. |
| `AceptarPostulacionServiceTest` | Aceptación general, postulación de otro proyecto, ya gestionada, sin cupo, postulante ya miembro y sin perfil activo. |
| `RechazarPostulacionServiceTest` | Rechazo general/actividad, ya gestionada, otro proyecto, sin perfil activo y sin permiso. |
| `BuscarPostulacionesProyectoServiceTest` | Unión de tipos, orden por estado, filtro por tipo, cupo lleno, sin perfil activo, proyecto inexistente y rango de fechas inválido. |

## 2. Pruebas de controlador

`PostulacionControllerTest`:

- búsqueda con filtros y paginación;
- aceptar general y actividad;
- rechazar general y actividad;
- `404` sin perfil activo o postulación inexistente;
- `409` postulación ya gestionada;
- `403` sin permiso;
- `400` rango de fechas inválido.

## 3. Pruebas de integración (PostgreSQL/Testcontainers)

| Clase | Casos |
| :--- | :--- |
| `CrearPostulacionIntegrationTest` | Aceptación general y de actividad, rechazo general y de actividad, auto-rechazo por cupo, disponibilidad, miembro existente, director bloqueado, altas en `CONFIRMADO` y búsqueda UC-74. |
| `GestionarPostulacionConcurrencyIntegrationTest` | Dos aceptaciones simultáneas de la misma postulación: una gana y la otra recibe conflicto; solo se crea una membresía y una notificación. |

## 4. Casos de aceptación

- [ ] Aceptar general crea membresía activa vinculada al requerimiento.
- [ ] Aceptar actividad crea asignación y notifica.
- [ ] Un director no puede postularse.
- [ ] Un miembro `MIEMBRO` puede postularse a una actividad.
- [ ] Un miembro activo no puede postularse a un general.
- [ ] El proyecto `CONFIRMADO` acepta altas si el requerimiento está insatisfecho.
- [ ] El rechazo deja la postulación en `RECHAZADA` y notifica.
- [ ] UC-74 ordena `PENDIENTE` → `ACEPTADA` → `RECHAZADA`.
- [ ] El cupo se calcula por activos y no por postulaciones aceptadas.
- [ ] Dos aceptaciones concurrentes no duplican membresías.

## 5. Comandos de ejecución

```bash
mvn -q -DskipTests clean test-compile
mvn -q -Dtest="CupoRequerimientoServiceTest,CrearPostulacionServiceTest,AceptarPostulacionServiceTest,RechazarPostulacionServiceTest,BuscarPostulacionesProyectoServiceTest,PostulacionControllerTest" test
# Casos representativos de UC-34/UC-74 y concurrencia
mvn -q -Dtest="CrearPostulacionIntegrationTest#aceptarGeneral_creaMiembroYRechazaLasDemasPendientes+aceptarActividad_creaAsignacionYNotifica+rechazarGeneral_cambiaEstadoYNotificaSinCrearMiembro+rechazarActividad_cambiaEstadoYNotificaSinCrearAsignacion+buscarPostulaciones_devuelveLasDelProyectoConCupoYFiltros+postularActividad_comoMiembroActivo_permite+postularGeneral_enProyectoConfirmado_permite,GestionarPostulacionConcurrencyIntegrationTest" test
```

> **Fallos preexistentes conocidos:** dos tests de integración de actividades/CPM
> (`CrearPostulacionIntegrationTest#actividad_sinHuecoLibre...` y
> `EliminarActividadIntegrationTest#eliminar_borraActividad...`) y algunos tests
> unitarios de actividades por `CronogramaService.calcular` devolviendo `null` en
> mocks. Se reproducen también en un worktree limpio sobre `HEAD`.
