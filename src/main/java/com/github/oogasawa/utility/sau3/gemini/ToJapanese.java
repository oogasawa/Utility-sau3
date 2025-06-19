package com.github.oogasawa.utility.sau3.gemini;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import org.json.*;

/**
 * This class reads English text from standard input,
 * sends it to Gemini AI for English-to-Japanese translation,
 * and prints the result to standard output.
 */
public class ToJapanese {

    public static void translate(String model, String apikey) throws Exception {

        String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apikey;

        // Read English text from standard input
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        StringBuilder inputBuilder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            inputBuilder.append(line).append("\n");
        }

        String promptText = "Translate the following English text into natural Japanese:\n\n"
                          + inputBuilder.toString().trim();

        String json = "{\"contents\": [{\"parts\": [{\"text\": " + toJsonString(promptText) + "}]}]}";

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
        JSONObject first = candidates.getJSONObject(0);
        JSONObject content = first.getJSONObject("content");
        JSONArray parts = content.getJSONArray("parts");
        String text = parts.getJSONObject(0).getString("text");

        System.out.println(text);
    }

    private static String toJsonString(String text) {
        return JSONObject.quote(text);
    }
}
