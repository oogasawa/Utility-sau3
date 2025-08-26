package com.github.oogasawa.utility.sau3.ex;


import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.github.oogasawa.utility.cli.CommandRepository;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;



public class ExCommands {

    private static final Logger logger = Logger.getLogger(ExCommands.class.getName());
    
    
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

        exExtractCommand();
        exMakefileCommand();
        
    }
    

    public void exExtractCommand() {
        Options opts = new Options();

        opts.addOption(Option.builder("i")
                .longOpt("infile")
                .hasArg(true)
                .argName("FILENAME")
                .desc("An input markdown file name.")
                .required(true)
                .build());

        this.cmdRepos.addCommand("Ex commands", "ex:extract", opts,
                "Extract scripts from a markdown file.",                             
                (CommandLine cl) -> {
                    String infile  = cl.getOptionValue("infile");


                    CodeExtractor extractor = new CodeExtractor();
                    try {
                        // Step 1: Parse Markdown
                        extractor.parseMarkdown(Paths.get(infile));
                        // Step 2: Generate script files
                        extractor.writeScriptFiles();
                    } catch (IOException e) {
                        logger.log(Level.SEVERE, "Can not open the file", e);
                    }
                });
        
    }    



    public void exMakefileCommand() {
        Options opts = new Options();

        opts.addOption(Option.builder("i")
                .longOpt("infile")
                .hasArg(true)
                .argName("FILENAME")
                .desc("An input markdown file name.")
                .required(true)
                .build());

        this.cmdRepos.addCommand("Ex commands", "ex:makefile", opts,
                "Generate a Makefile from a markdown file.",                             
                (CommandLine cl) -> {
                    String infile  = cl.getOptionValue("infile");

                    CodeExtractor extractor = new CodeExtractor();
                    try {
                        // Step 1: Parse Markdown
                        extractor.parseMarkdown(Paths.get(infile));
                        // Step 2: ... skip
                        // Step 3: Generate Makefile
                        extractor.generateMakefile(Paths.get("Makefile"));
                    } catch (IOException e) {
                        logger.log(Level.SEVERE, "Can not open the file", e);
                    }
                });
        
    }    


    
}
