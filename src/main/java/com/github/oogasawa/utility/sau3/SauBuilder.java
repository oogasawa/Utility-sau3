package com.github.oogasawa.utility.sau3;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.StringJoiner;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.github.oogasawa.utility.sau3.configjs.DocusaurusConfigUpdator;


/**
 * 
 */
public class SauBuilder {

    private static final Logger logger = Logger.getLogger(SauBuilder.class.getName());


    public void build(Path sauDir, String url) {

        if (url != null) {
            DocusaurusConfigUpdator.update(url, sauDir.toFile());
        }
        
        exec(sauDir,new String[]{"npx", "update-browserlist-db@latest"});
        exec(sauDir, new String[]{"yarn", "run", "build"});

    }



    
    
    /** Builds all docusaurus documents placed under the direcotry represented by the argument {@code baseDir}.
     * 
     * <p>It is assumed that multiple docusaurus documents are placed directly under the baseDir directory.</p>
     *
     * @param baseDir  A base directory of docusaurus documents. 
     */
    public void buildAll(Path baseDir, String url) {
        try {
            
            Files.list(baseDir)
                .sorted()
                .filter((d)->{return d.toFile().isDirectory();})
                .filter((d)->{return d.getFileName().toString().startsWith("doc_") || d.getFileName().toString().startsWith("sau_");})
                .forEach((d)->{

                        try {

                            System.out.println("## " + d.toAbsolutePath().toString());
                            
                            Process p1 = new ProcessBuilder("yarn")
                                .directory(d.toFile())
                                .inheritIO()
                                .start();

                            p1.waitFor();

                            if (url != null) {
                                DocusaurusConfigUpdator.update(url, d.toFile());
                            }
                            
                            Process p2 = new ProcessBuilder("bash", "deploy.sh")
                                .directory(d.toFile())
                                .inheritIO()
                                .start();

                            p2.waitFor();

                        }
                        catch (IOException e) {
                            logger.log(Level.SEVERE, "sau build failed while executing: " + d.toString(), e);
                        }
                        catch (InterruptedException e) {
                            logger.log(Level.WARNING, "Interrupted", e);
                        }

                    });
        } catch (IOException e) {

        }
    
    }

    

    
    public void deploy(Path sauDir, Path destDir) {
        
        exec(sauDir,new String[]{"rm", "-Rf", destDir.toAbsolutePath().toString()});
        exec(sauDir, new String[]{"cp", "-rp", "build", destDir.toAbsolutePath().toString()});

    }
    

    private void exec(Path sauDir, String[] command) {

        try {
            Process process = new ProcessBuilder(command)
                .directory(sauDir.toFile())
                .inheritIO()
                .start();
            
            process.waitFor();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "IOError at: " + join(command), e);
        } catch (InterruptedException e) {
            logger.log(Level.WARNING, "Interrupted", e);
        }
        
    }


    private String join(String[] strArray) {
        StringJoiner joiner = new StringJoiner(" ");
        for (String s: strArray) {
            joiner.add(s);
        }
        return joiner.toString();
    }




}
