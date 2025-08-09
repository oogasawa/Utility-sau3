package com.github.oogasawa.utility.sau3.sautest;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.github.oogasawa.utility.cli.CommandRepository;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;




public class SauTestCommands {

    private static final Logger logger = Logger.getLogger(SauTestCommands.class.getName());
    
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

        sautestGenerateCommand();
        sautestRunCommand();

    }

    

    public void sautestGenerateCommand() {
        Options opts = new Options();

        opts.addOption(Option.builder("markdownRootDir")
                        .option("d")
                        .longOpt("baseDir")
                        .hasArg(true)
                        .argName("baseDir")
                        .desc("Document base directory. (e.g., $HOME/works/nigsc_homepage4/docs)")
                        .required(false)
                        .build());

        
        opts.addOption(Option.builder("codeDir")
                        .option("c")
                        .longOpt("codeDir")
                        .hasArg(true)
                        .argName("file")
                        .desc("Code output directory. (e.g. $HOME/testgen)")
                        .required(true)
                        .build());

        
        this.cmdRepos.addCommand("sautest:generate", opts,
                "Saves the content of a SAVE-type block in markdown files.",
                             
                (CommandLine cl) -> {

                    Path markdownRootDir = Path.of(cl.getOptionValue("markdownRootDir", System.getenv("PWD")));
                    Path codeDir = Path.of(cl.getOptionValue("codeDir"));

                    try {
                        DocuScriptProcessor.processAllSaveBlocks(markdownRootDir, codeDir);
                    } catch (IOException e) {
                        logger.log(Level.SEVERE, "Can not save code blocks:", e);
                    }
                });

    }    

    


    public void sautestRunCommand() {
        Options opts = new Options();

        opts.addOption(Option.builder("codeDir")
                        .option("c")
                        .longOpt("codeDir")
                        .hasArg(true)
                        .argName("codeDir")
                        .desc("Test code directory. (default: current directory)")
                        .required(false)
                        .build());


        opts.addOption(Option.builder("division")
                        .option("s")
                        .longOpt("division")
                        .hasArg(true)
                        .argName("division")
                        .desc("Target division (e.g. general_analysis, personal_genome, DDBJ_service)")
                        .required(false)
                        .build());

        
        this.cmdRepos.addCommand("sautest:run", opts,
                       "Build and deploy javadoc files.",
                       (CommandLine cl)-> {
                                 
                            Path codeDir = Paths.get(cl.getOptionValue("codeDir", System.getenv("PWD")));
                            String division = cl.getOptionValue("division");

                            try {
                                DocuScriptProcessor.processAllExecuteBlocks(codeDir, division);
                            } catch (IOException | InterruptedException e) {
                                logger.log(Level.SEVERE, "Can not execute test code blocks: ", e);
                            }
                       });

    }


}
