# 🏫 Recred - Backend API

Bienvenido al repositorio del backend de **Recred**. Este proyecto provee la API RESTful central para gestionar la lógica de negocio, la integración de usuarios, las transacciones y las operaciones de la base de datos del sistema.

---

## 🛠️ Stack Tecnológico y Arquitectura

La selección de tecnologías para el backend de este proyecto fue realizada priorizando la estabilidad, la seguridad y los estándares actuales de la industria para aplicaciones de level empresarial.

* **Java 17 & Spring Boot 4:** Elegimos Java 17 por ser la versión LTS (*Long-Term Support*) más consolidada, garantizando parches de seguridad y soporte a largo plazo. Lo combinamos con Spring Boot 3, el framework estándar de la industria, para desarrollar nuestra API REST de manera ágil, aprovechando su robusto ecosistema y el manejo automático de dependencias.
* **PostgreSQL (vía Supabase):** Para la persistencia de datos utilizamos Supabase como proveedor de *Database-as-a-Service (DBaaS)*. Esto nos permite contar con una base de datos PostgreSQL relacional en la nube, asegurando alta disponibilidad, escalabilidad inmediata y la integridad transaccional que requiere nuestro modelo de datos.
* **AWS Cognito:** La gestión de identidades, autenticación y autorización está delegada en Amazon Cognito. Esta decisión arquitectónica nos permite implementar un sistema de seguridad de primer level (utilizando tokens JWT) y evitar el riesgo de gestionar contraseñas sensibles directamente en nuestra base de datos.
* **Amazon Web Services (AWS) para el Despliegue:** El entorno de producción del backend se aloja en la infraestructura de nube de AWS. Esto nos brinda un entorno de ejecución altamente confiable, aislado y con capacidad de escalar los recursos computacionales según el tráfico que reciba la aplicación.
* **Integración Continua (GitHub Actions):** Para garantizar la calidad del código en un equipo colaborativo, implementamos *pipelines* de CI con GitHub Actions. Cada vez que se intenta integrar código nuevo a la rama principal (`main`), un flujo automatizado compila la aplicación y ejecuta pruebas de validación (utilizando la base de datos H2 en memoria) para asegurar que ningún cambio rompa la estabilidad del servidor.

---

## 🚀 Requisitos Previos (Desarrollo Local)

Para levantar este proyecto y contribuir en tu entorno local, asegurate de tener instalado:

* [Java 17 (JDK)](https://adoptium.net/) - Distribución Temurin recomendada.
* [Maven](https://maven.apache.org/) - Gestor de dependencias.
* Entorno de Desarrollo (IDE) recomendado: IntelliJ IDEA.
* PostgreSQL (Local) o conexión a la base de datos de desarrollo.

---

## ⚙️ Instalación y Ejecución

1. **Clonar el repositorio:**
   ```bash
   git clone [https://github.com/tu-user/recreopago-backend.git](https://github.com/tu-user/recreopago-backend.git)