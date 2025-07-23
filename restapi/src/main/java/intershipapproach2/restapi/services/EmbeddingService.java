package intershipapproach2.restapi.services;


import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;

@Service
public class EmbeddingService {
    @Value("${cohere.api.key}")
    private  String API_KEY ;  // Replace with env variable later
    @Value("${cohere.embedding.endpoint}")
    private  String EMBEDDING_ENDPOINT ;

    public JSONArray getEmbedding(String text) throws Exception {
        JSONObject body = new JSONObject();
        body.put("model", "embed-english-v3.0");
        body.put("texts", new JSONArray().put(text));
        body.put("truncate", "END");
        body.put("input_type", "search_document");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(EMBEDDING_ENDPOINT))
                .header("Authorization", "Bearer " + API_KEY)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        HttpClient client = HttpClient.newHttpClient();// sending request through client
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("Embedding API Response: " + response.body());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Embedding API failed: " + response.body());
        }

        JSONObject json = new JSONObject(response.body());

        if (!json.has("embeddings")) {
            throw new RuntimeException(" 'embeddings' key missing in response: " + response.body());
        }

        return json.getJSONArray("embeddings").getJSONArray(0);
    }

}
