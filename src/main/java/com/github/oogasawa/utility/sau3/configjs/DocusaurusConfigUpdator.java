package com.github.oogasawa.utility.sau3.configjs;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DocusaurusConfigUpdator {

    private static final Logger logger = Logger.getLogger(DocusaurusConfigUpdator.class.getName());

    
    public static void update(String newUrl) {
        String filePath = "docusaurus.config.js"; // Original file.
        String backupFilePath = filePath + ".bak"; // Backup file.

        // Backup the original file.
        backupFile(filePath, backupFilePath);
        // replace the URL in the original file.
        replaceUrlInFile(filePath, newUrl);
    }

    private static void backupFile(String sourcePath, String destPath) {
        try {
            Files.copy(Paths.get(sourcePath), Paths.get(destPath));
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to backup the file.", e);
        }
    }

    public static void replaceUrlInFile(String filePath, String newUrl) {
        try {
            // Create a temporary file.
            String tempFile = filePath + ".tmp";
            try (BufferedReader reader = new BufferedReader(new FileReader(filePath));
                 BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().startsWith("url: '")) {
                        // URLの部分を新しいURLで置換
                        line = line.replaceAll("url: '.*',", "url: '" + newUrl + "',");
                    }
                    writer.write(line + System.lineSeparator());
                }
            }
            // Replace the original file with the temporary file.
            Files.move(Paths.get(tempFile), Paths.get(filePath), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to replace the URL in the file.", e);
        }
    }
}
