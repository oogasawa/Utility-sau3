package com.github.oogasawa.utility.sau3.opensearch;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;



/** This class reads the configuration file and sets its value to the field of this class.
 *
 * An example of a configuration file to be read is as follows
 * 
 * <pre>{@code
 * [index]
 * docusaurus_ja
 * 
 * [sitemap urls]
 * https://sc.ddbj.nig.ac.jp/sitemap.xml
 * http://localhost/~oogasawa/doc_Analyst001/sitemap.xml
 * http://localhost/~oogasawa/doc_CPP001/sitemap.xml
 * http://localhost/~oogasawa/doc_DBMS001/sitemap.xml
 * http://localhost/~oogasawa/doc_Infra001/sitemap.xml
 * http://localhost/~oogasawa/doc_Java001/sitemap.xml
 * http://localhost/~oogasawa/doc_SCI001/sitemap.xml
 * http://localhost/~oogasawa/doc_SCI002/sitemap.xml
 * http://localhost/~oogasawa/doc_SCI003/sitemap.xml
 * http://localhost/~oogasawa/doc_TypeScript001/sitemap.xml
 * http://localhost/~oogasawa/sau_Bioinfo01/sitemap.xml
 * http://localhost/~oogasawa/sau_English01/sitemap.xml
 * http://localhost/~oogasawa/sau_Utility/sitemap.xml
 * http://localhost/~oogasawa/sau_WebBLAST001/sitemap.xml
 * }</pre>
 *
 *
*/

public class IndexConf {


    String indexName = "docusaurus_ja";
    List<String> sitemapUrls = new ArrayList<>();


    public String getIndexName() {
        return this.indexName;
    }

    
    public List<String> getSitemapUrls() {
        return this.sitemapUrls;
    }


    /**
     * Read configuration from the specified path.
     * First tries to read from filesystem, then falls back to resources.
     *
     * @param configPath Path to configuration file (filesystem path or resource name)
     * @throws IOException if file cannot be read from either location
     */
    public void readConfigFile(String configPath) throws IOException {
        // Try filesystem first
        java.nio.file.Path path = java.nio.file.Paths.get(configPath);
        if (java.nio.file.Files.exists(path)) {
            try (BufferedReader reader = new BufferedReader(new FileReader(configPath))) {
                this.read(reader);
                return;
            }
        }

        // Fallback to resources
        try (InputStream is = getClass().getResourceAsStream("/" + configPath);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            if (is == null) {
                throw new IOException("Configuration file not found in filesystem or resources: " + configPath);
            }
            this.read(reader);
        }
    }

    /**
     * Read configuration from filesystem.
     * @deprecated Use readConfigFile() instead for automatic filesystem/resource handling
     */
    @Deprecated
    public void read(String file) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            this.read(reader);
        }
    }

    /**
     * Read configuration from resources.
     * @deprecated Use readConfigFile() instead for automatic filesystem/resource handling
     */
    @Deprecated
    public void readFromResource(String resourcePath) throws IOException {
        try (InputStream is = getClass().getResourceAsStream("/" + resourcePath);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            if (is == null) {
                throw new IOException("Resource not found: " + resourcePath);
            }
            this.read(reader);
        }
    }

    

    public void read(BufferedReader reader) throws IOException {
        int status = 0;
        String line = null;
        while ((line = reader.readLine()) != null) {

            line = line.trim();
            if (line.matches("\\[sitemap urls\\]")) {
                status = 1;
            }
            else if (status == 1 && line.length() > 0) {
                sitemapUrls.add(line);
            }
            else if (line.matches("\\[index\\]")) {
                status = 2;
            }
            else if (status == 2 && line.length() > 0) {
                this.indexName = line.trim();
            }
        }
    }

    
}
