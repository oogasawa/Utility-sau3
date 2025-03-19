package com.github.oogasawa.utility.sau3;

import java.nio.file.Path;
import com.github.oogasawa.utility.cli.CommandRepository;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class JavadocCommands {

    private static final Logger logger = LoggerFactory.getLogger(JavadocCommands.class);
    
    /**
     * The command repository used to register commands.
     */
    CommandRepository cmdRepos = null;
    
    /**
     * Registers all JAR-related commands in the given command repository.
     * 
     * @param cmds The command repository to register commands with.
     */
    public void setupCommands(CommandRepository cmds) {
        this.cmdRepos = cmds;

        javadocBuildAllCommand();
        javadocDeployCommand();

    }

    



    
    /** Build javadoc on all subdirectories.
    */
    public void javadocBuildAllCommand() {
        Options opts = new Options();

        opts.addOption(Option.builder("srcBaseDir")
                        .option("s")
                        .longOpt("srcBaseDir")
                        .hasArg(true)
                        // .argName("file")
                        .desc("Source base directory. (e.g., $HOME/works)")
                        .required(false)
                        .build());

        
        opts.addOption(Option.builder("targetBaseDir")
                        .option("t")
                        .longOpt("targetBaseDir")
                        .hasArg(true)
                        // .argName("file")
                        .desc("Target base directory. (e.g. $HOME/public_html/javadoc)")
                        .required(false)
                        .build());

        
        this.cmdRepos.addCommand("javadoc:buildAll", opts,
                "Build javadoc on all subdirectories.",
                             
                (CommandLine cl) -> {

                    JavadocProcessor builder = new JavadocProcessor();
                    String srcBasedir = cl.getOptionValue("srcBaseDir", System.getenv("HOME") + "/works");
                    String targetBasedir = cl.getOptionValue("targetBaseDir", System.getenv("HOME") + "/public_html/javadoc");

                    builder.buildAll(Path.of(srcBasedir), Path.of(targetBasedir));
                });

    }    

    


    public void javadocDeployCommand() {
        Options opts = new Options();

        opts.addOption(Option.builder("srcdir")
                        .option("s")
                        .longOpt("srcdir")
                        .hasArg(true)
                        .argName("srcdir")
                        .desc("The directory where the project is located. (default: current directory)")
                        .required(false)
                        .build());


        opts.addOption(Option.builder("destdir")
                        .option("d")
                        .longOpt("destdir")
                        .hasArg(true)
                        .argName("destdir")
                        .desc("The directory to which the javadoc files are deployed. (default: $HOME/public_html/javadoc/projectname)")
                        .required(false)
                        .build());

        
        this.cmdRepos.addCommand("javadoc:deploy", opts,
                       "Build and deploy javadoc files.",
                       (CommandLine cl)-> {
                                 
                            String srcdir = cl.getOptionValue("srcdir", System.getenv("PWD"));
                            String destdir = cl.getOptionValue("destdir", System.getenv("HOME") + "/public_html/javadoc/" + Path.of(srcdir).getFileName().toString());

                            JavadocProcessor processor = new JavadocProcessor();
                            processor.buildAndDeploy(Path.of(srcdir), Path.of(destdir));
                            logger.info("javadoc:deploy complete.");
                       });

    }


}
