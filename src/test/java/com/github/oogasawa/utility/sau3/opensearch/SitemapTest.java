package com.github.oogasawa.utility.sau3.opensearch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Deque;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@DisplayName("Sitemap parser test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SitemapTest {

    private static final Logger logger = LoggerFactory.getLogger(SitemapTest.class);

    
    @Test
    @Order(1)
    public void testReadTestSitemap() {

        Sitemap sitemap = new Sitemap();

        try {
            URL sitemapUrl = this.getClass().getClassLoader().getResource("test_sitemap.xml");
            logger.debug("sitemapUrl: " + sitemapUrl.toString());
            try (InputStream in = sitemapUrl.openStream()) {
                sitemap.parse(in);
            }

            Deque<SitemapEntry> sitemapEntries = sitemap.getSitemapEntries();
            assertEquals(6, sitemapEntries.size());

        } catch (IOException e) {
            logger.error("IOException at reading test_sitemap.xml", e);
        }
    }

}
