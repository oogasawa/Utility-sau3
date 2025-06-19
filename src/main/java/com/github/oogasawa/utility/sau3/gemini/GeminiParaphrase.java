package com.github.oogasawa.utility.sau3.gemini;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import org.json.*;

/**
 * A utility class that interacts with the Gemini AI API to generate
 * English paraphrases for user-provided text.
 *
 * <p>This class reads English input text from standard input, sends a
 * prompt to the Gemini API requesting paraphrased alternatives, and then
 * prints the resulting paraphrases to standard output.</p>
 *
 * <p>It uses the "gemini-2.0-flash" model via HTTP POST requests and expects
 * a JSON response in a specific structure.</p>
 *
 * <p>Note: The API key is hardcoded and must be kept secure or replaced with a
 * more secure configuration method in production environments.</p>
 *
 * @author osamu.ogasawara
 */
public class GeminiParaphrase {

    /**
     * Reads English input from standard input and prints a list of paraphrased
     * alternatives to standard output.
     *
     * <p>This method constructs a prompt that instructs the Gemini AI to
     * generate 3–5 natural-sounding English paraphrases for the input text.
     * The output is expected as a bullet-point list.</p>
     *
     * <p>Exceptions are thrown for I/O or API communication errors.</p>
     *
     * @param model  the Gemini model to use (e.g., "gemini-1.5-flash")
     * @param apikey your API key for accessing the Gemini API
     *
     * @throws Exception if any network, I/O, or JSON parsing error occurs
     */
    public static void suggest(String model, String apikey) throws Exception {

        String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apikey;

        // Read English text from standard input
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        StringBuilder inputBuilder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            inputBuilder.append(line).append("\n");
        }

        String prompt = "Please provide a few natural-sounding English paraphrases of the following phrase. "
                      + "List 3-5 alternatives as bullet points, with concise explanations:\n\n"
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
