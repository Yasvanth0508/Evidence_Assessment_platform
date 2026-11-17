package com.example.evidence.backend.repositoryanalysis.model;

import java.util.List;

public record ApiEndpointInfo(
        String controllerName,
        String methodName,
        String httpMethod,
        String path,
        List<ApiParameterInfo> parameters,
        String returnType
) {}