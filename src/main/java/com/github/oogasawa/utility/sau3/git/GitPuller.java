package com.github.oogasawa.utility.sau3.git;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class GitPuller {

     private static final Logger logger = LoggerFactory.getLogger(GitPuller.class);


    public void pull(Path baseDir) {
        try {

            Files.list(baseDir).sorted().filter((p) -> {
                return p.toFile().isDirectory();
            }).forEach((p) -> {

                Process process = null;
                String targetDir = p.toAbsolutePath().toString();
                System.out.println("## " + targetDir);
                
                try {
                    ProcessBuilder builder = new ProcessBuilder("git", "pull");
                    Map<String, String> env = builder.environment();
                    env.put("LANG", "en_US.UTF-8");

                    process = builder
                        .directory(p.toFile())
                        .inheritIO()
                        .start();

                    process.waitFor();

                } catch (IOException e) {
                    logger.error("IOException occurs in the directory: " + p.toString(), e);
                } catch (InterruptedException e) {
                    logger.warn("Interrupted", e);
                }

            });

        } catch (IOException e) {
            logger.error("IOException occurs while accessing: " + baseDir.toString(), e);
        }
    }

    
    
}
