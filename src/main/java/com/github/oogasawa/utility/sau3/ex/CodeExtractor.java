package com.github.oogasawa.utility.sau3.ex;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermission;
import java.util.*;
import java.util.regex.*;

/**
 * Parses Markdown to extract scripts and generates script files and a Makefile.
 * Supports specifying an optional result file that is used as the Makefile target.
 */
public class CodeExtractor {

    // Pattern to match either one or two filenames:
    // <!-- ex:name script_file -->
    // <!-- ex:name script_file, result_file -->
    private static final Pattern EX_NAME_PATTERN =
        Pattern.compile("<!--\\s*ex:name\\s+([^,\\s]+)(?:\\s*,\\s*([^\\s]+))?\\s*-->");

    /** Map<script_file, result_file_or_null> */
    private final LinkedHashMap<String, String> scripts = new LinkedHashMap<>();

    /**
     * Step 1: Parse Markdown input and extract scripts and optional result files.
     * 
     * @param markdownPath path to the Markdown file
     * @throws IOException if IO error occurs
     */
    public void parseMarkdown(Path markdownPath) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(markdownPath)) {
            String line;
            String currentScriptFile = null;
            boolean inCodeBlock = false;
            StringBuilder codeBuffer = new StringBuilder();

            while ((line = reader.readLine()) != null) {
                if (!inCodeBlock) {
                    Matcher matcher = EX_NAME_PATTERN.matcher(line);
                    if (matcher.find()) {
                        currentScriptFile = matcher.group(1);
                        String resultFile = matcher.group(2); // may be null
                        scripts.put(currentScriptFile, resultFile);
                        codeBuffer.setLength(0);
                        continue;
                    }
                    if (currentScriptFile != null && line.trim().startsWith("```")) {
                        inCodeBlock = true;
                        continue;
                    }
                } else {
                    if (line.trim().startsWith("```")) {
                        if (currentScriptFile != null) {
                            scripts.put(currentScriptFile, scripts.get(currentScriptFile)); // ensure present
                            scripts.replace(currentScriptFile, scripts.get(currentScriptFile), codeBuffer.toString());
                            // Note: Map<String, String> is used for both filenames and content? Need separate map for content.
                            // So refactor: use another Map<String,String> for content.
                            // We'll fix this below.
                        }
                        currentScriptFile = null;
                        inCodeBlock = false;
                        codeBuffer.setLength(0);
                        continue;
                    }
                    codeBuffer.append(line).append(System.lineSeparator());
                }
            }
        }
    }

    // The above has a problem: scripts map is currently Map<script_file, result_file>.
    // We need to separate storing script content from storing result file.
    // Let's add a separate map for script content:
    
    private final LinkedHashMap<String, String> scriptContents = new LinkedHashMap<>();
    
    public void parseMarkdownRefactored(Path markdownPath) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(markdownPath)) {
            String line;
            String currentScriptFile = null;
            boolean inCodeBlock = false;
            StringBuilder codeBuffer = new StringBuilder();

            while ((line = reader.readLine()) != null) {
                if (!inCodeBlock) {
                    Matcher matcher = EX_NAME_PATTERN.matcher(line);
                    if (matcher.find()) {
                        currentScriptFile = matcher.group(1);
                        String resultFile = matcher.group(2);
                        scripts.put(currentScriptFile, resultFile);
                        codeBuffer.setLength(0);
                        continue;
                    }
                    if (currentScriptFile != null && line.trim().startsWith("```")) {
                        inCodeBlock = true;
                        continue;
                    }
                } else {
                    if (line.trim().startsWith("```")) {
                        if (currentScriptFile != null) {
                            scriptContents.put(currentScriptFile, codeBuffer.toString());
                        }
                        currentScriptFile = null;
                        inCodeBlock = false;
                        codeBuffer.setLength(0);
                        continue;
                    }
                    codeBuffer.append(line).append(System.lineSeparator());
                }
            }
        }
    }

    /**
     * Step 2: Write parsed scripts to files.
     * 
     * @throws IOException if writing fails
     */
    public void writeScriptFiles() throws IOException {
        for (var entry : scriptContents.entrySet()) {
            String fileName = entry.getKey();
            String content = entry.getValue();
            Path path = Paths.get(fileName);
            Files.writeString(path, content);
            System.out.println("Wrote script file: " + fileName);
            // Try to set executable permission on Unix-like systems
            try {
                Set<PosixFilePermission> perms = Files.getPosixFilePermissions(path);
                perms.add(PosixFilePermission.OWNER_EXECUTE);
                Files.setPosixFilePermissions(path, perms);
            } catch (UnsupportedOperationException ignored) {}
        }
    }

    /**
     * Step 3: Generate a Makefile to run all extracted scripts sequentially.
     * If result file is specified, use it as target and run script only if result missing.
     * Otherwise, use a PHONY target for the script that always runs.
     * 
     * @param makefilePath Path to output Makefile.
     * @throws IOException if writing fails
     */
    public void generateMakefile(Path makefilePath) throws IOException {
        if (Files.exists(makefilePath)) {
            System.out.println("Makefile already exists at " + makefilePath + ", skipping generation.");
            return;
        }

        try (BufferedWriter writer = Files.newBufferedWriter(makefilePath)) {
            writer.write("# Auto-generated Makefile\n");

            // Collect all result files that are non-null and non-empty
            List<String> resultFiles = new ArrayList<>();
            for (String res : scripts.values()) {
                if (res != null && !res.isEmpty()) {
                    resultFiles.add(res);
                }
            }
            if (resultFiles.isEmpty()) {
                // fallback: use script files as phony targets
                writer.write("all: " + String.join(" ", scriptContents.keySet()) + "\n\n");
            } else {
                writer.write("all: " + String.join(" ", resultFiles) + "\n\n");
            }

            writer.write(".PHONY: all clean");
            // PHONY targets for scripts without result files:
            List<String> phonyScripts = new ArrayList<>();
            for (Map.Entry<String,String> e : scripts.entrySet()) {
                if (e.getValue() == null || e.getValue().isEmpty()) {
                    phonyScripts.add(e.getKey());
                }
            }
            if (!phonyScripts.isEmpty()) {
                writer.write(" " + String.join(" ", phonyScripts));
            }
            writer.write("\n\n");

            // Write rules
            for (Map.Entry<String,String> e : scripts.entrySet()) {
                String scriptFile = e.getKey();
                String resultFile = e.getValue();

                if (resultFile == null || resultFile.isEmpty()) {
                    // PHONY target: always run script
                    writer.write(scriptFile + ":\n");
                    writer.write("\t./" + scriptFile + "\n\n");
                } else {
                    // resultFile depends on scriptFile
                    writer.write(resultFile + ": " + scriptFile + "\n");
                    writer.write("\t./" + scriptFile + "\n\n");
                }
            }

            // Clean target removes all script files and result files (if any)
            writer.write("clean:\n");
            writer.write("\trm -f ");
            List<String> allFiles = new ArrayList<>(scriptContents.keySet());
            allFiles.addAll(resultFiles);
            writer.write(String.join(" ", allFiles));
            writer.write("\n");
        }
        System.out.println("Generated Makefile.");
    }

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.err.println("Usage: java CodeExtractor <input.md>");
            System.exit(1);
        }

        CodeExtractor extractor = new CodeExtractor();

        // Step 1: Parse Markdown (use refactored method)
        extractor.parseMarkdownRefactored(Paths.get(args[0]));

        // Step 2: Write script files
        extractor.writeScriptFiles();

        // Step 3: Generate Makefile
        extractor.generateMakefile(Paths.get("Makefile"));
    }
}
