package com.github.oogasawa.utility.sau3.opensearch;


import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpHost;
import org.jsoup.Connection;
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
import org.opensearch.action.get.GetRequest;
import org.opensearch.action.get.GetResponse;
import org.opensearch.client.RequestOptions;


public class Indexer {

    private static final Logger logger = Logger.getLogger(Indexer.class.getName());

    String text = null;
    String title = null;
    String url = null;
    String lastmod = null;

    
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
            logger.log(Level.SEVERE, "IOError at creating a mapping.", e);
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



    public void fetchHtml(String url) {

        try {
            Connection connection = Jsoup.connect(url).userAgent(
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                    .referrer("https://www.google.com/")
                    .header("Accept",
                            "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "ja,en-US;q=0.9,en;q=0.8").timeout(10000)
                    .followRedirects(true).method(Connection.Method.GET);

            // Add BASIC authentication for 133.39.114.45
            if (url.contains("133.39.114.45")) {
                String username = "nigsc";
                String password = "testnigsc";
                String auth = username + ":" + password;
                String encodedAuth = java.util.Base64.getEncoder().encodeToString(auth.getBytes());
                connection.header("Authorization", "Basic " + encodedAuth);
            }

            Connection.Response response = connection.execute();

            int statusCode = response.statusCode();
            if (statusCode != 200) {
                logger.log(Level.SEVERE, "Failed to fetch the document from the URL: " + url
                        + " - Status code: " + statusCode);
                return;
            }

            Document doc = response.parse();

            Element div = doc.select("div.docItemCol_VOVn").first();
            if (div != null) {
                this.text = div.text();
            } else {
                this.text = doc.body().text();
            }

            this.title = doc.title();
            this.url = url;

        } catch (org.jsoup.HttpStatusException e) {
            logger.log(Level.SEVERE, "HTTP error fetching URL: " + url + " - Status code: " + e.getStatusCode(),
                    e);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "IO error fetching URL: " + url, e);
        }
    }



    public void index(String url, String indexName) throws JsonProcessingException {

        this.fetchHtml(url);

        RestHighLevelClient client = new RestHighLevelClient(
                RestClient.builder(new HttpHost("localhost", 9200, "http")));

        Map<String, Object> jsonMap = new HashMap<>();
        // jsonMap.put("_id", calculateMD5(url));
        jsonMap.put("title", this.title);
        jsonMap.put("text", this.text);
        jsonMap.put("url", this.url);
        jsonMap.put("lastmod", this.lastmod); // Add lastmod to indexed document


        ObjectMapper mapper = new ObjectMapper();
        String jsonString = mapper.writeValueAsString(jsonMap);

        try {
            client.index(new org.opensearch.action.index.IndexRequest(indexName)
                         .source(jsonString, XContentType.JSON)
                         .id(calculateMD5(url)),
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



    // Method to calculate the MD5 hash
    public static String calculateMD5(String input) {
        try {
            // Get the MessageDigest instance for MD5
            MessageDigest md = MessageDigest.getInstance("MD5");

            // Convert input string to byte array and compute the hash
            byte[] hashBytes = md.digest(input.getBytes());

            // Convert the byte array to a hexadecimal string
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));  // Convert byte to hexadecimal format
            }
            return sb.toString();  // Return the hash (hexadecimal string)

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not found", e);
        }
    }

    /**
     * Check if a document exists in the index with the same URL and lastmod date.
     * This is used for incremental updates to avoid re-indexing unchanged documents.
     */
    public boolean documentExistsWithSameTimestamp(String url, String lastmod, String indexName) {
        RestHighLevelClient client = new RestHighLevelClient(
                RestClient.builder(new HttpHost("localhost", 9200, "http")));

        try {
            String documentId = calculateMD5(url);
            GetRequest getRequest = new GetRequest(indexName, documentId);

            if (!client.exists(getRequest, RequestOptions.DEFAULT)) {
                logger.info("Document does not exist in index: " + url);
                return false;
            }

            GetResponse getResponse = client.get(getRequest, RequestOptions.DEFAULT);
            if (getResponse.isExists()) {
                Map<String, Object> sourceAsMap = getResponse.getSourceAsMap();
                String existingLastmod = (String) sourceAsMap.get("lastmod");

                if (lastmod != null && lastmod.equals(existingLastmod)) {
                    logger.info("Document exists with same timestamp, skipping: " + url);
                    return true;
                } else {
                    logger.info("Document exists but timestamp differs, will update: " + url + " (existing: " + existingLastmod + ", new: " + lastmod + ")");
                    return false;
                }
            }
            return false;
        } catch (IOException e) {
            logger.log(Level.WARNING, "Error checking document existence for: " + url, e);
            return false; // If we can't check, assume it doesn't exist and proceed with indexing
        } finally {
            try {
                client.close();
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Failed to close the client.", e);
            }
        }
    }


}

    


