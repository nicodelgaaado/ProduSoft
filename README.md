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

## Infraestructura serverless
- Frontend servido en **Vercel** (https://produ-soft.vercel.app/).
- API de backend desplegada en **Render** como servicio web.
- Base de datos **PostgreSQL** provista por **Neon**, con conexión TLS.

## Ejecución local desde la terminal
1. **Requisitos previos**
   - Node.js ≥ 18 y npm (o pnpm/bun) instalados.
   - JDK 17 y Maven Wrapper (`mvnw`/`mvnw.cmd`) incluidos en el proyecto.
2. **Variables de entorno**
   - Para usar PostgreSQL externo define `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` (ver `backend/src/main/resources/application-render.properties`).
   - Si no defines nada, el perfil `local` usa H2 en memoria.
3. **Levantar el backend**
   ```powershell
   cd backend
   .\mvnw.cmd spring-boot:run
   # En macOS/Linux: ./mvnw spring-boot:run
   ```
   El servicio queda disponible en `http://localhost:8080`.
4. **Levantar el frontend**
   ```powershell
   cd frontend
   npm install
   npm run dev
   ```
   La interfaz se sirve en `http://localhost:3000` y consume la API local.

## Despliegue
El repositorio está preparado para integrarse con los pipelines nativos de Vercel (frontend), Render (backend) y Neon (base de datos). Ajusta las variables de entorno correspondientes en cada plataforma antes de publicar.
