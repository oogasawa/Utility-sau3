package com.github.oogasawa.utility.sau3.markdown;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MdGenerator {

     private static final Logger logger = LoggerFactory.getLogger(MdGenerator.class);
    
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


    /**
     * Generates a markdown file inside a directory named after the given document ID.
     * <p>
     * If the directory does not exist, it will be created. If the markdown file already exists,
     * this method does nothing.
     * </p>
     *
     * @param docId The unique identifier for the document. This will be used as the directory
     *              and file name.
     */
    public void generate(String docId) {
        Path currentDir = Path.of(System.getenv("PWD"));
        Path docDir = currentDir.resolve(docId);
        Path mdFilePath = docDir.resolve(docId + ".md");

        try {
            // Create the directory if it does not exist
            if (!Files.exists(docDir)) {
                Files.createDirectories(docDir);
            }

            // If the file already exists, do nothing
            if (Files.exists(mdFilePath)) {
                logger.info("File already exists: " + mdFilePath);
                return;
            }

            // Create and write to the markdown file
            List<String> lines = new ArrayList<>();
            lines.add("---");
            lines.add("id: " + UnnumberedDocId(docId));
            lines.add("---");

            Files.write(mdFilePath, lines);
            logger.info("File created: " + mdFilePath);
        } catch (IOException e) {
            logger.error("Can not create a file " + mdFilePath, e);
        }
    }
    
    


}
