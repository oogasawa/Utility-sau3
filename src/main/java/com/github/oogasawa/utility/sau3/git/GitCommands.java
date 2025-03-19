package com.github.oogasawa.utility.sau3.git;

import java.nio.file.Path;
import com.github.oogasawa.utility.cli.CommandRepository;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

public class GitCommands {

    /**
     * The command repository used to register commands.
     */
    CommandRepository cmdRepos = null;
    

    public void setupCommands(CommandRepository cmds) {

        gitpullCommand();
        gitPushAllCommand();
        gitStatusCommand();

    }
    

    /**  Execute git status command on each subdirectory.
     *
     */
    public void gitPushAllCommand() {
        Options opts = new Options();

        opts.addOption(Option.builder("list")
                        .option("l")
                        .longOpt("list")
                        .hasArg(true)
                        .argName("list")
                        .desc("A file containing the list of target directories.")
                        .required(false)
                        .build());


        this.cmdRepos.addCommand("git:pushAll", opts,
                       "Execute git push command on each subdirectory.",
                (CommandLine cl) -> {

                    String dirList = cl.getOptionValue("list");
                    GitAutomation.pushAll(Path.of(dirList));
                });

    }


    

    /**  Execute git pull on each subdirectory.
     *
     */
    public void gitpullCommand() {
        Options opts = new Options();

        opts.addOption(Option.builder("dir")
                        .option("d")
                        .longOpt("dir")
                        .hasArg(true)
                       
                        .argName("dir")
                        .desc("Target base directory.")
                        .required(false)
                        .build());


        this.cmdRepos.addCommand("git:pull", opts,
                       "Execute git pull on each subdirectory.",
                       (CommandLine cl)-> {
                                 GitPuller git = new GitPuller();
                                 String dir = cl.getOptionValue("dir");
                                 if (dir == null) {
                                     dir = System.getenv("PWD");
                                 }

                                 git.pullAll(Path.of(dir));
                       });
    }





    /**  Execute git status command on each subdirectory.
     *
     */
    public void gitStatusCommand() {
        Options opts = new Options();

        opts.addOption(Option.builder("dir")
                        .option("d")
                        .longOpt("dir")
                        .hasArg(true)
                        .argName("dir")
                        .desc("Target base directory.")
                        .required(false)
                        .build());


        this.cmdRepos.addCommand("git:status", opts,
                       "Execute git status command on each subdirectory.",
                (CommandLine cl) -> {

                    GitStatusChecker checker = new GitStatusChecker();
                    String dir = cl.getOptionValue("dir");
                    if (dir == null) {
                        dir = System.getenv("PWD");
                    }

                    checker.check(Path.of(dir));

                });

    }


  // ...
}
