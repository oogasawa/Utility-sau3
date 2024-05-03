package com.github.oogasawa.utility.sau3;

import java.io.IOException;
import java.nio.file.Path;

import com.github.oogasawa.utility.cli.CliCommands;
import com.github.oogasawa.utility.sau3.configjs.DocusaurusConfigUpdator;
import com.github.oogasawa.utility.sau3.git.GitPuller;
import com.github.oogasawa.utility.sau3.git.GitStatusChecker;
import com.github.oogasawa.utility.sau3.markdown.MdGenerator;
import com.github.oogasawa.utility.sau3.markdown.MdRenamer;
import com.github.oogasawa.utility.sau3.opensearch.IndexConf;
import com.github.oogasawa.utility.sau3.opensearch.Indexer;
import com.github.oogasawa.utility.sau3.opensearch.Sitemap;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class App
{

    private static final Logger logger = LoggerFactory.getLogger(App.class);

    String      synopsis = "java -jar Utility-sau3-fat.jar <command> <options>";
    CliCommands cmds     = new CliCommands();


    
    public static void main( String[] args )
    {
        App app = new App();

        // try {
        //     LogManager.getLogManager()
        //             .readConfiguration(App.class.getClassLoader().getResourceAsStream("logging.properties"));
        // } catch (IOException e) {
        //     e.printStackTrace();
        // }

        
        app.setupCommands();

        try {

            CommandLine cl = app.cmds.parse(args);
            String command = app.cmds.getCommand();
            
            if (command == null) {
                app.cmds.printCommandList(app.synopsis);
            }
            else if (app.cmds.hasCommand(command)) {
                app.cmds.execute(command, cl);
            }
            else {
                System.err.println("The specified command is not available: " + app.cmds.getCommand());
                app.cmds.printCommandList(app.synopsis);
            }

        } catch (ParseException e) {
            System.err.println("Parsing failed.  Reason: " + e.getMessage() + "\n");
            app.cmds.printCommandHelp(app.cmds.getCommand());
        } 
            
    
    }


    
    public void setupCommands() {

        gitpullCommand();
        gitStatusCommand();
        javadocBuildAllCommand();
        javadocDeployCommand();
        mdGenerateCommand();
        mdRenameCommand();

        sauBuildAllCommand();
        sauDeployCommand();
        sauIndexCommand();
        sauUrlCommand();

        
    }

    public void sleep(int msec) {
        try {
            Thread.sleep(msec);
        } catch (InterruptedException e) {
            logger.warn("Interrupted", e);
        }
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


        this.cmds.addCommand("git:pull", opts,
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


        this.cmds.addCommand("git:status", opts,
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

        
        this.cmds.addCommand("javadoc:buildAll", opts,
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

        
        this.cmds.addCommand("javadoc:deploy", opts,
                       "Build and deploy javadoc files.",
                       (CommandLine cl)-> {
                                 
                            String srcdir = cl.getOptionValue("srcdir", System.getenv("PWD"));
                            String destdir = cl.getOptionValue("destdir", System.getenv("HOME") + "/public_html/javadoc/" + Path.of(srcdir).getFileName().toString());

                            JavadocProcessor processor = new JavadocProcessor();
                            processor.buildAndDeploy(Path.of(srcdir), Path.of(destdir));
                            logger.info("javadoc:deploy complete.");
                       });

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

        
        this.cmds.addCommand("md:generate", opts,
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

        
        this.cmds.addCommand("md:rename", opts,
                "Rename a docusaurus document.",
                (CommandLine cl) -> {

                    MdRenamer renamer = new MdRenamer();
                    String origName = cl.getOptionValue("from");
                    String newName = cl.getOptionValue("to");

                    renamer.rename(origName, newName);

                });

    }    



    
    /** Build docusaurus documents on all subdirectories.
    */
    public void sauBuildAllCommand() {
        Options opts = new Options();

        opts.addOption(Option.builder("srcBaseDir")
                        .option("s")
                        .longOpt("srcBaseDir")
                        .hasArg(true)
                        // .argName("file")
                        .desc("Source base directory. (e.g., $HOME/works)")
                        .required(true)
                        .build());

        
        opts.addOption(Option.builder("targetBaseDir")
                        .option("t")
                        .longOpt("targetBaseDir")
                        .hasArg(true)
                        // .argName("file")
                        .desc("Target base directory. (e.g. $HOME/public_html)")
                        .required(false)
                        .build());

        
        opts.addOption(Option.builder("url")
                        .option("u")
                        .longOpt("url")
                        .hasArg(true)
                        .argName("url")
                        .desc("Change the URL of the Docusaurus site.")
                        .required(false)
                        .build());

        
        this.cmds.addCommand("sau:buildAll", opts,
                "Build docusaurus documents on all subdirectories.",
                             
                (CommandLine cl) -> {

                    DocusaurusProcessor builder = new DocusaurusProcessor();
                    String srcBasedir = cl.getOptionValue("srcBaseDir", System.getenv("HOME") + "/works");
                    String targetBasedir = cl.getOptionValue("targetBaseDir", System.getenv("HOME") + "/public_html");
                    //String url = cl.getOptionValue("url");

                    builder.buildAll(Path.of(srcBasedir), Path.of(targetBasedir));
                });

    }    




    /** Build docusaurus documents on all subdirectories.
    */
    public void sauDeployCommand() {
        Options opts = new Options();

        opts.addOption(Option.builder("srcdir")
                        .option("s")
                        .longOpt("srcdir")
                        .hasArg(true)
                        .argName("srcdir")
                        .desc("docusaurus directory. (default: current directory)")
                        .required(false)
                        .build());

        opts.addOption(Option.builder("targetdir")
                        .option("t")
                        .longOpt("targetdir")
                        .hasArg(true)
                        .argName("url")
                        .desc("target directory. (default: $HOME/public_html)")
                        .required(false)
                        .build());

        
        this.cmds.addCommand("sau:deploy", opts,
                "Build docusaurus documents and deploy them on the target directory.",
                             
                (CommandLine cl) -> {


                    String srcdir = cl.getOptionValue("srcdir", System.getenv("PWD"));
                    Path srcPath = Path.of(srcdir);
                    Path docName = srcPath.getFileName();
                    String destdir = cl.getOptionValue("targetdir",
                                                         System.getenv("HOME") + "/public_html/" + docName.toString());

                    DocusaurusProcessor builder = new DocusaurusProcessor();
                    builder.buildAndDeploy(Path.of(srcdir), Path.of(destdir));
                });

    }    



    
    
    /**  docusaurus:index  */
    public void sauIndexCommand() {
        Options opts = new Options();

        opts.addOption(Option.builder("conf")
                        .option("c")
                        .longOpt("conf")
                        .hasArg(true)
                        .argName("conf")
                        .desc("Configuration file.")
                        .required(true)
                        .build());


        this.cmds.addCommand("sau:index", opts,
                       "Making a full text index of multiple Docusaurus sites.",
                       (CommandLine cl)-> {
                                 logger.info("docusaurus:index");
                            String configFile = cl.getOptionValue("conf");

                            IndexConf indexConf = new IndexConf();
                            Indexer indexer = new Indexer();
                            //indexer.deleteIndexIfExists();
                            //indexer.createIndex();
                            try {
                                indexConf.read(configFile);
                                String indexName = indexConf.getIndexName();
                                for (String sitemapUrl : indexConf.getSitemapUrls()) {
                                    logger.info(sitemapUrl);
                                    Sitemap sitemap = new Sitemap();
                                    sitemap.parse(sitemapUrl);
                                    for (String docUrl : sitemap.getDocumentUrls()) {
                                        sleep(1000); 
                                        logger.info(docUrl);
                                        indexer.index(docUrl, indexName);
                                    }
                                }
                            } catch (IOException e) {
                                logger.error(String.format("Can not read %s : %s",
                                                           configFile, e.getMessage()),
                                             e);
                            }
                       });

    }


    /**  Change the url: setting in docusaurus.config.js to the value given in the options.
     *
     */
    public void sauUrlCommand() {
        Options opts = new Options();

        opts.addOption(Option.builder("url")
                        .option("u")
                        .longOpt("url")
                        .hasArg(true)
                        .argName("url")
                        .desc("Change the URL of the Docusaurus site.")
                        .required(true)
                        .build());


        this.cmds.addCommand("sau:url", opts,
                       "Change the URL of the Docusaurus site.",
                       (CommandLine cl)-> {
                                 logger.info("docusaurus:url");
                            String url = cl.getOptionValue("url");

                            DocusaurusConfigUpdator.update(url);
                       });

    }

    


}
