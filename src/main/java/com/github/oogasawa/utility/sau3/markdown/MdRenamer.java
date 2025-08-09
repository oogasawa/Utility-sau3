package com.github.oogasawa.utility.sau3.markdown;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;


/** This class is for renaming a Docusaurus document.
 */
public class MdRenamer {

    
    private static final Logger logger = Logger.getLogger(MdRenamer.class.getName());

    
    /** A document name before renaming.
     *
     * <p>
     * Example: {@code sec_BDD_220515_oo01}
     * </p>
     */
    String fromName = null;


    /** A document name before renaming.
     *
     * <p>
     * Example: {@code 010_BDD_220515_oo01}
     * </p>
     */
    String toName = null;


    /** Default constructor. */
    public MdRenamer() {}
    
    
    /** A constructor.
     * 
     * @param fromName  An original document name (e.g. {@code sec_BDD_220515_oo01})
     * @param toName  A new document name (e.g. {@code 010_BDD_220515_oo01})
     */ 
    public MdRenamer(String fromName, String toName) {
        this.fromName = fromName;
        this.toName = toName;        
    }
    
    
    static class Builder {
        String fromName = null;
        String toName = null; 

        Builder(String fromName, String toName) {
            this.fromName = fromName;
            this.toName = toName;
        }


        MdRenamer build() {
            return new MdRenamer(this.fromName, this.toName);
        }
    }    


    public void rename(String origName, String newName, Path dirPath) {

        // 1. Rename document directory
        // Here, it is assumed that document directory is the same name as origName.
        Path origDocDirPath = dirPath.resolve(origName);
        Path newDocDirPath = renameDocDir(origDocDirPath, newName);

        // 2. Rename markdown file.
        
        // Get original path of markdown file.
        Path origMdPath = searchMdPath(newDocDirPath);
        Path newMdPath = renameMdFile(origMdPath, newName);
        
        // 3. Rename document ID.
        renameDocId(newMdPath, newName);

    }


    public void rename(String origName, String newName) {
        rename(origName, newName, Path.of(System.getenv("PWD")));
    }
    


    /** Find document files (markdown format files) under the document directory and return their paths.
     *
     * <p>
     * The current convention is to give the document directory and the document file the same name,
     * but for old documents, the document file may have a name like doc.md, or the names may not match,
     * so this method search for a markdown file under the document directory to find and return the path to the document file.
     * </p>
     *
     * @param docDirPath An absolute path of document directory.
     * @return An absolute path of a document file written in markdown format.
     */
    public Path searchMdPath(Path docDirPath) {

        File docFileObj = Stream.of(docDirPath.toFile().listFiles()).filter(f -> {
            return f.isFile() && f.getName().endsWith(".md");
        }).findFirst().get();

        return Path.of(docFileObj.getAbsolutePath());

    }
    



    /** Rename the directory corresponding to the specified document.
     *
     * @param docDirPath A document directory. (e.g.{@code /home/you/sau_Doc01/docs/010_TestDoc_230725_oo01 } )
     *        Either full path or relative path is acceptable.
     *        If a relative path is given, it is assumed to be from the current directory.
     * @param newName New name of the document directory. (e.g. {@code new_name})
     * 
     * @return A {@code Path} object of the renamed directory. (e.g. {@code /home/you/sau_Doc01/docs/new_name})
     */
    public Path renameDocDir(Path docDirPath, String newName) {
        
        if (!docDirPath.isAbsolute()) {
            docDirPath = Path.of(System.getenv("PWD")).resolve(docDirPath);
        }

        Path newDirPath = docDirPath.getParent().resolve(newName);
        logger.fine("original document directory path: " + docDirPath.toString());
        logger.fine("new document directory path: " + newDirPath.toString());
        
        docDirPath.toFile().renameTo(new File(newDirPath.toString()));

        return docDirPath.getParent().resolve(newName);
    }
    

    /** Rename the file corresponding to the specified document.
     *
     * @param origMdPath A {@code Path} object of original file.
     *                   (e.g.{@code /home/you/sau_Doc01/docs/010_TestDoc_230725_oo01/doc.md } )
     * @param newName    New document name. (e.g. {@code new_name}. Extention is not included.)
     * @return The renamed file path.
     */    
    public Path renameMdFile(Path origMd, String newName) {

        Path newMd = origMd.getParent().resolve(newName + ".md");
        origMd.toFile().renameTo(new File(newMd.toString()));

        return newMd;
    }


    /** Rename the document ID in a markdown file.
     *
     * <p>
     * Replace docId written at the head of the document with the given string.
     * </p>
     *
     * <pre>{@code
     * ---
     * id: 010_BuildPath_230729_oo01
     * title: "The title of the document"
     * ---
     *
     * ... body of the document ...
     * }</pre>
     * 
     * @param filePath A Path object of a markdown file (in which the docId is written.).
     * @param newId A new document ID.
     */
    public void renameDocId(Path filePath, String newId) {

        // 1. Create a temporary markdown file.
        Path tmpfile = null;
        try {
            Path dirPath = filePath.getParent();
            File fObj = File.createTempFile("tmp", ".md", new File(dirPath.toString()));

            tmpfile = Path.of(fObj.toString());
        }
        catch (IOException e) {
            logger.log(Level.SEVERE, "Can not create a tempfile", e);
        }


        // 2. Copy the contents of the original markdown file
        //    to the temporary markdown file,
        //    with replacing the document ID.
        try (BufferedReader br = Files.newBufferedReader(filePath);
            BufferedWriter bw = Files.newBufferedWriter(tmpfile, StandardCharsets.UTF_8)) {

            Pattern pDocId = Pattern.compile("^id:\\s*(\\S+)");
            Pattern pHeaderMark = Pattern.compile("^---");
            
            int status = 0;
            String line = null;
            while ((line = br.readLine()) != null) {
                
                if (status < 2) {
                    Matcher m = pHeaderMark.matcher(line);
                    if (m.find()) {
                        status++;
                    }

                    m = pDocId.matcher(line);
                    if (m.find()) {
                        line = "id: " + MdGenerator.UnnumberedDocId(newId); 
                    }   
                }                
                
                bw.write(line);
                bw.write("\n");
            }
        }
        catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to copy a document to a temporary file.", e);
        }

        // 3. Rename the temporary markdown file to the original name.
        File origFileObj = new File(filePath.toString());
        origFileObj.delete();
        File tmpFileObj = new File(tmpfile.toString());
        tmpFileObj.renameTo(origFileObj);

    }


    
    
}
