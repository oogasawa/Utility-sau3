package com.github.oogasawa.utility.sau3;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import com.github.oogasawa.utility.cli.CliCommands;
import com.github.oogasawa.utility.sau3.configjs.DocusaurusConfigUpdator;
import com.github.oogasawa.utility.sau3.opensearch.IndexConf;
import com.github.oogasawa.utility.sau3.opensearch.Indexer;
import com.github.oogasawa.utility.sau3.opensearch.Sitemap;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;




public class App
{

    private static final Logger logger = Logger.getLogger(App.class.getName());

    String      synopsis = "java -jar Utility-sau3-fat.jar <command> <options>";
    CliCommands cmds     = new CliCommands();


    
    public static void main( String[] args )
    {
        App app = new App();

        try {
            LogManager.getLogManager()
                    .readConfiguration(App.class.getClassLoader().getResourceAsStream("logging.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        
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
    
        docusaurusIndexCommand();

    }

    public void sleep(int msec) {
        try {
            Thread.sleep(msec);
        } catch (InterruptedException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    
    /**  docusaurus:index  */
    public void docusaurusIndexCommand() {
        Options opts = new Options();

        opts.addOption(Option.builder("conf")
                        .option("c")
                        .longOpt("conf")
                        .hasArg(true)
                        .argName("conf")
                        .desc("Configuration file.")
                        .required(true)
                        .build());


        this.cmds.addCommand("docusaurus:index", opts,
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
                                        sleep(2000); 
                                        logger.info(docUrl);
                                        indexer.index(docUrl, indexName);
                                    }
                                }
                            } catch (IOException e) {
                                logger.log(Level.SEVERE,
                                           String.format("Can not read %s : %s", configFile, e.getMessage()), e);
                            }
                       });

    }


    /**  Change the url: setting in docusaurus.config.js to the value given in the options.
     *
     */
    public void docusaurusUrlCommand() {
        Options opts = new Options();

        opts.addOption(Option.builder("url")
                        .option("u")
                        .longOpt("url")
                        .hasArg(true)
                        .argName("url")
                        .desc("Change the URL of the Docusaurus site.")
                        .required(true)
                        .build());


        this.cmds.addCommand("docusaurus:url", opts,
                       "Change the URL of the Docusaurus site.",
                       (CommandLine cl)-> {
                                 logger.info("docusaurus:url");
                            String url = cl.getOptionValue("url");

                            DocusaurusConfigUpdator.update(url);
                       });

    }

    


}
