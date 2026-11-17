
package com.example.evidence.backend.repositoryanalysis.scanner;

import com.example.evidence.backend.repositoryanalysis.exception.RepositoryAnalysisException;
import com.example.evidence.backend.repositoryanalysis.model.ProjectStructure;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@Component
public class DefaultProjectStructureScanner
        implements ProjectStructureScanner {

    private static final Set<String> BUILD_FILES = Set.of(
            "pom.xml",
            "build.gradle",
            "build.gradle.kts",
            "settings.gradle",
            "settings.gradle.kts"
    );

    private static final Set<String> CONFIG_EXTENSIONS = Set.of(
            ".properties",
            ".yml",
            ".yaml"
    );

    @Override
    public ProjectStructure scan(Path backendRootPath) {

        // 1. Validate input
        if (backendRootPath == null ||
                !Files.isDirectory(backendRootPath)) {

            throw new RepositoryAnalysisException(
                    "Invalid backend project directory"
            );
        }

        // 2. Prepare result collections
        List<String> directories = new ArrayList<>();
        List<String> javaFiles = new ArrayList<>();
        List<String> buildFiles = new ArrayList<>();
        List<String> configurationFiles = new ArrayList<>();

        try {
            Path realRoot = backendRootPath.toRealPath();

            // 3. Traverse project recursively
            try (Stream<Path> paths = Files.walk(realRoot)) {

                paths.filter(path -> !path.equals(realRoot))
                        .forEach(path -> {

                            String relativePath = realRoot
                                    .relativize(path)
                                    .toString()
                                    .replace('\\', '/');

                            String fileName = path.getFileName()
                                    .toString();

                            // 4. Identify directories
                            if (Files.isDirectory(path)) {
                                directories.add(relativePath);
                                return;
                            }

                            // 5. Ignore non-regular files
                            if (!Files.isRegularFile(path)) {
                                return;
                            }

                            // 6. Identify Java files
                            if (fileName.endsWith(".java")) {
                                javaFiles.add(relativePath);
                            }

                            // 7. Identify build files
                            if (BUILD_FILES.contains(fileName)) {
                                buildFiles.add(relativePath);
                            }

                            // 8. Identify configuration files
                            if (hasConfigurationExtension(fileName)) {
                                configurationFiles.add(relativePath);
                            }
                        });
            }

            // 9. Sort results for consistent output
            directories.sort(Comparator.naturalOrder());
            javaFiles.sort(Comparator.naturalOrder());
            buildFiles.sort(Comparator.naturalOrder());
            configurationFiles.sort(Comparator.naturalOrder());

            // 10. Return structured result
            return new ProjectStructure(
                    realRoot.toString(),
                    directories,
                    javaFiles,
                    buildFiles,
                    configurationFiles
            );

        } catch (IOException exception) {

            throw new RepositoryAnalysisException(
                    "Failed to scan repository structure",
                    exception
            );
        }
    }

    private boolean hasConfigurationExtension(String fileName) {

        return CONFIG_EXTENSIONS.stream()
                .anyMatch(fileName::endsWith);
    }
}
