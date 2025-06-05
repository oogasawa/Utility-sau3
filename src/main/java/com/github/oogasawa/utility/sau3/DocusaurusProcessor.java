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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
   
    
    private static final Logger logger = LoggerFactory.getLogger(DocusaurusProcessor.class);



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
        try {
            Path projectDir = Paths.get("").toAbsolutePath();
            String projectName = projectDir.getFileName().toString();

            Path destDir = null;
            if (dest == null) {
                destDir = Paths.get(System.getProperty("user.home"), "public_html", projectName);
            }
            else {
                destDir = Paths.get(dest, projectName);
            }
            
            System.out.println("Building project: " + projectName);
            runCommand(new String[]{"npx", "browserslist@latest", "--update-db"});
            runCommandAndFilterOutput(new String[]{"yarn", "run", "build"});

            // Remove existing directory
            if (Files.exists(destDir)) {
                System.out.println("Removing existing: " + destDir);
                deleteDirectoryRecursively(destDir.toFile());
            }

            // Copy build output
            Path buildDir = projectDir.resolve("build");
            if (Files.exists(buildDir)) {
                System.out.println("Copying to: " + destDir);
                copyDirectory(buildDir, destDir);
            } else {
                System.err.println("Build directory not found: " + buildDir);
            }

        } catch (IOException e) {
            e.printStackTrace();
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
            if (line.matches("^-{60,}") || line.trim().matches(".*Update available.*")) {
                insideImportantBlock = true;
                System.out.println(line);
                continue;
            }

            else if (insideImportantBlock) {
                System.out.println(line);
                // End condition: when consecutive empty lines appear or the output transitions to a different section
                if (line.matches("^-{60,}")) {
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
