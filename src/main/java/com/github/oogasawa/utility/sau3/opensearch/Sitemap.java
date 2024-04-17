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
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class Sitemap {

    private static final Logger logger = LoggerFactory.getLogger(Sitemap.class);
    
    List<String> documentUrls = new ArrayList<>();


    
    public List<String> getDocumentUrls() {
        return this.documentUrls;
    }
    

    /** Parses {@code sitemap.xml}
     *
     * @param urlStrOfSitemapXml  URL of sitemap.xml
     */
    public void parse(String urlStrOfSitemapXml) {
        try {

            URL sitemapUrl = new URI(urlStrOfSitemapXml).toURL();
            InputStream in = sitemapUrl.openStream();
            
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
                    if (startElement.getName().getLocalPart().equals("url")) {
                        String docUrl = parseUrl(eventReader);
                        logger.info(docUrl);
                        this.documentUrls.add(docUrl);
                    }
                }
            }
        } catch (XMLStreamException
                 | FileNotFoundException
                 | URISyntaxException
                 | MalformedURLException e) {
            logger.error("Unable to access sitemap.xml", e);
        } catch (IOException e) {
            logger.error("General IOException", e);
        }
        

    }


    public String parseUrl(XMLEventReader eventReader) {
        String url = null;
        try {
            while (eventReader.hasNext()) {
                XMLEvent event = eventReader.nextEvent();
                if (event.isStartElement()) {
                    StartElement startElement = event.asStartElement();
                    // Check if we have a URL element
                    if (startElement.getName().getLocalPart().equals("loc")) {
                        event = eventReader.nextEvent();
                        url = event.asCharacters().getData();
                        break;
                    }
                }
            }
        } catch (XMLStreamException e) {
            logger.error("XMLStreamException", e);
        }
        return url;
    }
    
}


