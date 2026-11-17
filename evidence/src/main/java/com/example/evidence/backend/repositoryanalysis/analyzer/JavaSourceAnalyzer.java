
package com.example.evidence.backend.repositoryanalysis.analyzer;

import com.example.evidence.backend.repositoryanalysis.model.JavaClassInfo;

import java.nio.file.Path;
import java.util.List;

public interface JavaSourceAnalyzer {

    List<JavaClassInfo> analyze(
            Path backendRootPath,
            List<String> javaFiles
    );
}
