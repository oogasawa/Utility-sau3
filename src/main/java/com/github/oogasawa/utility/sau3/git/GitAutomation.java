package com.github.oogasawa.utility.sau3.git;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GitAutomation {

    private static final Logger logger = LoggerFactory.getLogger(GitAutomation.class);
    
    public static void pushAll(Path fileList) {
        // ファイル名（対象ディレクトリリストのファイル）
        //String fileName = "directories.txt";
        
        // 日付ベースのコミットメッセージを生成
        String commitMessage = "Update: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        
        // ファイルから対象ディレクトリを取得
        List<String> directories = getDirectoriesFromFile(fileList);
        
        if (directories.isEmpty()) {
            logger.warn("No directories to process. Please check the file: " + fileList.toString());
            return;
        }
        
        // 各ディレクトリで git コマンドを実行
        for (String dir : directories) {
            if (executeGitCommands(dir, commitMessage)) {
                System.out.println("Successfully processed: " + dir);
            } else {
                System.out.println("Failed to process: " + dir);
            }
        }
    }

    private static List<String> getDirectoriesFromFile(Path fileList) {
        List<String> directories = new ArrayList<>();
        
        try {
            List<String> lines = Files.readAllLines(fileList);
            for (String line : lines) {
                // コメント行や空行をスキップ
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                directories.add(line);
            }
        } catch (IOException e) {
            logger.error("Error reading file: " + fileList.toString());
            e.printStackTrace();
        }
        
        return directories;
    }

    private static boolean executeGitCommands(String directory, String commitMessage) {
        try {
            // `git add .`
            if (!runCommand(directory, "git add .")) return false;
            
            // `git commit -m "commitMessage"`
            if (!runCommand(directory, "git commit -m \"" + commitMessage + "\"")) return false;
            
            // `git push`
            if (!runCommand(directory, "git push")) return false;
            
            return true;
        } catch (Exception e) {
            logger.error("Error executing git commands in directory: " + directory);
            e.printStackTrace();
            return false;
        }
    }

    private static boolean runCommand(String directory, String command) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder();
        builder.command("sh", "-c", command);
        builder.directory(new File(directory));
        Process process = builder.start();
        
        // 出力を表示
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
             BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
            while ((line = errorReader.readLine()) != null) {
                System.err.println(line);
            }
        }
        
        return process.waitFor() == 0;
    }
}

