package com.github.oogasawa.utility.sau3.sautest;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Utility class for parsing Markdown files to extract code blocks annotated with
 * custom HTML comments like <!-- save: ... -->, <!-- execute on: ... -->, and <!-- expected: ... -->.
 */
public class MarkdownUtils {

    public static List<Path> getMarkdownFiles(Path root) throws IOException {
        List<Path> result = new ArrayList<>();
        Files.walk(root)
             .filter(p -> p.toString().endsWith(".md"))
             .forEach(result::add);
        return result;
    }

    public static List<ParsedBlock> parseMarkdown(Path file) throws IOException {
        List<String> lines = Files.readAllLines(file);
        List<ParsedBlock> blocks = new ArrayList<>();

        boolean inComment = false;
        boolean inCode = false;
        String savePath = null;
        List<String> executeDivisions = new ArrayList<>();
        String expectedTarget = null;
        List<String> codeBuffer = new ArrayList<>();

        for (String line : lines) {
            String trimmed = line.trim();

            if (trimmed.startsWith("<!--")) {
                inComment = true;
                if (trimmed.contains("save:")) {
                    savePath = extractDirectiveValue(trimmed, "save:");
                } else if (trimmed.contains("execute on:")) {
                    executeDivisions.clear();
                } else if (trimmed.contains("expected:")) {
                    expectedTarget = currentSaveTarget;
                }
                continue;
            }

            if (inComment && trimmed.startsWith("-")) {
                executeDivisions.add(trimmed.replace("-", "").trim());
                continue;
            }

            if (inComment && trimmed.endsWith("-->")) {
                inComment = false;
                continue;
            }

            if ((savePath != null || !executeDivisions.isEmpty() || expectedTarget != null) && trimmed.startsWith("```") && !inCode) {
                inCode = true;
                codeBuffer.clear();
                continue;
            }

            if (inCode && trimmed.startsWith("```") && !trimmed.equals("```")) {
                inCode = false;
                if (savePath != null) {
                    blocks.add(new ParsedBlock(ParsedBlock.Type.SAVE, savePath, new ArrayList<>(codeBuffer), file.toString()));
                    currentSaveTarget = savePath;
                    savePath = null;
                } else if (!executeDivisions.isEmpty()) {
                    for (String div : executeDivisions) {
                        blocks.add(new ParsedBlock(ParsedBlock.Type.EXECUTE, div, new ArrayList<>(codeBuffer), file.toString()));
                    }
                    executeDivisions.clear();
                } else if (expectedTarget != null) {
                    blocks.add(new ParsedBlock(ParsedBlock.Type.EXPECTED, expectedTarget, new ArrayList<>(codeBuffer), file.toString()));
                    expectedTarget = null;
                }
                codeBuffer.clear();
                continue;
            }

            if (inCode) {
                codeBuffer.add(line);
            }
        }

        return blocks;
    }

    private static String currentSaveTarget = null;

    private static String extractDirectiveValue(String line, String key) {
        int idx = line.indexOf(key);
        if (idx != -1) {
            return line.substring(idx + key.length()).replace("-->", "").trim();
        }
        return null;
    }
}
