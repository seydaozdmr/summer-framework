package io.summerframework.core.web;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.util.Map;

final class BodyBinder {

    private final Json json = new Json();

    Object bind(String body, Class<?> targetType) {
        if (targetType == String.class) {
            return body;
        }

        Object parsed = json.parse(body);
        return convert(parsed, targetType, "root");
    }

    Object bindScalar(String value, Class<?> targetType, String fieldName) {
        return convert(value, targetType, fieldName);
    }

    @SuppressWarnings("unchecked")
    private Object convert(Object value, Class<?> targetType, String fieldName) {
        if (value == null) {
            if (targetType.isPrimitive()) {
                throw new BadRequestException("Field '" + fieldName + "' cannot be null");
            }
            return null;
        }

        if (targetType.isAssignableFrom(value.getClass())) {
            return value;
        }

        if (targetType == Integer.class || targetType == int.class) {
            return toInteger(value, fieldName);
        }
        if (targetType == Long.class || targetType == long.class) {
            return toLong(value, fieldName);
        }
        if (targetType == Double.class || targetType == double.class) {
            return toDouble(value, fieldName);
        }
        if (targetType == Boolean.class || targetType == boolean.class) {
            return toBoolean(value, fieldName);
        }
        if (targetType == String.class) {
            return String.valueOf(value);
        }

        if (value instanceof Map<?, ?> rawMap) {
            Map<String, Object> map = (Map<String, Object>) rawMap;
            if (targetType.isRecord()) {
                return bindRecord(map, targetType);
            }
            return bindPojo(map, targetType);
        }

        throw new BadRequestException("Cannot bind field '" + fieldName + "' to " + targetType.getSimpleName());
    }

    private Object bindRecord(Map<String, Object> map, Class<?> targetType) {
        RecordComponent[] components = targetType.getRecordComponents();
        Object[] args = new Object[components.length];
        Class<?>[] argTypes = new Class<?>[components.length];

        for (int i = 0; i < components.length; i++) {
            RecordComponent component = components[i];
            argTypes[i] = component.getType();
            args[i] = convert(map.get(component.getName()), component.getType(), component.getName());
        }

        try {
            Constructor<?> constructor = targetType.getDeclaredConstructor(argTypes);
            constructor.setAccessible(true);
            return constructor.newInstance(args);
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException ex) {
            throw new IllegalStateException("Failed to bind record: " + targetType.getName(), ex);
        }
    }

    private Object bindPojo(Map<String, Object> map, Class<?> targetType) {
        Object instance;
        try {
            Constructor<?> constructor = targetType.getDeclaredConstructor();
            constructor.setAccessible(true);
            instance = constructor.newInstance();
        } catch (NoSuchMethodException ex) {
            throw new BadRequestException("Type " + targetType.getSimpleName() + " must have a no-arg constructor");
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException ex) {
            throw new IllegalStateException("Failed to create type: " + targetType.getName(), ex);
        }

        Class<?> current = targetType;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                if (!map.containsKey(field.getName())) {
                    continue;
                }
                Object raw = map.get(field.getName());
                Object converted = convert(raw, field.getType(), field.getName());
                try {
                    field.setAccessible(true);
                    field.set(instance, converted);
                } catch (IllegalAccessException ex) {
                    throw new IllegalStateException("Failed to bind field: " + field.getName(), ex);
                }
            }
            current = current.getSuperclass();
        }

        return instance;
    }

    private Integer toInteger(Object value, String fieldName) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text) {
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException ex) {
                throw new BadRequestException("Invalid integer for field '" + fieldName + "'");
            }
        }
        throw new BadRequestException("Invalid integer for field '" + fieldName + "'");
    }

    private Long toLong(Object value, String fieldName) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ex) {
                throw new BadRequestException("Invalid long for field '" + fieldName + "'");
            }
        }
        throw new BadRequestException("Invalid long for field '" + fieldName + "'");
    }

    private Double toDouble(Object value, String fieldName) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String text) {
            try {
                return Double.parseDouble(text);
            } catch (NumberFormatException ex) {
                throw new BadRequestException("Invalid number for field '" + fieldName + "'");
            }
        }
        throw new BadRequestException("Invalid number for field '" + fieldName + "'");
    }

    private Boolean toBoolean(Object value, String fieldName) {
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        if (value instanceof String text) {
            if ("true".equalsIgnoreCase(text)) {
                return true;
            }
            if ("false".equalsIgnoreCase(text)) {
                return false;
            }
        }
        throw new BadRequestException("Invalid boolean for field '" + fieldName + "'");
    }
}
