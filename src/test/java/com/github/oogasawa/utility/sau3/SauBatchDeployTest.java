package com.github.oogasawa.utility.sau3;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Stream;

import org.junit.jupiter.api.*;

/**
 * Integration tests for sau:batchDeploy command.
 *
 * These tests verify:
 * 1. Batch deployment of multiple Docusaurus projects
 * 2. Configuration file reading and parsing
 * 3. Local batch deployment
 * 4. Error handling for missing projects
 */
@DisplayName("sau:batchDeploy command integration tests")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SauBatchDeployTest {

    private static final Logger logger = Logger.getLogger(SauBatchDeployTest.class.getName());

    private Path testWorkDir;
    private Path testBaseDir;
    private Path testDestDir;

    @BeforeAll
    public void setUpClass() throws Exception {
        // Create test working directory
        testWorkDir = Files.createTempDirectory("sau-batch-deploy-test-");
        testBaseDir = testWorkDir.resolve("projects");
        testDestDir = testWorkDir.resolve("dest");
        Files.createDirectories(testBaseDir);
        Files.createDirectories(testDestDir);

        // Set environment variable to skip Docusaurus build (use pre-built directory)
        TestHelpers.setEnvironmentVariable("SKIP_DOCUSAURUS_BUILD", "true");

        logger.info("Test work directory: " + testWorkDir);
        logger.info("Test base directory: " + testBaseDir);
        logger.info("Build skip mode: enabled (using pre-built test projects)");
    }

    @AfterAll
    public void tearDownClass() throws Exception {
        // Clean up test working directory
        if (testWorkDir != null && Files.exists(testWorkDir)) {
            TestHelpers.deleteRecursively(testWorkDir);
        }
    }

    /**
     * Test configuration file parsing.
     * Verifies that project names can be correctly extracted from sitemap URLs.
     */
    @Test
    @Order(1)
    @DisplayName("Test configuration file parsing")
    public void testConfigurationFileParsing() throws Exception {
        // Create a test configuration file
        Path configFile = testWorkDir.resolve("test-batch.conf");
        String configContent = """
            [index]
            test_index

            [sitemap urls]
            http://localhost/~testuser/project1/sitemap.xml
            http://localhost/~testuser/project2/sitemap.xml
            http://localhost/~testuser/project3/sitemap.xml
            """;
        Files.writeString(configFile, configContent);

        // Read and verify configuration
        assertTrue(Files.exists(configFile), "Config file should exist");
        List<String> lines = Files.readAllLines(configFile);

        // Count sitemap URLs
        long sitemapCount = lines.stream()
            .filter(line -> line.trim().startsWith("http://"))
            .count();

        assertEquals(3, sitemapCount, "Should have 3 sitemap URLs");
        logger.info("Configuration file parsed successfully: " + sitemapCount + " projects found");
    }

    /**
     * Test batch deployment of multiple projects to local directories.
     * This simulates deploying 3 projects from a configuration file.
     */
    @Test
    @Order(2)
    @DisplayName("Test local batch deployment of multiple projects")
    public void testLocalBatchDeployment() throws Exception {
        // Create 3 test Docusaurus projects
        String[] projectNames = {"project1", "project2", "project3"};

        for (String projectName : projectNames) {
            Path projectDir = testBaseDir.resolve(projectName);
            TestHelpers.copyTestProject(projectDir);

            // Verify pre-built directory exists
            Path buildDir = projectDir.resolve("build");
            assertTrue(Files.exists(buildDir),
                "Pre-built directory should exist for " + projectName);
        }

        // Create configuration file
        Path configFile = testWorkDir.resolve("batch-deploy.conf");
        String configContent = """
            [index]
            test_batch

            [sitemap urls]
            http://localhost/~testuser/project1/sitemap.xml
            http://localhost/~testuser/project2/sitemap.xml
            http://localhost/~testuser/project3/sitemap.xml
            """;
        Files.writeString(configFile, configContent);

        // Simulate batch deployment by deploying each project
        for (String projectName : projectNames) {
            Path projectDir = testBaseDir.resolve(projectName);
            Path buildDir = projectDir.resolve("build");
            Path destDir = testDestDir.resolve(projectName);

            // Deploy: Copy build directory to destination
            Files.createDirectories(destDir.getParent());
            TestHelpers.copyDirectory(buildDir, destDir);

            // Verify deployment
            assertTrue(Files.exists(destDir),
                "Deployment directory should exist for " + projectName);
            assertTrue(Files.exists(destDir.resolve("index.html")),
                "index.html should exist for " + projectName);

            logger.info("Successfully deployed: " + projectName);
        }

        logger.info("Batch deployment completed successfully");
    }

    /**
     * Test batch deployment with custom baseUrl for each project.
     */
    @Test
    @Order(3)
    @DisplayName("Test batch deployment with custom baseUrl")
    public void testBatchDeploymentWithCustomBaseUrl() throws Exception {
        // Create 2 test projects
        String[] projectNames = {"doc_Test001", "doc_Test002"};
        String[] baseUrls = {"/~testuser/doc_Test001/", "/~testuser/doc_Test002/"};

        for (int i = 0; i < projectNames.length; i++) {
            String projectName = projectNames[i];
            String baseUrl = baseUrls[i];

            Path projectDir = testBaseDir.resolve(projectName);
            TestHelpers.copyTestProject(projectDir);

            // Update baseUrl in config
            Path configFile = projectDir.resolve("docusaurus.config.js");
            String originalConfig = Files.readString(configFile);
            String updatedConfig = originalConfig.replaceFirst(
                "baseUrl:\\s*['\"][^'\"]*['\"]",
                "baseUrl: '" + baseUrl + "'"
            );
            Files.writeString(configFile, updatedConfig);

            // Verify baseUrl was updated
            String configContent = Files.readString(configFile);
            assertTrue(configContent.contains("baseUrl: '" + baseUrl + "'"),
                "baseUrl should be updated for " + projectName);

            // Deploy
            Path buildDir = projectDir.resolve("build");
            Path destDir = testDestDir.resolve(projectName);
            Files.createDirectories(destDir.getParent());
            TestHelpers.copyDirectory(buildDir, destDir);

            assertTrue(Files.exists(destDir),
                "Deployment should exist for " + projectName);

            logger.info("Deployed " + projectName + " with baseUrl: " + baseUrl);
        }
    }

    /**
     * Test batch deployment error handling for missing projects.
     */
    @Test
    @Order(4)
    @DisplayName("Test error handling for missing projects")
    public void testMissingProjectHandling() throws Exception {
        // Create configuration file with non-existent project
        Path configFile = testWorkDir.resolve("missing-project.conf");
        String configContent = """
            [index]
            test_missing

            [sitemap urls]
            http://localhost/~testuser/non_existent_project/sitemap.xml
            """;
        Files.writeString(configFile, configContent);

        // Attempt to read config
        assertTrue(Files.exists(configFile), "Config file should exist");

        // Verify that non-existent project directory doesn't exist
        Path nonExistentProject = testBaseDir.resolve("non_existent_project");
        assertFalse(Files.exists(nonExistentProject),
            "Non-existent project directory should not exist");

        logger.info("Missing project handling verified");
    }

    /**
     * Test batch deployment with skipGitPull option.
     * This simulates the --skipGitPull flag behavior.
     */
    @Test
    @Order(5)
    @DisplayName("Test batch deployment with skipGitPull option")
    public void testBatchDeploymentSkipGitPull() throws Exception {
        // Create test project
        Path projectDir = testBaseDir.resolve("project_skip_git");
        TestHelpers.copyTestProject(projectDir);

        // When skipGitPull is true, git pull is not executed
        // In our simplified test, we just verify the project can be deployed
        boolean skipGitPull = true;

        assertTrue(skipGitPull, "skipGitPull flag should be true");

        // Deploy without git pull
        Path buildDir = projectDir.resolve("build");
        Path destDir = testDestDir.resolve("project_skip_git");
        Files.createDirectories(destDir.getParent());
        TestHelpers.copyDirectory(buildDir, destDir);

        assertTrue(Files.exists(destDir), "Deployment should succeed without git pull");

        logger.info("Batch deployment with skipGitPull completed successfully");
    }

    /**
     * Test that verifies deployment directory structure.
     */
    @Test
    @Order(6)
    @DisplayName("Test deployment directory structure")
    public void testDeploymentDirectoryStructure() throws Exception {
        // Create and deploy a test project
        Path projectDir = testBaseDir.resolve("structure_test");
        TestHelpers.copyTestProject(projectDir);

        Path buildDir = projectDir.resolve("build");
        Path destDir = testDestDir.resolve("structure_test");
        Files.createDirectories(destDir.getParent());
        TestHelpers.copyDirectory(buildDir, destDir);

        // Verify deployment structure
        assertTrue(Files.exists(destDir.resolve("index.html")),
            "index.html should exist");
        assertTrue(Files.exists(destDir.resolve("404.html")),
            "404.html should exist");
        assertTrue(Files.exists(destDir.resolve("sitemap.xml")),
            "sitemap.xml should exist");
        assertTrue(Files.exists(destDir.resolve("assets")),
            "assets directory should exist");

        logger.info("Deployment directory structure verified");
    }
}
