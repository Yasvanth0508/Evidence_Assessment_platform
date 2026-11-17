
package com.example.evidence.backend.repositoryanalysis.analyzer;

import com.example.evidence.backend.repositoryanalysis.exception.RepositoryAnalysisException;
import com.example.evidence.backend.repositoryanalysis.model.BuildConfiguration;
import com.example.evidence.backend.repositoryanalysis.model.DependencyInfo;

import org.springframework.stereotype.Component;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Component
public class DefaultBuildConfigurationAnalyzer
        implements BuildConfigurationAnalyzer {

    @Override
    public BuildConfiguration analyze(Path backendRootPath) {

        // 1. Validate backend directory
        if (backendRootPath == null ||
                !Files.isDirectory(backendRootPath)) {

            throw new RepositoryAnalysisException(
                    "Invalid backend project directory"
            );
        }

        // 2. Locate Maven configuration
        Path pomFile = backendRootPath.resolve("pom.xml");

        if (!Files.isRegularFile(pomFile)) {
            throw new RepositoryAnalysisException(
                    "Maven pom.xml not found"
            );
        }

        try {
            // 3. Create secure XML parser
            DocumentBuilderFactory factory =
                    createSecureFactory();

            DocumentBuilder builder =
                    factory.newDocumentBuilder();

            // 4. Parse pom.xml
            Document document =
                    builder.parse(pomFile.toFile());

            document.getDocumentElement().normalize();

            Element project = document.getDocumentElement();

            if (!"project".equals(localName(project))) {
                throw new RepositoryAnalysisException(
                        "Invalid Maven POM document"
                );
            }

            // 5. Extract Java version
            Element properties =
                    getDirectChild(project, "properties");

            String javaVersion = getChildText(
                    properties, "java.version"
            );

            if (javaVersion == null) {
                javaVersion = getChildText(
                        properties, "maven.compiler.release"
                );
            }

            if (javaVersion == null) {
                javaVersion = getChildText(
                        properties, "maven.compiler.source"
                );
            }

            // 6. Extract Spring Boot version
            String springBootVersion =
                    extractSpringBootVersion(project, properties);

            // 7. Extract dependencies
            List<DependencyInfo> dependencies =
                    extractDependencies(project);

            // 8. Extract Maven plugins
            List<String> plugins =
                    extractPlugins(project);

            // 9. Construct result
            return new BuildConfiguration(
                    "MAVEN",
                    javaVersion,
                    springBootVersion,
                    dependencies,
                    plugins
            );

        } catch (ParserConfigurationException |
                 SAXException |
                 IOException exception) {

            throw new RepositoryAnalysisException(
                    "Failed to parse Maven POM",
                    exception
            );
        }
    }

    private DocumentBuilderFactory createSecureFactory()
            throws ParserConfigurationException {

        DocumentBuilderFactory factory =
                DocumentBuilderFactory.newInstance();

        factory.setNamespaceAware(true);

        factory.setFeature(
                XMLConstants.FEATURE_SECURE_PROCESSING,
                true
        );

        factory.setFeature(
                "http://apache.org/xml/features/disallow-doctype-decl",
                true
        );

        factory.setFeature(
                "http://xml.org/sax/features/external-general-entities",
                false
        );

        factory.setFeature(
                "http://xml.org/sax/features/external-parameter-entities",
                false
        );

        factory.setFeature(
                "http://apache.org/xml/features/nonvalidating/load-external-dtd",
                false
        );

        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);

        factory.setAttribute(
                XMLConstants.ACCESS_EXTERNAL_DTD,
                ""
        );

        factory.setAttribute(
                XMLConstants.ACCESS_EXTERNAL_SCHEMA,
                ""
        );

        return factory;
    }

    private String extractSpringBootVersion(
            Element project,
            Element properties
    ) {

        // Check common Spring Boot version properties
        String version = getChildText(
                properties, "spring-boot.version"
        );

        if (version == null) {
            version = getChildText(
                    properties, "spring.boot.version"
            );
        }

        if (version != null) {
            return version;
        }

        // Check Spring Boot parent declaration
        Element parent = getDirectChild(project, "parent");

        if (parent != null) {

            String artifactId = getChildText(
                    parent, "artifactId"
            );

            if ("spring-boot-starter-parent".equals(artifactId)) {
                return getChildText(parent, "version");
            }
        }

        // Check Spring Boot Maven plugin version
        Element build = getDirectChild(project, "build");

        Element plugins = getDirectChild(build, "plugins");

        if (plugins != null) {

            for (Element plugin : getDirectChildren(
                    plugins, "plugin")) {

                String artifactId = getChildText(
                        plugin, "artifactId"
                );

                if ("spring-boot-maven-plugin".equals(artifactId)) {
                    return getChildText(plugin, "version");
                }
            }
        }

        return null;
    }

    private List<DependencyInfo> extractDependencies(
            Element project
    ) {

        List<DependencyInfo> dependencies =
                new ArrayList<>();

        Element dependenciesElement =
                getDirectChild(project, "dependencies");

        if (dependenciesElement == null) {
            return dependencies;
        }

        for (Element dependency :
                getDirectChildren(dependenciesElement, "dependency")) {

            String groupId = getChildText(
                    dependency, "groupId"
            );

            String artifactId = getChildText(
                    dependency, "artifactId"
            );

            String version = getChildText(
                    dependency, "version"
            );

            String scope = getChildText(
                    dependency, "scope"
            );

            if (scope == null) {
                scope = "compile";
            }

            dependencies.add(
                    new DependencyInfo(
                            groupId,
                            artifactId,
                            version,
                            scope
                    )
            );
        }

        return List.copyOf(dependencies);
    }

    private List<String> extractPlugins(Element project) {

        List<String> plugins = new ArrayList<>();

        Element build = getDirectChild(project, "build");

        Element pluginsElement =
                getDirectChild(build, "plugins");

        if (pluginsElement == null) {
            return plugins;
        }

        for (Element plugin :
                getDirectChildren(pluginsElement, "plugin")) {

            String groupId = getChildText(
                    plugin, "groupId"
            );

            String artifactId = getChildText(
                    plugin, "artifactId"
            );

            String version = getChildText(
                    plugin, "version"
            );

            if (artifactId != null) {

                if (groupId == null) {
                    groupId = "org.apache.maven.plugins";
                }

                String pluginInfo =
                        groupId + ":" + artifactId;

                if (version != null) {
                    pluginInfo += ":" + version;
                }

                plugins.add(pluginInfo);
            }
        }

        return List.copyOf(plugins);
    }

    // Returns a direct child element with the given name.
    private Element getDirectChild(
            Element parent,
            String name
    ) {

        if (parent == null) {
            return null;
        }

        for (Element child : getDirectChildren(parent, name)) {
            return child;
        }

        return null;
    }

    // Returns all direct child elements with the given name.
    private List<Element> getDirectChildren(
            Element parent,
            String name
    ) {

        List<Element> elements = new ArrayList<>();

        if (parent == null) {
            return elements;
        }

        NodeList children = parent.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {

            Node node = children.item(i);

            if (node instanceof Element element &&
                    name.equals(localName(element))) {

                elements.add(element);
            }
        }

        return elements;
    }

    // Handles XML elements with or without namespaces.
    private String localName(Element element) {

        return element.getLocalName() != null
                ? element.getLocalName()
                : element.getTagName();
    }

    // Extracts text from a direct child element.
    private String getChildText(
            Element parent,
            String childName
    ) {

        Element child = getDirectChild(parent, childName);

        if (child == null) {
            return null;
        }

        String value = child.getTextContent().trim();

        return value.isEmpty() ? null : value;
    }
}
