package com.example.evidence.backend.repositoryanalysis.git;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

@Component
public class RepositoryCleanupService {

    public void cleanup(Path repositoryPath, Exception originalException) {

        if (!Files.exists(repositoryPath)) {
            return;
        }

        try (var paths = Files.walk(repositoryPath)) {

            paths.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException exception) {
                            throw new UncheckedIOException(exception);
                        }
                    });

        } catch (IOException | UncheckedIOException exception) {
            originalException.addSuppressed(exception);
        }
    }
}