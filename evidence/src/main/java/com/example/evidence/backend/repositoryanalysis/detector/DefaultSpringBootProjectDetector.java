
package com.example.evidence.backend.repositoryanalysis.detector;

import com.example.evidence.backend.repositoryanalysis.exception.InvalidBackendProjectException;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

@Component
public class DefaultSpringBootProjectDetector
        implements SpringBootProjectDetector {

    @Override
    public void detect(Path backendRootPath) {

        // 1. Verify backend directory
        if (backendRootPath == null
                || !Files.isDirectory(backendRootPath)) {

            throw new InvalidBackendProjectException(
                    "Backend project directory does not exist"
            );
        }

        // 2. Identify build configuration
        boolean mavenProject = Files.exists(
                backendRootPath.resolve("pom.xml")
        );

        boolean gradleProject =
                Files.exists(backendRootPath.resolve("build.gradle"))
                || Files.exists(backendRootPath.resolve("build.gradle.kts"));

        if (!mavenProject && !gradleProject) {
            throw new InvalidBackendProjectException(
                    "No Maven or Gradle build configuration found"
            );
        }

        try {

            // 3. Verify Spring Boot declaration
            boolean springBootDeclared =
                    mavenProject
                            ? hasSpringBootInMaven(backendRootPath)
                            : hasSpringBootInGradle(backendRootPath);

            if (!springBootDeclared) {
                throw new InvalidBackendProjectException(
                        "Spring Boot dependency or plugin not found"
                );
            }

            // 4. Verify Java source directory
            Path javaSourcePath = backendRootPath
                    .resolve("src")
                    .resolve("main")
                    .resolve("java");

            if (!Files.isDirectory(javaSourcePath)) {
                throw new InvalidBackendProjectException(
                        "Java source directory not found"
                );
            }

            // 5. Find Java source files
            List<Path> javaFiles;

            try (Stream<Path> paths = Files.walk(javaSourcePath)) {
                javaFiles = paths
                        .filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(".java"))
                        .toList();
            }

            if (javaFiles.isEmpty()) {
                throw new InvalidBackendProjectException(
                        "No Java source files found"
                );
            }

            // 6. Find Spring Boot application entry point
            boolean applicationFound = false;

            for (Path javaFile : javaFiles) {

                String source = Files.readString(javaFile);

                if (source.contains("@SpringBootApplication")) {
                    applicationFound = true;
                    break;
                }
            }

            if (!applicationFound) {
                throw new InvalidBackendProjectException(
                        "Spring Boot application entry point not found"
                );
            }

        } catch (IOException exception) {

            throw new InvalidBackendProjectException(
                    "Unable to inspect Spring Boot project",
                    exception
            );
        }
    }

    private boolean hasSpringBootInMaven(Path backendRootPath)
            throws IOException {

        Path pomFile = backendRootPath.resolve("pom.xml");

        String pomContent = Files.readString(pomFile);

        return pomContent.contains("spring-boot-starter")
                || pomContent.contains("spring-boot-maven-plugin")
                || pomContent.contains("spring-boot-dependencies");
    }

    private boolean hasSpringBootInGradle(Path backendRootPath)
            throws IOException {

        Path gradleFile = Files.exists(
                backendRootPath.resolve("build.gradle")
        )
                ? backendRootPath.resolve("build.gradle")
                : backendRootPath.resolve("build.gradle.kts");

        String gradleContent = Files.readString(gradleFile);

        return gradleContent.contains("org.springframework.boot")
                || gradleContent.contains("spring-boot-starter");
    }
}
