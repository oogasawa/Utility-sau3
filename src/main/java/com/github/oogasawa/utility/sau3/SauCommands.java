package com.github.oogasawa.utility.sau3;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.github.oogasawa.utility.cli.CommandRepository;
import com.github.oogasawa.utility.cli.UtilityCliHelpFormatterBuilder;
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
        sauIndexWithMappingCommand();
    }


    

    public void sauBuildCommand() {
        Options opts = new Options();

        this.cmdRepos.addCommand("Docusaurus commands", "sau:build", opts,
                "Build the Docusaurus project with filtered output.",
                                 
                (CommandLine cl) -> {
                    DocusaurusProcessor.build();
                });

        registerHelp("sau:build",
                java.util.List.of("""
Build the Docusaurus project with filtered output. Noise from npm is filtered so you only see warnings and errors.
"""),
                java.util.List.of("""
sau3.java sau:build
  Run from the project root to generate production assets under the build/ directory.
"""));

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

        registerHelp("sau:deploy",
                java.util.List.of("""
Build the Docusaurus project on the local workstation and optionally push it to a remote server.
""",
                        """
Common flags:
  - --dest: parent directory for local deployment (defaults to ~/public_html).
  - --destServer: remote server in user@host form; omit to deploy locally.
  - --destDir: document root on the remote server (defaults to $HOME/public_html).
  - --sourceDir: Docusaurus project directory (defaults to current directory).
  - --baseUrl: base URL injected into docusaurus.config.js before building.
"""),
                java.util.List.of("""
sau3.java sau:deploy --destServer web-admin@web1.example.org --destDir /var/www/html/docs \\n  --sourceDir ~/works/docs/site --baseUrl /docs/doc_Analyst001/en/
"""));

        registerHelp("sau:deploy",
                java.util.List.of("""
Build the Docusaurus project on the local workstation and optionally push it to a remote server.
""",
                        """
Common flags:
  - --dest: parent directory for local deployment (defaults to ~/public_html).
  - --destServer: remote server in user@host form; omit to deploy locally.
  - --destDir: document root on the remote server (defaults to $HOME/public_html).
  - --sourceDir: Docusaurus project directory (defaults to current directory).
  - --baseUrl: base URL injected into docusaurus.config.js before building.
"""),
                java.util.List.of("""
sau3.java sau:deploy --destServer web-admin@web1.example.org --destDir /var/www/html/docs \\n  --sourceDir ~/works/docs/site --baseUrl /docs/doc_Analyst001/en/
"""));

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

        registerHelp("sau:batchDeploy",
                java.util.List.of("""
Batch deploy multiple Docusaurus projects based on a configuration file.
"""),
                java.util.List.of("""
sau3.java sau:batchDeploy --conf docusaurus_en.conf --baseDir ~/works/docs \\n  --destServer web-admin@web1.example.org
"""));

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
                                // Try to read from resources first, then fallback to file system
                                try {
                                    indexConf.readFromResource(configFile);
                                } catch (IOException e) {
                                    logger.info("Resource not found, trying file system: " + configFile);
                                    indexConf.read(configFile);
                                }
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

        registerHelp("sau:index",
                java.util.List.of("""
Create a full-text index by crawling the sitemaps listed in a configuration file.
"""),
                java.util.List.of("""
sau3.java sau:index --conf docusaurus_ja.conf
  Reads each sitemap URL in the config and indexes the referenced pages into OpenSearch.
"""));

    }

    

    public void sauStartCommand() {
        Options opts = new Options();

        this.cmdRepos.addCommand("Docusaurus commands", "sau:start", opts,
                "Start the Docusaurus development server with filtered output and automatic port conflict handling.",                             
                (CommandLine cl) -> {
                    DocusaurusProcessor.startDocusaurus();
                });

        registerHelp("sau:start",
                java.util.List.of("""
Start the Docusaurus development server with filtered output and automatic port conflict handling.
"""),
                java.util.List.of("""
sau3.java sau:start
  Launches npm run start -- --port 3000 with automatic port retry logic and log filtering.
"""));

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


        // Add days option for updateIndex
        Options updateOpts = new Options();
        updateOpts.addOption(Option.builder("c")
                        .longOpt("conf")
                        .hasArg(true)
                        .argName("conf")
                        .desc("Configuration files (comma-separated for multiple configs)")
                        .required(true)
                        .build());
        updateOpts.addOption(Option.builder("d")
                        .longOpt("days")
                        .hasArg(true)
                        .argName("days")
                        .desc("Number of days to look back for updates (default: 3)")
                        .required(false)
                        .build());

        this.cmdRepos.addCommand("Docusaurus commands", "sau:updateIndex", updateOpts,
                       "Update a full text index of multiple Docusaurus sites.",
                       (CommandLine cl)-> {
                            logger.info("docusaurus:updateIndex");
                            String configFiles = cl.getOptionValue("conf");
                            int daysBack = Integer.parseInt(cl.getOptionValue("days", "3")); // Default: 3 days
                            logger.info("Looking for updates within last " + daysBack + " days");

                            String[] configs = configFiles.split(",");

                            for (String configFile : configs) {
                                configFile = configFile.trim();
                                logger.info("Processing config: " + configFile);

                                IndexConf indexConf = new IndexConf();
                                Indexer indexer = new Indexer();
                                //indexer.deleteIndexIfExists();
                                //indexer.createIndex();
                                try {
                                    // Try to read from resources first, then fallback to file system
                                    try {
                                        indexConf.readFromResource(configFile);
                                    } catch (IOException e) {
                                        logger.info("Resource not found, trying file system: " + configFile);
                                        indexConf.read(configFile);
                                    }
                                    String indexName = indexConf.getIndexName();
                                    for (String sitemapUrl : indexConf.getSitemapUrls()) {
                                        logger.info(sitemapUrl);
                                        Sitemap sitemap = new Sitemap();
                                        sitemap.parse(sitemapUrl);
                                        for (SitemapEntry entry : sitemap.getSitemapEntries()) {
                                            if (entry.getLastmod() == null) {
                                                continue;
                                            }
                                            else if(DateChecker.isWithinLastNDays(entry.getLastmod(), daysBack)) {
                                                // Check if document already exists with same timestamp
                                                if (!indexer.documentExistsWithSameTimestamp(entry.getUrl(), entry.getLastmod(), indexName)) {
                                                    sleep(1000);
                                                    logger.info(String.format("Indexing: %s, %s", entry.getUrl(), entry.getLastmod()));
                                                    indexer.index(entry.getUrl(), indexName);
                                                }
                                            }
                                        }
                                    }
                                } catch (IOException e) {
                                    logger.log(Level.SEVERE, String.format("Can not process %s : %s",
                                                               configFile, e.getMessage()), e);
                                }
                            }
                       });

        registerHelp("sau:updateIndex",
                java.util.List.of("""
Update a full-text index by reindexing pages that changed recently.
"""),
                java.util.List.of("""
sau3.java sau:updateIndex --conf docusaurus_en.conf --days 7
  Reindexes pages modified within the last 7 days for each site in the configuration file.
"""));

    }


    /**  sau:indexWithMapping  */
    public void sauIndexWithMappingCommand() {
        Options opts = new Options();

        opts.addOption(Option.builder("conf")
                        .option("c")
                        .longOpt("conf")
                        .hasArg(true)
                        .argName("conf")
                        .desc("Configuration files (comma-separated for multiple configs)")
                        .required(true)
                        .build());

        opts.addOption(Option.builder("mapping")
                        .option("m")
                        .longOpt("mapping")
                        .hasArg(true)
                        .argName("mapping")
                        .desc("Mapping JSON files (comma-separated, optional)")
                        .required(false)
                        .build());

        this.cmdRepos.addCommand("Docusaurus commands", "sau:indexWithMapping", opts,
                       "Create ElasticSearch mapping and index from multiple configuration files.",
                       (CommandLine cl)-> {
                            logger.info("sau:indexWithMapping");
                            String configFiles = cl.getOptionValue("conf");
                            String mappingFiles = cl.getOptionValue("mapping");

                            String[] configs = configFiles.split(",");
                            String[] mappings = mappingFiles != null ? mappingFiles.split(",") : null;

                            for (int i = 0; i < configs.length; i++) {
                                String configFile = configs[i].trim();
                                String mappingFile = mappings != null && i < mappings.length ?
                                                   mappings[i].trim() : null;

                                logger.info("Processing config: " + configFile +
                                          (mappingFile != null ? " with mapping: " + mappingFile : ""));

                                IndexConf indexConf = new IndexConf();
                                Indexer indexer = new Indexer();

                                try {
                                    // Try to read from resources first, then fallback to file system
                                    try {
                                        indexConf.readFromResource(configFile);
                                    } catch (IOException e) {
                                        logger.info("Resource not found, trying file system: " + configFile);
                                        indexConf.read(configFile);
                                    }

                                    String indexName = indexConf.getIndexName();

                                    // Create mapping if specified
                                    if (mappingFile != null) {
                                        logger.info("Creating ElasticSearch mapping for index: " + indexName);
                                        createElasticSearchMapping(indexName, mappingFile);
                                    }

                                    // Index documents
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
                                    logger.log(Level.SEVERE, String.format("Can not process %s : %s",
                                                               configFile, e.getMessage()), e);
                                }
                            }
                       });

        registerHelp("sau:indexWithMapping",
                java.util.List.of("""
Create ElasticSearch mapping and index from multiple configuration files.
"""),
                java.util.List.of("""
sau3.java sau:indexWithMapping --conf configs/docs.conf,configs/blog.conf --mapping mappings/docs.json,mappings/blog.json
  Applies each mapping file before indexing the corresponding site content into ElasticSearch.
"""));
    }

    private void createElasticSearchMapping(String indexName, String mappingFile) {
        try {
            // Use curl command to create mapping (similar to shell script)
            String[] curlCommand = {
                "curl", "-X", "PUT",
                "-H", "Content-Type: application/json",
                "-d", "@" + mappingFile,
                "http://localhost:9200/" + indexName
            };

            ProcessBuilder pb = new ProcessBuilder(curlCommand);
            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                logger.info("Successfully created mapping for index: " + indexName);
            } else {
                logger.log(Level.WARNING, "Failed to create mapping for index: " + indexName);
            }

        } catch (IOException | InterruptedException e) {
            logger.log(Level.SEVERE, "Exception while creating mapping for index: " + indexName, e);
        }
    }



    



    
    private void registerHelp(String command, List<String> descriptionBlocks, List<String> exampleBlocks) {
        UtilityCliHelpFormatterBuilder builder = new UtilityCliHelpFormatterBuilder()
                .clearSections()
                .addUsageSection("Usage");

        if (descriptionBlocks != null && !descriptionBlocks.isEmpty()) {
            builder.addCustomSection("Description", descriptionBlocks);
        }

        if (exampleBlocks != null && !exampleBlocks.isEmpty()) {
            builder.addCustomSection("Examples", exampleBlocks);
        }

        builder.addOptionsSection("Options");
        this.cmdRepos.configureCommandHelpFormatter(command, builder);
    }


    public void sleep(int msec) {
        try {
            Thread.sleep(msec);
        } catch (InterruptedException e) {
            logger.log(Level.WARNING, "Interrupted", e);
        }
    }


    
}
