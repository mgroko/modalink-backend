# Estado del Sistema — Versión 0.1 (Línea Base Formal)

> **Fase 5: Estado del Sistema (Proceso Unificado)**  
> **Fecha de Corte**: 04 de Septiembre de 2026  
> **Versión**: v0.1-baseline  
> **Fuente de Verdad**: [bd.sql](file:///C:/Users/HP/Desktop/modalink-backend/docs/0_nota_presentacion/bd.sql)

---

## 1. Resumen Ejecutivo de la Versión

La versión **v0.1** consolida la primera iteración completa del backend de **ModaLink** y la estructuración integral de su documentación bajo el Proceso Unificado (UP). 

### Hitos Alcanzados en esta Versión:
1. **Línea Base de Documentación**:
   - Fase 0: Formalización de objetivos (`OBJ-01` a `OBJ-05`), requisitos de información (`RI-01` a `RI-14`) y arquitectura modular (`MOD-F-01` a `MOD-F-09`).
   - Fase 1: Catálogo completo de 69 Casos de Uso Extendidos (`UC-01` a `UC-69`).
   - Fase 2: Aspectos técnicos del modelo de dominio traduciendo las 38 tablas de `bd.sql`, diagramas DSS y Contratos formales.
   - Fase 3: Casos de uso reales con navegación web y diagramas DSD aplicando patrones de diseño.
   - Fase 4: Auditoría integral de trazabilidad e identificación de deuda técnica.
2. **Núcleo Transaccional Implementado y Testeado**:
   - Autenticación JWT con cookie httpOnly, registro con DNI/género/ubicación y roles globales (`UC-01`, `UC-02`, `UC-03`).
   - Gestión administrativa de usuarios con deshabilitación por motivo obligatorio y reactivación temporal programada (`UC-04`, `UC-05`, `UC-06`).
   - Solicitud de baja en el sistema y reactivación (`UC-07`).
   - Módulo de calendario y agenda con soporte de **jornada corrida y jornada partida** (`hora_fin_manana`, `hora_inicio_tarde`) y bloqueos manuales sin solapamiento (`UC-17`, `UC-18`).
   - Catálogo de profesiones y características técnicas (`UC-58`, `UC-59`).

---

## 2. Matriz General de Avance por Fase UP

| Módulo Funcional | Casos de Uso | Fase 0 (Nota) | Fase 1 (Requisitos) | Fase 2 (Análisis) | Fase 3 (Diseño) | Implementado & Testeado |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: |
| **MOD-F-01: Usuarios** | UC-01 a UC-08 | 100% | 100% | 100% | 100% | 100% (8/8) |
| **MOD-F-02: Perfiles** | UC-10 a UC-18 | 100% | 100% | 100% | 100% | 60% (Parcial) |
| **MOD-F-03: Publicaciones** | UC-19 a UC-22 | 100% | 100% | 100% | 100% | 0% (Planificado) |
| **MOD-F-04: Interacción** | UC-41 a UC-46 | 100% | 100% | 100% | 100% | 0% (Planificado) |
| **MOD-F-05: Disponibilidad** | UC-17, UC-18 | 100% | 100% | 100% | 100% | 100% (2/2) |
| **MOD-F-06: Proyectos** | UC-24 a UC-38 | 100% | 100% | 100% | 100% | 0% (Siguiente iteración) |
| **MOD-F-07: Convocatorias** | UC-49 a UC-52 | 100% | 100% | 100% | 100% | 0% (Siguiente iteración) |

---

## 3. Correcciones Arquitectónicas y Mejoras Incorporadas

- **Sincronización de Base de Datos**: Migraciones Flyway `V1` a `V19` consolidadas y verificadas contra el archivo físico `bd.sql` de 38 tablas.
- **Doble Capa de Validación**: Errores reportados por la base de datos se traducen en excepciones semánticas de dominio (`DniDuplicadoException`, `SolapamientoHorarioException`, etc.) asegurando que el frontend no contenga lógica de negocio.
- **Control de Cobertura**: Verificación obligatoria de testing JaCoCo previo a cada avance de módulo.
