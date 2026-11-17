
package com.example.evidence.backend.repositoryanalysis.analyzer;

import com.example.evidence.backend.repositoryanalysis.exception.RepositoryAnalysisException;
import com.example.evidence.backend.repositoryanalysis.model.ApiEndpointInfo;
import com.example.evidence.backend.repositoryanalysis.model.ApiParameterInfo;
import com.example.evidence.backend.repositoryanalysis.model.JavaAnnotationInfo;
import com.example.evidence.backend.repositoryanalysis.model.JavaClassInfo;
import com.example.evidence.backend.repositoryanalysis.model.JavaMethodInfo;
import com.example.evidence.backend.repositoryanalysis.model.JavaParameterInfo;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class DefaultApiEndpointAnalyzer implements ApiEndpointAnalyzer {

    private static final Map<String, String> HTTP_MAPPING_TYPES = Map.of(
            "GetMapping", "GET",
            "PostMapping", "POST",
            "PutMapping", "PUT",
            "PatchMapping", "PATCH",
            "DeleteMapping", "DELETE"
    );

    private static final List<String> REQUEST_PARAMETER_ANNOTATIONS = List.of(
            "PathVariable",
            "RequestParam",
            "RequestBody",
            "RequestHeader",
            "CookieValue",
            "ModelAttribute",
            "RequestPart"
    );

    @Override
    public List<ApiEndpointInfo> analyze(List<JavaClassInfo> javaClasses) {

        if (javaClasses == null) {
            throw new RepositoryAnalysisException(
                    "Java class list cannot be null"
            );
        }

        List<ApiEndpointInfo> endpoints = new ArrayList<>();

        for (JavaClassInfo javaClass : javaClasses) {

            if (!isController(javaClass)) {
                continue;
            }

            String basePath = getMappingPath(javaClass.annotations());

            for (JavaMethodInfo method : javaClass.methods()) {

                JavaAnnotationInfo mapping =
                        findMappingAnnotation(method.annotations());

                if (mapping == null) {
                    continue;
                }

                String httpMethod = getHttpMethod(mapping);

                List<String> paths = getMappingPaths(mapping);

                List<ApiParameterInfo> parameters =
                        extractParameters(method.parameters());

                for (String methodPath : paths) {

                    String fullPath = combinePaths(basePath, methodPath);

                    endpoints.add(new ApiEndpointInfo(
                            javaClass.className(),
                            method.name(),
                            httpMethod,
                            fullPath,
                            parameters,
                            method.returnType()
                    ));
                }
            }
        }

        return List.copyOf(endpoints);
    }

    private boolean isController(JavaClassInfo javaClass) {

        return javaClass.annotations().stream()
                .anyMatch(annotation ->
                        annotation.name().equals("RestController")
                                || annotation.name().equals("Controller")
                );
    }

    private JavaAnnotationInfo findMappingAnnotation(
            List<JavaAnnotationInfo> annotations
    ) {

        return annotations.stream()
                .filter(this::isMappingAnnotation)
                .findFirst()
                .orElse(null);
    }

    private boolean isMappingAnnotation(JavaAnnotationInfo annotation) {

        return HTTP_MAPPING_TYPES.containsKey(annotation.name())
                || annotation.name().equals("RequestMapping");
    }

    private String getHttpMethod(JavaAnnotationInfo annotation) {

        String mappingName = annotation.name();

        if (HTTP_MAPPING_TYPES.containsKey(mappingName)) {
            return HTTP_MAPPING_TYPES.get(mappingName);
        }

        return getRequestMappingMethod(annotation);
    }

    private String getRequestMappingMethod(
            JavaAnnotationInfo annotation
    ) {

        String method = annotation.attributes().get("method");

        if (method == null) {
            return "ANY";
        }

        // Example: {RequestMethod.GET, RequestMethod.POST}
        if (method.startsWith("{") && method.endsWith("}")) {
            return "MULTIPLE";
        }

        int separator = method.lastIndexOf('.');

        return separator >= 0
                ? method.substring(separator + 1)
                : method;
    }

    private String getMappingPath(
            List<JavaAnnotationInfo> annotations
    ) {

        for (JavaAnnotationInfo annotation : annotations) {

            if (!isMappingAnnotation(annotation)) {
                continue;
            }

            List<String> paths = getMappingPaths(annotation);

            if (!paths.isEmpty()) {
                return paths.get(0);
            }
        }

        return "";
    }

    private List<String> getMappingPaths(
            JavaAnnotationInfo annotation
    ) {

        Map<String, String> attributes = annotation.attributes();

        String path = attributes.get("path");

        if (path == null) {
            path = attributes.get("value");
        }

        if (path == null || path.isBlank()) {
            return List.of("");
        }

        path = path.trim();

        // Supports a simple array: {"/users", "/customers"}
        if (path.startsWith("{") && path.endsWith("}")) {

            String values = path.substring(1, path.length() - 1);

            List<String> paths = new ArrayList<>();

            for (String value : values.split(",")) {
                String cleaned = cleanPath(value);

                if (!cleaned.isBlank()) {
                    paths.add(cleaned);
                }
            }

            return paths.isEmpty() ? List.of("") : List.copyOf(paths);
        }

        return List.of(cleanPath(path));
    }

    private String cleanPath(String path) {

        String cleaned = path.trim();

        if (cleaned.startsWith("\"") && cleaned.endsWith("\"")) {
            cleaned = cleaned.substring(1, cleaned.length() - 1);
        }

        if (cleaned.startsWith("'") && cleaned.endsWith("'")) {
            cleaned = cleaned.substring(1, cleaned.length() - 1);
        }

        return cleaned;
    }

    private String combinePaths(String basePath, String methodPath) {

        String base = basePath == null ? "" : basePath.trim();
        String method = methodPath == null ? "" : methodPath.trim();

        String combined = "/" + base + "/" + method;

        combined = combined.replaceAll("/+", "/");

        if (combined.length() > 1 && combined.endsWith("/")) {
            combined = combined.substring(0, combined.length() - 1);
        }

        return combined;
    }

    private List<ApiParameterInfo> extractParameters(
            List<JavaParameterInfo> parameters
    ) {

        List<ApiParameterInfo> result = new ArrayList<>();

        for (JavaParameterInfo parameter : parameters) {

            List<String> annotations = parameter.annotations()
                    .stream()
                    .map(JavaAnnotationInfo::name)
                    .toList();

            boolean required = getRequiredStatus(parameter.annotations());

            result.add(new ApiParameterInfo(
                    parameter.name(),
                    parameter.type(),
                    annotations,
                    required
            ));
        }

        return List.copyOf(result);
    }

    private boolean getRequiredStatus(
            List<JavaAnnotationInfo> annotations
    ) {

        for (JavaAnnotationInfo annotation : annotations) {

            if (!REQUEST_PARAMETER_ANNOTATIONS.contains(annotation.name())) {
                continue;
            }

            String requiredValue = annotation.attributes().get("required");

            if ("false".equals(requiredValue)) {
                return false;
            }
        }

        return true;
    }
}
