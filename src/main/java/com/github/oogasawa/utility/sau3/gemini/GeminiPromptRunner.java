package com.github.oogasawa.utility.sau3.gemini;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import org.json.*;

/**
 * A simple utility class that reads a prompt string from standard input,
 * sends it to the Gemini API, and prints the response to standard output.
 *
 * <p>This class uses the Gemini "gemini-1.5-flash" model to generate
 * natural language responses based on arbitrary input prompts.</p>
 *
 * <p><strong>Security Note:</strong> The API key is hardcoded for demonstration
 * purposes and should be externalized or securely managed in production systems.</p>
 *
 * @author osamu.ogasawara
 */
public class GeminiPromptRunner {

    /**
     * Reads a prompt from standard input, sends it to the Gemini API,
     * and prints the resulting response to standard output.
     *
     * @param args Not used
     * @throws Exception if a network, I/O, or JSON parsing error occurs
     */
    public static void execute(String model, String apikey) throws Exception {
        // API configuration

        String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apikey;

        // Read prompt text from standard input
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        StringBuilder promptBuilder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            promptBuilder.append(line).append("\n");
        }

        // Prepare the JSON request body
        String prompt = promptBuilder.toString().trim();
        String jsonRequest = "{\"contents\": [{\"parts\": [{\"text\": " + JSONObject.quote(prompt) + "}]}]}";

        // Open HTTP connection to Gemini API
        HttpURLConnection conn = (HttpURLConnection) new URL(endpoint).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        conn.setDoOutput(true);

        // Write request body
        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonRequest.getBytes(StandardCharsets.UTF_8));
        }

        // Read response from API
        InputStream is = conn.getResponseCode() == 200 ? conn.getInputStream() : conn.getErrorStream();
        BufferedReader responseReader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        StringBuilder responseBuilder = new StringBuilder();
        while ((line = responseReader.readLine()) != null) {
            responseBuilder.append(line);
        }

        // Parse the JSON response and extract the text result
        JSONObject obj = new JSONObject(responseBuilder.toString());
        JSONArray candidates = obj.getJSONArray("candidates");
        JSONObject content = candidates.getJSONObject(0).getJSONObject("content");
        JSONArray parts = content.getJSONArray("parts");
        String result = parts.getJSONObject(0).getString("text").trim();

        // Output the result to standard output
        System.out.println(result);
    }
}
