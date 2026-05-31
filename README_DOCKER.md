# Dockerización del Backend + PostgreSQL

Este documento explica cómo funciona el entorno Docker del proyecto, qué archivos intervienen, cómo levantar la aplicación y cuál es el flujo de trabajo recomendado para que cualquier integrante del equipo pueda correr el backend y la base de datos sin tener que adivinar cómo está armado.

---

## 1. Objetivo de esta configuración

El objetivo es poder levantar en local:

- Backend Java / Spring Boot
- Base de datos PostgreSQL
- Variables de entorno
- Health checks
- Comunicación backend → base de datos

Todo esto usando Docker y Docker Compose.

La idea es que cualquier desarrollador pueda clonar el proyecto, configurar su archivo `.env` y levantar todo con un solo comando.

---

## 2. ¿Qué es Docker?

Docker permite ejecutar aplicaciones dentro de contenedores.

Un contenedor es un entorno aislado que contiene lo necesario para ejecutar una parte del sistema. Por ejemplo:

- PostgreSQL corre en su propio contenedor.
- El backend corre en otro contenedor.
- Ambos se comunican entre sí mediante una red interna creada por Docker Compose.

Esto evita problemas típicos como:

- “En mi máquina funciona”.
- Diferencias de versiones de Java.
- Diferencias de versiones de PostgreSQL.
- Configuraciones locales inconsistentes.
- Tener que instalar manualmente todos los servicios.

---

## 3. Conceptos importantes

### Imagen

Una imagen es una plantilla para crear contenedores.

Ejemplo:

```yaml
image: postgres:16
```

Eso significa que Docker descarga y usa la imagen oficial de PostgreSQL versión 16.

---

### Contenedor

Un contenedor es una instancia en ejecución de una imagen.

Ejemplo:

- `recred-db` es el contenedor de PostgreSQL.
- `recred-backend` es el contenedor del backend.

---

### Dockerfile

El `Dockerfile` define cómo se construye la imagen de nuestra aplicación.

Para PostgreSQL no necesitamos Dockerfile porque usamos una imagen oficial.

Para el backend sí necesitamos Dockerfile porque es código propio.

---

### Docker Compose

`docker-compose.yml` sirve para definir varios servicios y levantarlos juntos.

En este proyecto define:

- Servicio `db`
- Servicio `backend`
- Volumen persistente para la DB
- Variables de entorno
- Health checks
- Dependencia entre backend y DB

---

### Volumen

Un volumen permite persistir datos aunque se apague o elimine el contenedor.

En este proyecto usamos:

```yaml
volumes:
  db_data:
```

Eso evita que la base se borre cada vez que apagamos Docker.

---

### Red interna

Docker Compose crea automáticamente una red interna entre servicios.

Gracias a eso, el backend puede conectarse a PostgreSQL usando el name del servicio:

```txt
postgres
```

No se usa `localhost` entre contenedores.

---

## 4. Estructura de archivos relacionados con Docker

En la raíz del proyecto deberían existir estos archivos:

```txt
.
├── Dockerfile
├── docker-compose.yml
├── .dockerignore
├── .env
├── .env.example
├── pom.xml
├── mvnw
├── .mvn/
└── src/
```

---

## 5. Archivo `.env`

El archivo `.env` contiene variables de entorno para desarrollo local.

Ejemplo:

```env
POSTGRES_DB=recred_local
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres

DB_URL=jdbc:postgresql://postgres:5432/recred_local
DB_USERNAME=postgres
DB_PASSWORD=postgres
```

### Importante

Cuando el backend corre dentro de Docker, la URL debe usar:

```txt
postgres
```

Ejemplo correcto:

```env
DB_URL=jdbc:postgresql://postgres:5432/recred_local
```

Ejemplo incorrecto dentro de Docker:

```env
DB_URL=jdbc:postgresql://localhost:5432/recred_local
```

`localhost` dentro del contenedor del backend apunta al propio backend, no al contenedor de PostgreSQL.

---

## 6. Archivo `.env.example`

El archivo `.env` no debería subirse al repositorio porque puede contener credenciales reales.

Sí se recomienda subir un `.env.example` para que el equipo sepa qué variables necesita.

Ejemplo recomendado:

```env
POSTGRES_DB=recred_local
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres

DB_URL=jdbc:postgresql://postgres:5432/recred_local
DB_USERNAME=postgres
DB_PASSWORD=postgres
```

Cada desarrollador puede copiarlo así:

```bash
cp .env.example .env
```

En Windows PowerShell:

```powershell
Copy-Item .env.example .env
```

---

## 7. Configuración de Spring Boot

En `application.properties` usamos variables de entorno con valores por defecto:

```properties
spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5432/recred_local}
spring.datasource.username=${DB_USERNAME:postgres}
spring.datasource.password=${DB_PASSWORD:tu_clave_local}
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
```

Esto permite dos formas de ejecución:

### Backend corriendo localmente sin Docker

```properties
jdbc:postgresql://localhost:5432/recred_local
```

### Backend corriendo dentro de Docker

```properties
jdbc:postgresql://postgres:5432/recred_local
```

La diferencia es importante:

| Contexto | Host de la DB |
|---|---|
| Backend en la PC local | `localhost` |
| Backend en Docker | `postgres` |

---

## 8. Dockerfile del backend

El backend se construye con un Dockerfile multi-stage.

Ejemplo:

```dockerfile
FROM eclipse-temurin:17-jdk AS build

WORKDIR /app

COPY .mvn .mvn
COPY mvnw .
COPY pom.xml .
COPY checkstyle.xml .

RUN ./mvnw dependency:go-offline

COPY src src

RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

### ¿Por qué multi-stage?

Porque usamos una imagen con JDK para compilar y una imagen más liviana con JRE para ejecutar.

Primera etapa:

```dockerfile
FROM eclipse-temurin:17-jdk AS build
```

Sirve para compilar el proyecto.

Segunda etapa:

```dockerfile
FROM eclipse-temurin:17-jre
```

Sirve para ejecutar el `.jar`.

Esto reduce el tamaño final de la imagen y evita llevar herramientas de compilación al contenedor final.

---

## 9. `.dockerignore`

El archivo `.dockerignore` evita copiar archivos innecesarios al build de Docker.

Ejemplo:

```txt
target
.git
.gitignore
.idea
.vscode
*.log
.env
```

Esto mejora la velocidad del build y evita copiar archivos sensibles o innecesarios.

---

## 10. `docker-compose.yml`

Ejemplo completo:

```yaml
name: recred

services:
  db:
    image: postgres:16
    container_name: recred-db
    restart: unless-stopped
    env_file:
      - .env
    environment:
      POSTGRES_DB: ${POSTGRES_DB}
      POSTGRES_USER: ${POSTGRES_USER}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
    ports:
      - "5432:5432"
    volumes:
      - db_data:/var/lib/postgresql/data
    networks:
      default:
        aliases:
          - recred-db
          - postgres
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${POSTGRES_USER} -d ${POSTGRES_DB}"]
      interval: 5s
      timeout: 5s
      retries: 5

  backend:
    image: ${BACKEND_IMAGE:-recred-backend:local}
    build:
      context: .
      dockerfile: Dockerfile
    container_name: recred-backend
    restart: unless-stopped
    env_file:
      - .env
    ports:
      - "8080:8080"
    depends_on:
      db:
        condition: service_healthy

volumes:
  db_data:
```

---

## 11. ¿Cómo se comunican backend y DB?

Dentro de Docker Compose, los servicios se comunican por name.

El servicio de DB se llama:

```yaml
db:
```

El contenedor se llama `recred-db` y tambien tiene alias de red `postgres` para mantener compatibilidad con configuraciones anteriores. Entonces el backend puede conectarse usando:

```txt
jdbc:postgresql://postgres:5432/recred_local
```

Docker resuelve automaticamente el hostname `postgres`, `db` o `recred-db` hacia el contenedor correcto. Desde la maquina local, PostgreSQL queda publicado en `localhost:5432`.

---

## 12. Healthcheck de PostgreSQL

El healthcheck verifica si PostgreSQL está listo para recibir conexiones.

```yaml
healthcheck:
  test: ["CMD-SHELL", "pg_isready -U ${POSTGRES_USER} -d ${POSTGRES_DB}"]
  interval: 5s
  timeout: 5s
  retries: 5
```

Esto es importante porque `depends_on` por sí solo solo espera que el contenedor exista, pero no garantiza que la DB esté lista.

Con:

```yaml
depends_on:
  postgres:
    condition: service_healthy
```

el backend espera a que PostgreSQL esté saludable antes de arrancar.

---

## 13. Spring Actuator Health

El proyecto usa Spring Actuator para verificar el estado de la aplicación.

Endpoint:

```txt
http://localhost:8080/actuator/health
```

Respuesta esperada:

```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP"
    }
  }
}
```

Si `db` aparece en `UP`, significa que el backend logró conectarse correctamente a PostgreSQL.

---

## 14. Comandos principales

### Levantar todo

```bash
docker compose up --build
```

### Levantar en segundo plano

```bash
docker compose up --build -d
```

### Ver contenedores corriendo

```bash
docker ps
```

### Ver logs del backend

```bash
docker logs recred-backend
```

### Ver logs de PostgreSQL

```bash
docker logs recred-db
```

### Apagar contenedores

```bash
docker compose down
```

### Apagar y borrar datos de la DB

```bash
docker compose down -v
```

Usar `-v` con cuidado porque borra el volumen de PostgreSQL.

---

## 15. Cómo verificar que todo funciona

### 1. Levantar el entorno

```bash
docker compose up --build
```

### 2. Entrar al health endpoint

```txt
http://localhost:8080/actuator/health
```

### 3. Validar respuesta

La respuesta esperada debe tener:

```json
"status": "UP"
```

y dentro de `components`:

```json
"db": {
  "status": "UP"
}
```

Esto confirma:

- Backend levantado.
- PostgreSQL levantado.
- Conexión backend → DB correcta.
- Actuator funcionando.

---

## 16. Cómo entrar manualmente a PostgreSQL

```bash
docker exec -it recred-db psql -U postgres -d recred_local
```

Listar tablas:

```sql
\dt
```

Listar bases de datos:

```sql
\l
```

Salir:

```sql
\q
```

---

## 17. Flujo de trabajo recomendado

### Primera vez

```bash
cp .env.example .env
docker compose up --build
```

En Windows PowerShell:

```powershell
Copy-Item .env.example .env
docker compose up --build
```

### Uso diario

```bash
docker compose up
```

### Después de cambiar Dockerfile, dependencias o configuración

```bash
docker compose up --build
```

### Si se rompe la DB local o cambiaron credenciales

```bash
docker compose down -v
docker compose up --build
```

---

## 18. CI/CD

El proyecto tambien tiene pipeline de GitHub Actions.

El pipeline esta dividido en:

### CI

Corre en push y pull request a `main` y `develop`.

Responsabilidades:

- Descargar codigo.
- Configurar Java 17.
- Ejecutar Checkstyle.
- Compilar, correr tests y verificar el build con Maven.
- Usar `COGNITO_ISSUER_URI` desde GitHub Secrets durante la verificacion, si el entorno lo necesita.

### Deploy

Corre solo en push a `main`.

Responsabilidades:

- Descargar el codigo.
- Construir la imagen Docker del backend.
- Etiquetar la imagen como `recred-backend:${{ github.sha }}` y `recred-backend:latest`.
- Guardar la imagen como `recred-backend.tar.gz`.
- Copiar `recred-backend.tar.gz` y `docker-compose.yml` a `/home/ubuntu/recred-app` en EC2.
- Cargar la imagen en EC2 con `docker load`.
- Levantar los contenedores con `docker compose up -d --no-build`.
- Limpiar imagenes Docker sin uso con `docker image prune -f`.

La condicion importante es:

```yaml
if: github.event_name == 'push' && github.ref == 'refs/heads/main'
```

Esto evita deploys accidentales desde pull requests o desde `develop`.

### Imagen usada en produccion

En local, `docker-compose.yml` puede construir el backend desde el `Dockerfile`:

```yaml
image: ${BACKEND_IMAGE:-recred-backend:local}
build:
  context: .
  dockerfile: Dockerfile
```

En AWS, el workflow ya manda una imagen construida. Por eso ejecuta:

```bash
sudo env BACKEND_IMAGE=recred-backend:${{ github.sha }} docker compose up -d --no-build
```

`BACKEND_IMAGE` fuerza a Compose a usar la imagen exacta generada por ese commit. `--no-build` evita que EC2 intente compilar el proyecto otra vez.

### Requisitos en EC2

Antes de ejecutar el deploy, la instancia EC2 debe tener:

- Docker instalado y funcionando.
- Docker Compose disponible como `docker compose`.
- Directorio `/home/ubuntu/recred-app`.
- Archivo `/home/ubuntu/recred-app/.env` con las variables reales.
- Puerto `8080` disponible para el backend.

El deploy tambien ejecuta:

```bash
sudo systemctl stop recred.service || true
sudo docker rm -f recred-backend || true
```

Esto frena el servicio viejo, si existe, y elimina un contenedor anterior del backend para evitar conflictos de name o de puerto. No borra el volumen de PostgreSQL.

---

## 19. Checklist antes de subir cambios

Antes de abrir PR o mergear:

```bash
docker compose up --build
```

Verificar:

- `http://localhost:8080/actuator/health` responde `UP`.
- `db` responde `UP`.
- No hay errores en logs del backend.
- No hay errores en logs de PostgreSQL.
- `.env` no está incluido en Git.
- `.env.example` sí está incluido.
- `Dockerfile`, `docker-compose.yml`, `.dockerignore` y este README están actualizados.

---

## 20. Resumen final

Con esta configuración tenemos:

- Backend Java dockerizado.
- PostgreSQL dockerizado.
- Comunicación interna entre contenedores.
- Variables de entorno.
- Volumen persistente para datos.
- Healthcheck de PostgreSQL.
- Health endpoint del backend con Actuator.
- CI/CD separado entre integración y deploy.

El flujo esperado es:

```txt
docker compose up --build
        |
        v
PostgreSQL inicia
        |
        v
PostgreSQL pasa healthcheck
        |
        v
Backend inicia
        |
        v
Backend se conecta a PostgreSQL
        |
        v
Actuator devuelve status UP
```

Si el endpoint `/actuator/health` devuelve `UP` y el componente `db` está en `UP`, el entorno está funcionando correctamente.
