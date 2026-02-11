package io.summerframework.core.web;

import io.summerframework.core.web.annotation.PathVariable;
import io.summerframework.core.web.annotation.RequestBody;
import io.summerframework.core.web.annotation.RequestHeader;
import io.summerframework.core.web.annotation.RequestParam;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

final class RouteDefinition {

    private final HttpMethod method;
    private final String path;
    private final Object controller;
    private final Method handlerMethod;
    private final BodyBinder binder;
    private final List<ParameterBinding> parameterBindings;
    private final String[] templateSegments;

    RouteDefinition(HttpMethod method, String path, Object controller, Method handlerMethod) {
        this.method = method;
        this.path = path;
        this.controller = controller;
        this.handlerMethod = handlerMethod;
        this.binder = new BodyBinder();
        this.templateSegments = splitPath(path);
        this.parameterBindings = resolveBindings(handlerMethod, templateVariableNames());
    }

    HttpMethod method() {
        return method;
    }

    String path() {
        return path;
    }

    Map<String, String> matchPath(String requestPath) {
        String[] requestSegments = splitPath(requestPath);
        if (requestSegments.length != templateSegments.length) {
            return null;
        }

        Map<String, String> variables = new LinkedHashMap<>();
        for (int i = 0; i < templateSegments.length; i++) {
            String templateSegment = templateSegments[i];
            String requestSegment = requestSegments[i];

            if (isVariableSegment(templateSegment)) {
                variables.put(variableName(templateSegment), requestSegment);
                continue;
            }

            if (!templateSegment.equals(requestSegment)) {
                return null;
            }
        }

        return variables;
    }

    Object invoke(String body,
                  Map<String, String> pathVariables,
                  Map<String, List<String>> queryParameters,
                  Map<String, List<String>> headers) {
        Object[] args = new Object[handlerMethod.getParameterCount()];

        for (ParameterBinding binding : parameterBindings) {
            Object arg = switch (binding.kind()) {
                case BODY -> binder.bind(body, binding.type());
                case PATH_VARIABLE -> resolvePathVariable(binding, pathVariables);
                case REQUEST_PARAM -> resolveRequestParam(binding, queryParameters);
                case REQUEST_HEADER -> resolveRequestHeader(binding, headers);
            };
            args[binding.index()] = arg;
        }

        try {
            handlerMethod.setAccessible(true);
            return handlerMethod.invoke(controller, args);
        } catch (IllegalAccessException ex) {
            throw new IllegalStateException("Failed to access route method: " + handlerMethod, ex);
        } catch (InvocationTargetException ex) {
            Throwable targetException = ex.getTargetException();
            if (targetException instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException(targetException.getMessage(), targetException);
        }
    }

    private Object resolvePathVariable(ParameterBinding binding, Map<String, String> pathVariables) {
        String raw = pathVariables.get(binding.name());
        if (raw == null) {
            throw new BadRequestException("Missing path variable '" + binding.name() + "'");
        }
        if (binding.collection()) {
            throw new BadRequestException("@PathVariable does not support collection binding for '" + binding.name() + "'");
        }
        return binder.bindScalar(raw, binding.type(), binding.name());
    }

    private Object resolveRequestParam(ParameterBinding binding, Map<String, List<String>> queryParameters) {
        return resolveMultiValueSource("request param", binding, queryParameters.get(binding.name()));
    }

    private Object resolveRequestHeader(ParameterBinding binding, Map<String, List<String>> headers) {
        return resolveMultiValueSource("request header", binding, headers.get(binding.name().toLowerCase()));
    }

    private Object resolveMultiValueSource(String sourceName, ParameterBinding binding, List<String> rawValues) {
        List<String> values = rawValues;

        if (values == null || values.isEmpty()) {
            if (binding.defaultValue() != null) {
                values = List.of(binding.defaultValue());
            } else if (binding.required()) {
                throw new BadRequestException("Missing " + sourceName + " '" + binding.name() + "'");
            } else if (binding.collection()) {
                return List.of();
            } else {
                if (binding.type().isPrimitive()) {
                    throw new BadRequestException(sourceName + " '" + binding.name() + "' is required for primitive type");
                }
                return null;
            }
        }

        if (binding.collection()) {
            List<Object> converted = new ArrayList<>(values.size());
            for (String value : values) {
                converted.add(binder.bindScalar(value, binding.elementType(), binding.name()));
            }
            return converted;
        }

        return binder.bindScalar(values.get(0), binding.type(), binding.name());
    }

    private List<ParameterBinding> resolveBindings(Method method, Set<String> templateVariableNames) {
        Parameter[] parameters = method.getParameters();
        List<ParameterBinding> bindings = new ArrayList<>(parameters.length);
        int requestBodyCount = 0;

        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            RequestBody requestBody = parameter.getAnnotation(RequestBody.class);
            PathVariable pathVariable = parameter.getAnnotation(PathVariable.class);
            RequestParam requestParam = parameter.getAnnotation(RequestParam.class);
            RequestHeader requestHeader = parameter.getAnnotation(RequestHeader.class);

            int markerCount = countNonNull(requestBody, pathVariable, requestParam, requestHeader);
            if (markerCount == 0) {
                throw new IllegalStateException("Unsupported parameter in route method. Use @RequestBody, @PathVariable, @RequestParam or @RequestHeader: " + method);
            }
            if (markerCount > 1) {
                throw new IllegalStateException("Multiple web binding annotations found on parameter in " + method);
            }

            boolean collection = List.class.isAssignableFrom(parameter.getType());
            Class<?> elementType = collection ? resolveCollectionElementType(parameter) : parameter.getType();

            if (requestBody != null) {
                requestBodyCount++;
                if (requestBodyCount > 1) {
                    throw new IllegalStateException("Only one @RequestBody parameter is supported: " + method);
                }
                if (collection) {
                    throw new IllegalStateException("@RequestBody collection binding is not supported in this version: " + method);
                }
                bindings.add(ParameterBinding.body(i, parameter.getType()));
                continue;
            }

            if (pathVariable != null) {
                String name = resolveParameterName(pathVariable.value(), parameter, "@PathVariable");
                if (!templateVariableNames.contains(name)) {
                    throw new IllegalStateException("Path variable '" + name + "' is not present in route template " + path);
                }
                bindings.add(ParameterBinding.pathVariable(i, parameter.getType(), name));
                continue;
            }

            if (requestParam != null) {
                String name = resolveParameterName(requestParam.value(), parameter, "@RequestParam");
                String defaultValue = requestParam.defaultValue();
                boolean hasDefault = !RequestParam.NO_DEFAULT_VALUE.equals(defaultValue);
                boolean required = requestParam.required() && !hasDefault;
                bindings.add(ParameterBinding.requestParam(
                        i,
                        parameter.getType(),
                        elementType,
                        collection,
                        name,
                        required,
                        hasDefault ? defaultValue : null));
                continue;
            }

            String name = resolveParameterName(requestHeader.value(), parameter, "@RequestHeader");
            String defaultValue = requestHeader.defaultValue();
            boolean hasDefault = !RequestHeader.NO_DEFAULT_VALUE.equals(defaultValue);
            boolean required = requestHeader.required() && !hasDefault;
            bindings.add(ParameterBinding.requestHeader(
                    i,
                    parameter.getType(),
                    elementType,
                    collection,
                    name,
                    required,
                    hasDefault ? defaultValue : null));
        }

        return bindings;
    }

    private Class<?> resolveCollectionElementType(Parameter parameter) {
        Type genericType = parameter.getParameterizedType();
        if (!(genericType instanceof ParameterizedType parameterizedType)) {
            throw new IllegalStateException("Collection parameter must declare generic type: " + parameter);
        }

        Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
        if (actualTypeArguments.length != 1) {
            throw new IllegalStateException("Collection parameter must have exactly one generic type: " + parameter);
        }

        Type elementType = actualTypeArguments[0];
        if (elementType instanceof Class<?> elementClass) {
            return elementClass;
        }

        if (elementType instanceof ParameterizedType nested && nested.getRawType() instanceof Class<?> rawType) {
            return rawType;
        }

        throw new IllegalStateException("Unsupported collection element type for parameter: " + parameter);
    }

    private Set<String> templateVariableNames() {
        return java.util.Arrays.stream(templateSegments)
                .filter(this::isVariableSegment)
                .map(this::variableName)
                .collect(Collectors.toSet());
    }

    private String resolveParameterName(String explicitName, Parameter parameter, String annotationName) {
        if (explicitName != null && !explicitName.isBlank()) {
            return explicitName;
        }
        if (!parameter.isNamePresent()) {
            throw new IllegalStateException(annotationName + " requires explicit value when parameter names are not available");
        }
        return parameter.getName();
    }

    private boolean isVariableSegment(String segment) {
        return segment.startsWith("{") && segment.endsWith("}") && segment.length() > 2;
    }

    private String variableName(String segment) {
        return segment.substring(1, segment.length() - 1);
    }

    private String[] splitPath(String value) {
        if ("/".equals(value)) {
            return new String[0];
        }
        return value.substring(1).split("/");
    }

    private int countNonNull(Object... values) {
        int count = 0;
        for (Object value : values) {
            if (value != null) {
                count++;
            }
        }
        return count;
    }

    private enum BindingKind {
        BODY,
        PATH_VARIABLE,
        REQUEST_PARAM,
        REQUEST_HEADER
    }

    private record ParameterBinding(
            BindingKind kind,
            int index,
            Class<?> type,
            Class<?> elementType,
            boolean collection,
            String name,
            boolean required,
            String defaultValue
    ) {
        static ParameterBinding body(int index, Class<?> type) {
            return new ParameterBinding(BindingKind.BODY, index, type, type, false, null, false, null);
        }

        static ParameterBinding pathVariable(int index, Class<?> type, String name) {
            return new ParameterBinding(BindingKind.PATH_VARIABLE, index, type, type, false, name, true, null);
        }

        static ParameterBinding requestParam(
                int index,
                Class<?> type,
                Class<?> elementType,
                boolean collection,
                String name,
                boolean required,
                String defaultValue
        ) {
            return new ParameterBinding(BindingKind.REQUEST_PARAM, index, type, elementType, collection, name, required, defaultValue);
        }

        static ParameterBinding requestHeader(
                int index,
                Class<?> type,
                Class<?> elementType,
                boolean collection,
                String name,
                boolean required,
                String defaultValue
        ) {
            return new ParameterBinding(BindingKind.REQUEST_HEADER, index, type, elementType, collection, name, required, defaultValue);
        }
    }
}
