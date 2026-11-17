
package com.example.evidence.backend.repositoryanalysis.analyzer;

import com.example.evidence.backend.repositoryanalysis.model.BuildConfiguration;
import com.example.evidence.backend.repositoryanalysis.model.DependencyInfo;

import java.nio.file.Path;

public class BuildConfigurationAnalyzerManualTest {

    public static void main(String[] args) {

        // Change this path to your cloned repository's backend directory.
        Path projectPath = Path.of("./temp-clone/evidence-platform/evidence");

        System.out.println("=================================");
        System.out.println(" BUILD CONFIGURATION ANALYSIS");
        System.out.println("=================================");

        try {
            BuildConfiguration configuration;

            if (projectPath.resolve("pom.xml").toFile().isFile()) {

                System.out.println("\nUsing Maven Analyzer...");

                DefaultBuildConfigurationAnalyzer analyzer =
                        new DefaultBuildConfigurationAnalyzer();

                configuration = analyzer.analyze(projectPath);

            } else if (
                    projectPath.resolve("build.gradle").toFile().isFile()
                    || projectPath.resolve("build.gradle.kts").toFile().isFile()
            ) {

                System.out.println("\nUsing Gradle Analyzer...");

                DefaultGradleConfigurationAnalyzer analyzer =
                        new DefaultGradleConfigurationAnalyzer();

                configuration = analyzer.analyze(projectPath);

            } else {
                throw new IllegalStateException(
                        "No Maven or Gradle build file found."
                );
            }

            printConfiguration(configuration);

        } catch (Exception exception) {

            System.err.println("\nAnalysis failed!");
            System.err.println("Reason: " + exception.getMessage());

            exception.printStackTrace();
        }
    }

    private static void printConfiguration(
            BuildConfiguration configuration
    ) {

        System.out.println("\n========== BUILD DETAILS ==========");

        System.out.println("Build Tool       : " +
                configuration.buildTool());

        System.out.println("Java Version     : " +
                configuration.javaVersion());

        System.out.println("Spring Boot      : " +
                configuration.springBootVersion());

        System.out.println("\n========== DEPENDENCIES ==========");

        for (DependencyInfo dependency :
                configuration.dependencies()) {

            System.out.println("--------------------------------");
            System.out.println("Group ID    : " + dependency.groupId());
            System.out.println("Artifact ID : " + dependency.artifactId());
            System.out.println("Version     : " + dependency.version());
            System.out.println("Scope       : " + dependency.scope());
        }

        System.out.println("\n========== PLUGINS ==========");

        for (String plugin : configuration.plugins()) {
            System.out.println("- " + plugin);
        }

        System.out.println("\n========== ANALYSIS COMPLETE ==========");
    }
}
