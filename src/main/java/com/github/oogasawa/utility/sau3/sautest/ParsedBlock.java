package com.github.oogasawa.utility.sau3.sautest;

import java.util.List;

/**
 * Represents a parsed code block from a Markdown file.
 * Can be one of:
 * <ul>
 *   <li>SAVE: to be written to a file</li>
 *   <li>EXECUTE: to be executed for a given division</li>
 *   <li>EXPECTED: expected output for a test script</li>
 * </ul>
 */
public class ParsedBlock {

    public enum Type {
        SAVE,
        EXECUTE,
        EXPECTED
    }

    public final Type type;

    /**
     * For SAVE: relative file path to save to  
     * For EXECUTE: the division name to run on  
     * For EXPECTED: the relative path of the corresponding script (same as SAVE target)
     */
    public final String pathOrDivision;

    /** Lines of code or expected output content */
    public final List<String> codeLines;

    /** The source Markdown file where this block was extracted */
    public final String sourceFile;

    public ParsedBlock(Type type, String pathOrDivision, List<String> codeLines, String sourceFile) {
        this.type = type;
        this.pathOrDivision = pathOrDivision;
        this.codeLines = codeLines;
        this.sourceFile = sourceFile;
    }
}
