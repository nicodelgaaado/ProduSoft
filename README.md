# ProduSoft Workflow

ProduSoft es una plataforma para coordinar órdenes de trabajo industriales con apoyo de IA. Ofrece paneles específicos para operadores y supervisores, asegurando trazabilidad desde la preparación hasta la entrega.

## Características principales
- Panel de operador con colas por etapa, checklists interactivos y captura de tiempos, notas y excepciones.
- Panel de supervisor para crear órdenes, ajustar prioridades y aprobar saltos o retrabajos sobre cada etapa.
- Asistente contextual de IA integrado en ambas vistas, servido vía Ollama con el modelo `gpt-oss:20b-cloud`, para resolver dudas y sugerir acciones.
- Autenticación con roles (operador/supervisor) y políticas de seguridad Spring Security.
- Persistencia transaccional de órdenes, estados y checklists en PostgreSQL con esquema versionado.

## Stack tecnológico
- Frontend: Next.js 14 con TypeScript y componentes propios (`frontend/`).
- Backend: Spring Boot 3 (Java 17) con JPA, Security y controladores REST (`backend/`).
- Base de datos: PostgreSQL administrada en Neon (Render usa HikariCP). En desarrollo local se soporta H2 en memoria.


