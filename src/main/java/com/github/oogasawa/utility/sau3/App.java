package com.github.oogasawa.utility.sau3;

import com.github.oogasawa.utility.cli.CommandRepository;
import com.github.oogasawa.utility.sau3.ex.ExCommands;
import com.github.oogasawa.utility.sau3.gemini.GeminiCommands;
import com.github.oogasawa.utility.sau3.git.GitCommands;
import com.github.oogasawa.utility.sau3.markdown.MdCommands;
import com.github.oogasawa.utility.sau3.sautest.SauTestCommands;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import java.util.logging.Logger;


public class App
{

    private static final Logger logger = Logger.getLogger(App.class.getName());

    String      synopsis = "java -jar Utility-sau3-VERSION.jar <command> <options>";
    CommandRepository cmdRepos     = new CommandRepository();


    
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

            CommandLine cl = app.cmdRepos.parse(args);
            String command = app.cmdRepos.getGivenCommand();
            
            if (command == null) {
                app.cmdRepos.printCommandList(app.synopsis);
            }
            else if (app.cmdRepos.hasCommand(command)) {
                app.cmdRepos.execute(command, cl);
            }
            else {
                System.err.println("The specified command is not available: " + app.cmdRepos.getGivenCommand());
                app.cmdRepos.printCommandList(app.synopsis);
            }

        } catch (ParseException e) {
            System.err.println("Parsing failed.  Reason: " + e.getMessage() + "\n");
            app.cmdRepos.printCommandHelp(app.cmdRepos.getGivenCommand());
        } 
            
    
    }


    
    public void setupCommands() {

        ExCommands exCommands = new ExCommands();
        exCommands.setupCommands(this.cmdRepos);
        
        GeminiCommands geminiCommands = new GeminiCommands();
        geminiCommands.setupCommands(this.cmdRepos);
        
        GitCommands gitCommands = new GitCommands();
        gitCommands.setupCommands(this.cmdRepos);


        JavadocCommands javadocCommands = new JavadocCommands();
        javadocCommands.setupCommands(this.cmdRepos);


        MdCommands mdCommands = new MdCommands();
        mdCommands.setupCommands(this.cmdRepos);


        SauCommands sauCommands = new SauCommands();
        sauCommands.setupCommands(this.cmdRepos);

        SauTestCommands sauTestCommands = new SauTestCommands();
        sauTestCommands.setupCommands(this.cmdRepos);

        
    }

}
