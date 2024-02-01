package com.github.oogasawa.utility.sau3.opensearch;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class IndexConf {

    private static final Logger logger = Logger.getLogger(IndexConf.class.getName());

    String indexName = "docusaurus_ja";
    List<String> sitemapUrls = new ArrayList<>();


    public String getIndexName() {
        return this.indexName;
    }

    
    public List<String> getSitemapUrls() {
        return this.sitemapUrls;
    }


    public void read(String file) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
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
