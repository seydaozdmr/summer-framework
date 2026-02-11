package io.summerframework.core.web;

import java.util.Map;

record RouteMatch(RouteDefinition route, Map<String, String> pathVariables) {
}
