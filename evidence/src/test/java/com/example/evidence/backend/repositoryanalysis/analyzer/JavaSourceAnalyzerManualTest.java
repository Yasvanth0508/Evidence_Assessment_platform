
package com.example.evidence.backend.repositoryanalysis.analyzer;

import com.example.evidence.backend.repositoryanalysis.model.JavaAnnotationInfo;
import com.example.evidence.backend.repositoryanalysis.model.JavaClassInfo;
import com.example.evidence.backend.repositoryanalysis.model.JavaFieldInfo;
import com.example.evidence.backend.repositoryanalysis.model.JavaMethodInfo;
import com.example.evidence.backend.repositoryanalysis.model.JavaParameterInfo;
import com.example.evidence.backend.repositoryanalysis.model.ProjectStructure;
import com.example.evidence.backend.repositoryanalysis.scanner.DefaultProjectStructureScanner;

import java.nio.file.Path;
import java.util.List;

public class JavaSourceAnalyzerManualTest {

    public static void main(String[] args) {

        Path projectPath = Path.of(
                "./temp-clone/evidence-platform/evidence"
        );

        try {
            // STEP 1: Discover Java files
            System.out.println("STEP 1: Scanning project...");

            DefaultProjectStructureScanner scanner =
                    new DefaultProjectStructureScanner();

            ProjectStructure structure = scanner.scan(projectPath);

            System.out.println(
                    "Java files discovered: " + structure.javaFiles().size()
            );

            // STEP 2: Analyze Java source files
            System.out.println("\nSTEP 2: Analyzing Java source...");

            DefaultJavaSourceAnalyzer analyzer =
                    new DefaultJavaSourceAnalyzer();

            List<JavaClassInfo> classes = analyzer.analyze(
                    projectPath,
                    structure.javaFiles()
            );

            // STEP 3: Print extracted information
            System.out.println("\n================================");
            System.out.println("       JAVA SOURCE ANALYSIS");
            System.out.println("================================");

            for (JavaClassInfo classInfo : classes) {

                System.out.println("\n--------------------------------");
                System.out.println("Class       : " + classInfo.className());
                System.out.println("Package     : " + classInfo.packageName());
                System.out.println("Type        : " + classInfo.declarationType());
                System.out.println("Access      : " + classInfo.accessModifier());
                System.out.println("Parent      : " + classInfo.parentClass());
                System.out.println("Interfaces  : " +
                        classInfo.implementedInterfaces());
                System.out.println("Source      : " + classInfo.sourceFile());

                // Class annotations
                System.out.println("\nClass Annotations:");

                printAnnotations(classInfo.annotations());

                // Fields
                System.out.println("\nFields:");

                for (JavaFieldInfo field : classInfo.fields()) {

                    System.out.println(
                            "  " + field.accessModifier()
                                    + " " + field.type()
                                    + " " + field.name()
                    );

                    System.out.println(
                            "    Static      : " + field.isStatic()
                    );

                    System.out.println(
                            "    Final       : " + field.isFinal()
                    );

                    System.out.println(
                            "    Annotations : " + field.annotations()
                    );
                }

                // Methods
                System.out.println("\nMethods:");

                for (JavaMethodInfo method : classInfo.methods()) {

                    System.out.println(
                            "  " + method.accessModifier()
                                    + " " + method.returnType()
                                    + " " + method.name()
                                    + "(" + formatParameters(method.parameters()) + ")"
                    );

                    System.out.println(
                            "    Static      : " + method.isStatic()
                    );

                    System.out.println("    Annotations:");

                    printAnnotations(method.annotations());

                    // Method parameters
                    if (!method.parameters().isEmpty()) {

                        System.out.println("    Parameters:");

                        for (JavaParameterInfo parameter :
                                method.parameters()) {

                            System.out.println(
                                    "      " + parameter.type()
                                            + " " + parameter.name()
                            );

                            System.out.println(
                                    "        Annotations: "
                                            + parameter.annotations()
                            );
                        }
                    }
                }
            }

            // STEP 4: Summary
            System.out.println("\n================================");
            System.out.println("         ANALYSIS SUMMARY");
            System.out.println("================================");

            System.out.println(
                    "Java files scanned : " + structure.javaFiles().size()
            );

            System.out.println(
                    "Declarations found : " + classes.size()
            );

            System.out.println("\nJava source analysis completed.");

        } catch (Exception exception) {

            System.err.println("\nAnalysis failed!");

            System.err.println(
                    "Reason: " + exception.getMessage()
            );

            exception.printStackTrace();
        }
    }

    private static String formatParameters(
            List<JavaParameterInfo> parameters
    ) {

        return parameters.stream()
                .map(parameter ->
                        parameter.type() + " " + parameter.name()
                )
                .collect(java.util.stream.Collectors.joining(", "));
    }

    private static void printAnnotations(
            List<JavaAnnotationInfo> annotations
    ) {

        if (annotations.isEmpty()) {
            System.out.println("  No annotations");
            return;
        }

        for (JavaAnnotationInfo annotation : annotations) {

            System.out.println(
                    "  @" + annotation.name()
                            + " " + annotation.attributes()
            );
        }
    }
}
