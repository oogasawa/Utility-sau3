package com.github.oogasawa.utility.sau3;

import java.io.File;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.oogasawa.utility.process.ProcessFacade;
import com.github.oogasawa.utility.process.ProcessFacade.StdioMode;
import com.github.oogasawa.utility.sau3.configjs.DocusaurusConfigUpdator;

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
        deploy();            // Build and deploy to public_html
        startDocusaurus();   // Optionally run dev server
    }


    /**
     * Build the current Docusaurus project and deploy its output to 
     * {@code ~/public_html/{projectName}}.
     *
     * <p>This method assumes it is executed from the project root directory.</p>
     */
    public static void deploy() {
        try {
            Path projectDir = Paths.get("").toAbsolutePath();
            String projectName = projectDir.getFileName().toString();
            Path destDir = Paths.get(System.getProperty("user.home"), "public_html", projectName);

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
     * <p>Automatically responds to port conflict prompts with 'Y'. Only important
     * output is shown, such as errors, warnings, and successful launch URLs.</p>
     */
    public static void startDocusaurus() {
        ProcessBuilder builder = new ProcessBuilder("yarn", "start", "--host", "0.0.0.0");
        builder.redirectErrorStream(true);

        try {
            Process process = builder.start();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            String fullUrl = null;
            String port = null;

            while ((line = reader.readLine()) != null) {
                String stripped = line.trim();

                if (stripped.matches("(?i).*Would you like to run the app on another port instead\\?.*")) {
                    System.out.println("[Auto-response] Y");
                    writer.write("Y\n");
                    writer.flush();
                }

                Matcher matcher = PORT_PATTERN.matcher(stripped);
                if (matcher.find()) {
                    port = matcher.group(1);
                    fullUrl = matcher.group(0);
                }

                if (shouldDisplayLine(stripped)) {
                    System.out.println(stripped);
                }
            }

            process.waitFor();

            if (fullUrl != null) {
                System.out.println("\nDocusaurus is running at: " + fullUrl);
            } else if (port != null) {
                System.out.println("\nDocusaurus is running on port " + port);
            } else {
                System.out.println("\nCould not determine the running port.");
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
        while ((line = reader.readLine()) != null) {
            if (shouldDisplayLine(line)) {
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

    // ========================================================================
    // High level APIs
    // ========================================================================
    
    /**
     * Build all Docusaurus projects located directly under the specified base directory.
     *
     * <p>Directories with names starting with {@code "doc_"} or {@code "sau_"} are considered
     * valid Docusaurus projects.</p>
     *
     * @param srcBaseDir The root directory containing multiple Docusaurus projects.
     * @param destBaseDir The base directory to which each project's output will be deployed.
     */
    public void buildAll(Path srcBaseDir, Path destBaseDir) {
        try {
            
            Files.list(srcBaseDir)
                .sorted()
                .filter((Path sauDir)->{return sauDir.toFile().isDirectory();})
                .filter((Path sauDir)->{return sauDir.getFileName().toString().startsWith("doc_") || sauDir.getFileName().toString().startsWith("sau_");})
                .forEach((Path sauDir)->{
                        logger.info("Building docusaurus document: " + sauDir.toString());
                    Path destDir = destBaseDir.resolve(sauDir.getFileName());
                    buildAndDeploy(sauDir, destDir);
                    });
        } catch (IOException e) {
            logger.error("Failed to list directories under the base directory: " + srcBaseDir.toString(), e);
        }
    
    }

    /**
     * Build a single Docusaurus project and deploy its contents.
     *
     * @param sauDir The directory containing the source Docusaurus project.
     * @param destDir The target directory for the deployment.
     */
    public void buildAndDeploy(Path sauDir, Path destDir)  {
        build(sauDir, null);
        deploy(sauDir, destDir);
    }
    


    // ========================================================================
    // Primitive APIs
    // ========================================================================

    
    /**
     * Build the Docusaurus project located at the given directory.
     *
     * <p>If a {@code url} is provided, it is injected into the config.js file before building.</p>
     *
     * @param sauDir The path to the Docusaurus project directory.
     * @param url The optional base URL to update in the configuration.
     */
    void build(Path sauDir, String url) {

        if (url != null) {
            DocusaurusConfigUpdator.update(url, sauDir.toFile());
        }

        ProcessFacade pf = new ProcessFacade()
            .directory(sauDir)
            .stdioMode(StdioMode.INHERIT)
            .environment("LANG", "en_US.UTF-8");
        
        
        pf.exec("npx", "update-browserslist-db@latest");
        pf.exec("yarn", "run", "build");

    }

    
    /**
     * Deploy a previously built Docusaurus site to the specified destination directory.
     *
     * <p>The method assumes a {@code build/} directory exists under the source directory.
     * It deletes any existing deployment before copying files.</p>
     *
     * @param sauDir The Docusaurus project directory.
     * @param destDir The destination deployment directory.
     */
    void deploy(Path sauDir, Path destDir) {

        // If the build directory in the sauDir does not exist, the deployment is skipped.
        if (!Files.exists(sauDir.resolve("build"))) {
            logger.error("Docusaurus directory does not exist. The deployment was skipped.");
            return;
        }
        

        // Ensure that the parent directory of destDir (e.g. $HOME/public_html) exists.
        if (!Files.exists(destDir.getParent())) {
            try {
                Files.createDirectories(destDir.getParent());
            } catch (IOException e) {
                logger.error("Failed to create the destination directory: " + destDir.toString(), e);
            }
        }



        ProcessFacade pf = new ProcessFacade()
            .directory(sauDir)
            .stdioMode(StdioMode.INHERIT)
            .environment("LANG", "en_US.UTF-8");

        
        // If the destination directory (e.g. $HOME/public_html/doc_SCI001) exists, delete it once.
        if (Files.exists(destDir)) {
            pf.exec("rm", "-rf", destDir.toAbsolutePath().toString());
        }

        // copy the build directory to the destination directory.
        pf.exec("cp", "-rp", "build", destDir.toAbsolutePath().toString());

    }
    

    

}
