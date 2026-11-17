
package com.example.evidence.backend.repositoryanalysis.analyzer;

import com.example.evidence.backend.repositoryanalysis.model.ApiEndpointInfo;
import com.example.evidence.backend.repositoryanalysis.model.ApiParameterInfo;
import com.example.evidence.backend.repositoryanalysis.model.JavaClassInfo;
import com.example.evidence.backend.repositoryanalysis.model.ProjectStructure;
import com.example.evidence.backend.repositoryanalysis.scanner.DefaultProjectStructureScanner;

import java.nio.file.Path;
import java.util.List;

public class ApiEndpointAnalyzerManualTest {

    public static void main(String[] args) {

        // Change this to your cloned Spring Boot project path.
        Path projectPath = Path.of("./temp-clone/evidence-platform/evidence`");

        try {
            // STEP 1: Scan project structure
            System.out.println("STEP 1: Scanning project...");

            DefaultProjectStructureScanner scanner =
                    new DefaultProjectStructureScanner();

            ProjectStructure structure = scanner.scan(projectPath);

            System.out.println(
                    "Java files discovered: " + structure.javaFiles().size()
            );

            // STEP 2: Analyze Java source files
            System.out.println("\nSTEP 2: Analyzing Java source...");

            DefaultJavaSourceAnalyzer javaAnalyzer =
                    new DefaultJavaSourceAnalyzer();

            List<JavaClassInfo> javaClasses = javaAnalyzer.analyze(
                    projectPath,
                    structure.javaFiles()
            );

            System.out.println(
                    "Java declarations extracted: " + javaClasses.size()
            );

            // STEP 3: Analyze REST endpoints
            System.out.println("\nSTEP 3: Analyzing API endpoints...");

            DefaultApiEndpointAnalyzer endpointAnalyzer =
                    new DefaultApiEndpointAnalyzer();

            List<ApiEndpointInfo> endpoints =
                    endpointAnalyzer.analyze(javaClasses);

            // STEP 4: Print endpoint information
            System.out.println("\n====================================");
            System.out.println("       API ENDPOINT ANALYSIS");
            System.out.println("====================================");

            for (ApiEndpointInfo endpoint : endpoints) {

                System.out.println("\n------------------------------------");

                System.out.println(
                        "Controller  : " + endpoint.controllerName()
                );

                System.out.println(
                        "Method Name : " + endpoint.methodName()
                );

                System.out.println(
                        "HTTP Method : " + endpoint.httpMethod()
                );

                System.out.println(
                        "Endpoint    : " + endpoint.path()
                );

                System.out.println(
                        "Return Type : " + endpoint.returnType()
                );

                System.out.println("\nParameters:");

                if (endpoint.parameters().isEmpty()) {
                    System.out.println("No parameters");
                }

                for (ApiParameterInfo parameter : endpoint.parameters()) {

                    System.out.println(
                            "  Name        : " + parameter.name()
                    );

                    System.out.println(
                            "  Type        : " + parameter.type()
                    );

                    System.out.println(
                            "  Annotations : " + parameter.annotations()
                    );

                    System.out.println(
                            "  Required    : " + parameter.required()
                    );

                    System.out.println();
                }
            }

            // STEP 5: Summary
            System.out.println("\n====================================");
            System.out.println("           ANALYSIS SUMMARY");
            System.out.println("====================================");

            System.out.println(
                    "Java declarations : " + javaClasses.size()
            );

            System.out.println(
                    "REST endpoints    : " + endpoints.size()
            );

            System.out.println("\nAnalysis completed successfully.");

        } catch (Exception exception) {

            System.err.println("\nAnalysis failed!");

            System.err.println(
                    "Reason: " + exception.getMessage()
            );

            exception.printStackTrace();
        }
    }
}
