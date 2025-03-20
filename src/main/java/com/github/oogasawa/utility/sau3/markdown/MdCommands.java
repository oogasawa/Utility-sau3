package com.github.oogasawa.utility.sau3.markdown;

import com.github.oogasawa.utility.cli.CommandRepository;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class MdCommands {

    private static final Logger logger = LoggerFactory.getLogger(MdCommands.class);
    
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

        mdGenerateCommand();
        mdRenameCommand();
    }


    
    
    /**  Generate a docusaurus document template.
     *
     */
    public void mdGenerateCommand() {
        Options opts = new Options();


        opts.addOption(Option.builder("docId")
                        .option("id")
                        .longOpt("docId")
                        .hasArg(true)
                        .argName("docId")
                        .desc("Document ID (e.g. Content_220720_oo01)")
                        .required(true)
                        .build());

        
        this.cmdRepos.addCommand("markdown commands", "md:generate", opts,
                "Generate a docusaurus document template.",
                (CommandLine cl) -> {

                    String docId = cl.getOptionValue("docId");
                    MdGenerator generator = new MdGenerator();
                    generator.generate(docId);

                });

    }


    
    /** Rename a docusaurus document.
     *
     * <h4>Prerequisites:</h4>
     * 
     * <ul>
     * <li>The markdown text should be placed in an independent directory along with
     * the image files and other files used in it. In other words, one directory
     * corresponds to one markdown file.</li>
     * <li>This directory and markdown file should have the same name. The ID of the
     * markdown file should be the same string as the file name.</li>
     * </ul>
     *
     * An example of the prerequisites is as follows.
     * 
     * <pre>{@code 
     * $ tree
     * .
     * └── 080_ProcessDB_230720_akats01
     *     ├── 080_ProcessDB_230720_akats01.md
     *     ├── process_db_1.png
     *     ├── process_db_2.png
     *     ├── process_db_3.png
     *     └── process_db_4.png
     * 
     * }</pre>
     *
     *
     * where the ID of {@code  080_ProcessDB_230720_akats01.md} is {@code 080_ProcessDB_230720_akats01},
     * this can be verified as follows.
     *
     * <pre>{@code 
     * $ head -n 5 080_ProcessDB_230720_akats01/080_ProcessDB_230720_akats01.md 
     * ---
     * id: 080_ProcessDB_230720_akats01
     * title: "Process Database"
     * ---
     *
     * }</pre>
     *
     * <h4>How to start the program</h4>
     *
     *
     * If you call the program as follows,
     * the program changes the name of a directory,
     * the name of a file in that directory,
     * and the ID in the file (they all have the same name, which is specifired in the argument of {@code -f})
     * to the name given by the `-t' argument.
     * 
     * <pre>{@code
     * java -jar Utility-sau-fat.jar md:changeName -f 000_ProcessDB_230720_akats01 -t 010_ProcessDB_230720_akats01 
     * }</pre>
     * 
     * 
     */
    public void mdRenameCommand() {
        Options opts = new Options();

        
        opts.addOption(Option.builder("from")
                        .option("f")
                        .longOpt("from")
                        .hasArg(true)
                        .argName("from")
                        .desc("Document name before conversion")
                        .required(true)
                        .build());

        opts.addOption(Option.builder("to")
                        .option("t")
                        .longOpt("to")
                        .hasArg(true)
                        .argName("to")
                        .desc("Document name after conversion")
                        .required(true)
                        .build());

        
        this.cmdRepos.addCommand("markdown commands", "md:rename", opts,
                "Rename a docusaurus document.",
                (CommandLine cl) -> {

                    MdRenamer renamer = new MdRenamer();
                    String origName = cl.getOptionValue("from");
                    String newName = cl.getOptionValue("to");

                    renamer.rename(origName, newName);

                });

    }    



    

}
