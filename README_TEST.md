# Guía de Testing y Automatización - RecreoPago Backend

Esta guía detalla la nueva estructura jerárquica de tests, el uso de perfiles de Maven y la estrategia de Pipeline optimizada para el proyecto.

## 1. Estructura y Responsabilidades
Hemos organizado la suite de pruebas para separar la lógica de negocio de la infraestructura pesada, permitiendo un desarrollo más ágil.

*   **`src/test/.../unit/` (El Cerebro):** Lógica pura de negocio (Casos de Uso y Modelos).
    *   **Tecnología:** JUnit 5 + Mockito (`@ExtendWith(MockitoExtension.class)`).
    *   **Impacto:** Sin carga de Spring, ejecución instantánea.
*   **`src/test/.../integration/` (Los Cables):** Adaptadores de infraestructura y servicios externos.
    *   **Tecnología:** Predominantemente Mockito (`@ExtendWith(MockitoExtension.class)`) para validar el mapeo entre Dominio y Entidades. `@SpringBootTest` se utiliza para "BackendApplicationTests".
    *   **Impacto:** Validación quirúrgica de herramientas y repositorios sin el costo temporal de levantar una base de datos real o el contexto de Spring.
*   **`src/test/.../web/` (El Contrato):** Validación de la API y ruteo.
    *   **Tecnología:** Mockito puro para lógica de controladores y `@WebMvcTest` para validar contratos y seguridad.
    *   **Impacto:** Equilibrio entre velocidad de testeo y validación del ruteo de Spring.

---

## 2. Maven Profiles (Comandos Resumidos)
Se han configurado perfiles en el `pom.xml` para evitar comandos largos y complejos.

| Suite | Comando | Tiempo Estimado |
| :--- | :--- | :--- |
| **Solo Unitarios** | `./mvnw test -Punit` 
| **Solo Web** | `./mvnw test -Pweb` 
| **Solo Integración** | `./mvnw test -Pintegration - ./mvnw failsafe:integration-test`
| **Suite Completa** | `./mvnw test` 

---

## 3. Guía de Trabajo Sugerida

### A. Durante el Desarrollo (Feedback Instantáneo)
Cada vez que modifiques lógica en un Caso de Uso o Modelo:
*   **Ejecutá:** `./mvnw test -Punit`
*   **Por qué:** Te asegura en segundos que no rompiste la lógica de negocio sin necesidad de levantar bases de datos.

### B. Antes del Git Push (Red de Seguridad)
Cuando termines tu tarea y estés listo para subir los cambios:
*   **Ejecutá:** `./mvnw test`
*   **Por qué:** Es responsabilidad del desarrollador asegurar que la suite completa (incluyendo DB y APIs) pase localmente. Evitá que el pipeline falle 10 minutos después.

---

## 4. Pipeline Inteligente (GitHub Actions)
El archivo `.github/workflows/automation.yml` ahora diferencia automáticamente el tipo de rama para "procurar un pipeline rápido y efectivo":

*   **En ramas `feature/*` (Push diario):** Corre **solo unitarios** (`-Punit`). Feedback en < 1 minuto.
*   **En `develop` o `main` (Merge/Pull Request):** Corre **suite completa** (`mvn clean verify`). Máxima seguridad antes de producción.

**Fragmento de la lógica implementada:**
```yaml
        run: |
          if [[ "${{ github.ref }}" == "refs/heads/main" || "${{ github.ref }}" == "refs/heads/develop" ]]; then
            echo "Rama protegida detectada. Corriendo suite completa (verify)..."
            ./mvnw clean verify
          else
            echo "Rama de feature detectada. Corriendo solo tests unitarios..."
            ./mvnw clean test -Punit
          fi
```

---

## 5. Reporte de Cobertura (Jacoco)
Para verificar qué partes del código te falta testear:
1. Ejecutá la suite completa: `./mvnw test`
2. Generá el reporte: `./mvnw jacoco:report`
3. Abrí el archivo: `target/site/jacoco/index.html`

**Estado Actual:**
*   **Cobertura Global:** 99%
*   **Efectividad:** 100% (1673+ tests verdes)

---

## 6. Reglas de Oro para el Equipo
1.  **Prohibición de Wildcards:** NUNCA usar `import com.recred...*`. Usar imports específicos para optimizar el compilador y el pipeline.
2.  **Pureza Unitaria:** Si creás un test en `unit/`, está **prohibido** usar `@SpringBootTest`. Si necesitás Spring, el test pertenece a `integration/`.
3.  **Mantener el Espejo:** Si creás un componente en `infrastructure/mi-servicio`, su test debe vivir en `integration/mi-servicio`.


## 7. Nuevos test de IT
1. **Descripcion:**  Maven Failsafe Plugin ( mvn verify ): Está configurado para correr las clases que terminan en  *IT.java  (Integration Tests).
2. **Activar en POM:** Por defecto, Maven trae activado el  maven-surefire-plugin  (que corre los  *Test.java  en la fase  test ), pero NO trae activado por defecto el plugin que se encarga de buscar y correr los  *IT.java  en la fase  verify. Asi lo activamos -> <goal>integration-test</goal>.
3. **Cantidad actual:** Tests run: 156.
4. **Comando para los test con terminación IT:** `./mvnw failsafe:integration-test`.
5. **OBSERVACIÓN:** Este comando `./mvnw test -Pintegration` corre los de integracion mockeados y los de integracion-repositorio de terminación 'IT'.