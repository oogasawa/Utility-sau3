package com.github.oogasawa.utility.sau3.opensearch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

 

public class IndexConfTest {

    private static final Logger logger = Logger.getLogger(IndexConf.class.getName());


    @DisplayName(" - Scenario01 : Reading sitemaps from a configuration file.")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    @Nested
    class Scenario01 {

        @Test
        @Order(1)
        public void read_sitemap_URLs() {

            IndexConf indexConf = new IndexConf();
 
            try (InputStream in = getClass().getResourceAsStream("/index.conf");
                 BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
                indexConf.read(reader);
                        
            } catch (IOException e) {
                logger.log(Level.SEVERE, e.getMessage(), e);
            }

            List<String> sitemapUrls = indexConf.getSitemapUrls();

            String[] answer = {
                "https://sc.ddbj.nig.ac.jp/sitemap.xml",
                "http://localhost/doc_Infra001/sitemap.xml"
            };

            assertEquals(answer.length, sitemapUrls.size());
            
            for (int i=0; i<answer.length; i++) {
                assertEquals(answer[i], sitemapUrls.get(i));
                logger.info(sitemapUrls.get(i));
            }

        }


        @Test
        @Order(2)
        public void parse_sitemap_xml() {

            Sitemap sitemap = new Sitemap();
            sitemap.parse("https://sc.ddbj.nig.ac.jp/sitemap.xml");
        
            List<String> docUrls = sitemap.getDocumentUrls();

            String[] answer = {
                "https://sc.ddbj.nig.ac.jp/blog",
            };

            for (int i=0; i<answer.length; i++) {
                assertTrue(docUrls.contains(answer[i]));
                logger.info(answer[i]);
            }

        }


        

    } // end of the nested class.



    
}
