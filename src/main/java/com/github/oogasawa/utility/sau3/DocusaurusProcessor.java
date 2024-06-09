package com.github.oogasawa.utility.sau3;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.github.oogasawa.utility.process.ProcessFacade;
import com.github.oogasawa.utility.process.ProcessFacade.StdioMode;
import com.github.oogasawa.utility.sau3.configjs.DocusaurusConfigUpdator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


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



    // ========================================================================
    // High level APIs
    // ========================================================================
    
    
    /** Builds all docusaurus documents placed under the direcotry represented by the argument {@code baseDir}.
     * 
     * <p>It is assumed that multiple docusaurus documents are placed directly under the baseDir directory.</p>
     *
     * @param srcBaseDir  A base directory of docusaurus documents. (e.g., "$HOME/works".)
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

    /** Builds a docusaurus document placed under the directory represented by the argument {@code sauDir},
     * and deploys it to the directory represented by the argument {@code destDir}.
     *
     * @param sauDir  A directory of the docusaurus document to be deployed.(e.g., "$HOME/works/doc_SCI001".)
     * @param destDir  A directory to deploy the docusaurus document. (e.g., "$HOME/public_html/doc_SCI001".)
     */
    public void buildAndDeploy(Path sauDir, Path destDir)  {
        build(sauDir, null);
        deploy(sauDir, destDir);
    }
    


    // ========================================================================
    // Primitive APIs
    // ========================================================================

    

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
     * Deploys the docusaurus document placed under the directory represented by the argument {@code sauDir}.
     * 
     * <p>The docusaurus document is deployed to the directory represented by the argument {@code destDir}.</p>
     *
     * @param sauDir  A directory of the docusaurus document to be deployed.(e.g., "$HOME/works/doc_SCI001".)
     * @param destDir  A directory to deploy the docusaurus document. (e.g., "$HOME/public_html/doc_SCI001".)
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
