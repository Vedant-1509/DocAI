package intershipapproach2.restapi.services;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

// VectorDBService.java pinecone is used

//this code is responsible for uploading chunks to vectorDB and get the top K response from the same
@Service
public class VectorDBService {

    private final String apiKey = "pcsk_4MNais_1yu6Qi3PqAj5RxhiGQYcT2dM8bYtmV746y25DQfsMqPxDvwoqpM4sSfs756eUC";
    private final String indexUrl = "https://docai-vwyabpy.svc.aped-4627-b74a.pinecone.io";

    public void upsertVector(String id, JSONArray embedding, Map<String, String> metadata) throws IOException, InterruptedException {
        JSONObject vector = new JSONObject();
        vector.put("id", id);
        vector.put("values", embedding);
        vector.put("metadata", metadata);

        JSONObject body = new JSONObject();
        body.put("vectors", new JSONArray().put(vector));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(indexUrl + "/vectors/upsert"))
                .header("Api-Key", apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        HttpClient client = HttpClient.newHttpClient();
        client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    public JSONArray queryTopK(JSONArray queryEmbedding, int topK, String userId) throws Exception {
        JSONObject filter = new JSONObject();
        filter.put("user_id", userId);

        JSONObject body = new JSONObject();
        body.put("vector", queryEmbedding);
        body.put("topK", topK);
        body.put("includeMetadata", true);
        body.put("filter", filter);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(indexUrl + "/query"))
                .header("Api-Key", apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        JSONObject responseBody = new JSONObject(response.body());
        return responseBody.getJSONArray("matches");
    }
}

