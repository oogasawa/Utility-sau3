package com.github.oogasawa.utility.sau3.esjavadoc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import java.util.stream.Stream;
import java.nio.file.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** The {@code EsJavadocCopier} class is a utility class to copy javadoc files to a target directory.
 *    
 *    <p>It reads a list of directories from a file and copies them to a target directory.</p>
 *
 *    <p>An example of a file representing a directory listing is as follows:</p>
 *
 *    <pre>{@code
 *    ./qa/die-with-dignity/build/docs/javadoc
 *    ./qa/wildfly/build/docs/javadoc
 *    ./libs/nio/build/docs/javadoc
 *    ./libs/core/build/docs/javadoc
 *    ./libs/cli/build/docs/javadoc
 *    ./libs/ssl-config/build/docs/javadoc
 *    ./libs/dissect/build/docs/javadoc
 *    ./libs/geo/build/docs/javadoc
 *    ./libs/secure-sm/build/docs/javadoc
 *    ./libs/compress/build/docs/javadoc
 *    }</pre>
 *
 *    <p>Each line represents a directory to be copied. The directory is relative to the current directory.</p>
 *
 *    <p>This file is the output of the find command as follows:</p>
 *
 *    <pre>{@code
 *    cd ~/tmp
 *    git clone https://github.com/opensearch-project/OpenSearch.git
 *    cd OpenSearch
 *    ./gradlew javadoc
 *    find ./ -name "javadoc" | grep -v '/tmp/javadoc' > dir_list.txt
 *    }</pre>
 *
 *    <p>This command creates nearly 90 javadoc directories;
 *    the {@code EsJavadocCopier} class is intended to create
 *    and copy these directories under the target directory.</p>
 * 
 */
public class EsJavadocCopier {

    private static final Logger logger = LoggerFactory.getLogger(EsJavadocCopier.class);


    public void deploy(File dirList, Path targetDir) {

        try {
            Files.createDirectories(targetDir);
            copyJavadocs(dirList, targetDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void copyJavadocs(File dirList, Path targetDir) {

        try (BufferedReader reader = new BufferedReader(new FileReader(dirList))) {
            Stream<String> stream = reader.lines();

            stream.forEach((String dir)->{
                
                try {
                Path currentDirRelativePath = Paths.get(dir);
                Path combinedPath = targetDir.resolve(currentDirRelativePath);
                Files.createDirectories(combinedPath);

                logger.info(combinedPath.toString());
                
                Process p = new ProcessBuilder("cp", "-r", dir, combinedPath.getParent().toString())
                    .directory(new File("."))
                    .start();
                
                p.waitFor();
                }
                catch (IOException e) {
                    logger.error(String.format("Error occurred while copying %s to %s",
                                             dir, targetDir.toString()), e);
                }
                catch (InterruptedException e) {
                    logger.warn("Interrupted", e);
                }
                });
            
        }
        catch (IOException e) {
            logger.error(String.format("Error occurred while reading %s", dirList.toString()), e);
        }
    }
}

