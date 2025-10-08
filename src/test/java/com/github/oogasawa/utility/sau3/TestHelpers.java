package com.github.oogasawa.utility.sau3;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Helper utilities for Docusaurus deployment tests.
 *
 * This class provides common functionality used across multiple test classes:
 * - File operations (copy, delete)
 * - Dependency installation
 * - Environment checks
 * - Environment variable manipulation (for testing)
 */
public class TestHelpers {

    private static final Logger logger = Logger.getLogger(TestHelpers.class.getName());

    /**
     * Copy test Docusaurus project template from resources to destination directory.
     *
     * @param dest Destination path where the project should be copied
     * @throws IOException if copy operation fails
     */
    public static void copyTestProject(Path dest) throws IOException {
        Path templateDir = Paths.get("src/test/resources/test-docusaurus-project");

        if (!Files.exists(templateDir)) {
            throw new IOException("Test project template not found: " + templateDir);
        }

        // Copy directory recursively
        try (Stream<Path> walk = Files.walk(templateDir)) {
            walk.forEach(source -> {
                try {
                    Path target = dest.resolve(templateDir.relativize(source));
                    if (Files.isDirectory(source)) {
                        Files.createDirectories(target);
                    } else {
                        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException e) {
                    throw new RuntimeException("Failed to copy: " + source, e);
                }
            });
        }

        logger.info("Test project copied to: " + dest);
    }

    /**
     * Delete directory recursively.
     *
     * @param path Path to delete
     * @throws IOException if deletion fails
     */
    public static void deleteRecursively(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }

        try (Stream<Path> walk = Files.walk(path)) {
            walk.sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
        }

        logger.fine("Deleted recursively: " + path);
    }

    /**
     * Copy directory recursively.
     *
     * @param source Source directory
     * @param target Target directory
     * @throws IOException if copy fails
     */
    public static void copyDirectory(Path source, Path target) throws IOException {
        if (!Files.exists(source)) {
            throw new IOException("Source directory does not exist: " + source);
        }

        try (Stream<Path> walk = Files.walk(source)) {
            walk.forEach(src -> {
                try {
                    Path dest = target.resolve(source.relativize(src));
                    if (Files.isDirectory(src)) {
                        Files.createDirectories(dest);
                    } else {
                        Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException e) {
                    throw new RuntimeException("Failed to copy: " + src, e);
                }
            });
        }

        logger.fine("Copied directory from " + source + " to " + target);
    }

    /**
     * Check if yarn is available in the system.
     *
     * @return true if yarn command is available, false otherwise
     */
    public static boolean isYarnAvailable() {
        try {
            ProcessBuilder pb = new ProcessBuilder("yarn", "--version");
            Process process = pb.start();
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            boolean available = finished && process.exitValue() == 0;

            if (available) {
                logger.info("Yarn is available");
            } else {
                logger.warning("Yarn is not available");
            }

            return available;
        } catch (Exception e) {
            logger.warning("Failed to check yarn availability: " + e.getMessage());
            return false;
        }
    }

    /**
     * Check if npm is available in the system.
     *
     * @return true if npm command is available, false otherwise
     */
    public static boolean isNpmAvailable() {
        try {
            ProcessBuilder pb = new ProcessBuilder("npm", "--version");
            Process process = pb.start();
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            boolean available = finished && process.exitValue() == 0;

            if (available) {
                logger.info("npm is available");
            } else {
                logger.warning("npm is not available");
            }

            return available;
        } catch (Exception e) {
            logger.warning("Failed to check npm availability: " + e.getMessage());
            return false;
        }
    }

    /**
     * Check if Docker is available and running.
     *
     * @return true if Docker is available, false otherwise
     */
    public static boolean isDockerAvailable() {
        try {
            ProcessBuilder pb = new ProcessBuilder("docker", "info");
            Process process = pb.start();
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            boolean available = finished && process.exitValue() == 0;

            if (available) {
                logger.info("Docker is available");
            } else {
                logger.warning("Docker is not available");
            }

            return available;
        } catch (Exception e) {
            logger.warning("Failed to check Docker availability: " + e.getMessage());
            return false;
        }
    }

    /**
     * Install dependencies for a Docusaurus project using yarn.
     *
     * @param projectDir Path to the Docusaurus project directory
     * @throws IOException if installation fails
     * @throws InterruptedException if installation is interrupted
     */
    public static void installDependencies(Path projectDir) throws IOException, InterruptedException {
        logger.info("Installing dependencies for project: " + projectDir);

        if (!Files.exists(projectDir.resolve("package.json"))) {
            throw new IOException("package.json not found in: " + projectDir);
        }

        ProcessBuilder pb = new ProcessBuilder("yarn", "install", "--prefer-offline", "--silent");
        pb.directory(projectDir.toFile());
        pb.redirectErrorStream(true);

        Process process = pb.start();

        // Capture output
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                logger.fine("yarn: " + line);
            }
        }

        boolean finished = process.waitFor(10, TimeUnit.MINUTES);

        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("Yarn install timed out after 10 minutes");
        }

        if (process.exitValue() != 0) {
            logger.severe("Yarn install output:\n" + output.toString());
            throw new RuntimeException("Yarn install failed with exit code: " + process.exitValue());
        }

        logger.info("Dependencies installed successfully");
    }

    /**
     * Read the content of docusaurus.config.js file.
     *
     * @param projectDir Path to the Docusaurus project directory
     * @return Content of the config file
     * @throws IOException if reading fails
     */
    public static String readDocusaurusConfig(Path projectDir) throws IOException {
        Path configFile = projectDir.resolve("docusaurus.config.js");
        if (!Files.exists(configFile)) {
            configFile = projectDir.resolve("docusaurus.config.ts");
        }

        if (!Files.exists(configFile)) {
            throw new IOException("Docusaurus config file not found in: " + projectDir);
        }

        return Files.readString(configFile);
    }

    /**
     * Verify that baseUrl was updated in docusaurus.config.js.
     *
     * @param projectDir Path to the Docusaurus project directory
     * @param expectedBaseUrl Expected baseUrl value
     * @return true if baseUrl matches, false otherwise
     * @throws IOException if reading config fails
     */
    public static boolean verifyBaseUrl(Path projectDir, String expectedBaseUrl) throws IOException {
        String configContent = readDocusaurusConfig(projectDir);
        String expectedLine = "baseUrl: '" + expectedBaseUrl + "'";

        boolean verified = configContent.contains(expectedLine);

        if (verified) {
            logger.info("baseUrl verified: " + expectedBaseUrl);
        } else {
            logger.warning("baseUrl verification failed. Expected: " + expectedBaseUrl);
            logger.warning("Config content:\n" + configContent);
        }

        return verified;
    }

    /**
     * Verify that URL was updated in docusaurus.config.js.
     *
     * @param projectDir Path to the Docusaurus project directory
     * @param expectedUrl Expected URL value
     * @return true if URL matches, false otherwise
     * @throws IOException if reading config fails
     */
    public static boolean verifyUrl(Path projectDir, String expectedUrl) throws IOException {
        String configContent = readDocusaurusConfig(projectDir);
        String expectedLine = "url: '" + expectedUrl + "'";

        boolean verified = configContent.contains(expectedLine);

        if (verified) {
            logger.info("URL verified: " + expectedUrl);
        } else {
            logger.warning("URL verification failed. Expected: " + expectedUrl);
        }

        return verified;
    }

    /**
     * Execute a shell command and return the output.
     *
     * @param command Command to execute
     * @param workingDir Working directory for the command
     * @param timeoutSeconds Timeout in seconds
     * @return Command output
     * @throws IOException if execution fails
     * @throws InterruptedException if execution is interrupted
     */
    public static String executeCommand(String[] command, Path workingDir, int timeoutSeconds)
            throws IOException, InterruptedException {

        ProcessBuilder pb = new ProcessBuilder(command);
        if (workingDir != null) {
            pb.directory(workingDir.toFile());
        }
        pb.redirectErrorStream(true);

        Process process = pb.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);

        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("Command timed out after " + timeoutSeconds + " seconds");
        }

        return output.toString();
    }

    /**
     * Create a minimal test Docusaurus project for testing error handling.
     *
     * @param projectDir Directory where the test project should be created
     * @throws IOException if creation fails
     */
    public static void createBrokenTestProject(Path projectDir) throws IOException {
        Files.createDirectories(projectDir);

        // Create a minimal broken package.json
        Path packageJson = projectDir.resolve("package.json");
        Files.writeString(packageJson, """
            {
              "name": "broken-test-project",
              "version": "0.0.1",
              "scripts": {
                "build": "exit 1"
              }
            }
            """);

        // Create a broken config file
        Path configFile = projectDir.resolve("docusaurus.config.js");
        Files.writeString(configFile, "module.exports = { invalid: syntax");

        logger.info("Created broken test project at: " + projectDir);
    }

    /**
     * Verify that a deployment directory contains expected files.
     *
     * @param deployDir Deployment directory to check
     * @return true if basic Docusaurus files exist, false otherwise
     */
    public static boolean verifyDeployment(Path deployDir) {
        boolean indexExists = Files.exists(deployDir.resolve("index.html"));
        boolean assetsExists = Files.exists(deployDir.resolve("assets"));

        boolean verified = indexExists;

        if (verified) {
            logger.info("Deployment verified at: " + deployDir);
        } else {
            logger.warning("Deployment verification failed at: " + deployDir);
            logger.warning("index.html exists: " + indexExists);
            logger.warning("assets exists: " + assetsExists);
        }

        return verified;
    }

    /**
     * Print environment information for debugging.
     */
    public static void printEnvironmentInfo() {
        logger.info("=== Environment Information ===");
        logger.info("Java version: " + System.getProperty("java.version"));
        logger.info("OS: " + System.getProperty("os.name") + " " + System.getProperty("os.version"));
        logger.info("User: " + System.getProperty("user.name"));
        logger.info("Working directory: " + System.getProperty("user.dir"));
        logger.info("Yarn available: " + isYarnAvailable());
        logger.info("npm available: " + isNpmAvailable());
        logger.info("Docker available: " + isDockerAvailable());
        logger.info("==============================");
    }

    /**
     * Set an environment variable for testing purposes.
     *
     * This uses reflection to modify the environment variables map.
     * This is a hack and should only be used in tests.
     *
     * @param key Environment variable name
     * @param value Environment variable value
     */
    public static void setEnvironmentVariable(String key, String value) {
        try {
            Class<?> processEnvironmentClass = Class.forName("java.lang.ProcessEnvironment");
            Field theEnvironmentField = processEnvironmentClass.getDeclaredField("theEnvironment");
            theEnvironmentField.setAccessible(true);

            @SuppressWarnings("unchecked")
            Map<String, String> env = (Map<String, String>) theEnvironmentField.get(null);
            env.put(key, value);

            Field theCaseInsensitiveEnvironmentField = processEnvironmentClass.getDeclaredField("theCaseInsensitiveEnvironment");
            theCaseInsensitiveEnvironmentField.setAccessible(true);

            @SuppressWarnings("unchecked")
            Map<String, String> cienv = (Map<String, String>) theCaseInsensitiveEnvironmentField.get(null);
            cienv.put(key, value);

            logger.info("Set environment variable: " + key + "=" + value);
        } catch (Exception e) {
            logger.warning("Failed to set environment variable: " + e.getMessage());
            // Try alternative method for newer Java versions
            try {
                Map<String, String> env = new HashMap<>(System.getenv());
                env.put(key, value);
                Collections.addAll(env.keySet());
            } catch (Exception e2) {
                logger.warning("Alternative method also failed: " + e2.getMessage());
            }
        }
    }
}
