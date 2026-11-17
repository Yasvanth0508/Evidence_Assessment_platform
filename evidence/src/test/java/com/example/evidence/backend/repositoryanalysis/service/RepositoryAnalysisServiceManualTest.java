
package com.example.evidence.backend.repositoryanalysis.service;

import com.example.evidence.backend.repositoryanalysis.analyzer.*;
import com.example.evidence.backend.repositoryanalysis.model.*;
import com.example.evidence.backend.repositoryanalysis.scanner.DefaultProjectStructureScanner;

import java.nio.file.Path;

public class RepositoryAnalysisServiceManualTest {

    public static void main(String[] args) {

        Path projectPath = Path.of(
                "./temp-clone/evidence-platform/evidence"
        );

        try {
            // Create individual components
            DefaultRepositoryAnalysisService service =
                    new DefaultRepositoryAnalysisService(
                            new DefaultProjectStructureScanner(),
                            new DefaultBuildConfigurationAnalyzer(),
                            new DefaultGradleConfigurationAnalyzer(),
                            new DefaultJavaSourceAnalyzer(),
                            new DefaultApiEndpointAnalyzer()
                    );

            // Execute complete repository analysis
            RepositoryAnalysisResult result =
                    service.analyze(projectPath);

            // Print consolidated results
            System.out.println("\n========== REPOSITORY ANALYSIS ==========");

            System.out.println("\nPROJECT STRUCTURE");

            System.out.println("Directories: "
                    + result.projectStructure().directories().size());

            System.out.println("Java Files: "
                    + result.projectStructure().javaFiles().size());

            System.out.println("\nBUILD CONFIGURATION");

            System.out.println("Build Tool: "
                    + result.buildConfiguration().buildTool());

            System.out.println("Java Version: "
                    + result.buildConfiguration().javaVersion());

            System.out.println("Spring Boot Version: "
                    + result.buildConfiguration().springBootVersion());

            System.out.println("Dependencies: "
                    + result.buildConfiguration().dependencies().size());

            System.out.println("Plugins: "
                    + result.buildConfiguration().plugins().size());

            System.out.println("\nJAVA SOURCE ANALYSIS");

            System.out.println("Declarations: "
                    + result.javaClasses().size());

            System.out.println("\nAPI ENDPOINT ANALYSIS");

            System.out.println("Endpoints: "
                    + result.apiEndpoints().size());

            for (ApiEndpointInfo endpoint : result.apiEndpoints()) {

                System.out.println(
                        endpoint.httpMethod() + " "
                                + endpoint.path()
                                + " -> " + endpoint.controllerName()
                                + "." + endpoint.methodName()
                );
            }

            System.out.println("\n========== ANALYSIS COMPLETED ==========");

        } catch (Exception exception) {

            System.err.println("Analysis failed: " + exception.getMessage());
            exception.printStackTrace();
        }
    }
}
