package com.github.oogasawa.utility.sau3;

import com.github.oogasawa.utility.cli.CommandRepository;
import com.github.oogasawa.utility.sau3.git.GitCommands;
import com.github.oogasawa.utility.sau3.markdown.MdCommands;
import com.github.oogasawa.utility.sau3.sautest.SauTestCommands;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class App
{

    private static final Logger logger = LoggerFactory.getLogger(App.class);

    String      synopsis = "java -jar Utility-sau3-fat.jar <command> <options>";
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
