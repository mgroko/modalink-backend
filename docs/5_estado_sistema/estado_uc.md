# Estado de casos de uso — ModaLink

Seguimiento del avance de implementación por caso de uso (UC), en base al documento
de requerimientos (Roko María Guillermina, Iteración 01). Agrupados según los
diagramas de casos de uso del propio documento (Figuras 2 a 7).

**Estados posibles:**

- `No iniciado` — sin código asociado.
- `En progreso` — implementación parcial o en curso.
- `Implementado` — funcionalidad completa, sin cobertura de tests.
- `Implementado y testeado` — funcionalidad completa con tests unitarios y/o de integración.

Actualizar esta tabla en el mismo commit/PR que cierra o avanza un UC. Referenciar el
número de UC en el mensaje de commit (ej. `feat(UC-03): registro de usuario`) para que
el historial de git sirva de bitácora adicional.

---

## Gestión de usuarios y administración (15 UC)

| UC    | Nombre                                           | Estado                  | Endpoint / clase                                                                                                                                 | Tests | Notas                                                                                                  |
| ----- | ------------------------------------------------ | ----------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------ | ----- | ------------------------------------------------------------------------------------------------------ |
| UC-01 | Iniciar sesión                                   | Implementado y testeado | `POST /auth/login`                                                                                                                               | Sí    | Primera iteracion. Cuentas `PENDIENTE_BAJA` reciben 403 sin cookie (modal de reactivación, ficha módulo usuario §2.4) |
| UC-02 | Cerrar sesión                                    | Implementado y testeado | `POST /auth/logout`                                                                                                                              | Sí    | Primera iteracion                                                                                      |
| UC-03 | Registrarse                                      | Implementado y testeado | `POST /auth/registro`                                                                                                                            | Sí    | Primera iteracion                                                                                      |
| UC-04 | Deshabilitar usuario                             | Implementado y testeado | `/admin/usuarios/{id}/deshabilitar`                                                                                                              | Sí    | Primera iteracion. Motivo obligatorio + duración opcional (días) con reactivación automática al vencer |
| UC-05 | Habilitar usuario                                | Implementado y testeado | `/admin/usuarios/{id}/habilitar`                                                                                                                 | Sí    | Primera iteracion                                                                                      |
| UC-06 | Buscar usuario                                   | Implementado y testeado | `GET /admin/usuarios`                                                                                                                            | Sí    | Primera iteracion                                                                                      |
| UC-07 | Solicitar baja en el sistema                     | Implementado y testeado | `POST /usuario/solicitar-baja`, `POST /usuario/reactivar-cuenta` y `POST /auth/reactivar-cuenta`                                                 | Sí    | Primera iteracion. Expiración diaria por scheduler + configuración (`/admin/configuracion/schedulers/baja`) |
| UC-08 | Modificar datos personales                       | Implementado y testeado | `PUT /usuario/datos-personales`                                                                                                                  | Sí    | Primera iteracion                                                                                      |
| UC-09 | Autenticar mediante Google OAuth                 | No iniciado             |                                                                                                                                                  |       | Primera iteracion                                                                                      |
| UC-56 | Gestionar habilidades del sistema                | No iniciado             |                                                                                                                                                  |       | Segunda iteracion                                                                                      |
| UC-57 | Gestionar características técnicas por profesión | Implementado y testeado           | `GET/POST /admin/caracteristicas-tecnicas`, `PUT/DELETE /admin/caracteristicas-tecnicas/{id}`, `POST /admin/caracteristicas-tecnicas/{id}/valores` | Sí    | Segunda iteracion                                                                                      |
| UC-58 | Buscar características técnicas                  | Implementado y testeado | `GET /profesiones/{id}/caracteristicas-tecnicas`                                                                                                 | Sí    | Primera iteracion                                                                                      |
| UC-59 | Buscar profesiones                               | Implementado y testeado | `GET /profesiones`                                                                                                                               | Sí    | Primera iteracion                                                                                      |
| UC-67 | Generar informe de auditoría                     | No iniciado             |                                                                                                                                                  |       | Segunda iteracion                                                                                      |
| UC-74 | Gestionar unidades de medida                     | Implementado y testeado | `GET/POST /admin/unidades-medida`, `GET/PUT/DELETE /admin/unidades-medida/{id}`                                                                   | Sí    | Segunda iteracion. Borrado físico (solo si no está asociada a una característica técnica); `tipoDatoPermitido` NUMERICO o TEXTO. **Pendiente:** notificación a los perfiles afectados al modificar la unidad (postcondición 2.1) |

## Gestión de perfiles y disponibilidad (13 UC)

| UC    | Nombre                                  | Estado                  | Endpoint / clase                                                      | Tests             | Notas                                                                                                                                                          |
| ----- | --------------------------------------- | ----------------------- | --------------------------------------------------------------------- | ----------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| UC-10 | Crear perfil                            | Implementado y testeado | `POST /perfiles`                                                      | Sí                | Primera iteracion                                                                                                                                              |
| UC-11 | Editar perfil                           | Implementado y testeado            | `GET /perfiles/{idPerfil}` y `PUT /perfiles/{idPerfil}`               | Sí (servicio/DTO) | Primera iteracion                                                                                                                                              |
| UC-12 | Eliminar perfil                         | Implementado y testeado             | `DELETE /perfiles/{idPerfil}` y `POST /perfiles/{idPerfil}/reactivar` | Sí (servicio/DTO) | Primera iteracion                                                                                                                                              |
| UC-13 | Cambiar perfil activo                   | Implementado y testeado                | `PATCH /perfiles/{idPerfil}/activar`                                  |                   | Primera iteracion                                                                                                                                              |
| UC-14 | Ver perfil                              | Implementado y testeado | `GET /perfiles/{idPerfil}` (perfil público completo o propio) y `GET /usuarios/me/perfiles` | Sí                | Primera iteracion                                                                                                                                              |
| UC-15 | Reportar perfil                         | No iniciado             |                                                                       |                   | Segunda iteracion                                                                                                                                              |
| UC-16 | Buscar perfil                           | Implementado y testeado | `GET /perfiles/buscar`                                                | Sí                | Primera iteracion                                                                                                                                              |
| UC-17 | Asignar en calendario día disponible    | Implementado y testeado | `DELETE /calendario/bloqueos/{idBloqueo}`                             | Sí                | Módulo calendario: `GET /calendario` (agenda completa) y `PUT /calendario/jornada` (jornada laboral de corrido o partida —bloques de mañana y tarde— + margen) |
| UC-18 | Asignar en calendario día no disponible | Implementado y testeado | `POST /calendario/bloqueos`                                           | Sí                | Módulo calendario; bloqueos manuales sin solapamiento                                                                                                          |
| UC-71 | Configurar jornada laboral              | Implementado y testeado | `PUT /calendario/jornada` y `GET /calendario`                          | Sí                | Segunda iteracion. Jornada corrida o partida (bloques mañana/tarde) + margen de actividad                                                                      |
| UC-23 | Configurar términos y condiciones       | No iniciado             |                                                                       |                   | Segunda iteracion                                                                                                                                              |
| UC-47 | Gestionar habilidades                   | No iniciado             |                                                                       |                   | Segunda iteracion                                                                                                                                              |
| UC-48 | Buscar habilidades                      | No iniciado             |                                                                       |                   | Segunda iteracion                                                                                                                                              |

## Gestión de publicaciones (11 UC)

| UC    | Nombre                             | Estado      | Endpoint / clase | Tests | Notas             |
| ----- | ---------------------------------- | ----------- | ---------------- | ----- | ----------------- |
| UC-19 | Alta publicación                   | No iniciado |                  |       | Segunda iteracion |
| UC-20 | Baja publicación                   | No iniciado |                  |       | Segunda iteracion |
| UC-21 | Modificar publicación              | No iniciado |                  |       | Segunda iteracion |
| UC-22 | Buscar publicación                 | No iniciado |                  |       | Segunda iteracion |
| UC-61 | Enviar solicitud de colaboración   | No iniciado |                  |       | Segunda iteracion |
| UC-62 | Aceptar solicitud de colaboración  | No iniciado |                  |       | Segunda iteracion |
| UC-63 | Rechazar solicitud de colaboración | No iniciado |                  |       | Segunda iteracion |
| UC-64 | Eliminar solicitud de colaboración | No iniciado |                  |       | Segunda iteracion |
| UC-65 | Buscar solicitudes de colaboración | No iniciado |                  |       | Segunda iteracion |
| UC-66 | Interactuar con publicación        | No iniciado |                  |       | Segunda iteracion |
| UC-69 | Reportar publicación               | No iniciado |                  |       | Segunda iteracion |

## Gestión de proyectos (25 UC)

| UC    | Nombre                               | Estado      | Endpoint / clase | Tests | Notas             |
| ----- | ------------------------------------ | ----------- | ---------------- | ----- | ----------------- |
| UC-24 | Crear proyecto                       | Implementado y testeado | `POST /proyectos` | Sí    | Contrato completo: proyecto en Borrador, director asignado, planificación inicializada, requerimientosGral (cantidad/profesión/características/habilidades) y moodboard opcionales |
| UC-25 | Publicar proyecto                    | Implementado y testeado | `POST /proyectos/{idProyecto}/publicar` | Sí    | Transición Borrador → Publicado; exige permiso `PUBLICAR_PROYECTO` (rol Director) e información básica completa (ubicación, fecha de entrega, moodboard, ≥1 requerimiento de personal). 400 si falta información, 409 si el estado no es Borrador. Visibilidad según privacidad (Público visible para no miembros) y auditoría vía trigger `trg_auditar_cambio_proyecto` |
| UC-26 | Dar de alta postulación a proyecto   | No iniciado |                  |       | Primera iteracion |
| UC-27 | Dar de baja postulación a proyecto   | No iniciado |                  |       | Primera iteracion |
| UC-28 | Aceptar solicitud de incorporación   | No iniciado |                  |       | Primera iteracion |
| UC-29 | Buscar proyecto                      | Implementado y testeado | `GET /proyectos/buscar` | Sí    | Criterios opcionales (nombre, estado, director); browse-able sin parámetros como "Buscar perfil". Requiere perfil activo (404 si no). Visibilidad: miembro activo ve cualquier proyecto; no miembro solo ve no-borradores Públicos (Borrador y Privado solo miembros, interino hasta contactos UC-43..46) |
| UC-29 bis | Buscar proyectos propios          | Implementado y testeado | `GET /proyectos/propios` | Sí    | Catálogo del perfil activo (membresía activa como director o miembro); filtros opcionales nombre/estado/rol, rol devuelto por proyecto y próxima actividad futura sin N+1; 404 si no hay perfil activo |
| UC-30 | Buscar invitaciones                  | No iniciado |                  |       | Primera iteracion |
| UC-31 | Invitar a proyecto                   | No iniciado |                  |       | Primera iteracion |
| UC-32 | Eliminar invitación a proyecto       | No iniciado |                  |       | Primera iteracion |
| UC-33 | Visualizar cronograma de proyecto    | No iniciado |                  |       | Primera iteracion |
| UC-34 | Gestionar postulaciones              | No iniciado |                  |       | Primera iteracion |
| UC-35 | Cancelar proyecto                    | No iniciado |                  |       | Primera iteracion |
| UC-36 | Modificar proyecto                   | Implementado y testeado | `PUT /proyectos/{idProyecto}` | Sí    | Estados Borrador/Publicado/Confirmado; permiso `MODIFICAR_PROYECTO`; nombre inmutable; requerimientos con postulación ACEPTADA no modificables (se conservan si se omiten); reemplazo de objetivos y requerimientos no satisfechos; en Publicado/Confirmado exige ubicación y toda la información básica; `satisfecho` expuesto en `requerimientosGral` |
| UC-37 | Finalizar proyecto                   | No iniciado |                  |       | Primera iteracion |
| UC-38 | Eliminar integrante                  | No iniciado |                  |       | Primera iteracion |
| UC-40 | Darse de baja de proyecto            | No iniciado |                  |       | Primera iteracion |
| UC-41 | Enviar mensaje a chat grupal         | No iniciado |                  |       | Segunda iteracion |
| UC-49 | Crear actividad en planificación     | No iniciado |                  |       | Primera iteracion |
| UC-50 | Modificar actividad de planificación | No iniciado |                  |       | Primera iteracion |
| UC-51 | Eliminar actividad de planificación  | No iniciado |                  |       | Primera iteracion |
| UC-52 | Buscar actividades                   | No iniciado |                  |       | Primera iteracion |
| UC-60 | Confirmar proyecto                   | No iniciado |                  |       | Primera iteracion |
| UC-70 | Asignar actividad a miembro          | No iniciado |                  |       | Segunda iteracion |
| UC-72 | Buscar requerimiento general         | No iniciado |                  |       | Segunda iteracion |
| UC-73 | Buscar requerimiento actividad       | No iniciado |                  |       | Segunda iteracion |

## Gestión de contratos (5 UC)

| UC    | Nombre             | Estado      | Endpoint / clase | Tests | Notas             |
| ----- | ------------------ | ----------- | ---------------- | ----- | ----------------- |
| UC-39 | Generar contrato   | No iniciado |                  |       | Segunda iteracion |
| UC-53 | Modificar contrato | No iniciado |                  |       | Segunda iteracion |
| UC-54 | Eliminar contrato  | No iniciado |                  |       | Segunda iteracion |
| UC-55 | Firmar contrato    | No iniciado |                  |       | Segunda iteracion |
| UC-68 | Buscar contrato    | No iniciado |                  |       | Segunda iteracion |

## Gestión de conexiones y mensajería (5 UC)

| UC    | Nombre                            | Estado      | Endpoint / clase | Tests | Notas             |
| ----- | --------------------------------- | ----------- | ---------------- | ----- | ----------------- |
| UC-42 | Enviar mensaje privado            | No iniciado |                  |       | Segunda iteracion |
| UC-43 | Enviar solicitud de conexión      | No iniciado |                  |       | Segunda iteracion |
| UC-44 | Eliminar conexión                 | No iniciado |                  |       | Segunda iteracion |
| UC-45 | Gestionar solicitudes de conexión | No iniciado |                  |       | Segunda iteracion |
| UC-46 | Buscar solicitudes de conexión    | No iniciado |                  |       | Segunda iteracion |

---

## Resumen

| Módulo                             | Total UC | Implementado y testeado | Implementado | En progreso | No iniciado |
| ---------------------------------- | -------- | ----------------------- | ------------ | ----------- | ----------- |
| Gestión de usuarios y administración | 15     | 12                      | 0            | 0           | 3           |
| Gestión de perfiles y disponibilidad | 13     | 9                       | 0            | 0           | 4           |
| Gestión de publicaciones           | 11       | 0                       | 0            | 0           | 11          |
| Gestión de proyectos               | 25       | 4                       | 0            | 0           | 21          |
| Gestión de contratos               | 5        | 0                       | 0            | 0           | 5           |
| Gestión de conexiones y mensajería | 5        | 0                       | 0            | 0           | 5           |
| **Total**                          | **74**   | **25**                  | **0**        | **0**       | **49**      |

> **Nota sobre el total:** el catálogo formal de requisitos ([`casos_de_uso.md`](../1_requisitos/casos_de_uso.md))
> comprende **74 casos de uso** (`UC-01` a `UC-74`). Con el avance del desarrollo se incorporaron
> `UC-70` *Asignar actividad a miembro*, `UC-71` *Configurar jornada laboral*, `UC-72` *Buscar requerimiento general*,
> `UC-73` *Buscar requerimiento actividad* y `UC-74` *Gestionar unidades de medida*, pasando de 69 a 74.
>
> El escenario **`UC-29 bis` *Buscar proyectos propios*** es un caso derivado del desarrollo (ficha técnica propia)
> que no forma parte del catálogo numerado; se listan sus filas en el módulo de proyectos solo a efectos de
> seguimiento y **no se computa** en el total ni en los porcentajes anteriores.

**Avance global:** 25 de 74 casos de uso implementados y testeados = **33,8 %** del catálogo de requisitos.
