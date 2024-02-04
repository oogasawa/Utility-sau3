package com.github.oogasawa.utility.sau3.markdown;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MdGenerator {

    private static final Logger logger = Logger.getLogger(MdGenerator.class.getName());
    
    /**  Default constructor. */
    public MdGenerator() {}
    

    /** Removes the leading digits of docId, if it exists.
     *
     * <p>
     * If the docID is prefixed with a number, return the string with the number removed. If not, the original string is returned.
     * </p>
     * 
     * <p>
     * <h4>Example:</h4>
     * Convert from {@code 010_Troubleshoot_230826_oo01}  to {@code Troubleshoot_230826_oo01}.
     * </p>
     *
     * @param docId A document ID.
     * @return Unnumbered document ID.
    */
    public static String UnnumberedDocId(String docId) {

        String result = docId;
        
        Pattern p = Pattern.compile("^[0-9]+[_-](.+)");
        Matcher m = p.matcher(docId);
        if(m.matches()) {
            result = m.group(1);
        }

        return result;
    }


    
    public void generate(String docId) {

        String outfile = docId + ".md";

        try {
            // 1, make a document directory
            Path currentDir = Path.of(System.getenv("PWD"));
            Process p = new ProcessBuilder("mkdir", docId).directory(currentDir.toFile()).start();
            p.waitFor();

            // 2, generate the markdown file.

            List<String> lines = new ArrayList<String>();
            lines.add("---");
            lines.add("id: " + UnnumberedDocId(docId));
            lines.add("---");

            Path mdFilePath = currentDir.resolve(docId).resolve(docId + ".md");
            Files.write(mdFilePath, lines);
        }
        catch (IOException e) {
            logger.log(Level.SEVERE, "Can not create a file " + docId + ".md", e);
        }
        catch (InterruptedException e) {
            logger.log(Level.WARNING, "Interrupted", e);
        }
    }






}
