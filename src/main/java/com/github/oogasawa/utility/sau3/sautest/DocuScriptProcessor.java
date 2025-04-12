package com.github.oogasawa.utility.sau3.sautest;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * DocuScriptProcessor is a unified tool that parses Markdown files to:
 * <ul>
 *   <li>Save code blocks after <!-- save: path --> comments to the specified file path</li>
 *   <li>Execute code blocks after <!-- execute on: ... --> comments if matching a target division</li>
 *   <li>Compare script output with expected text defined by <!-- expected: XX% match --></li>
 * </ul>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * java DocuScriptProcessor <markdown_dir> <output_dir> [division]
 * }</pre>
 *
 * <p>If division is specified, only execute blocks that match that division.</p>
 */
public class DocuScriptProcessor {

    public static void main(String[] args) throws IOException, InterruptedException {
        String targetDir = args.length >= 1 ? args[0] : ".";
        String outputDir = args.length >= 2 ? args[1] : "output";
        String division = args.length >= 3 ? args[2] : null;

        if (division == null) {
            processAllSaveBlocks(Paths.get(targetDir), Paths.get(outputDir));
        } else {
            processAllExecuteBlocks(Paths.get(outputDir), division);
        }

        System.out.println("[INFO] All operations completed.");
    }

    public static void processAllSaveBlocks(Path rootDir, Path outputDir) throws IOException {
        List<Path> files = MarkdownUtils.getMarkdownFiles(rootDir);
        Map<String, Set<String>> execMap = new LinkedHashMap<>();

        for (Path file : files) {
            List<ParsedBlock> blocks = MarkdownUtils.parseMarkdown(file);
            for (ParsedBlock block : blocks) {
                if (block.type == ParsedBlock.Type.SAVE) {
                    saveBlock(block, outputDir);
                    execMap.putIfAbsent(block.pathOrDivision, new LinkedHashSet<>());
                }
                if (block.type == ParsedBlock.Type.EXECUTE) {
                    execMap.computeIfAbsent(block.pathOrDivision, k -> new LinkedHashSet<>()).add(block.pathOrDivision);
                }
                if (block.type == ParsedBlock.Type.EXPECTED) {
                    saveExpectedOutput(block, outputDir);
                }
            }
        }

        Path mappingFile = outputDir.resolve("exec-map.tsv");
        try (BufferedWriter writer = Files.newBufferedWriter(mappingFile)) {
            for (Map.Entry<String, Set<String>> entry : execMap.entrySet()) {
                for (String division : entry.getValue()) {
                    writer.write(entry.getKey() + "\t" + division);
                    writer.newLine();
                }
            }
        }
        System.out.println("[META] exec-map.tsv written to " + mappingFile);
    }

    private static void saveExpectedOutput(ParsedBlock block, Path outputRoot) throws IOException {
        Path scriptPath = outputRoot.resolve(block.pathOrDivision);
        Path expectedPath = scriptPath.resolveSibling(scriptPath.getFileName().toString() + ".expected.txt");
        Files.createDirectories(expectedPath.getParent());
        Files.write(expectedPath, block.codeLines);
        System.out.println("[EXPECTED] " + expectedPath);
    }

    public static void processAllExecuteBlocks(Path outputDir, String division) throws IOException, InterruptedException {
        Path mappingFile = outputDir.resolve("exec-map.tsv");
        if (!Files.exists(mappingFile)) {
            System.err.println("[WARN] exec-map.tsv not found: " + mappingFile);
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(mappingFile)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\t");
                if (parts.length == 2 && parts[1].equals(division)) {
                    Path scriptPath = outputDir.resolve(parts[0]);
                    runScriptWithExpectedComparison(scriptPath);
                }
            }
        }
    }

    private static void runScriptWithExpectedComparison(Path scriptPath) throws IOException, InterruptedException {
        if (!Files.exists(scriptPath)) {
            System.err.println("[ERROR] Script not found: " + scriptPath);
            return;
        }

        File scriptFile = scriptPath.toFile();
        scriptFile.setExecutable(true);

        ProcessBuilder pb = new ProcessBuilder(scriptPath.toAbsolutePath().toString());
        pb.redirectErrorStream(true);
        Process proc = pb.start();

        String output;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            output = sb.toString();
        }

        int exit = proc.waitFor();
        System.out.printf("[EXEC] %s [EXIT CODE: %d]%n", scriptPath.getFileName(), exit);

        Path expectedPath = scriptPath.resolveSibling(scriptPath.getFileName().toString() + ".expected.txt");
        if (Files.exists(expectedPath)) {
            String expected = Files.readString(expectedPath);
            int match = computeMatchPercentage(output, expected);
            System.out.printf("[COMPARE] Output match: %d%%%n", match);
        }
    }

    private static int computeMatchPercentage(String actual, String expected) {
        String a = actual.strip();
        String b = expected.strip();

        int distance = levenshteinDistance(a, b);
        int maxLength = Math.max(a.length(), b.length());

        return maxLength == 0 ? 100 : (100 * (maxLength - distance)) / maxLength;
    }

    private static int levenshteinDistance(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];

        for (int i = 0; i <= a.length(); i++) {
            for (int j = 0; j <= b.length(); j++) {
                if (i == 0) dp[i][j] = j;
                else if (j == 0) dp[i][j] = i;
                else dp[i][j] = Math.min(Math.min(
                        dp[i - 1][j] + 1,
                        dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + (a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1));
            }
        }
        return dp[a.length()][b.length()];
    }

    private static void saveBlock(ParsedBlock block, Path outputRoot) throws IOException {
        Path path = outputRoot.resolve(block.pathOrDivision);
        Files.createDirectories(path.getParent());
        Files.write(path, block.codeLines);
        System.out.println("[SAVED] " + path);
    }

    private static void runScript(Path scriptPath) throws IOException, InterruptedException {
        runScriptWithExpectedComparison(scriptPath);
    }

    private static void runBlock(ParsedBlock block) throws IOException, InterruptedException {
        File tempScript = File.createTempFile("docutest_", ".sh");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempScript))) {
            writer.write("#!/bin/bash\n");
            for (String line : block.codeLines) {
                writer.write(line);
                writer.newLine();
            }
        }
        tempScript.setExecutable(true);

        System.out.printf("[EXEC] %s from %s%n", block.pathOrDivision, block.sourceFile);
        ProcessBuilder pb = new ProcessBuilder(tempScript.getAbsolutePath());
        pb.inheritIO();
        Process proc = pb.start();
        int exit = proc.waitFor();
        System.out.printf("[EXIT] code = %d%n", exit);
        tempScript.delete();
    }
}


