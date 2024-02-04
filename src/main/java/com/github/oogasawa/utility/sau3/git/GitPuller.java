package com.github.oogasawa.utility.sau3.git;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GitPuller {

    private static final Logger logger = Logger.getLogger(GitPuller.class.getName());

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
                    logger.log(Level.SEVERE, "IOException occurs in the directory: " + p.toString(), e);
                } catch (InterruptedException e) {
                    logger.log(Level.WARNING, "Interrupted", e);
                }

            });

        } catch (IOException e) {
            logger.log(Level.SEVERE, "IOException occurs while accessing: " + baseDir.toString(), e);
        }
    }

    
    
}
