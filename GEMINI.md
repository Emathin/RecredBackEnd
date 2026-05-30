c# Briefing de Proyecto: recreopago-backend

## 1. Visión Holística del Organismo (Arquitectura Hexagonal/Clean)
El sistema se organiza en capas desacopladas donde el dominio es el centro:
- **`/domain/models` (Corazón):** Objetos de negocio puros, sin dependencias de frameworks (evitar Lombok aquí si es posible para mantener la pureza). Contienen la lógica de validación interna.
- **`/application` (Cerebro):** Casos de Uso (clases planas con método `execute`) que orquestan la lógica. Utilizan `Records` para los comandos de entrada.
- **`/infrastructure/persistence` (Extremidades):** Implementaciones de repositorios que actúan como adaptadores. Realizan el mapeo manual entre el Dominio y las Entidades JPA.
- **`/infrastructure/entities` (Capa de Datos):** Entidades JPA con anotaciones de base de datos y Lombok (`@Getter`, `@Setter`, `@NoArgsConstructor`).
- **`/config/UseCases.java` (Sistema Nervioso):** Registro central de Beans para los Casos de Uso. **Todo nuevo Caso de Uso debe registrarse aquí.**

## 2. Stack Tecnológico Principal
- Java 17, Spring Boot 4.0.x
- PostgreSQL (Prod) / H2 (Test), JPA / Hibernate
- JUnit 5, Mockito (Testing)
- Lombok (Solo en infraestructura/entidades)

## 3. Reglas de Oro para Generación de Código

1.  **SOLID & POO:** Priorizar la composición sobre la herencia y la responsabilidad única.
2.  **CASOS DE USO:** Deben ser clases simples, inyectadas a través de constructores y registradas en `com.recred.backend.config.UseCases`.
3.  **COMMANDS & DTOs:** Utilizar **Java Records** para los comandos de entrada de los Casos de Uso (ej: `BudgetCommand`).
4.  **MAPEO MANUAL:** 
    - Las Entidades deben tener un método `toDomain()`.
    - Los Repositorios Implementación deben tener un método privado `toEntity(Modelo, Entidad)`.
5.  **LOGICA EN EL DOMINIO:** Extraer validaciones y cálculos complejos a los modelos de dominio (ej: `modelo.validar()`) en lugar de dejarlos en el Caso de Uso.
6.  **IF SIN LLAVES:** NO agregar llaves `{}` en bloques `if` de una sola línea.
7.  **EXTRACCIÓN CONDICIONAL:** Métodos privados booleanos con nombres descriptivos para condiciones complejas (ej: `isVigente()`).
8.  **EVITA HARDCODEO:** Usar Enums para estados y constantes/properties para valores de configuración.
9.  **EXCEPCIONES SEMÁNTICAS:** Crear excepciones específicas en `/domain/exceptions` que hereden de `RuntimeException` para errores de negocio.
10. **TESTING (Obligatorio):**
    - Usar `@ExtendWith(MockitoExtension.class)`.
    - Nombres de tests descriptivos en **español** (ej: `execute_CreaPresupuesto_YLoGuarda`).
    - Utilizar `ArgumentCaptor` para verificar estados complejos en los mocks.
11. **IDIOMA:** Explicaciones en **español**, código (variables, clases, métodos) en **inglés**.
12. **CERO COMENTARIOS:** El código debe hablar por sí mismo mediante nombres limpios y estructura clara.

## 4. Estructura de Archivos (Convenciones)
- `application/`: `VerboNombre.java` (UseCase), `NombreCommand.java` (Record).
- `domain/models/`: `Nombre.java` (POJO).
- `infrastructure/persistence/`: `NombreRepositoryImpl.java` y `SpringDataNombreRepository.java`.
- `infrastructure/web/`: `NombreController.java` (Uso de DTOs para entrada/salida).
