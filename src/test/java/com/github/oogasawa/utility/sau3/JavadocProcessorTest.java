package com.github.oogasawa.utility.sau3;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;


import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@DisplayName("javadoc:deploy command test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class JavadocProcessorTest {

    private static final Logger logger = LoggerFactory.getLogger(JavadocProcessorTest.class.getName());


    Path testSrcBaseDir = null;
    Path testDestBaseDir = null;

    @BeforeEach
    public void setUp() {
        try {
            // Read logging configuration.
            //LogManager.getLogManager().readConfiguration(JavadocProcessorTest.class.getClassLoader().getResourceAsStream("logging.properties"));

            // Create temporary directory for a working space.
            testSrcBaseDir = Files.createTempDirectory(Path.of(System.getenv("PWD")), "srcdir");
            logger.debug("srcdir absolute path = " + testSrcBaseDir);
            assertTrue((new File(testSrcBaseDir.toString())).exists());

            testDestBaseDir = Files.createTempDirectory(Path.of(System.getenv("PWD")), "destdir");
            logger.debug("destdir absolute path = " + testDestBaseDir);
            assertTrue((new File(testDestBaseDir.toString())).exists());

        } catch (IOException e) {
            logger.error("Could not create tmpdir", e);
        }
    }


    @AfterEach
    public void tearDown() {
        try {
            if (Files.exists(testSrcBaseDir)) {
                Files.walk(testSrcBaseDir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
            }
            assertFalse(Files.exists(testSrcBaseDir));

            if (Files.exists(testDestBaseDir)) {
                Files.walk(testDestBaseDir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
            }

            assertFalse(Files.exists(testDestBaseDir));
            
        } catch (IOException e) {
            logger.error("Could not delete tmpdir", e);
        }
    }

    
    @Test
    @Order(1)
    public void testDeleteRecursively() {

        try {
            // Create a test directory with some files and subdirectories
            Files.createFile(testSrcBaseDir.resolve("file1.txt"));
            Files.createFile(testSrcBaseDir.resolve("file2.txt"));
            Files.createDirectories(testSrcBaseDir.resolve("subdir1"));
            Files.createDirectories(testSrcBaseDir.resolve("subdir2"));
            Files.createFile(testSrcBaseDir.resolve("subdir2").resolve("file21.txt"));
            Files.createFile(testSrcBaseDir.resolve("subdir2").resolve("file22.txt"));


            assertTrue(Files.exists(testSrcBaseDir.resolve("file1.txt")));
            assertTrue(Files.exists(testSrcBaseDir.resolve("file2.txt")));
            assertTrue(Files.exists(testSrcBaseDir.resolve("subdir1")));
            assertTrue(Files.exists(testSrcBaseDir.resolve("subdir2").resolve("file21.txt")));
            assertTrue(Files.exists(testSrcBaseDir.resolve("subdir2").resolve("file22.txt")));

            
            // Delete the test directory recursively.
            JavadocProcessor processor = new JavadocProcessor();
            processor.deleteRecursively(testSrcBaseDir);
            assertFalse(Files.exists(testSrcBaseDir));

        } catch (IOException e) {
            logger.error("Recursive deletion of the temporary directory could not completed: "
                         + testSrcBaseDir.toString(),
                         e);
        }

    }

    
    @Test
    @Order(2)
    public void testDeploy() {

        try {
            // Create a test javadoc directory.
            Path javadocDir = testSrcBaseDir.resolve("target/site/apidocs");
            Files.createDirectories(javadocDir);

            // Create a test javadoc file.
            Files.createFile(javadocDir.resolve("index.html"));

            // Check if the javadocDir contains the test javadoc directory.
            assertTrue(Files.exists(javadocDir));
            assertTrue(Files.exists(javadocDir.resolve("index.html")));

            
            Path destDir = testDestBaseDir.resolve("your_project");
            
            // Deploy the test javadoc directory to the test destination directory.
            // (the test destination directory will be created if it does not exist.)
            JavadocProcessor processor = new JavadocProcessor();
            logger.debug("testSrcBaseDir: " + testSrcBaseDir.toString());
            logger.debug("testDestBaseDir: " + Files.exists(testDestBaseDir));
            logger.debug("testDestBaseDir: " + testDestBaseDir.toString());
            logger.debug("testDestBaseDir: " + Files.exists(testDestBaseDir));
            logger.debug("javadocDir: " + javadocDir.toString());
            logger.debug("javadocDir: " + Files.exists(javadocDir));
            logger.debug("destDir: " + destDir.toString());
            logger.debug("destDir: " + Files.exists(destDir));

            processor.deploy(javadocDir, destDir);

            
            
            // Check if the test destination directory contains the test javadoc directory.
            assertTrue(Files.exists(testDestBaseDir.resolve("your_project")));
            assertTrue(Files.exists(testDestBaseDir.resolve("your_project").resolve("index.html")));

        } catch (IOException e) {
            logger.error("Deployment of the test javadoc directory could not completed.", e);
        }
    }

}
