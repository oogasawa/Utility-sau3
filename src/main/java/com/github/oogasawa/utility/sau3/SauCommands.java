package com.github.oogasawa.utility.sau3;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.github.oogasawa.utility.cli.CommandRepository;
import com.github.oogasawa.utility.sau3.configjs.DocusaurusConfigUpdator;
import com.github.oogasawa.utility.sau3.opensearch.DateChecker;
import com.github.oogasawa.utility.sau3.opensearch.IndexConf;
import com.github.oogasawa.utility.sau3.opensearch.Indexer;
import com.github.oogasawa.utility.sau3.opensearch.Sitemap;
import com.github.oogasawa.utility.sau3.opensearch.SitemapEntry;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;



public class SauCommands {

    private static final Logger logger = Logger.getLogger(SauCommands.class.getName());
    
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

        sauBuildCommand();
        sauDeployCommand();
        sauBatchDeployCommand();
        sauIndexCommand();
        sauStartCommand();
        sauUpdateIndexCommand();
    }


    

    public void sauBuildCommand() {
        Options opts = new Options();

        this.cmdRepos.addCommand("Docusaurus commands", "sau:build", opts,
                "Build the Docusaurus project with filtered output.",
                                 
                (CommandLine cl) -> {
                    DocusaurusProcessor.build();
                });

    }    

    

    public void sauDeployCommand() {
        Options opts = new Options();

        opts.addOption(Option.builder("dest")
                       .option("d")
                       .longOpt("dest")
                       .hasArg(true)
                       .argName("dest")
                       .desc("Destination directory. (e.g. /var/www/html, $HOME/public_html; default is $HOME/public_html)")
                       .required(false)
                       .build());

        opts.addOption(Option.builder("destServer")
                       .option("s")
                       .longOpt("destServer")
                       .hasArg(true)
                       .argName("destServer")
                       .desc("Destination server name/IP address (e.g. 133.39.114.45)")
                       .required(false)
                       .build());

        opts.addOption(Option.builder("destDir")
                       .longOpt("destDir")
                       .hasArg(true)
                       .argName("destDir")
                       .desc("Remote destination directory path (e.g. $HOME/public_html, /var/www/html)")
                       .required(false)
                       .build());

        opts.addOption(Option.builder("sourceDir")
                       .longOpt("sourceDir")
                       .hasArg(true)
                       .argName("sourceDir")
                       .desc("Docusaurus project source directory (default: current directory)")
                       .required(false)
                       .build());

        opts.addOption(Option.builder("baseUrl")
                       .longOpt("baseUrl")
                       .hasArg(true)
                       .argName("baseUrl")
                       .desc("Base URL for Docusaurus site (e.g. /~username/projectname/; default: auto-generated)")
                       .required(false)
                       .build());


        this.cmdRepos.addCommand("Docusaurus commands", "sau:deploy", opts,
                "Build the Docusaurus project with filtered output and deploy it to the public_html directory.",

                (CommandLine cl) -> {
                    String dest = cl.getOptionValue("dest");
                    String destServer = cl.getOptionValue("destServer");
                    String destDir = cl.getOptionValue("destDir");
                    String sourceDir = cl.getOptionValue("sourceDir", System.getProperty("user.dir"));
                    String baseUrl = cl.getOptionValue("baseUrl");
                    DocusaurusProcessor.deploy(dest, destServer, destDir, sourceDir, baseUrl);
                });

    }



    public void sauBatchDeployCommand() {
        Options opts = new Options();

        opts.addOption(Option.builder("conf")
                       .option("c")
                       .longOpt("conf")
                       .hasArg(true)
                       .argName("conf")
                       .desc("Configuration file containing project list (default: docusaurus_ja.conf)")
                       .required(false)
                       .build());

        opts.addOption(Option.builder("destServer")
                       .option("s")
                       .longOpt("destServer")
                       .hasArg(true)
                       .argName("destServer")
                       .desc("Destination server name/IP address for all projects")
                       .required(false)
                       .build());

        opts.addOption(Option.builder("baseDir")
                       .longOpt("baseDir")
                       .hasArg(true)
                       .argName("baseDir")
                       .desc("Base directory where all projects are located (default: current directory)")
                       .required(false)
                       .build());

        opts.addOption(Option.builder("skipGitPull")
                       .longOpt("skipGitPull")
                       .hasArg(false)
                       .desc("Skip git pull for all projects")
                       .required(false)
                       .build());

        this.cmdRepos.addCommand("Docusaurus commands", "sau:batchDeploy", opts,
                "Batch deploy multiple Docusaurus projects from configuration file.",

                (CommandLine cl) -> {
                    String configFile = cl.getOptionValue("conf", "docusaurus_ja.conf");
                    String destServer = cl.getOptionValue("destServer");
                    String baseDir = cl.getOptionValue("baseDir", System.getProperty("user.dir"));
                    boolean skipGitPull = cl.hasOption("skipGitPull");

                    DocusaurusProcessor.batchDeploy(configFile, destServer, baseDir, skipGitPull);
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


        this.cmdRepos.addCommand("Docusaurus commands", "sau:index", opts,
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
                                    for (SitemapEntry entry : sitemap.getSitemapEntries()) {
                                        sleep(1000);
                                        logger.info(String.format("%s, %s", entry.getUrl(), entry.getLastmod()));
                                        indexer.index(entry.getUrl(), indexName);
                                    }
                                }
                            } catch (IOException e) {
                                logger.log(Level.SEVERE, String.format("Can not read %s : %s",
                                                           configFile, e.getMessage()),
                                             e);
                            }
                       });

    }

    

    public void sauStartCommand() {
        Options opts = new Options();

        this.cmdRepos.addCommand("Docusaurus commands", "sau:start", opts,
                "Start the Docusaurus development server with filtered output and automatic port conflict handling.",                             
                (CommandLine cl) -> {
                    DocusaurusProcessor.startDocusaurus();
                });

    }    




    
    /**  docusaurus:update_index  */
    public void sauUpdateIndexCommand() {
        Options opts = new Options();

        opts.addOption(Option.builder("conf")
                        .option("c")
                        .longOpt("conf")
                        .hasArg(true)
                        .argName("conf")
                        .desc("Configuration file.")
                        .required(true)
                        .build());


        this.cmdRepos.addCommand("Docusaurus commands", "sau:updateIndex", opts,
                       "Update a full text index of multiple Docusaurus sites.",
                       (CommandLine cl)-> {
                            logger.info("docusaurus:updateIndex");
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
                                    for (SitemapEntry entry : sitemap.getSitemapEntries()) {
                                        if (entry.getLastmod() == null) {
                                            continue;
                                        }
                                        else if(DateChecker.isWithinLastNDays(entry.getLastmod(), 3)) {
                                            sleep(1000);
                                            logger.info(String.format("%s, %s", entry.getUrl(), entry.getLastmod()));
                                            indexer.index(entry.getUrl(), indexName);
                                        }
                                    }
                                }
                            } catch (IOException e) {
                                logger.log(Level.SEVERE, String.format("Can not read %s : %s",
                                                           configFile, e.getMessage()),
                                             e);
                            }
                       });

        
    }



    



    
    public void sleep(int msec) {
        try {
            Thread.sleep(msec);
        } catch (InterruptedException e) {
            logger.log(Level.WARNING, "Interrupted", e);
        }
    }


    
}
