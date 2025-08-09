package com.github.oogasawa.utility.sau3.ex;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermission;
import java.util.*;
import java.util.regex.*;

/**
 * Parses Markdown to extract scripts and generates script files and a Makefile.
 */
public class CodeExtractor {

    private static final Pattern EX_NAME_PATTERN = Pattern.compile("<!--\\s*ex:name\\s+(\\S+)\\s*-->");
    
    /** Parsed scripts: Map of filename -> script content */
    private final LinkedHashMap<String, String> scripts = new LinkedHashMap<>();

    /**
     * Step 1: Parse Markdown input and extract scripts.
     * 
     * @param markdownPath path to the Markdown file
     * @throws IOException if IO error occurs
     */
    public void parseMarkdown(Path markdownPath) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(markdownPath)) {
            String line;
            String currentFileName = null;
            boolean inCodeBlock = false;
            StringBuilder codeBuffer = new StringBuilder();

            while ((line = reader.readLine()) != null) {
                if (!inCodeBlock) {
                    Matcher matcher = EX_NAME_PATTERN.matcher(line);
                    if (matcher.find()) {
                        currentFileName = matcher.group(1);
                        codeBuffer.setLength(0);
                        continue;
                    }
                    if (currentFileName != null && line.trim().startsWith("```")) {
                        inCodeBlock = true;
                        continue;
                    }
                } else {
                    if (line.trim().startsWith("```")) {
                        if (currentFileName != null) {
                            scripts.put(currentFileName, codeBuffer.toString());
                        }
                        currentFileName = null;
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
        for (var entry : scripts.entrySet()) {
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
     * 
     * @param makefilePath Path to output Makefile.
     * @throws IOException if writing fails
     */
    public void generateMakefile(Path makefilePath) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(makefilePath)) {
            writer.write("# Auto-generated Makefile\n");
            writer.write("SCRIPTS := " + String.join(" ", scripts.keySet()) + "\n\n");
            writer.write("all: $(SCRIPTS)\n\n");
            for (String script : scripts.keySet()) {
                writer.write(script + ":\n");
                writer.write("\t./" + script + "\n\n");
            }
            writer.write("clean:\n");
            writer.write("\trm -f " + String.join(" ", scripts.keySet()) + "\n");
        }
        System.out.println("Generated Makefile.");
    }

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.err.println("Usage: java MarkdownProcessor <input.md>");
            System.exit(1);
        }

        CodeExtractor processor = new CodeExtractor();

        // Step 1: Parse Markdown
        processor.parseMarkdown(Paths.get(args[0]));

        // Step 2: Generate script files
        processor.writeScriptFiles();

        // Step 3: Generate Makefile
        processor.generateMakefile(Paths.get("Makefile"));
    }
}

