package com.example.evidence.backend.repositoryanalysis.model;

import java.util.List;

public record ProjectStructure(

        String projectRoot,

        List<String> directories,

        List<String> javaFiles,

        List<String> buildFiles,

        List<String> configurationFiles

) {}