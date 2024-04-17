package com.github.oogasawa.utility.sau3.git;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GitStatusChecker {

     private static final Logger logger = LoggerFactory.getLogger(GitStatusChecker.class);
    
    public void check(Path baseDir) {
        try {

            Files.list(baseDir).sorted().filter((p) -> {
                return p.toFile().isDirectory();
            }).forEach((p) -> {
                Process process = null;
                String targetDir = null;
                Pattern successPattern = Pattern.compile("nothing to commit, working tree clean");
                try {
                    process = new ProcessBuilder("pwd").directory(p.toFile()).start();

                    process.waitFor();

                    targetDir = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
                    // System.out.print(targetDir);
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                    System.exit(-1);
                }

                try {
                    ProcessBuilder builder = new ProcessBuilder("git", "status");
                    Map<String, String> env = builder.environment();
                    env.put("LANG", "en_US.UTF-8");

                    process = builder.directory(p.toFile()).start();

                    process.waitFor();

                    String stdoutStr = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

                    Matcher m = successPattern.matcher(stdoutStr);
                    if (m.find()) {
                        System.out.println(String.format("%s ... ok", targetDir));
                    }
                    else {
                        System.out.println(String.format("%s ... updated", targetDir));
                    }

                } catch (IOException e) {
                    System.out.println(" ... not pushed");
                } catch (InterruptedException e) {
                    logger.warn("Interrupted", e);
                }

            });

        } catch (IOException e) {
            logger.error("IOException occurs while accessing: " + baseDir.toString(), e);
        }
    }

}
