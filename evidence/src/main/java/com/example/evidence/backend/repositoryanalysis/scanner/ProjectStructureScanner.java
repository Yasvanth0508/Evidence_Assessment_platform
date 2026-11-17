package com.example.evidence.backend.repositoryanalysis.scanner;

import com.example.evidence.backend.repositoryanalysis.model.ProjectStructure;

import java.nio.file.Path;

public interface ProjectStructureScanner {

    ProjectStructure scan(Path backendRootPath);

}