package com.github.oogasawa.utility.sau3;

import java.io.IOException;
import java.nio.file.Path;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class SauCommands {

    private static final Logger logger = LoggerFactory.getLogger(SauCommands.class);
    
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

        sauBuildAllCommand();
        sauDeployCommand();
        sauIndexCommand();
        sauUpdateIndexCommand();
        sauUrlCommand();
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

        
        this.cmdRepos.addCommand("sau:buildAll", opts,
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

        
        this.cmdRepos.addCommand("sau:deploy", opts,
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


        this.cmdRepos.addCommand("sau:index", opts,
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
                                logger.error(String.format("Can not read %s : %s",
                                                           configFile, e.getMessage()),
                                             e);
                            }
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


        this.cmdRepos.addCommand("sau:updateIndex", opts,
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


        this.cmdRepos.addCommand("sau:url", opts,
                       "Change the URL of the Docusaurus site.",
                       (CommandLine cl)-> {
                                 logger.info("docusaurus:url");
                            String url = cl.getOptionValue("url");

                            DocusaurusConfigUpdator.update(url);
                       });

    }


    
    public void sleep(int msec) {
        try {
            Thread.sleep(msec);
        } catch (InterruptedException e) {
            logger.warn("Interrupted", e);
        }
    }


    
}
