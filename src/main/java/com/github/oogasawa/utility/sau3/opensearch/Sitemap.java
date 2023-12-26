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
import java.util.logging.Level;
import java.util.logging.Logger;


public class Sitemap {

    private static final Logger logger = Logger.getLogger(Sitemap.class.getName());
    
    List<String> documentUrls = new ArrayList<>();


    
    public List<String> getDocumentUrls() {
        return this.documentUrls;
    }
    

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
        } catch (XMLStreamException | FileNotFoundException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        } catch (URISyntaxException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
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
            e.printStackTrace();
        }
        return url;
    }
    
}


