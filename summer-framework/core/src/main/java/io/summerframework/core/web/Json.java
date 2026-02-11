package io.summerframework.core.web;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class Json {

    String stringify(Object value) {
        StringBuilder builder = new StringBuilder();
        writeValue(value, builder);
        return builder.toString();
    }

    Object parse(String source) {
        if (source == null || source.isBlank()) {
            return null;
        }
        return new Parser(source).parse();
    }

    @SuppressWarnings("unchecked")
    Map<String, Object> parseObject(String source) {
        Object parsed = parse(source);
        if (parsed == null) {
            return new LinkedHashMap<>();
        }
        if (!(parsed instanceof Map<?, ?> map)) {
            throw new BadRequestException("Request body must be a JSON object");
        }
        return (Map<String, Object>) map;
    }

    private void writeValue(Object value, StringBuilder builder) {
        if (value == null) {
            builder.append("null");
            return;
        }

        if (value instanceof String stringValue) {
            writeString(stringValue, builder);
            return;
        }

        if (value instanceof Number || value instanceof Boolean) {
            builder.append(value);
            return;
        }

        if (value instanceof Map<?, ?> mapValue) {
            writeMap(mapValue, builder);
            return;
        }

        if (value instanceof Iterable<?> iterable) {
            writeIterable(iterable, builder);
            return;
        }

        if (value.getClass().isArray()) {
            writeArray(value, builder);
            return;
        }

        if (value.getClass().isRecord()) {
            writeRecord(value, builder);
            return;
        }

        writePojo(value, builder);
    }

    private void writeMap(Map<?, ?> map, StringBuilder builder) {
        builder.append('{');
        boolean first = true;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!first) {
                builder.append(',');
            }
            first = false;
            writeString(String.valueOf(entry.getKey()), builder);
            builder.append(':');
            writeValue(entry.getValue(), builder);
        }
        builder.append('}');
    }

    private void writeIterable(Iterable<?> iterable, StringBuilder builder) {
        builder.append('[');
        boolean first = true;
        for (Object item : iterable) {
            if (!first) {
                builder.append(',');
            }
            first = false;
            writeValue(item, builder);
        }
        builder.append(']');
    }

    private void writeArray(Object array, StringBuilder builder) {
        builder.append('[');
        int length = Array.getLength(array);
        for (int i = 0; i < length; i++) {
            if (i > 0) {
                builder.append(',');
            }
            writeValue(Array.get(array, i), builder);
        }
        builder.append(']');
    }

    private void writeRecord(Object record, StringBuilder builder) {
        builder.append('{');
        RecordComponent[] components = record.getClass().getRecordComponents();
        for (int i = 0; i < components.length; i++) {
            if (i > 0) {
                builder.append(',');
            }
            RecordComponent component = components[i];
            writeString(component.getName(), builder);
            builder.append(':');
            try {
                writeValue(component.getAccessor().invoke(record), builder);
            } catch (IllegalAccessException | InvocationTargetException ex) {
                throw new IllegalStateException("Failed to read record component: " + component.getName(), ex);
            }
        }
        builder.append('}');
    }

    private void writePojo(Object pojo, StringBuilder builder) {
        builder.append('{');
        boolean first = true;
        Class<?> current = pojo.getClass();
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                if (!first) {
                    builder.append(',');
                }
                first = false;
                field.setAccessible(true);
                writeString(field.getName(), builder);
                builder.append(':');
                try {
                    writeValue(field.get(pojo), builder);
                } catch (IllegalAccessException ex) {
                    throw new IllegalStateException("Failed to serialize field: " + field.getName(), ex);
                }
            }
            current = current.getSuperclass();
        }
        builder.append('}');
    }

    private void writeString(String value, StringBuilder builder) {
        builder.append('"');
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '"' -> builder.append("\\\"");
                case '\\' -> builder.append("\\\\");
                case '\b' -> builder.append("\\b");
                case '\f' -> builder.append("\\f");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                default -> {
                    if (ch < 0x20) {
                        builder.append(String.format("\\u%04x", (int) ch));
                    } else {
                        builder.append(ch);
                    }
                }
            }
        }
        builder.append('"');
    }

    private static final class Parser {

        private final String source;
        private int index;

        private Parser(String source) {
            this.source = source;
        }

        private Object parse() {
            skipWhitespace();
            Object value = parseValue();
            skipWhitespace();
            if (index != source.length()) {
                throw error("Unexpected token");
            }
            return value;
        }

        private Object parseValue() {
            skipWhitespace();
            if (index >= source.length()) {
                throw error("Unexpected end of input");
            }

            char token = source.charAt(index);
            return switch (token) {
                case '{' -> parseObject();
                case '[' -> parseArray();
                case '"' -> parseString();
                case 't' -> parseLiteral("true", Boolean.TRUE);
                case 'f' -> parseLiteral("false", Boolean.FALSE);
                case 'n' -> parseLiteral("null", null);
                default -> {
                    if (token == '-' || Character.isDigit(token)) {
                        yield parseNumber();
                    }
                    throw error("Unexpected token: " + token);
                }
            };
        }

        private Map<String, Object> parseObject() {
            expect('{');
            skipWhitespace();
            Map<String, Object> values = new LinkedHashMap<>();
            if (peek('}')) {
                expect('}');
                return values;
            }

            while (true) {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                expect(':');
                Object value = parseValue();
                values.put(key, value);
                skipWhitespace();
                if (peek('}')) {
                    expect('}');
                    return values;
                }
                expect(',');
            }
        }

        private List<Object> parseArray() {
            expect('[');
            skipWhitespace();
            List<Object> values = new ArrayList<>();
            if (peek(']')) {
                expect(']');
                return values;
            }

            while (true) {
                values.add(parseValue());
                skipWhitespace();
                if (peek(']')) {
                    expect(']');
                    return values;
                }
                expect(',');
            }
        }

        private String parseString() {
            expect('"');
            StringBuilder builder = new StringBuilder();
            while (index < source.length()) {
                char ch = source.charAt(index++);
                if (ch == '"') {
                    return builder.toString();
                }
                if (ch == '\\') {
                    if (index >= source.length()) {
                        throw error("Invalid escape");
                    }
                    char escaped = source.charAt(index++);
                    switch (escaped) {
                        case '"' -> builder.append('"');
                        case '\\' -> builder.append('\\');
                        case '/' -> builder.append('/');
                        case 'b' -> builder.append('\b');
                        case 'f' -> builder.append('\f');
                        case 'n' -> builder.append('\n');
                        case 'r' -> builder.append('\r');
                        case 't' -> builder.append('\t');
                        case 'u' -> builder.append(parseUnicode());
                        default -> throw error("Invalid escape: " + escaped);
                    }
                    continue;
                }
                builder.append(ch);
            }
            throw error("Unterminated string");
        }

        private char parseUnicode() {
            if (index + 4 > source.length()) {
                throw error("Invalid unicode escape");
            }
            String hex = source.substring(index, index + 4);
            index += 4;
            try {
                return (char) Integer.parseInt(hex, 16);
            } catch (NumberFormatException ex) {
                throw error("Invalid unicode escape: " + hex);
            }
        }

        private Object parseNumber() {
            int start = index;
            if (source.charAt(index) == '-') {
                index++;
            }
            consumeDigits();
            boolean decimal = false;
            if (index < source.length() && source.charAt(index) == '.') {
                decimal = true;
                index++;
                consumeDigits();
            }
            if (index < source.length() && (source.charAt(index) == 'e' || source.charAt(index) == 'E')) {
                decimal = true;
                index++;
                if (index < source.length() && (source.charAt(index) == '+' || source.charAt(index) == '-')) {
                    index++;
                }
                consumeDigits();
            }

            String token = source.substring(start, index);
            try {
                if (decimal) {
                    return Double.parseDouble(token);
                }
                return Long.parseLong(token);
            } catch (NumberFormatException ex) {
                throw error("Invalid number: " + token);
            }
        }

        private void consumeDigits() {
            if (index >= source.length() || !Character.isDigit(source.charAt(index))) {
                throw error("Expected digit");
            }
            while (index < source.length() && Character.isDigit(source.charAt(index))) {
                index++;
            }
        }

        private Object parseLiteral(String token, Object value) {
            if (source.startsWith(token, index)) {
                index += token.length();
                return value;
            }
            throw error("Invalid literal");
        }

        private void skipWhitespace() {
            while (index < source.length() && Character.isWhitespace(source.charAt(index))) {
                index++;
            }
        }

        private void expect(char expected) {
            if (index >= source.length() || source.charAt(index) != expected) {
                throw error("Expected '" + expected + "'");
            }
            index++;
        }

        private boolean peek(char token) {
            return index < source.length() && source.charAt(index) == token;
        }

        private BadRequestException error(String message) {
            return new BadRequestException(message + " at position " + index);
        }
    }
}
