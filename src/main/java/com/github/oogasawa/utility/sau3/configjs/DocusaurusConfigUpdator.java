package com.github.oogasawa.utility.sau3.configjs;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * This class adjusts docusaurus.config.js in the batch process depending on where the docusaurus site is deployed.
 *
 * <p>
 * For example, if you deploy {@code nigsc_homepage2} for publication at {@code https://sc.ddbj.nig.ac.jp},
 * the {@code url:} must be {@code 'https://sc.ddbj.nig.ac.jp/'}
 * and the {@code baseUrl:} must be {@code '/'},
 * whereas if you deploy to {@code public_html} on your local machine 
 * you need to rewrite {@code url:} {@code'http://192.168.0.10/'} and {@code {baseUrl:} {@code '/~oogasawa/nigsc_homepage2/'}
 *
 * This class is for this process.
 * </p>
 */
public class DocusaurusConfigUpdator {

    private static final Logger logger = Logger.getLogger(DocusaurusConfigUpdator.class.getName());
    

    public static boolean containsStringInUrlLine(String filePath, String searchString) {
        boolean found = false;

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("url:") && line.contains(searchString)) {
                    found = true;
                    break; // 文字列が見つかったらループを終了
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return found;
    }

    public static boolean containsStringInFile(String filePath, String searchString) {
        boolean found = false;

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains(searchString)) {
                    found = true;
                    break; // 文字列が見つかったらループを終了
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return found;
    }

    
    private static void backupFile(String sourcePath, String destPath) {
        try {
            if (Files.exists(Paths.get(destPath))) {
                Files.delete(Paths.get(destPath));
            }
            Files.copy(Paths.get(sourcePath), Paths.get(destPath));
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to backup the file.", e);
        }
    }

    public static void replaceUrlInFile(String filePath, String newUrl) {
        replaceUrlInFile(filePath, newUrl, null);
    }

    public static void replaceUrlInFile(String filePath, String newUrl, String newBaseUrl) {
        replaceUrlInFile(filePath, newUrl, newBaseUrl, null);
    }

    public static void replaceUrlInFile(String filePath, String newUrl, String newBaseUrl, String newSearchServer) {
        logger.info("Starting config file update: " + filePath);
        logger.info("Target newUrl: " + newUrl);
        logger.info("Target newBaseUrl: " + newBaseUrl);
        logger.info("Target newSearchServer: " + newSearchServer);

        try {
            // Log original file content
            logger.info("Reading original config file...");
            try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
                String line;
                int lineNumber = 0;
                while ((line = reader.readLine()) != null) {
                    lineNumber++;
                    if (line.trim().startsWith("url: '") || line.trim().startsWith("baseUrl: '") ||
                        line.contains("8080/search")) {
                        logger.info("Line " + lineNumber + " BEFORE: " + line.trim());
                    }
                }
            }

            // Create a temporary file.
            String tempFile = filePath + ".tmp";
            boolean urlUpdated = false;
            boolean baseUrlUpdated = false;
            boolean searchUrlUpdated = false;

            try (BufferedReader reader = new BufferedReader(new FileReader(filePath));
                 BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {

                String line;
                int lineNumber = 0;
                while ((line = reader.readLine()) != null) {
                    lineNumber++;
                    String originalLine = line;

                    if (line.trim().startsWith("url: '")) {
                        // URLの部分を新しいURLで置換
                        line = line.replaceAll("url: '.*',", "url: '" + newUrl + "',");
                        urlUpdated = true;
                        logger.info("Line " + lineNumber + " AFTER (url): " + line.trim());
                    } else if (newBaseUrl != null && line.trim().startsWith("baseUrl: '")) {
                        // baseURLの部分を新しいbaseURLで置換
                        line = line.replaceAll("baseUrl: '[^']*'", "baseUrl: '" + newBaseUrl + "'");
                        baseUrlUpdated = true;
                        logger.info("Line " + lineNumber + " AFTER (baseUrl): " + line.trim());
                    } else if (newSearchServer != null && line.contains("8080/search")) {
                        // Search URL を新しいサーバーで置換
                        // パターン: href="http://any-ip:8080/search" → href="http://newServer:8080/search"
                        logger.info("SEARCH URL MATCH FOUND: Line " + lineNumber + " contains 8080/search: " + line.trim());
                        String searchOriginalLine = line;
                        String newSearchUrl = "http://" + newSearchServer + ":8080/search";
                        line = line.replaceAll("href=\"http://[^:]+:8080/search\"", "href=\"" + newSearchUrl + "\"");
                        if (!searchOriginalLine.equals(line)) {
                            searchUrlUpdated = true;
                            logger.info("SEARCH URL REPLACED: Line " + lineNumber + " AFTER: " + line.trim());
                        } else {
                            logger.info("SEARCH URL NOT REPLACED: Pattern did not match on line " + lineNumber);
                        }
                    }
                    writer.write(line + System.lineSeparator());
                }
            }

            // Replace the original file with the temporary file.
            Files.move(Paths.get(tempFile), Paths.get(filePath), java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            logger.info("Config file update completed. URL updated: " + urlUpdated + ", BaseUrl updated: " + baseUrlUpdated + ", SearchUrl updated: " + searchUrlUpdated);

            // Verify the changes
            logger.info("Verifying updated config file...");
            try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
                String line;
                int lineNumber = 0;
                while ((line = reader.readLine()) != null) {
                    lineNumber++;
                    if (line.trim().startsWith("url: '") || line.trim().startsWith("baseUrl: '") ||
                        line.contains("8080/search")) {
                        logger.info("Line " + lineNumber + " FINAL: " + line.trim());
                    }
                }
            }

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to replace the URL in the file.", e);
        }
    }


    public static void update(String newUrl) {
        update(newUrl, null, null, null);
    }

    public static void update(String newUrl, File sauDir) {
        update(newUrl, null, sauDir, null);
    }

    public static void update(String newUrl, String newBaseUrl, File sauDir, String projectName) {
        update(newUrl, newBaseUrl, sauDir, projectName, null);
    }

    public static void update(String newUrl, String newBaseUrl, File sauDir, String projectName, String newSearchServer) {
        logger.info("POSITIVE_CONTROL: DocusaurusConfigUpdator.update method called");
        logger.info("DEBUG: DocusaurusConfigUpdator.update called with newUrl=" + newUrl + ", newBaseUrl=" + newBaseUrl + ", sauDir=" + sauDir + ", projectName=" + projectName + ", newSearchServer=" + newSearchServer);

        if (sauDir == null) {
            // Get the current directory.
            sauDir = System.getProperty("user.dir") != null ? new File(System.getProperty("user.dir")) : new File(".");
        }

        // Try both .js and .ts config files
        String jsFilePath = sauDir.getAbsolutePath() + "/docusaurus.config.js";
        String tsFilePath = sauDir.getAbsolutePath() + "/docusaurus.config.ts";
        String filePath = null;

        if (Files.exists(Paths.get(jsFilePath))) {
            filePath = jsFilePath;
        } else if (Files.exists(Paths.get(tsFilePath))) {
            filePath = tsFilePath;
        } else {
            logger.log(Level.WARNING, "Neither docusaurus.config.js nor docusaurus.config.ts found in: " + sauDir.getAbsolutePath());
            return;
        }

        String backupFilePath = filePath + ".bak"; // Backup file.

        // If newBaseUrl is null but we have a projectName, generate baseUrl
        String baseUrlToUse = newBaseUrl;
        if (baseUrlToUse == null && projectName != null) {
            baseUrlToUse = "/~" + System.getProperty("user.name") + "/" + projectName + "/";
        }

        boolean needsUpdate = !containsStringInUrlLine(filePath, newUrl);
        boolean needsSearchUrlUpdate = (newSearchServer != null && containsStringInFile(filePath, "8080/search"));

        logger.info("DEBUG UPDATE CONDITIONS: needsUpdate=" + needsUpdate + ", needsSearchUrlUpdate=" + needsSearchUrlUpdate);
        logger.info("DEBUG: newSearchServer=" + newSearchServer);
        logger.info("DEBUG: containsStringInFile(filePath, \"8080/search\")=" + containsStringInFile(filePath, "8080/search"));

        if (needsUpdate || needsSearchUrlUpdate) {
            // Backup the original file.
            backupFile(filePath, backupFilePath);
            // replace the URL, baseUrl, and searchUrl in the original file.
            replaceUrlInFile(filePath, newUrl, baseUrlToUse, newSearchServer);
        }
    }


    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: DocusaurusConfigUpdator <newUrl> [newBaseUrl]");
            System.exit(1);
        }
        String newUrl = args[0];
        String newBaseUrl = args.length > 1 ? args[1] : null;

        System.out.println("Testing direct update: newUrl=" + newUrl + ", newBaseUrl=" + newBaseUrl);
        update(newUrl, newBaseUrl, null, null);
        System.out.println("Update completed.");
    }
}
