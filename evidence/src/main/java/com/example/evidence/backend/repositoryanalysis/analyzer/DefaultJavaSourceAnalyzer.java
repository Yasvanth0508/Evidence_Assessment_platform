
package com.example.evidence.backend.repositoryanalysis.analyzer;

import com.example.evidence.backend.repositoryanalysis.exception.RepositoryAnalysisException;
import com.example.evidence.backend.repositoryanalysis.model.*;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class DefaultJavaSourceAnalyzer implements JavaSourceAnalyzer {

    public DefaultJavaSourceAnalyzer() {
        StaticJavaParser.getParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21);
    }

    @Override
    public List<JavaClassInfo> analyze(
            Path backendRootPath,
            List<String> javaFiles
    ) {

        if (backendRootPath == null ||
                !Files.isDirectory(backendRootPath)) {
            throw new RepositoryAnalysisException(
                    "Invalid backend project directory"
            );
        }

        if (javaFiles == null) {
            throw new RepositoryAnalysisException(
                    "Java file list cannot be null"
            );
        }

        try {
            Path realRoot = backendRootPath.toRealPath();

            List<JavaClassInfo> results = new ArrayList<>();

            for (String javaFile : javaFiles) {

                if (javaFile == null || javaFile.isBlank()) {
                    continue;
                }

                Path relativePath = Path.of(javaFile);

                if (relativePath.isAbsolute()) {
                    throw new RepositoryAnalysisException(
                            "Absolute Java file path is not allowed"
                    );
                }

                Path resolvedPath = realRoot.resolve(relativePath).normalize();

                if (!resolvedPath.startsWith(realRoot)) {
                    throw new RepositoryAnalysisException(
                            "Java file path escapes backend root"
                    );
                }

                if (!Files.isRegularFile(resolvedPath)) {
                    throw new RepositoryAnalysisException(
                            "Java source file not found: " + javaFile
                    );
                }

                Path realFile = resolvedPath.toRealPath();

                if (!realFile.startsWith(realRoot)) {
                    throw new RepositoryAnalysisException(
                            "Java source file resolves outside backend root"
                    );
                }

                CompilationUnit compilationUnit =
                        StaticJavaParser.parse(realFile);

                String packageName = compilationUnit
                        .getPackageDeclaration()
                        .map(declaration -> declaration.getNameAsString())
                        .orElse("");

                for (TypeDeclaration<?> type : compilationUnit.getTypes()) {

                    results.add(extractClassInfo(
                            type,
                            packageName,
                            javaFile
                    ));
                }
            }

            return List.copyOf(results);

        } catch (IOException | RuntimeException exception) {

            if (exception instanceof RepositoryAnalysisException analysisException) {
                throw analysisException;
            }

            throw new RepositoryAnalysisException(
                    "Failed to analyze Java source files",
                    exception
            );
        }
    }

    private JavaClassInfo extractClassInfo(
            TypeDeclaration<?> type,
            String packageName,
            String sourceFile
    ) {

        return new JavaClassInfo(
                packageName,
                type.getNameAsString(),
                getDeclarationType(type),
                extractAnnotations(type.getAnnotations()),
                type.getAccessSpecifier().asString(),
                getParentClass(type),
                getImplementedInterfaces(type),
                extractFields(type),
                extractMethods(type),
                sourceFile
        );
    }

    private String getDeclarationType(TypeDeclaration<?> type) {

        if (type instanceof ClassOrInterfaceDeclaration declaration) {
            return declaration.isInterface() ? "INTERFACE" : "CLASS";
        }

        if (type instanceof EnumDeclaration) {
            return "ENUM";
        }

        if (type instanceof RecordDeclaration) {
            return "RECORD";
        }

        if (type instanceof AnnotationDeclaration) {
            return "ANNOTATION";
        }

        return "UNKNOWN";
    }

    private String getParentClass(TypeDeclaration<?> type) {

        if (type instanceof ClassOrInterfaceDeclaration declaration
                && !declaration.isInterface()
                && !declaration.getExtendedTypes().isEmpty()) {

            return declaration.getExtendedTypes().get(0).asString();
        }

        return null;
    }

    private List<String> getImplementedInterfaces(TypeDeclaration<?> type) {

        if (type instanceof ClassOrInterfaceDeclaration declaration) {
            return declaration.getImplementedTypes()
                    .stream()
                    .map(Object::toString)
                    .toList();
        }

        if (type instanceof EnumDeclaration declaration) {
            return declaration.getImplementedTypes()
                    .stream()
                    .map(Object::toString)
                    .toList();
        }

        if (type instanceof RecordDeclaration declaration) {
            return declaration.getImplementedTypes()
                    .stream()
                    .map(Object::toString)
                    .toList();
        }

        return List.of();
    }

    private List<JavaAnnotationInfo> extractAnnotations(
            NodeList<AnnotationExpr> annotations
    ) {

        List<JavaAnnotationInfo> result = new ArrayList<>();

        for (AnnotationExpr annotation : annotations) {

            Map<String, String> attributes = new LinkedHashMap<>();

            if (annotation instanceof SingleMemberAnnotationExpr single) {

                attributes.put(
                        "value",
                        single.getMemberValue().toString()
                );

            } else if (annotation instanceof NormalAnnotationExpr normal) {

                normal.getPairs().forEach(pair ->
                        attributes.put(
                                pair.getNameAsString(),
                                pair.getValue().toString()
                        )
                );
            }

            result.add(new JavaAnnotationInfo(
                    annotation.getNameAsString(),
                    attributes
            ));
        }

        return List.copyOf(result);
    }

    private List<JavaFieldInfo> extractFields(TypeDeclaration<?> type) {

        List<JavaFieldInfo> fields = new ArrayList<>();

        for (BodyDeclaration<?> member : type.getMembers()) {

            if (!(member instanceof FieldDeclaration field)) {
                continue;
            }

            List<JavaAnnotationInfo> annotations =
                    extractAnnotations(field.getAnnotations());

            for (VariableDeclarator variable : field.getVariables()) {

                fields.add(new JavaFieldInfo(
                        variable.getNameAsString(),
                        variable.getType().asString(),
                        annotations,
                        field.getAccessSpecifier().asString(),
                        field.isStatic(),
                        field.isFinal()
                ));
            }
        }

        return List.copyOf(fields);
    }

    private List<JavaMethodInfo> extractMethods(TypeDeclaration<?> type) {

        List<JavaMethodInfo> methods = new ArrayList<>();

        for (BodyDeclaration<?> member : type.getMembers()) {

            if (!(member instanceof MethodDeclaration method)) {
                continue;
            }

            List<JavaParameterInfo> parameters =
                    method.getParameters()
                            .stream()
                            .map(parameter -> new JavaParameterInfo(
                                    parameter.getNameAsString(),
                                    parameter.getType().asString(),
                                    extractAnnotations(parameter.getAnnotations())
                            ))
                            .toList();

            methods.add(new JavaMethodInfo(
                    method.getNameAsString(),
                    method.getType().asString(),
                    parameters,
                    extractAnnotations(method.getAnnotations()),
                    method.getAccessSpecifier().asString(),
                    method.isStatic()
            ));
        }

        return List.copyOf(methods);
    }
}
