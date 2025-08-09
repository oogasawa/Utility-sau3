package com.github.oogasawa.utility.sau3.git;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class GitStatusChecker {

    private static final Logger logger = Logger.getLogger(GitStatusChecker.class.getName());

    public void check(Path baseDir) {
        try {

            Pattern successPattern = Pattern.compile("nothing to commit, working tree clean");
            
            Files.list(baseDir).sorted()
                .filter((Path p) -> p.toFile().isDirectory())
                .forEach((Path p) -> {
                    //logger.info(String.format("processing directory: %s", p.getFileName().toString()));
                    try {
                        ProcessBuilder builder = new ProcessBuilder("git", "status");
                        Map<String, String> env = builder.environment();
                        env.put("LANG", "en_US.UTF-8");

                        Process process = builder.directory(p.toFile()).start();

                        process.waitFor();

                        String stdoutStr = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

                        Matcher m = successPattern.matcher(stdoutStr);
                        if (m.find()) {
                            System.out.println(String.format("%s ... ok", p.getFileName().toString()));
                        }
                        else {
                            System.out.println(String.format("%s ... updated", p.getFileName().toString()));
                        }

                    } catch (IOException e) {
                        System.out.println(" ... not pushed");
                    } catch (InterruptedException e) {
                        logger.log(Level.WARNING, "Interrupted", e);
                    }

            });

        } catch (IOException e) {
            logger.log(Level.SEVERE, "IOException occurs while accessing: " + baseDir.toString(), e);
        }
    }

}
