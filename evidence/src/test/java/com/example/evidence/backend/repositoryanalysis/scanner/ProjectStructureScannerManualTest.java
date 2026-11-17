
package com.example.evidence.backend.repositoryanalysis.scanner;

import com.example.evidence.backend.repositoryanalysis.model.ProjectStructure;

import java.nio.file.Path;

public class ProjectStructureScannerManualTest {

    public static void main(String[] args) {

        // 1. Create scanner instance
        ProjectStructureScanner scanner =
                new DefaultProjectStructureScanner();

        // 2. Provide an existing cloned backend directory
        Path backendRoot = Path.of(
                "./temp-clone/evidence-platform/evidence"
        );

        // 3. Execute scanning
        ProjectStructure result = scanner.scan(backendRoot);

        // 4. Display project root
        System.out.println("\n========== PROJECT ROOT ==========");
        System.out.println(result.projectRoot());

        // 5. Display directories
        System.out.println("\n========== DIRECTORIES ==========");

        result.directories().forEach(
                System.out::println
        );

        // 6. Display Java files
        System.out.println("\n========== JAVA FILES ==========");

        result.javaFiles().forEach(
                System.out::println
        );

        // 7. Display build files
        System.out.println("\n========== BUILD FILES ==========");

        result.buildFiles().forEach(
                System.out::println
        );

        // 8. Display configuration files
        System.out.println("\n========== CONFIGURATION FILES ==========");

        result.configurationFiles().forEach(
                System.out::println
        );

        System.out.println("\n========== SCANNING COMPLETED ==========");
    }
}
