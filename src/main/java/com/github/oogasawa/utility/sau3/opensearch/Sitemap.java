package com.github.oogasawa.utility.sau3.opensearch;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.XMLStreamException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.HttpURLConnection;
import java.util.Base64;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;




public class Sitemap {

    private static final Logger logger = Logger.getLogger(Sitemap.class.getName());
    
    Deque<SitemapEntry> sitemapEntries = new ArrayDeque<>();


    
    public Deque<SitemapEntry> getSitemapEntries() {
        return this.sitemapEntries;
    }



    

    /** Parses {@code sitemap.xml}
     *
     * @param urlStrOfSitemapXml  URL of sitemap.xml
     */
    public void parse(String urlStrOfSitemapXml) {
        try {

            URL sitemapUrl = new URI(urlStrOfSitemapXml).toURL();
            try (InputStream in = openStreamWithAuth(sitemapUrl)) {
                parse(in);
            }

        } catch (FileNotFoundException
                 | URISyntaxException
                 | MalformedURLException e) {
            logger.log(Level.SEVERE, "Unable to access sitemap.xml", e);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "General IOException", e);
        }


    }

    /**
     * Opens an InputStream with BASIC authentication support
     *
     * @param url URL to open
     * @return InputStream with authentication headers
     * @throws IOException if connection fails
     */
    private InputStream openStreamWithAuth(URL url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        // Add BASIC authentication for 133.39.114.45
        if (url.getHost().equals("133.39.114.45")) {
            String username = "nigsc";
            String password = "testnigsc";
            String auth = username + ":" + password;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
            connection.setRequestProperty("Authorization", "Basic " + encodedAuth);
        }

        return connection.getInputStream();
    }


    
    /** Parses {@code sitemap.xml}
     *
     * @param in An InputStream object of sitemap.xml
     */
    public void parse(InputStream in) {
        try {

            // Create a new XMLInputFactory
            XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
            // Setup a new eventReader
            XMLEventReader eventReader = xmlInputFactory.createXMLEventReader(in);
            // Read the XML document
            while (eventReader.hasNext()) {
                XMLEvent event = eventReader.nextEvent();
                if (event.isStartElement()) {
                    StartElement startElement = event.asStartElement();
                    // Check if we have a URL element
                    logger.fine("startElement of parse(inputStream in): " + startElement.getName().getLocalPart());
                    if (startElement.getName().getLocalPart().equals("url")) {
                                                SitemapEntry entry = parseUrl(eventReader);
                        logger.info(String.format("%s, %s", entry.getUrl(), entry.getLastmod()));
                        this.sitemapEntries.push(entry);
                    }
                }
            }
        } catch (XMLStreamException e) {
            logger.log(Level.SEVERE, "Unable to access sitemap.xml", e);
        }
        
    }

    

    public SitemapEntry parseUrl(XMLEventReader eventReader) {
        SitemapEntry entry = new SitemapEntry();
        try {
            while (eventReader.hasNext()) {
                XMLEvent event = eventReader.nextEvent();
                if (event.isStartElement()) {
                    StartElement startElement = event.asStartElement();
                    logger.fine("startElement of parseUrl: " + startElement.getName().getLocalPart());
                    // Check if we have a URL element
                    if (startElement.getName().getLocalPart().equals("loc")) {
                        event = eventReader.nextEvent();
                        entry.setUrl(event.asCharacters().getData());
                    }
                    else if (startElement.getName().getLocalPart().equals("lastmod")) {
                        event = eventReader.nextEvent();
                        entry.setLastmod(event.asCharacters().getData());
                    }
                }
                else if (event.isEndElement()) {
                    if (event.asEndElement().getName().getLocalPart().equals("url")) {
                        break;
                    }
                }
            }
        } catch (XMLStreamException e) {
            logger.log(Level.SEVERE, "XMLStreamException", e);
        }
        return entry;
    }
    
}



    
