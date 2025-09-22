package com.github.oogasawa.utility.sau3;


import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A utility class for building and deploying Docusaurus documentation projects
 * with intelligent output filtering.
 * 
 * <p><strong>Main Purpose:</strong><br>
 * This class is designed to reduce overhead caused by excessive console output
 * during Docusaurus build and development server startup processes (especially in
 * environments like Emacs eshell), by filtering and displaying only meaningful
 * messages such as errors, warnings, and launch URLs.</p>
 * 
 * <p>The class provides both high-level methods for batch processing multiple
 * Docusaurus projects and low-level primitives for individual build and deploy tasks.
 * It also includes a development server launcher with automatic port conflict resolution.</p>
 * 
 * <p>Expected usage includes:</p>
 * <ul>
 *   <li>Reducing screen rendering time during builds by suppressing verbose output</li>
 *   <li>Automating common tasks like build, deploy, and dev-server startup</li>
 *   <li>Batch processing multiple documentation directories</li>
 * </ul>
 * 
 * <p><strong>Dependencies:</strong></p>
 * <ul>
 *   <li>{@code yarn}</li>
 *   <li>{@code npx}</li>
 *   <li>{@link com.github.oogasawa.utility.process.ProcessFacade}</li>
 *   <li>{@code SLF4J} for logging</li>
 * </ul>
 * 
 * @author Osamu Ogasawara <osamu.ogasawara@gmail.com> 
 */
public class DocusaurusProcessor {

    //
    //  This class is composed of the following categories:
    // 
    //  - Fields and Constructors: Includes class fields and constructors.
    //  - High level APIs: Provides methods for specialized tasks.
    //  - Primitive APIs: Offers basic operations and data processing methods.
    //  


    // ========================================================================
    // Fields and Constructors
    // ========================================================================
   
    
    private static final Logger logger = Logger.getLogger(DocusaurusProcessor.class.getName());



    private static final String[] IMPORTANT_KEYWORDS = {
        "ERROR", "Error", "WARN", "Warning",
        "http://", "https://",
        "Compiled", "successfully",
        "available", "Docusaurus website is running at",
        "onUntruncatedBlogPosts", "onInlineAuthors"
    };

    private static final Pattern PORT_PATTERN = Pattern.compile(
        "http://(?:localhost|0\\.0\\.0\\.0):(\\d+)[^\\s]*"
    );

    /**
     * Example entry point for standalone testing or demo execution.
     *
     * <p>It executes both the build & deploy process and starts the dev server.
     */
    public static void main(String[] args) {
        // Example usage
        deploy(null);            // Build and deploy to public_html
        startDocusaurus();   // Optionally run dev server
    }


    

    /**
     * Builds the current Docusaurus project while filtering the console output
     * to suppress unnecessary build messages.
     *
     * <p>This method assumes it is executed from the project root directory.</p>
     * 
     * <p>
     * This method is intended to reduce verbosity during the build process by capturing and
     * filtering output from underlying build tools. Only essential progress messages (e.g., project
     * name, destination paths, and errors) are displayed.
     * </p>
     */
    public static void build() {
        try {
            Path projectDir = Paths.get("").toAbsolutePath();
            String projectName = projectDir.getFileName().toString();

            
            System.out.println("Building project: " + projectName);
            runCommand(new String[]{"npx", "browserslist@latest", "--update-db"});
            runCommandAndFilterOutput(new String[]{"yarn", "run", "build"});

        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    /**
     * Builds the current Docusaurus project and deploys the generated output to a target directory,
     * while filtering the console output to suppress unnecessary build messages.
     *
     * <p>
     * This method is intended to reduce verbosity during the build process by capturing and
     * filtering output from underlying build tools. Only essential progress messages (e.g., project
     * name, destination paths, and errors) are displayed.
     * </p>
     *
     * <p>
     * It performs the following steps:
     * </p>
     * <ul>
     * <li>Determines the current project name from the working directory.</li>
     * <li>If {@code dest} is {@code null}, uses {@code ~/public_html/{projectName}} as the
     * deployment path.</li>
     * <li>Executes {@code npx browserslist@latest --update-db} to update browser data.</li>
     * <li>Runs the Docusaurus build process using {@code yarn run build}, with filtered
     * output.</li>
     * <li>Removes any existing deployment directory at the destination.</li>
     * <li>Copies the contents of the {@code build} directory to the target deployment path.</li>
     * </ul>
     *
     * @param dest the parent directory where the built site should be deployed; if {@code null},
     *        defaults to {@code ~/public_html/{projectName}}.
     * @see #runCommandAndFilterOutput(String[]) for output filtering behavior
     */
    public static void deploy(String dest) {
        deploy(dest, null, null, null, null);
    }

    public static void deploy(String dest, String destServer, String destDir) {
        deploy(dest, destServer, destDir, null, null);
    }

    public static void deploy(String dest, String destServer, String destDir, String sourceDir) {
        deploy(dest, destServer, destDir, sourceDir, null);
    }

    public static void deploy(String dest, String destServer, String destDir, String sourceDir, String baseUrl) {
        try {
            // Use sourceDir if specified, otherwise use current directory
            Path projectDir = sourceDir != null ? Paths.get(sourceDir).toAbsolutePath() : Paths.get("").toAbsolutePath();
            String projectName = projectDir.getFileName().toString();

            // Determine destination directory
            Path localDestDir = null;
            if (dest == null) {
                localDestDir = Paths.get(System.getProperty("user.home"), "public_html", projectName);
            } else {
                localDestDir = Paths.get(dest, projectName);
            }

            System.out.println("Building project: " + projectName + " (source: " + projectDir + ")");

            // Update docusaurus config BEFORE building if destServer is specified
            if (destServer != null) {
                String serverUrl = destServer.startsWith("http") ? destServer : "http://" + destServer + "/";
                String finalBaseUrl;

                // Use provided baseUrl if specified
                if (baseUrl != null) {
                    finalBaseUrl = baseUrl;
                } else {
                    // Auto-generate baseUrl based on deployment location
                    if ((destDir != null && destDir.contains("public_html")) ||
                        (dest != null && dest.contains("public_html")) ||
                        (destDir == null)) {  // If destDir is not specified, assume public_html
                        finalBaseUrl = "/~" + System.getProperty("user.name") + "/" + projectName + "/";
                    } else {
                        finalBaseUrl = "/";
                    }
                }

                System.out.println("Updating Docusaurus config BEFORE build: url=" + serverUrl + ", baseUrl=" + finalBaseUrl);
                com.github.oogasawa.utility.sau3.configjs.DocusaurusConfigUpdator.update(
                    serverUrl, finalBaseUrl, projectDir.toFile(), projectName);
            }

            // Change to the project directory for building
            try {
                // Debug: Verify config file before build
                if (destServer != null) {
                    System.out.println("DEBUG: Verifying config before build...");
                    Path configFile = projectDir.resolve("docusaurus.config.js");
                    if (Files.exists(configFile)) {
                        try {
                            java.util.List<String> lines = Files.readAllLines(configFile);
                            for (int i = 0; i < lines.size(); i++) {
                                String line = lines.get(i);
                                if (line.contains("url:") || line.contains("baseUrl:")) {
                                    System.out.println("DEBUG: Config line " + (i+1) + ": " + line.trim());
                                }
                            }
                        } catch (Exception e) {
                            System.err.println("DEBUG: Failed to read config file: " + e.getMessage());
                        }
                    }
                }

                ProcessBuilder browserListBuilder = new ProcessBuilder("npx", "browserslist@latest", "--update-db");
                browserListBuilder.directory(projectDir.toFile());
                browserListBuilder.inheritIO();
                Process browserListProcess = browserListBuilder.start();
                browserListProcess.waitFor();

                // Run yarn build in the project directory
                runCommandInDirectoryWithFilter(new String[]{"yarn", "run", "build"}, projectDir.toFile());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Handle remote deployment
            if (destServer != null) {
                // Deploy to remote server
                Path buildDir = projectDir.resolve("build");
                if (Files.exists(buildDir)) {
                    // Use destDir if specified, otherwise use default $HOME/public_html
                    String effectiveDestDir = destDir != null ? destDir : "$HOME/public_html";
                    String remotePath = effectiveDestDir.replace("$HOME", "~") + "/" + projectName;
                    System.out.println("Deploying to remote server: " + destServer + ":" + remotePath);

                    // Test SSH connection first
                    if (!testSSHConnection(destServer)) {
                        System.err.println("Failed to connect to remote server: " + destServer);
                        return;
                    }

                    // Remove existing remote directory completely, then recreate
                    if (!removeAndCreateRemoteDirectory(destServer, remotePath)) {
                        logger.log(Level.SEVERE, "Failed to prepare remote directory: " + destServer + ":" + remotePath);
                        return;
                    }

                    // Use tar+scp+extract for faster deployment
                    System.out.println("Creating tar archive...");
                    String tarFileName = projectName + "_build.tar.gz";
                    Path tarFilePath = projectDir.resolve(tarFileName);

                    // Create tar archive of build directory
                    if (!runCommandWithResult(new String[]{"tar", "-czf", tarFilePath.toString(),
                                                          "-C", buildDir.toString(), "."})) {
                        System.err.println("Failed to create tar archive");
                        return;
                    }

                    System.out.println("Transferring archive to remote server...");
                    // Transfer tar file via scp
                    if (!runCommandWithResult(new String[]{"scp", "-o", "StrictHostKeyChecking=no",
                                                          tarFilePath.toString(),
                                                          destServer + ":~/" + tarFileName})) {
                        System.err.println("Failed to transfer archive to remote server");
                        // Clean up local tar file
                        try { Files.deleteIfExists(tarFilePath); } catch (Exception e) {}
                        return;
                    }

                    System.out.println("Extracting archive on remote server...");
                    // Extract tar on remote server, set permissions, and clean up
                    String extractCommands = "mkdir -p " + remotePath + " && " +
                                           "tar -xzf ~/" + tarFileName + " -C " + remotePath + " && " +
                                           "find " + remotePath + " -type f -exec chmod 644 {} \\; && " +
                                           "find " + remotePath + " -type d -exec chmod 755 {} \\; && " +
                                           "rm ~/" + tarFileName;
                    if (!runCommandWithResult(new String[]{"ssh", "-o", "StrictHostKeyChecking=no",
                                                          destServer, extractCommands})) {
                        System.err.println("Failed to extract archive on remote server");
                        // Clean up local tar file
                        try { Files.deleteIfExists(tarFilePath); } catch (Exception e) {}
                        return;
                    }

                    System.out.println("Set proper web permissions (files: 644, directories: 755)");

                    // Clean up local tar file
                    try {
                        Files.deleteIfExists(tarFilePath);
                        System.out.println("Cleaned up temporary archive");
                    } catch (Exception e) {
                        System.err.println("Warning: Failed to clean up local tar file: " + tarFilePath);
                    }

                    System.out.println("Remote deployment completed successfully!");
                } else {
                    System.err.println("Build directory not found: " + buildDir);
                }
            } else {
                // Local deployment
                if (Files.exists(localDestDir)) {
                    System.out.println("Removing existing: " + localDestDir);
                    deleteDirectoryRecursively(localDestDir.toFile());
                }

                Path buildDir = projectDir.resolve("build");
                if (Files.exists(buildDir)) {
                    System.out.println("Copying to: " + localDestDir);
                    copyDirectory(buildDir, localDestDir);
                } else {
                    System.err.println("Build directory not found: " + buildDir);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * Batch deploy multiple Docusaurus projects based on configuration file.
     *
     * @param configFile Path to configuration file containing project list
     * @param destServer Optional destination server for remote deployment
     * @param baseDir Base directory where all projects are located
     * @param skipGitPull Whether to skip git pull for all projects
     */
    public static void batchDeploy(String configFile, String destServer, String baseDir, boolean skipGitPull) {
        logger.info("Starting batch deployment...");
        logger.info("Config file: " + configFile);
        logger.info("Base directory: " + baseDir);
        logger.info("Destination server: " + (destServer != null ? destServer : "local"));
        logger.info("Skip git pull: " + skipGitPull);

        try {
            // Read project list from configuration file
            java.util.List<String> projectNames = readProjectListFromConfig(configFile);

            if (projectNames.isEmpty()) {
                logger.log(Level.WARNING, "No projects found in configuration file: " + configFile);
                return;
            }

            logger.info("Found " + projectNames.size() + " projects to deploy");

            int successCount = 0;
            int failureCount = 0;

            for (String projectName : projectNames) {
                Path projectDir = Paths.get(baseDir, projectName);

                logger.info("=== Processing project: " + projectName + " ===");

                if (!Files.exists(projectDir)) {
                    logger.log(Level.WARNING, "Project directory not found: " + projectDir);
                    failureCount++;
                    continue;
                }

                try {
                    // Git pull if not skipped
                    if (!skipGitPull) {
                        logger.info("Executing git pull for " + projectName);
                        if (!runGitPull(projectDir.toString())) {
                            logger.log(Level.WARNING, "Git pull failed for " + projectName + ", continuing with deployment...");
                        }
                    }

                    // Deploy project
                    logger.info("Deploying " + projectName);
                    if (destServer != null) {
                        // Remote deployment
                        deploy(null, destServer, null, projectDir.toString(), null);
                    } else {
                        // Local deployment
                        deploy(null, null, null, projectDir.toString(), null);
                    }

                    logger.info("Successfully deployed: " + projectName);
                    successCount++;

                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Failed to deploy " + projectName + ": " + e.getMessage(), e);
                    failureCount++;
                }

                logger.info(""); // Empty line for readability
            }

            logger.info("=== Batch deployment completed ===");
            logger.info("Successful deployments: " + successCount);
            logger.info("Failed deployments: " + failureCount);

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Batch deployment failed: " + e.getMessage(), e);
        }
    }

    /**
     * Read project list from configuration file.
     */
    private static java.util.List<String> readProjectListFromConfig(String configFile) throws java.io.IOException {
        java.util.List<String> projectNames = new java.util.ArrayList<>();
        Path configPath = Paths.get(configFile);

        if (!Files.exists(configPath)) {
            throw new java.io.IOException("Configuration file not found: " + configFile);
        }

        java.util.List<String> lines = Files.readAllLines(configPath);
        boolean inSitemapSection = false;

        for (String line : lines) {
            line = line.trim();

            if (line.equals("[sitemap urls]")) {
                inSitemapSection = true;
                continue;
            }

            if (line.startsWith("[") && !line.equals("[sitemap urls]")) {
                inSitemapSection = false;
                continue;
            }

            if (inSitemapSection && !line.isEmpty() && line.startsWith("http://")) {
                // Extract project name from URL: http://localhost/~oogasawa/PROJECT_NAME/sitemap.xml
                String[] parts = line.split("/");
                if (parts.length >= 4) {
                    String userProject = parts[3]; // ~oogasawa
                    if (parts.length >= 5) {
                        String projectName = parts[4]; // PROJECT_NAME
                        if (!projectName.equals("sitemap.xml")) {
                            projectNames.add(projectName);
                        }
                    }
                }
            }
        }

        return projectNames;
    }

    /**
     * Execute git pull in the specified directory.
     */
    private static boolean runGitPull(String projectDir) {
        try {
            ProcessBuilder gitPullBuilder = new ProcessBuilder("git", "pull");
            gitPullBuilder.directory(new File(projectDir));
            gitPullBuilder.redirectErrorStream(true);
            Process gitPullProcess = gitPullBuilder.start();

            // Capture output
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(gitPullProcess.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            int exitCode = gitPullProcess.waitFor();

            if (exitCode == 0) {
                logger.info("Git pull output: " + output.toString().trim());
                return true;
            } else {
                logger.log(Level.WARNING, "Git pull failed (exit code: " + exitCode + ")");
                logger.log(Level.WARNING, "Git pull output: " + output.toString().trim());
                return false;
            }

        } catch (IOException | InterruptedException e) {
            logger.log(Level.SEVERE, "Exception during git pull: " + e.getMessage(), e);
            return false;
        }
    }


    /**
     * Start the Docusaurus development server in the current directory.
     * 
     * <p>
     * Automatically responds to port conflict prompts with 'Y'. Only important output is shown,
     * such as errors, warnings, and successful launch URLs.
     * </p>
     */
    public static void startDocusaurus() {
        int startPort = 3001;
        int endPort = 3010;
        int selectedPort = -1;

        for (int port = startPort; port <= endPort; port++) {
            try (ServerSocket socket = new ServerSocket(port)) {
                selectedPort = port;
                break; // 空いていたらそのポートを使う
            } catch (IOException e) {
                // このポートは使用中なのでスキップ
            }

        }

        if (selectedPort == -1) {
            System.err.println(
                    "[ERROR] No available port found between " + startPort + " and " + endPort);
            return;
        }

        System.out.println("Launching Docusaurus on available port: " + selectedPort);

        ProcessBuilder builder = new ProcessBuilder("yarn", "start", "--host", "0.0.0.0");
        builder.environment().put("PORT", String.valueOf(selectedPort));
        builder.redirectErrorStream(true);

        try {
            Process process = builder.start();
            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(process.getInputStream()));
            Pattern urlPattern = Pattern.compile("http://localhost:(\\d+)");
            AtomicReference<String> fullUrl = new AtomicReference<>(null);
            AtomicReference<Boolean> startupFailed = new AtomicReference<>(false);

            Thread outputReader = new Thread(() -> {
                try {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String stripped = line.trim();

                        if (stripped.contains("Something is already running on port")) {
                            startupFailed.set(true);
                        }

                        Matcher matcher = urlPattern.matcher(stripped);
                        if (matcher.find()) {

                            fullUrl.set(matcher.group(0));
                        }

                        if (shouldDisplayLine(stripped)) {
                            System.out.println(stripped);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            outputReader.start();
            process.waitFor();
            outputReader.join();

            if (startupFailed.get()) {
                System.out.println(
                        "\n[ERROR] Docusaurus failed to start because the port was already in use.");
            } else if (fullUrl.get() != null) {
                System.out.println("\nDocusaurus is running at: " + fullUrl.get());
            } else {
                System.out.println("\nDocusaurus was started on port " + selectedPort
                        + ", but URL was not printed.");
            }

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }



    /**
     * Determine whether a given line should be printed, based on keyword matching.
     */
    private static boolean shouldDisplayLine(String line) {
        for (String keyword : IMPORTANT_KEYWORDS) {
            if (line.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Run a shell command and stream output directly to the console.
     */
    private static void runCommand(String[] command) throws IOException {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.inheritIO();
        Process process = builder.start();
        try {
            process.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Run a shell command and return true if it succeeds (exit code 0).
     */
    private static boolean runCommandWithResult(String[] command) {
        try {
            ProcessBuilder builder = new ProcessBuilder(command);
            Process process = builder.start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (IOException | InterruptedException e) {
            logger.log(java.util.logging.Level.SEVERE, "Command execution failed: " + String.join(" ", command), e);
            return false;
        }
    }

    /**
     * Run a shell command with detailed error logging.
     */
    private static boolean runCommandWithDetailedResult(String[] command, String operation) {
        try {
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.redirectErrorStream(true);
            Process process = builder.start();

            // Capture output
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();

            if (exitCode == 0) {
                logger.info("Successfully executed " + operation + ": " + String.join(" ", command));
                if (output.length() > 0) {
                    logger.info("Command output: " + output.toString().trim());
                }
                return true;
            } else {
                logger.log(Level.SEVERE, "Failed to " + operation + " (exit code: " + exitCode + ")");
                logger.log(Level.SEVERE, "Command: " + String.join(" ", command));
                if (output.length() > 0) {
                    logger.log(Level.SEVERE, "Error output: " + output.toString().trim());
                }
                return false;
            }
        } catch (IOException | InterruptedException e) {
            logger.log(Level.SEVERE, "Exception while trying to " + operation + ": " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Test SSH connection to the remote server.
     */
    private static boolean testSSHConnection(String server) {
        System.out.println("Testing SSH connection to " + server + "...");
        return runCommandWithResult(new String[]{"ssh", "-o", "ConnectTimeout=10",
                                                "-o", "BatchMode=yes",
                                                "-o", "StrictHostKeyChecking=no",
                                                server, "echo", "SSH connection successful"});
    }

    /**
     * Remove existing remote directory completely, then recreate it.
     */
    private static boolean removeAndCreateRemoteDirectory(String server, String remotePath) {
        logger.info("Removing existing remote directory: " + remotePath);
        // First, remove the directory completely (ignore errors if it doesn't exist)
        runCommandWithDetailedResult(new String[]{"ssh", "-o", "BatchMode=yes",
                                                 "-o", "StrictHostKeyChecking=no",
                                                 server, "rm", "-rf", remotePath},
                                     "remove directory");

        logger.info("Creating remote directory: " + remotePath);
        // Then create it fresh
        return runCommandWithDetailedResult(new String[]{"ssh", "-o", "BatchMode=yes",
                                                        "-o", "StrictHostKeyChecking=no",
                                                        server, "mkdir", "-p", remotePath},
                                           "create directory");
    }

    /**
     * Create remote directory using SSH.
     */
    private static boolean createRemoteDirectory(String server, String remotePath) {
        logger.info("Creating remote directory: " + remotePath);
        return runCommandWithResult(new String[]{"ssh", "-o", "BatchMode=yes",
                                                "-o", "StrictHostKeyChecking=no",
                                                server, "mkdir", "-p", remotePath});
    }

    /**
     * Clean remote directory contents using SSH.
     */
    private static boolean cleanRemoteDirectory(String server, String remotePath) {
        System.out.println("Cleaning remote directory: " + remotePath);
        // Remove contents but keep the directory itself
        return runCommandWithResult(new String[]{"ssh", "-o", "BatchMode=yes",
                                                "-o", "StrictHostKeyChecking=no",
                                                server, "rm", "-rf", remotePath + "/*"});
    }

    /**
     * Run a shell command in a specific directory with output filtering.
     */
    private static void runCommandInDirectoryWithFilter(String[] command, File directory) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(directory);
        builder.redirectErrorStream(true);
        Process process = builder.start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        boolean insideImportantBlock = false;

        while ((line = reader.readLine()) != null) {
            // Detect start or end of a block with many dashes or centered title
            if (line.matches("-{60,}") || line.trim().matches(".*Update available.*")) {
                insideImportantBlock = true;
                System.out.println(line);
                continue;
            }

            else if (insideImportantBlock) {
                System.out.println(line);
                // End condition: when consecutive empty lines appear or the output transitions to a different section
                if (line.matches("-{60,}")) {
                    insideImportantBlock = false;
                }
                continue;
            }

            // fallback: match by keywords
            else if (shouldDisplayLine(line)) {
                System.out.println(line);
            }
        }

        process.waitFor();
    }

    /**
     * Run a shell command and print only relevant output lines.
     */
    private static void runCommandAndFilterOutput(String[] command) throws IOException {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);
        Process process = builder.start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        boolean insideImportantBlock = false;

        while ((line = reader.readLine()) != null) {
            // Detect start or end of a block with many dashes or centered title
            if (line.matches("-{60,}") || line.trim().matches(".*Update available.*")) {
                insideImportantBlock = true;
                System.out.println(line);
                continue;
            }

            else if (insideImportantBlock) {
                System.out.println(line);
                // End condition: when consecutive empty lines appear or the output transitions to a different section
                if (line.matches("-{60,}")) {
                    insideImportantBlock = false;
                }
                continue;
            }

            // fallback: match by keywords
            else if (shouldDisplayLine(line)) {
                System.out.println(line);
            }
        }

        try {
            process.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    

    /**
     * Recursively delete a directory and all of its contents.
     */
    private static void deleteDirectoryRecursively(File dir) throws IOException {
        if (!dir.exists()) return;
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                deleteDirectoryRecursively(file);
            } else {
                if (!file.delete()) throw new IOException("Failed to delete file: " + file);
            }
        }
        if (!dir.delete()) throw new IOException("Failed to delete directory: " + dir);
    }

    /**
     * Recursively copy a directory and its contents.
     */
    private static void copyDirectory(Path source, Path target) throws IOException {
        Files.walk(source).forEach(src -> {
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

    // // ========================================================================
    // // High level APIs
    // // ========================================================================
    
    // /**
    //  * Build all Docusaurus projects located directly under the specified base directory.
    //  *
    //  * <p>Directories with names starting with {@code "doc_"} or {@code "sau_"} are considered
    //  * valid Docusaurus projects.</p>
    //  *
    //  * @param srcBaseDir The root directory containing multiple Docusaurus projects.
    //  * @param destBaseDir The base directory to which each project's output will be deployed.
    //  */
    // public void buildAll(Path srcBaseDir, Path destBaseDir) {
    //     try {
            
    //         Files.list(srcBaseDir)
    //             .sorted()
    //             .filter((Path sauDir)->{return sauDir.toFile().isDirectory();})
    //             .filter((Path sauDir)->{return sauDir.getFileName().toString().startsWith("doc_") || sauDir.getFileName().toString().startsWith("sau_");})
    //             .forEach((Path sauDir)->{
    //                     logger.info("Building docusaurus document: " + sauDir.toString());
    //                 Path destDir = destBaseDir.resolve(sauDir.getFileName());
    //                 buildAndDeploy(sauDir, destDir);
    //                 });
    //     } catch (IOException e) {
    //         logger.error("Failed to list directories under the base directory: " + srcBaseDir.toString(), e);
    //     }
    
    // }

    // /**
    //  * Build a single Docusaurus project and deploy its contents.
    //  *
    //  * @param sauDir The directory containing the source Docusaurus project.
    //  * @param destDir The target directory for the deployment.
    //  */
    // public void buildAndDeploy(Path sauDir, Path destDir)  {
    //     build(sauDir, null);
    //     deploy(sauDir, destDir);
    // }
    


    // ========================================================================
    // Primitive APIs
    // ========================================================================

    
    // /**
    //  * Build the Docusaurus project located at the given directory.
    //  *
    //  * <p>If a {@code url} is provided, it is injected into the config.js file before building.</p>
    //  *
    //  * @param sauDir The path to the Docusaurus project directory.
    //  * @param url The optional base URL to update in the configuration.
    //  */
    // void build(Path sauDir, String url) {

    //     if (url != null) {
    //         DocusaurusConfigUpdator.update(url, sauDir.toFile());
    //     }

    //     ProcessFacade pf = new ProcessFacade()
    //         .directory(sauDir)
    //         .stdioMode(StdioMode.INHERIT)
    //         .environment("LANG", "en_US.UTF-8");
        
        
    //     pf.exec("npx", "update-browserslist-db@latest");
    //     pf.exec("yarn", "run", "build");

    // }

    
    // /**
    //  * Deploy a previously built Docusaurus site to the specified destination directory.
    //  *
    //  * <p>The method assumes a {@code build/} directory exists under the source directory.
    //  * It deletes any existing deployment before copying files.</p>
    //  *
    //  * @param sauDir The Docusaurus project directory.
    //  * @param destDir The destination deployment directory.
    //  */
    // void deploy(Path sauDir, Path destDir) {

    //     // If the build directory in the sauDir does not exist, the deployment is skipped.
    //     if (!Files.exists(sauDir.resolve("build"))) {
    //         logger.error("Docusaurus directory does not exist. The deployment was skipped.");
    //         return;
    //     }
        

    //     // Ensure that the parent directory of destDir (e.g. $HOME/public_html) exists.
    //     if (!Files.exists(destDir.getParent())) {
    //         try {
    //             Files.createDirectories(destDir.getParent());
    //         } catch (IOException e) {
    //             logger.error("Failed to create the destination directory: " + destDir.toString(), e);
    //         }
    //     }



    //     ProcessFacade pf = new ProcessFacade()
    //         .directory(sauDir)
    //         .stdioMode(StdioMode.INHERIT)
    //         .environment("LANG", "en_US.UTF-8");

        
    //     // If the destination directory (e.g. $HOME/public_html/doc_SCI001) exists, delete it once.
    //     if (Files.exists(destDir)) {
    //         pf.exec("rm", "-rf", destDir.toAbsolutePath().toString());
    //     }

    //     // copy the build directory to the destination directory.
    //     pf.exec("cp", "-rp", "build", destDir.toAbsolutePath().toString());

    // }
    

    

}
