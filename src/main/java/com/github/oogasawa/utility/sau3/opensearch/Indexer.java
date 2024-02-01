package com.github.oogasawa.utility.sau3.opensearch;


import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.StreamReadConstraints;

import org.apache.http.HttpHost;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.client.indices.CreateIndexRequest;
import org.opensearch.client.indices.CreateIndexResponse;
import org.opensearch.client.indices.GetIndexRequest;
import org.opensearch.common.xcontent.XContentType;
import org.opensearch.common.xcontent.json.JsonXContent;
import org.opensearch.action.admin.indices.delete.DeleteIndexRequest;
import org.opensearch.client.RequestOptions;



public class Indexer {

    private static final Logger logger = Logger.getLogger(Indexer.class.getName());

    String text = null;
    String title = null;
    String url = null;

    
    public void createIndex(String indexName) {

        String jsonMap = this.createMapping();


        RestHighLevelClient client = new RestHighLevelClient(
                RestClient.builder(new HttpHost("localhost", 9200, "http")));

        try {
        // CreateIndexRequestを作成
        CreateIndexRequest request = new CreateIndexRequest(indexName);

        // マッピングを設定
        request.source(jsonMap, JsonXContent.contentBuilder().contentType());

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

    

    public String createMapping() {

        StringJoiner stringJoiner = new StringJoiner("\n");

        stringJoiner.add("{");
        stringJoiner.add("  \"settings\": {");
        stringJoiner.add("    \"analysis\": {");
        stringJoiner.add("      \"analyzer\": {");
        stringJoiner.add("        \"my_japanese_analyzer\": {");
        stringJoiner.add("          \"type\": \"kuromoji\"");
        stringJoiner.add("        }");
        stringJoiner.add("      }");
        stringJoiner.add("    }");
        stringJoiner.add("  },");
        stringJoiner.add("  \"mappings\": {");
        stringJoiner.add("    \"properties\": {");
        stringJoiner.add("      \"text\": {");
        stringJoiner.add("        \"type\": \"text\",");
        stringJoiner.add("        \"analyzer\": \"my_japanese_analyzer\"");
        stringJoiner.add("      },");
        stringJoiner.add("      \"title\": {");
        stringJoiner.add("        \"type\": \"text\",");
        stringJoiner.add("        \"analyzer\": \"my_japanese_analyzer\"");
        stringJoiner.add("      },");
        stringJoiner.add("      \"url\": {");
        stringJoiner.add("        \"type\": \"keyword\"");
        stringJoiner.add("      }");
        stringJoiner.add("    }");
        stringJoiner.add("  }");
        stringJoiner.add("}");
        
        return stringJoiner.toString();
    }



    
    
    public void deleteIndexIfExists(String indexName)  {

        RestHighLevelClient client = new RestHighLevelClient(
                RestClient.builder(new HttpHost("localhost", 9200, "http")));

        try {
            GetIndexRequest getIndexRequest = new GetIndexRequest(indexName);
            boolean exists = client.indices().exists(getIndexRequest, RequestOptions.DEFAULT);

            if (exists) {
                DeleteIndexRequest deleteIndexRequest = new DeleteIndexRequest(indexName);
                client.indices().delete(deleteIndexRequest, RequestOptions.DEFAULT);
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "IOError at deleting: " + indexName, e);
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

            // Select an element with a class name.
            Element div = doc.select("div.docItemCol_VOVn").first();
        
            // When the element is found, get the text.
            if (div != null) {
                this.text = div.text();
            } else { // Extracts text from the body
                this.text = doc.body().text(); 
            }

            this.title = doc.title(); // Extracts the title of the HTML document
            this.url = url;

        } catch (IOException e) {
            logger.log(Level.SEVERE,
                    "Failed to fetch the document from the URL: " + url + "\n" + e.getMessage(), e);
        }

    }



    public void index(String url, String indexName) throws JsonProcessingException {

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

    


