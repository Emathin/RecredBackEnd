package com.recred.backend.coverage;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class BeanAccessorsCoverageTest {

    private static final List<String> PACKAGES = List.of(
            "com.recred.backend.domain.models",
            "com.recred.backend.infrastructure.entities",
            "com.recred.backend.infrastructure.web"
    );

    @TestFactory
    Stream<DynamicTest> beanAccessorsShouldRoundTripValues() throws Exception {
        return classes().stream()
                .filter(this::isConcreteClass)
                .filter(this::hasNoArgsConstructor)
                .map(type -> DynamicTest.dynamicTest(type.getName(), () -> coverBean(type)));
    }

    @TestFactory
    Stream<DynamicTest> recordComponentsShouldExposeConstructorValues() throws Exception {
        return classes().stream()
                .filter(Class::isRecord)
                .map(type -> DynamicTest.dynamicTest(type.getName(), () -> coverRecord(type)));
    }

    private List<Class<?>> classes() throws Exception {
        Path classesRoot = Path.of(System.getProperty("user.dir"), "target", "classes");
        List<Class<?>> types = new ArrayList<>();

        for (String packageName : PACKAGES) {
            Path packagePath = classesRoot.resolve(packageName.replace('.', '/'));
            if (Files.notExists(packagePath)) {
                continue;
            }
            try (Stream<Path> paths = Files.walk(packagePath)) {
                paths.filter(path -> path.toString().endsWith(".class"))
                        .filter(path -> !path.getFileName().toString().equals("package-info.class"))
                        .map(path -> className(classesRoot, path))
                        .map(this::loadClass)
                        .flatMap(Optional::stream)
                        .forEach(types::add);
            }
        }

        return types.stream()
                .distinct()
                .sorted(Comparator.comparing(Class::getName))
                .toList();
    }

    private String className(Path classesRoot, Path classFile) {
        String relativePath = classesRoot.relativize(classFile).toString();
        return relativePath
                .replace(".class", "")
                .replace('\\', '.')
                .replace('/', '.');
    }

    private Optional<Class<?>> loadClass(String className) {
        try {
            return Optional.of(Class.forName(className));
        } catch (ClassNotFoundException | NoClassDefFoundError ignored) {
            return Optional.empty();
        }
    }

    private boolean isConcreteClass(Class<?> type) {
        int modifiers = type.getModifiers();
        return !type.isRecord()
                && !type.isEnum()
                && !type.isInterface()
                && !Modifier.isAbstract(modifiers);
    }

    private boolean hasNoArgsConstructor(Class<?> type) {
        return noArgsConstructor(type).isPresent();
    }

    private void coverBean(Class<?> type) throws Exception {
        Object instance = instantiate(type);

        for (Method setter : setters(type)) {
            Object value = sampleValue(setter.getParameterTypes()[0]);
            setter.setAccessible(true);
            setter.invoke(instance, value);
            Optional<Method> getter = getter(type, setter);
            if (getter.isPresent()) {
                Method getterMethod = getter.get();
                getterMethod.setAccessible(true);
                assertSameOrEquals(value, getterMethod.invoke(instance));
            }
        }
    }

    private List<Method> setters(Class<?> type) {
        return Stream.of(type.getDeclaredMethods())
                .filter(method -> method.getName().startsWith("set"))
                .filter(method -> method.getParameterCount() == 1)
                .filter(method -> !Modifier.isStatic(method.getModifiers()))
                .sorted(Comparator.comparing(Method::getName))
                .toList();
    }

    private Optional<Method> getter(Class<?> type, Method setter) {
        String property = setter.getName().substring(3);
        return method(type, "get" + property)
                .or(() -> method(type, "is" + property));
    }

    private Optional<Method> method(Class<?> type, String name) {
        return Stream.of(type.getDeclaredMethods())
                .filter(candidate -> candidate.getName().equals(name))
                .filter(candidate -> candidate.getParameterCount() == 0)
                .findFirst();
    }

    private void coverRecord(Class<?> type) throws Exception {
        RecordComponent[] recordComponents = type.getRecordComponents();
        Class<?>[] parameterTypes = Stream.of(recordComponents)
                .map(RecordComponent::getType)
                .toArray(Class<?>[]::new);
        Constructor<?> constructor = type.getDeclaredConstructor(parameterTypes);
        constructor.setAccessible(true);
        Object[] args = Stream.of(parameterTypes)
                .map(this::sampleValue)
                .toArray();
        Object record;
        try {
            record = constructor.newInstance(args);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return;
        }
        Method[] components = type.getDeclaredMethods();

        for (int i = 0; i < recordComponents.length; i++) {
            String name = recordComponents[i].getName();
            Method accessor = Stream.of(components)
                    .filter(method -> method.getName().equals(name))
                    .filter(method -> method.getParameterCount() == 0)
                    .findFirst()
                    .orElseThrow();
            accessor.setAccessible(true);
            assertSameOrEquals(args[i], accessor.invoke(record));
        }
    }

    private Object instantiate(Class<?> type) throws ReflectiveOperationException {
        Constructor<?> constructor = noArgsConstructor(type).orElseThrow();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }

    private Optional<Constructor<?>> noArgsConstructor(Class<?> type) {
        return Stream.of(type.getDeclaredConstructors())
                .filter(constructor -> constructor.getParameterCount() == 0)
                .findFirst();
    }

    private Object sampleValue(Class<?> type) {
        if (type == String.class) {
            return "valor";
        }
        if (type == UUID.class) {
            return UUID.randomUUID();
        }
        if (type == BigDecimal.class) {
            return new BigDecimal("10.50");
        }
        if (type == LocalDate.class) {
            return LocalDate.of(2026, 6, 15);
        }
        if (type == LocalDateTime.class) {
            return LocalDateTime.of(2026, 6, 15, 12, 30);
        }
        if (type == LocalTime.class) {
            return LocalTime.of(12, 30);
        }
        if (type == Boolean.class || type == boolean.class) {
            return true;
        }
        if (type == Integer.class || type == int.class) {
            return 7;
        }
        if (type == Long.class || type == long.class) {
            return 7L;
        }
        if (type == Double.class || type == double.class) {
            return 7.0;
        }
        if (type == Float.class || type == float.class) {
            return 7.0f;
        }
        if (type == Short.class || type == short.class) {
            return (short) 7;
        }
        if (type == Byte.class || type == byte.class) {
            return (byte) 7;
        }
        if (type == Character.class || type == char.class) {
            return 'a';
        }
        if (type.isEnum()) {
            return type.getEnumConstants()[0];
        }
        if (type.isArray()) {
            return Array.newInstance(type.getComponentType(), 0);
        }
        if (List.class.isAssignableFrom(type) || Collection.class == type) {
            return new ArrayList<>();
        }
        if (Set.class.isAssignableFrom(type)) {
            return new HashSet<>();
        }
        if (Map.class.isAssignableFrom(type)) {
            return new HashMap<>();
        }
        if (Optional.class.isAssignableFrom(type)) {
            return Optional.empty();
        }
        return instantiateSample(type);
    }

    private Object instantiateSample(Class<?> type) {
        if (type.isInterface() || Modifier.isAbstract(type.getModifiers())) {
            return null;
        }
        try {
            return instantiate(type);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    private void assertSameOrEquals(Object expected, Object actual) {
        if (expected instanceof BigDecimal expectedDecimal && actual instanceof BigDecimal) {
            assertEquals(0, expectedDecimal.compareTo((BigDecimal) actual));
            return;
        }
        if (expected == null || actual == null || isValueType(expected.getClass())) {
            assertEquals(expected, actual);
            return;
        }
        assertSame(expected, actual);
    }

    private boolean isValueType(Class<?> type) {
        return type.isPrimitive()
                || type.isEnum()
                || type.getName().startsWith("java.")
                || type.getName().startsWith("jakarta.");
    }
}
