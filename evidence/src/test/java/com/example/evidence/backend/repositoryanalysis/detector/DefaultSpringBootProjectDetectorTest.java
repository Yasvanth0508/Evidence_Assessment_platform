package com.example.evidence.backend.repositoryanalysis.detector;

import com.example.evidence.backend.repositoryanalysis.exception.InvalidBackendProjectException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("DefaultSpringBootProjectDetector Unit Tests")
class DefaultSpringBootProjectDetectorTest {

    private DefaultSpringBootProjectDetector detector;

    @BeforeEach
    void setUp() {
        detector = new DefaultSpringBootProjectDetector();
    }

    @Nested
    @DisplayName("Directory Validation Tests")
    class DirectoryValidationTests {

        @Test
        @DisplayName("Should throw exception when backend root path is null")
        void detect_nullPath_throwsException() {
            assertThatThrownBy(() -> detector.detect(null))
                    .isInstanceOf(InvalidBackendProjectException.class)
                    .hasMessage("Backend project directory does not exist");
        }

        @Test
        @DisplayName("Should throw exception when backend root path does not exist")
        void detect_nonExistentPath_throwsException(@TempDir Path tempDir) {
            Path nonExistent = tempDir.resolve("nonexistent");

            assertThatThrownBy(() -> detector.detect(nonExistent))
                    .isInstanceOf(InvalidBackendProjectException.class)
                    .hasMessage("Backend project directory does not exist");
        }

        @Test
        @DisplayName("Should throw exception when backend root path is a file")
        void detect_pathIsFile_throwsException(@TempDir Path tempDir) throws IOException {
            Path file = Files.writeString(tempDir.resolve("file.txt"), "hello");

            assertThatThrownBy(() -> detector.detect(file))
                    .isInstanceOf(InvalidBackendProjectException.class)
                    .hasMessage("Backend project directory does not exist");
        }
    }

    @Nested
    @DisplayName("Build Configuration Detection Tests")
    class BuildConfigTests {

        @Test
        @DisplayName("Should throw exception when no build file exists")
        void detect_noBuildConfiguration_throwsException(@TempDir Path tempDir) {
            assertThatThrownBy(() -> detector.detect(tempDir))
                    .isInstanceOf(InvalidBackendProjectException.class)
                    .hasMessage("No Maven or Gradle build configuration found");
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   ", "\n"})
        @DisplayName("Should throw exception when pom.xml is empty or whitespace")
        void detect_emptyPomXml_throwsException(String emptyContent, @TempDir Path tempDir) throws IOException {
            Files.writeString(tempDir.resolve("pom.xml"), emptyContent);

            assertThatThrownBy(() -> detector.detect(tempDir))
                    .isInstanceOf(InvalidBackendProjectException.class)
                    .hasMessage("Spring Boot dependency or plugin not found");
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   ", "\n"})
        @DisplayName("Should throw exception when build.gradle is empty or whitespace")
        void detect_emptyBuildGradle_throwsException(String emptyContent, @TempDir Path tempDir) throws IOException {
            Files.writeString(tempDir.resolve("build.gradle"), emptyContent);

            assertThatThrownBy(() -> detector.detect(tempDir))
                    .isInstanceOf(InvalidBackendProjectException.class)
                    .hasMessage("Spring Boot dependency or plugin not found");
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   ", "\n"})
        @DisplayName("Should throw exception when build.gradle.kts is empty or whitespace")
        void detect_emptyBuildGradleKts_throwsException(String emptyContent, @TempDir Path tempDir) throws IOException {
            Files.writeString(tempDir.resolve("build.gradle.kts"), emptyContent);

            assertThatThrownBy(() -> detector.detect(tempDir))
                    .isInstanceOf(InvalidBackendProjectException.class)
                    .hasMessage("Spring Boot dependency or plugin not found");
        }

        @Test
        @DisplayName("Should throw exception when Maven pom.xml lacks Spring Boot")
        void detect_mavenWithoutSpringBoot_throwsException(@TempDir Path tempDir) throws IOException {
            Files.writeString(tempDir.resolve("pom.xml"), """
                    <project>
                        <groupId>com.example</groupId>
                        <artifactId>plain-java</artifactId>
                        <version>1.0.0</version>
                    </project>
                    """);

            assertThatThrownBy(() -> detector.detect(tempDir))
                    .isInstanceOf(InvalidBackendProjectException.class)
                    .hasMessage("Spring Boot dependency or plugin not found");
        }

        @Test
        @DisplayName("Should throw exception when Gradle build.gradle lacks Spring Boot")
        void detect_gradleWithoutSpringBoot_throwsException(@TempDir Path tempDir) throws IOException {
            Files.writeString(tempDir.resolve("build.gradle"), """
                    plugins {
                        id 'java'
                    }
                    dependencies {
                        implementation 'com.google.guava:guava:33.0.0-jre'
                    }
                    """);

            assertThatThrownBy(() -> detector.detect(tempDir))
                    .isInstanceOf(InvalidBackendProjectException.class)
                    .hasMessage("Spring Boot dependency or plugin not found");
        }
    }

    @Nested
    @DisplayName("Source Directory and File Verification Tests")
    class SourceVerificationTests {

        @Test
        @DisplayName("Should throw exception when src/main/java directory does not exist")
        void detect_missingJavaSourceDirectory_throwsException(@TempDir Path tempDir) throws IOException {
            Files.writeString(tempDir.resolve("pom.xml"), """
                    <project>
                        <dependencies>
                            <dependency>
                                <groupId>org.springframework.boot</groupId>
                                <artifactId>spring-boot-starter</artifactId>
                            </dependency>
                        </dependencies>
                    </project>
                    """);

            assertThatThrownBy(() -> detector.detect(tempDir))
                    .isInstanceOf(InvalidBackendProjectException.class)
                    .hasMessage("Java source directory not found");
        }

        @Test
        @DisplayName("Should throw exception when src/main/java contains no files")
        void detect_emptyJavaSourceDirectory_throwsException(@TempDir Path tempDir) throws IOException {
            Files.writeString(tempDir.resolve("pom.xml"), """
                    <project>
                        <dependencies>
                            <dependency>
                                <groupId>org.springframework.boot</groupId>
                                <artifactId>spring-boot-starter</artifactId>
                            </dependency>
                        </dependencies>
                    </project>
                    """);
            Files.createDirectories(tempDir.resolve("src/main/java"));

            assertThatThrownBy(() -> detector.detect(tempDir))
                    .isInstanceOf(InvalidBackendProjectException.class)
                    .hasMessage("No Java source files found");
        }

        @Test
        @DisplayName("Should throw exception when src/main/java contains only non-Java files")
        void detect_nonJavaFilesOnly_throwsException(@TempDir Path tempDir) throws IOException {
            Files.writeString(tempDir.resolve("pom.xml"), """
                    <project>
                        <dependencies>
                            <dependency>
                                <groupId>org.springframework.boot</groupId>
                                <artifactId>spring-boot-starter</artifactId>
                            </dependency>
                        </dependencies>
                    </project>
                    """);
            Path javaDir = Files.createDirectories(tempDir.resolve("src/main/java"));
            Files.writeString(javaDir.resolve("README.md"), "# Not a java file");
            Files.writeString(javaDir.resolve("application.properties"), "key=value");

            assertThatThrownBy(() -> detector.detect(tempDir))
                    .isInstanceOf(InvalidBackendProjectException.class)
                    .hasMessage("No Java source files found");
        }

        @Test
        @DisplayName("Should throw exception when Java files exist but none contain @SpringBootApplication")
        void detect_missingSpringBootApplicationAnnotation_throwsException(@TempDir Path tempDir) throws IOException {
            Files.writeString(tempDir.resolve("pom.xml"), """
                    <project>
                        <dependencies>
                            <dependency>
                                <groupId>org.springframework.boot</groupId>
                                <artifactId>spring-boot-starter</artifactId>
                            </dependency>
                        </dependencies>
                    </project>
                    """);
            Path javaDir = Files.createDirectories(tempDir.resolve("src/main/java/com/example"));
            Files.writeString(javaDir.resolve("UserService.java"), """
                    package com.example;
                    public class UserService {
                        public void doSomething() {}
                    }
                    """);

            assertThatThrownBy(() -> detector.detect(tempDir))
                    .isInstanceOf(InvalidBackendProjectException.class)
                    .hasMessage("Spring Boot application entry point not found");
        }
    }

    @Nested
    @DisplayName("Successful Detection Tests")
    class SuccessfulDetectionTests {

        @Test
        @DisplayName("Should successfully detect valid Maven Spring Boot project with spring-boot-starter")
        void detect_validMavenWithStarter_succeeds(@TempDir Path tempDir) throws IOException {
            Files.writeString(tempDir.resolve("pom.xml"), """
                    <project>
                        <dependencies>
                            <dependency>
                                <groupId>org.springframework.boot</groupId>
                                <artifactId>spring-boot-starter-web</artifactId>
                            </dependency>
                        </dependencies>
                    </project>
                    """);
            Path javaDir = Files.createDirectories(tempDir.resolve("src/main/java/com/example/demo"));
            Files.writeString(javaDir.resolve("DemoApplication.java"), """
                    package com.example.demo;
                    import org.springframework.boot.autoconfigure.SpringBootApplication;
                    
                    @SpringBootApplication
                    public class DemoApplication {}
                    """);

            assertThatCode(() -> detector.detect(tempDir))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should successfully detect valid Maven Spring Boot project with spring-boot-maven-plugin")
        void detect_validMavenWithPlugin_succeeds(@TempDir Path tempDir) throws IOException {
            Files.writeString(tempDir.resolve("pom.xml"), """
                    <project>
                        <build>
                            <plugins>
                                <plugin>
                                    <groupId>org.springframework.boot</groupId>
                                    <artifactId>spring-boot-maven-plugin</artifactId>
                                </plugin>
                            </plugins>
                        </build>
                    </project>
                    """);
            Path javaDir = Files.createDirectories(tempDir.resolve("src/main/java/com/example/demo"));
            Files.writeString(javaDir.resolve("DemoApplication.java"), """
                    package com.example.demo;
                    @SpringBootApplication
                    public class DemoApplication {}
                    """);

            assertThatCode(() -> detector.detect(tempDir))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should successfully detect valid Maven Spring Boot project with spring-boot-dependencies")
        void detect_validMavenWithDependencies_succeeds(@TempDir Path tempDir) throws IOException {
            Files.writeString(tempDir.resolve("pom.xml"), """
                    <project>
                        <dependencyManagement>
                            <dependencies>
                                <dependency>
                                    <groupId>org.springframework.boot</groupId>
                                    <artifactId>spring-boot-dependencies</artifactId>
                                    <type>pom</type>
                                    <scope>import</scope>
                                </dependency>
                            </dependencies>
                        </dependencyManagement>
                    </project>
                    """);
            Path javaDir = Files.createDirectories(tempDir.resolve("src/main/java/com/example/demo"));
            Files.writeString(javaDir.resolve("DemoApplication.java"), """
                    package com.example.demo;
                    @SpringBootApplication
                    public class DemoApplication {}
                    """);

            assertThatCode(() -> detector.detect(tempDir))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should successfully detect valid Gradle Groovy project with org.springframework.boot")
        void detect_validGradleGroovy_succeeds(@TempDir Path tempDir) throws IOException {
            Files.writeString(tempDir.resolve("build.gradle"), """
                    plugins {
                        id 'org.springframework.boot' version '3.2.0'
                    }
                    """);
            Path javaDir = Files.createDirectories(tempDir.resolve("src/main/java/com/example/demo"));
            Files.writeString(javaDir.resolve("DemoApplication.java"), """
                    package com.example.demo;
                    @SpringBootApplication
                    public class DemoApplication {}
                    """);

            assertThatCode(() -> detector.detect(tempDir))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should successfully detect valid Gradle Kotlin DSL project with org.springframework.boot")
        void detect_validGradleKotlinDsl_succeeds(@TempDir Path tempDir) throws IOException {
            Files.writeString(tempDir.resolve("build.gradle.kts"), """
                    plugins {
                        id("org.springframework.boot") version "3.2.0"
                    }
                    """);
            Path javaDir = Files.createDirectories(tempDir.resolve("src/main/java/com/example/demo"));
            Files.writeString(javaDir.resolve("DemoApplication.java"), """
                    package com.example.demo;
                    @SpringBootApplication
                    public class DemoApplication {}
                    """);

            assertThatCode(() -> detector.detect(tempDir))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should successfully detect valid Gradle project with spring-boot-starter")
        void detect_validGradleWithStarter_succeeds(@TempDir Path tempDir) throws IOException {
            Files.writeString(tempDir.resolve("build.gradle"), """
                    dependencies {
                        implementation 'org.springframework.boot:spring-boot-starter'
                    }
                    """);
            Path javaDir = Files.createDirectories(tempDir.resolve("src/main/java/com/example/demo"));
            Files.writeString(javaDir.resolve("DemoApplication.java"), """
                    package com.example.demo;
                    @SpringBootApplication
                    public class DemoApplication {}
                    """);

            assertThatCode(() -> detector.detect(tempDir))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should detect entry point in deeply nested Java source directories")
        void detect_deeplyNestedSourceDirectories_succeeds(@TempDir Path tempDir) throws IOException {
            Files.writeString(tempDir.resolve("pom.xml"), "<project>spring-boot-starter</project>");
            Path javaDir = Files.createDirectories(tempDir.resolve("src/main/java/a/b/c/d/e/f"));
            Files.writeString(javaDir.resolve("NestedApplication.java"), """
                    package a.b.c.d.e.f;
                    @SpringBootApplication
                    public class NestedApplication {}
                    """);

            assertThatCode(() -> detector.detect(tempDir))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should detect when multiple Java files exist and one has @SpringBootApplication")
        void detect_multipleJavaFiles_succeeds(@TempDir Path tempDir) throws IOException {
            Files.writeString(tempDir.resolve("pom.xml"), "<project>spring-boot-starter</project>");
            Path packageDir = Files.createDirectories(tempDir.resolve("src/main/java/com/example"));
            Files.writeString(packageDir.resolve("User.java"), "package com.example; public class User {}");
            Files.writeString(packageDir.resolve("UserRepository.java"), "package com.example; public interface UserRepository {}");
            Files.writeString(packageDir.resolve("App.java"), "package com.example; @SpringBootApplication public class App {}");

            assertThatCode(() -> detector.detect(tempDir))
                    .doesNotThrowAnyException();
        }
    }
}
