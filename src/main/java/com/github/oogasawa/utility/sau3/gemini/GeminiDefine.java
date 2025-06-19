package com.github.oogasawa.utility.sau3.gemini;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import org.json.*;

/**
 * A utility class that queries Gemini AI for the meaning or definition
 * of a given English word or phrase.
 *
 * <p>This tool reads a word or phrase from standard input, sends it to
 * Gemini AI with a request to explain the meaning clearly, and prints the
 * result to standard output.</p>
 */
public class GeminiDefine {


    /**
     * Reads an English word or phrase from standard input and prints
     * its definition or meaning using Gemini AI.
     *
     * <p>The explanation is targeted at language learners and includes
     * clear, concise definitions and optionally 1–2 usage examples.
     * Paraphrasing is avoided in favor of direct definitions.</p>
     *
     * @param model  the Gemini model to use (e.g., "gemini-1.5-flash")
     * @param apikey your API key for accessing the Gemini API
     * @throws Exception if any network, I/O, or JSON parsing error occurs
     */
    public static void define(String model, String apikey) throws Exception {

        String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apikey;

        // Read word or phrase from standard input
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        StringBuilder inputBuilder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            inputBuilder.append(line).append("\n");
        }

        String prompt = "What is the meaning of the following English word or phrase? "
                      + "Please explain clearly and concisely, as if to a language learner. "
                      + "Do not paraphrase. Just explain the meaning and give 1–2 usage examples if appropriate:\n\n"
                      + inputBuilder.toString().trim();

        String json = "{\"contents\": [{\"parts\": [{\"text\": " + JSONObject.quote(prompt) + "}]}]}";

        HttpURLConnection conn = (HttpURLConnection) new URL(endpoint).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        conn.setDoOutput(true);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(json.getBytes(StandardCharsets.UTF_8));
        }

        InputStream is = conn.getResponseCode() == 200 ? conn.getInputStream() : conn.getErrorStream();
        BufferedReader respReader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        StringBuilder respBuilder = new StringBuilder();
        while ((line = respReader.readLine()) != null) {
            respBuilder.append(line);
        }

        JSONObject obj = new JSONObject(respBuilder.toString());
        JSONArray candidates = obj.getJSONArray("candidates");
        JSONObject content = candidates.getJSONObject(0).getJSONObject("content");
        JSONArray parts = content.getJSONArray("parts");
        String result = parts.getJSONObject(0).getString("text");

        System.out.println(result);
    }
}
