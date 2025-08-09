package com.github.oogasawa.utility.sau3.git;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.github.oogasawa.utility.process.ProcessFacade;
import com.github.oogasawa.utility.process.ProcessFacade.StdioData;
import com.github.oogasawa.utility.process.ProcessFacade.StdioMode;


public class GitPuller {

     private static final Logger logger = Logger.getLogger(GitPuller.class.getName());


    public void pullAll(Path baseDir) {
        try {
            Files.list(baseDir).sorted().filter((Path p) -> {
                return p.toFile().isDirectory();
            }).forEach((Path p) -> {

                ProcessFacade pf = new ProcessFacade();

                System.out.println("## " + p.toString());
                StdioData result = pf.directory(p)
                    .environment("LANG", "en_US.UTF-8")
                    .stdioMode(StdioMode.INHERIT_AND_STORE)
                    .exec("git", "pull");
                });

        } catch (IOException e) {
            logger.log(Level.SEVERE, "IOException", e);
        }
    }
    
}
