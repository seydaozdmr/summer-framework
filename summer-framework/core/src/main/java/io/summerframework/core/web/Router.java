package io.summerframework.core.web;

import io.summerframework.core.context.AnnotationApplicationContext;
import io.summerframework.core.web.annotation.DeleteMapping;
import io.summerframework.core.web.annotation.GetMapping;
import io.summerframework.core.web.annotation.PatchMapping;
import io.summerframework.core.web.annotation.PostMapping;
import io.summerframework.core.web.annotation.PutMapping;
import io.summerframework.core.web.annotation.RequestMapping;
import io.summerframework.core.web.annotation.RestController;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

final class Router {

    private final Set<RouteDefinition> routes = new LinkedHashSet<>();

    static Router fromContext(AnnotationApplicationContext context) {
        Router router = new Router();
        Collection<String> beanNames = context.getBeanDefinitionNames();

        for (String beanName : beanNames) {
            Class<?> beanType = context.getBeanType(beanName);
            if (!beanType.isAnnotationPresent(RestController.class)) {
                continue;
            }

            Object controller = context.getBean(beanName);
            String basePath = "";
            RequestMapping requestMapping = beanType.getAnnotation(RequestMapping.class);
            if (requestMapping != null) {
                basePath = requestMapping.value();
            }

            for (Method method : beanType.getDeclaredMethods()) {
                if (method.isAnnotationPresent(GetMapping.class)) {
                    String fullPath = join(basePath, method.getAnnotation(GetMapping.class).value());
                    router.register(new RouteDefinition(HttpMethod.GET, fullPath, controller, method));
                }
                if (method.isAnnotationPresent(PostMapping.class)) {
                    String fullPath = join(basePath, method.getAnnotation(PostMapping.class).value());
                    router.register(new RouteDefinition(HttpMethod.POST, fullPath, controller, method));
                }
                if (method.isAnnotationPresent(PutMapping.class)) {
                    String fullPath = join(basePath, method.getAnnotation(PutMapping.class).value());
                    router.register(new RouteDefinition(HttpMethod.PUT, fullPath, controller, method));
                }
                if (method.isAnnotationPresent(DeleteMapping.class)) {
                    String fullPath = join(basePath, method.getAnnotation(DeleteMapping.class).value());
                    router.register(new RouteDefinition(HttpMethod.DELETE, fullPath, controller, method));
                }
                if (method.isAnnotationPresent(PatchMapping.class)) {
                    String fullPath = join(basePath, method.getAnnotation(PatchMapping.class).value());
                    router.register(new RouteDefinition(HttpMethod.PATCH, fullPath, controller, method));
                }
            }
        }

        return router;
    }

    RouteMatch resolve(String method, String path) {
        HttpMethod httpMethod = HttpMethod.from(method);
        String normalizedPath = normalize(path);

        for (RouteDefinition route : routes) {
            if (route.method() != httpMethod) {
                continue;
            }
            Map<String, String> pathVariables = route.matchPath(normalizedPath);
            if (pathVariables != null) {
                return new RouteMatch(route, pathVariables);
            }
        }

        return null;
    }

    Collection<RouteDefinition> getAllRoutes() {
        return routes;
    }

    private void register(RouteDefinition route) {
        for (RouteDefinition existing : routes) {
            if (existing.method() == route.method() && existing.path().equals(route.path())) {
                throw new IllegalStateException("Duplicate route detected: " + route.method() + " " + route.path());
            }
        }
        routes.add(route);
    }

    private static String join(String basePath, String methodPath) {
        return normalize(basePath + "/" + methodPath);
    }

    private static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return "/";
        }
        String normalized = raw.trim().replaceAll("/{2,}", "/");
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        if (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
