package com.example.evidence.backend.repositoryanalysis.git;

import com.example.evidence.backend.repositoryanalysis.model.RepositoryCloneResult;

import java.nio.file.Path;

public class RepositoryClonerManualTest {

    public static void main(String[] args) {

        JGitRepositoryCloner cloner =
                new JGitRepositoryCloner();

        RepositoryCloneResult result =
                cloner.cloneRepository(
                        "https://github.com/Yasvanth0508/Evidence_Assessment_platform.git",
                        "main",
                        Path.of("./temp-clone/evidence-platform")
                );

        System.out.println("Clone successful!");
        System.out.println("Path: " + result.repositoryLocalPath());
        System.out.println("Branch: " + result.branch());
        System.out.println("Commit SHA: " + result.commitSha());
    }
}