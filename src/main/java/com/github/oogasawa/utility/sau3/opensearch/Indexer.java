package com.github.oogasawa.utility.sau3.opensearch;


import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.StreamReadConstraints;

import org.apache.http.HttpHost;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.client.indices.CreateIndexRequest;
import org.opensearch.client.indices.CreateIndexResponse;
import org.opensearch.client.indices.GetIndexRequest;
import org.opensearch.common.xcontent.XContentType;


import org.opensearch.action.admin.indices.delete.DeleteIndexRequest;
import org.opensearch.client.RequestOptions;




public class Indexer {

    private static final Logger logger = Logger.getLogger(Indexer.class.getName());

    String indexName = "docusaurus";
    
    
    String text = null;
    String title = null;
    String url = null;

    
    public void createIndex() {

        Map<String, Object> jsonMap = this.createMapping();


        RestHighLevelClient client = new RestHighLevelClient(
                RestClient.builder(new HttpHost("localhost", 9200, "http")));

        try {
        // CreateIndexRequestを作成
        CreateIndexRequest request = new CreateIndexRequest(indexName);

        // マッピングを設定
        request.source(jsonMap);

        // インデックスの作成
        CreateIndexResponse createIndexResponse = client.indices().create(request, RequestOptions.DEFAULT);


        }
        catch (IOException e) {
            //logger.log(Level.SEVERE, "IOError at creating a mapping: " + jsonString, e);
        }
        finally {
            try {
                client.close();
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Failed to close the client.", e);
            }    
        }
        
    }

    

    public Map<String, Object> createMapping() {
        Map<String, Object> jsonMap = new HashMap<>();
        Map<String, Object> mappings = new HashMap<>();
        Map<String, Object> properties = new HashMap<>();
        Map<String, Object> title = new HashMap<>();
        Map<String, Object> text = new HashMap<>();
        Map<String, Object> url = new HashMap<>();

        title.put("type", "text");
        text.put("type", "text");
        url.put("type", "text");
        properties.put("title", title);
        properties.put("text", text);
        properties.put("url", url);
        mappings.put("properties", properties);
        jsonMap.put("mappings", mappings);


        return jsonMap;
    }


    
    public void deleteIndexIfExists()  {

        RestHighLevelClient client = new RestHighLevelClient(
                RestClient.builder(new HttpHost("localhost", 9200, "http")));

        try {
            GetIndexRequest getIndexRequest = new GetIndexRequest(this.indexName);
            boolean exists = client.indices().exists(getIndexRequest, RequestOptions.DEFAULT);

            if (exists) {
                DeleteIndexRequest deleteIndexRequest = new DeleteIndexRequest(this.indexName);
                client.indices().delete(deleteIndexRequest, RequestOptions.DEFAULT);
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "IOError at deleting: " + this.indexName, e);
        } finally {
            try {
                client.close();
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Failed to close the client.", e);
            }
        }

    }

        

    
    public void fetchHtml(String url)  {

        Document doc;
        try {
            doc = Jsoup.connect(url).get();
            text = doc.body().text(); // Extracts text from the body
            title = doc.title(); // Extracts the title of the HTML document
            this.url = url;

        } catch (IOException e) {
            logger.log(Level.SEVERE,
                    "Failed to fetch the document from the URL: " + url + "\n" + e.getMessage(), e);
        }

    }



    public void index(String url) throws JsonProcessingException {

        this.fetchHtml(url);

        RestHighLevelClient client = new RestHighLevelClient(
                RestClient.builder(new HttpHost("localhost", 9200, "http")));

        Map<String, Object> jsonMap = new HashMap<>();
        jsonMap.put("title", title);
        jsonMap.put("text", text);
        jsonMap.put("url", url);

        ObjectMapper mapper = new ObjectMapper();
        String jsonString = mapper.writeValueAsString(jsonMap);

        try {
            client.index(new org.opensearch.action.index.IndexRequest(indexName).source(jsonString, XContentType.JSON),
                    RequestOptions.DEFAULT);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "IOError at indexing: " + jsonString, e);
        } finally {
            try {
                client.close();
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Failed to close the client.", e);
            }
        }
    }

    
}

    


