package com.github.oogasawa.utility.sau3.gemini;

import java.io.IOException;
import com.github.oogasawa.utility.cli.CommandRepository;
import com.github.oogasawa.utility.sau3.configjs.DocusaurusConfigUpdator;
import com.github.oogasawa.utility.sau3.gemini.GeminiParaphrase;
import com.github.oogasawa.utility.sau3.gemini.ToEnglish;
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



public class GeminiCommands {

    private static final Logger logger = LoggerFactory.getLogger(GeminiCommands.class);
    
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

        geminiDefineCommand();
        geminiParaphraseCommand();
        geminiRunCommand();
        geminiToEnglishCommand();
        geminiToJapaneseCommand();

    }


    

    public void geminiDefineCommand() {
        Options opts = new Options();

        opts.addOption(Option.builder("model")
                .option("m")
                .longOpt("model")
                .hasArg(true)
                .argName("model")
                .desc("An AI model name (e.g. gemini-1.5-flush).")
                .required(false)
                .build());

        opts.addOption(Option.builder("apikey")
                .option("k")
                .longOpt("apikey")
                .hasArg(true)
                .argName("apikey")
                .desc("Your API key.")
                .required(true)
                .build());


        
        this.cmdRepos.addCommand("Gemini commands", "gemini:define", opts,
                "Return definition of a word or a phrase using Gemini AI",                             
                (CommandLine cl) -> {
                    String model  = cl.getOptionValue("model", "gemini-1.5-flush");
                    String apikey = cl.getOptionValue("apikey");
                                     
                    try {
                        GeminiDefine.define(model, apikey);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
        
    }    


    
    public void geminiParaphraseCommand() {
        Options opts = new Options();


        opts.addOption(Option.builder("model")
                .option("m")
                .longOpt("model")
                .hasArg(true)
                .argName("model")
                .desc("An AI model name (e.g. gemini-1.5-flush).")
                .required(false)
                .build());

        opts.addOption(Option.builder("apikey")
                .option("k")
                .longOpt("apikey")
                .hasArg(true)
                .argName("apikey")
                .desc("Your API key.")
                .required(true)
                .build());

        
        this.cmdRepos.addCommand("Gemini commands", "gemini:paraphrase", opts,
                "Rephrase English text into alternative expressions with similar meaning using Gemini AI",                             
                (CommandLine cl) -> {
                    try {
                        
                        String model  = cl.getOptionValue("model", "gemini-1.5-flush");
                        String apikey = cl.getOptionValue("apikey");
                        GeminiParaphrase.suggest(model, apikey);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

    }    


    
    public void geminiRunCommand() {
        Options opts = new Options();


        opts.addOption(Option.builder("model")
                .option("m")
                .longOpt("model")
                .hasArg(true)
                .argName("model")
                .desc("An AI model name (e.g. gemini-1.5-flush).")
                .required(false)
                .build());

        opts.addOption(Option.builder("apikey")
                .option("k")
                .longOpt("apikey")
                .hasArg(true)
                .argName("apikey")
                .desc("Your API key.")
                .required(true)
                .build());

        
        this.cmdRepos.addCommand("Gemini commands", "gemini:run", opts,
                "Simply run gemini with the given prompt.",
                (CommandLine cl) -> {
                    try {                        
                        String model  = cl.getOptionValue("model", "gemini-1.5-flush");
                        String apikey = cl.getOptionValue("apikey");
                        GeminiPromptRunner.execute(model, apikey);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

    }    



    
    public void geminiToEnglishCommand() {
        Options opts = new Options();

        opts.addOption(Option.builder("model")
                .option("m")
                .longOpt("model")
                .hasArg(true)
                .argName("model")
                .desc("An AI model name (e.g. gemini-1.5-flush).")
                .required(false)
                .build());

        opts.addOption(Option.builder("apikey")
                .option("k")
                .longOpt("apikey")
                .hasArg(true)
                .argName("apikey")
                .desc("Your API key.")
                .required(true)
                .build());


        
        this.cmdRepos.addCommand("Gemini commands", "gemini:toEnglish", opts,
                "Translate Japanese to English with gemini AI",                             
                (CommandLine cl) -> {
                    try {

                        String model  = cl.getOptionValue("model", "gemini-1.5-flush");
                        String apikey = cl.getOptionValue("apikey");

                        
                        ToEnglish.translate(model, apikey);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

    }    


        
    public void geminiToJapaneseCommand() {
        Options opts = new Options();

        opts.addOption(Option.builder("model")
                .option("m")
                .longOpt("model")
                .hasArg(true)
                .argName("model")
                .desc("An AI model name (e.g. gemini-1.5-flush).")
                .required(false)
                .build());

        opts.addOption(Option.builder("apikey")
                .option("k")
                .longOpt("apikey")
                .hasArg(true)
                .argName("apikey")
                .desc("Your API key.")
                .required(true)
                .build());


        
        this.cmdRepos.addCommand("Gemini commands", "gemini:toJapanese", opts,
                "Translate English to Japanese with gemini AI",                             
                (CommandLine cl) -> {
                    try {

                        String model  = cl.getOptionValue("model", "gemini-1.5-flush");
                        String apikey = cl.getOptionValue("apikey");
                        
                        ToJapanese.translate(model, apikey);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
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
