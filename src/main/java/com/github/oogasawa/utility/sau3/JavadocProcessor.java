package com.github.oogasawa.utility.sau3;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.github.oogasawa.utility.process.ProcessFacade;
import com.github.oogasawa.utility.process.ProcessFacade.StdioData;
import com.github.oogasawa.utility.process.ProcessFacade.StdioMode;


/** This class builds Javadoc API documents and depoloys them to a specified directory. */
public class JavadocProcessor {

        /*
     * This class is composed of the following categories:
     *
     * <ul>
     * <li>Fields and Constructors: Includes class fields and constructors.</li>
     * <li>High level APIs: Provides methods for specialized tasks.</li>
     * <li>Primitive APIs: Offers basic operations and data processing methods.</li>
     * </ul>
     */


    // ========================================================================
    // Fields and Constructors
    // ========================================================================
    
    private static final Logger logger = Logger.getLogger(JavadocProcessor.class.getName());
    

    // ========================================================================
    // High level APIs
    // ========================================================================

    public void buildAll(Path srcBaseDir, Path destBaseDir) {
        try {
            
            Files.list(srcBaseDir)
                .sorted()
                .filter((Path javaDir)->{return javaDir.toFile().isDirectory();})
                .filter((Path javaDir)->{return isJavaProjectDir(javaDir);})
                .forEach((Path javaDir)->{
                        logger.info("Building javadoc: " + javaDir.toString());
                    Path destDir = destBaseDir.resolve(javaDir.getFileName());
                    buildAndDeploy(javaDir, destDir);
                    });
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to list directories under the base directory: " + srcBaseDir.toString(), e);
        }
    
    }

    

    
    /** Builds javadoc documents and deploys them to a specified destination directory.
     *
     * @param srcdir A java project directory.(e.g. {@code $HOME/works/your_project})
     * @param destDir A destination directory where javadoc files are copied. (e.g. {@code $HOME/public_html/javadoc/your_project})
     */
    public void buildAndDeploy(Path srcdir, Path destDir)  {

        this.build(srcdir);        
        this.deploy(srcdir.resolve("target/site/apidocs"), destDir);

        this.buildTestJavadoc(srcdir);
        // Directory for the test javadoc documents.
        Path destDir2 = destDir.resolveSibling(destDir.getFileName() + "-test");
        this.deploy(srcdir.resolve("target/site/testapidocs"), destDir2);
        
    }


    /** Builds javadoc documents.
     *
     * @param srcdir A java project directory.(e.g. {@code $HOME/works/your_project})
     * 
     * @return A result of the build process.
     */
    public StdioData build(Path srcdir) {

        ProcessFacade pf = new ProcessFacade()
            .directory(srcdir)
            .stdioMode(StdioMode.INHERIT)
            .environment("LANG", "en_US.UTF-8");
        

        StdioData result = null;
        if (Files.exists(srcdir.resolve("mvnw"))) {
            result = pf.exec("./mvnw", "javadoc:javadoc");
        } else {
            result = pf.exec("mvn", "javadoc:javadoc");
        }

        return result;
    }


    
    /** Generate test javadoc documents. 
     * (This method executes `mvn javadoc:test-javadoc` command.)
     * 
     * @param srcdir A java project directory.(e.g. {@code $HOME/works/your_project})
     * @return A result of the build process.
     */
    public StdioData buildTestJavadoc(Path srcdir) {

        ProcessFacade pf = new ProcessFacade()
            .directory(srcdir)
            .stdioMode(StdioMode.INHERIT)
            .environment("LANG", "en_US.UTF-8");
        

        StdioData result = null;
        if (Files.exists(srcdir.resolve("mvnw"))) {
            result = pf.exec("./mvnw", "javadoc:test-javadoc");
        } else {
            result = pf.exec("mvn", "javadoc:test-javadoc");
        }

        return result;
    }




    /** Deploys javadoc documents to a specified destination directory.
     *
     * It is assumed that the javadoc files have already been created in the directory {@code srcdir/target/site/apidocs}.
     * 
     * If the destination directry ({@code destDir}) does not exist, it is created.
     *
     * @param javadocDir A javadoc directory.(e.g. {@code $HOME/works/your_project/target/site/apidocs})
     * @param destDir A destination directory where javadoc files are copied. (e.g. {@code $HOME/public_html/javadoc/your_project})
     */
    public void deploy(Path javadocDir, Path destDir) {

        if (!Files.exists(javadocDir)) {
            logger.log(Level.SEVERE, "Javadoc directory does not exist. The deployment was skipped.");
            return;
        }

        // If the destination directory (e.g. ~/public_html/javadoc/your_project ) exists, delete it once.
        if (Files.exists(destDir)) {
            deleteRecursively(destDir);
        }

        // If the parent directory of the destination directory does not exist, create it.
        if (!Files.exists(destDir.getParent())) {
            try {
                Files.createDirectories(destDir.getParent());
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to create a directory: " + destDir.getParent().toString());
            }
        }

        // Copy the javadoc files to the destination directory.
        ProcessFacade pf = new ProcessFacade()
            .stdioMode(StdioMode.INHERIT)
            .environment("LANG", "en_US.UTF-8");
        

        StdioData result = pf.exec("cp", "-rp", javadocDir.toString(), destDir.toString());

        if (result.getExitValue() != 0) {
            logger.log(Level.SEVERE, "Failed to copy javadoc files: " + javadocDir.toString() + " -> " + destDir.toString());
        }

    }


    // ========================================================================
    // Primitive APIs
    // ========================================================================


    /** Forcibly delete a directory even if files remain in the directory.
     *
     * @param dir  A directory to be deleted.
     */ 
    void deleteRecursively(Path dir) {
        ProcessFacade pf = new ProcessFacade()
            .stdioMode(StdioMode.INHERIT)
            .environment("LANG", "en_US.UTF-8");

        pf.exec("rm", "-rf", dir.toString());
    }


    boolean isJavaProjectDir(Path dir) {
        return Files.exists(dir.resolve("pom.xml"));
    }
}
