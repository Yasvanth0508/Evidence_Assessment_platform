package com.example.evidence.backend.repositoryanalysis.analyzer;

import com.example.evidence.backend.repositoryanalysis.exception.RepositoryAnalysisException;
import com.example.evidence.backend.repositoryanalysis.model.BuildConfiguration;
import com.example.evidence.backend.repositoryanalysis.model.DependencyInfo;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class DefaultGradleConfigurationAnalyzer {

    private static final Pattern DEPENDENCY_PATTERN = Pattern.compile(
            "(implementation|api|compileOnly|runtimeOnly|testImplementation|testRuntimeOnly)"
                    + "\\s*\\(?\\s*[\"']([^:\"']+):([^:\"']+)(?::([^\"']+))?[\"']\\s*\\)?"
    );

    private static final Pattern JAVA_VERSION_PATTERN = Pattern.compile(
            "(?:JavaVersion\\.VERSION_([0-9_]+)|"
                    + "JavaLanguageVersion\\.of\\s*\\(\\s*(\\d+)\\s*\\))"
    );

    private static final Pattern SPRING_BOOT_VERSION_PATTERN = Pattern.compile(
            "org\\.springframework\\.boot['\"]?\\s*"
                    + "(?:version\\s*[:=]\\s*['\"]([^'\"]+)['\"])?"
    );

    private static final Pattern PLUGIN_PATTERN = Pattern.compile(
            "id\\s*\\(?\\s*['\"]([^'\"]+)['\"]"
                    + "(?:\\s+version\\s+['\"]([^'\"]+)['\"])?"
    );

    public BuildConfiguration analyze(Path backendRootPath) {

        if (backendRootPath == null ||
                !Files.isDirectory(backendRootPath)) {
            throw new RepositoryAnalysisException(
                    "Invalid backend project directory"
            );
        }

        Path gradleFile = backendRootPath.resolve("build.gradle");

        if (!Files.isRegularFile(gradleFile)) {
            gradleFile = backendRootPath.resolve("build.gradle.kts");
        }

        if (!Files.isRegularFile(gradleFile)) {
            throw new RepositoryAnalysisException(
                    "Gradle build file not found"
            );
        }

        try {
            String content = Files.readString(gradleFile);

            List<DependencyInfo> dependencies =
                    extractDependencies(content);

            List<String> plugins = extractPlugins(content);

            String javaVersion = extractJavaVersion(content);

            String springBootVersion =
                    extractSpringBootVersion(content);

            return new BuildConfiguration(
                    "GRADLE",
                    javaVersion,
                    springBootVersion,
                    dependencies,
                    plugins
            );

        } catch (IOException exception) {
            throw new RepositoryAnalysisException(
                    "Failed to read Gradle build file",
                    exception
            );
        }
    }

    private List<DependencyInfo> extractDependencies(String content) {

        List<DependencyInfo> dependencies = new ArrayList<>();

        Matcher matcher = DEPENDENCY_PATTERN.matcher(content);

        while (matcher.find()) {

            String scope = matcher.group(1);
            String groupId = matcher.group(2);
            String artifactId = matcher.group(3);
            String version = matcher.group(4);

            dependencies.add(new DependencyInfo(
                    groupId,
                    artifactId,
                    version,
                    scope
            ));
        }

        return List.copyOf(dependencies);
    }

    private List<String> extractPlugins(String content) {

        List<String> plugins = new ArrayList<>();

        Matcher matcher = PLUGIN_PATTERN.matcher(content);

        while (matcher.find()) {

            String pluginId = matcher.group(1);
            String version = matcher.group(2);

            String pluginInfo = pluginId;

            if (version != null) {
                pluginInfo += ":" + version;
            }

            plugins.add(pluginInfo);
        }

        return List.copyOf(plugins);
    }

    private String extractJavaVersion(String content) {

        Matcher matcher = JAVA_VERSION_PATTERN.matcher(content);

        if (!matcher.find()) {
            return null;
        }

        String version = matcher.group(1) != null
                ? matcher.group(1).replace("_", ".")
                : matcher.group(2);

        return version;
    }

    private String extractSpringBootVersion(String content) {

        Matcher matcher = SPRING_BOOT_VERSION_PATTERN.matcher(content);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }
}