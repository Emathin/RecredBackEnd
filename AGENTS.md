# Briefing de Proyecto: recred-backend

## 1. Visión Holística del Organismo

El proyecto es un sistema con órganos interconectados, no partes aisladas.
- **`/infrastructure/web` (Piel):** Controladores que reciben peticiones.
- **`/application` (Cerebro):** Casos de Uso que orquestan la lógica.
- **`/domain` (Corazón):** Modelos de negocio (`models`), excepciones (`exceptions`), interfaces de repositorios (`repositories`) y lógica de dominio pura (`implementation`). Es el núcleo vital.
- **`/infrastructure/persistence` (Extremidades):** Implementaciones de repositorios que interactúan con la BBDD usando las Entidades JPA de `/infrastructure/entities`.
- **`/config` (Sistema Nervioso):** Clases de configuración de Spring que conectan (inyectan) todas las capas desacopladas.
- **`/test` (Sistema Inmunitario):** Pruebas unitarias y de integración que garantizan la salud del código y son ejecutadas por el CI/CD.

## 2. Stack Tecnológico Principal
- Java 17, Spring Boot, Maven
- PostgreSQL (producción), H2 (tests)
- AWS Cognito, Docker, AWS EC2
- CI/CD con GitHub Actions

## 3. Reglas de Oro para Generación de Código

Cada fragmento de código que generes DEBE cumplir con lo siguiente:

1.  SOLID: Aplicar SOLID cuando aporte claridad real. Evitar abstracciones innecesarias. Priorizar KISS.
2.  POO: Seguir el paradigma de Programación Orientada a Objetos.
3.  Legibilidad: El código debe ser autoexplicativo.
4.  CERO COMENTARIOS: No incluyas comentarios en el código fuente. Las explicaciones se dan en el chat.
5.  TESTS: Incluir su test correspondiente. Justifica en el chat si un test no es necesario (ej: para DTOs simples).
6.  UBICACIÓN: Indicar claramente la ubicación del archivo según la arquitectura descrita.
7.  REUTILIZACIÓN: Usar siempre las clases, modelos y configuraciones ya existentes en el proyecto.
8.  FLEXIBILIDAD: Estar abierto a la adición de nuevas herramientas o modificaciones en la arquitectura.
9.  IF SIN LLAVES: NO me agregues llaves en un bloque `if` cuando la línea interior sea solo una.
10. EXTRACCIÓN CONDICIONAL: Extrae condiciones booleanas complejas en funciones (métodos) que devuelvan un booleano para maximizar la legibilidad siempre que puedas.
11. EVITA HARDCODEO: Antes de usar valores "hardcodeados" (mágicos), piensa en una opción más legible y limpia (enums, constantes, properties, variables de entorno, etc.) para el código.
12. MANEJO DE EXCEPCIONES: Contempla el manejo de excepciones creando y utilizando excepciones propias en la capa de `/dominio/exceptions` que modelen semánticamente los errores del negocio, manteniendo la coherencia con las entidades existentes.

No modificar archivos no relacionados con la tarea.
No hacer refactors grandes salvo que sean solicitados explícitamente.
Mantener los cambios pequeños, seguros y revisables.