# Reporte de Cambios: Feature School Registration

## Corrección de Pruebas Unitarias
Se solucionaron los errores en la suite de `CognitoStudentIdentityServiceImplTest` causados por el refactor hacia `BaseCognitoIdentityService`:
- **`NoSuchField httpClient`**: Se actualizó el método de reflexión `setField` para buscar los campos (`httpClient`, `clock`) en la superclase (`target.getClass().getSuperclass().getDeclaredField(name)`).
- **Mensaje de Excepción Inesperado**: Se actualizó el test `createStudentUserThrowsWhenCognitoReturnsErrorStatus` para validar el nuevo mensaje de excepción propagado por la clase base (`"No se pudo invocar Cognito"` en lugar de `"No se pudo crear el usuario en Cognito"`).

## Pruebas de Integración para Repositorios
Se implementaron y actualizaron las pruebas de integración en base de datos (con entorno Dockerizado) para cubrir los repositorios asociados al feature:

### 1. `SchoolRegistrationRequestRepositoryImplIT` (NUEVO)
Se creó la suite completa para el nuevo repositorio `SchoolRegistrationRequestRepositoryImpl`, cubriendo:
- `save(SchoolRegistrationRequest request)`: Verifica la persistencia de una solicitud.
- `findById(UUID id)`: Verifica la obtención por ID.
- `findByStatus(SchoolRegistrationStatus status)`: Verifica el listado por estado (e.g. PENDING).
- `existsBySchoolEmailAndStatus(...)`: Validación de duplicados de Email para el colegio.
- `existsBySchoolCueAndStatus(...)`: Validación de duplicados de CUE.
- `existsByDirectorEmailAndStatus(...)`: Validación de duplicados de Email para el director.

### 2. `ColegioRepositoryImplIT` (ACTUALIZADO)
Se añadieron aserciones para cubrir los nuevos métodos expuestos e implementados en `ColegioRepositoryImpl`:
- `save(Colegio colegio)`: Se probó la persistencia de un nuevo colegio en BD.
- `existsByEmail(String email)`: Se verificó la validación de correos electrónicos pre-existentes.
- `existsByCue(String cue)`: Se verificó la validación de CUE pre-existentes.

Todos los tests mencionados ejecutan exitosamente bajo el perfil `-Pintegration`.
