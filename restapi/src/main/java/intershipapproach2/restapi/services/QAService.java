package intershipapproach2.restapi.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.json.JSONObject;
import org.json.JSONArray;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
public class QAService {

    @Autowired
    WebCrawlerService crawler;
    @Autowired
    EmbeddingService embedder;
    @Autowired
    VectorDBService vectorDb;

public String processUrl(String url, String companyName, String question) {
    try {
        if (url == null || url.isBlank()) return "URL cannot be empty.";

        // ✅ Step 1: Extract content
        String text = crawler.extractTextFromUrl(url);
        System.out.println("Extracted text length: " + text.length());

        // ✅ Step 2: Chunk text
        List<String> chunks = splitText(text, 200, 40); // Increased chunk size and overlap
        String documentId = UUID.randomUUID().toString();

        int i = 1;
        for (String chunk : chunks) {
            JSONArray embedding = embedder.getEmbedding(chunk);
            String vectorId = companyName + "_" + documentId + "_chunk_" + i++;

            Map<String, String> metadata = Map.of(
                    "companyName", companyName,
                    "documentId", documentId,
                    "text", chunk
            );

            vectorDb.upsertVector(vectorId, embedding, metadata);
        }

        System.out.println("Stored " + chunks.size() + " chunks for URL: " + url);

        // ✅ Step 3: If no question, return early
        if (question == null || question.trim().isEmpty()) {
            return "URL indexed successfully. No question provided.";
        }

        // ✅ Step 4: Embed question
        JSONArray questionEmbedding = embedder.getEmbedding(question);

        // ✅ Step 5: Query relevant chunks
        JSONArray matches = vectorDb.queryTopK(questionEmbedding, 5, companyName);
        if (matches.length() == 0) {
            return "Sorry, no relevant information found for your question.";
        }

        // ✅ Step 6: Build context
        StringBuilder context = new StringBuilder();
        for (int j = 0; j < matches.length(); j++) {
            JSONObject match = matches.getJSONObject(j);
            double score = match.getDouble("score");
            String matchedText = match.getJSONObject("metadata").getString("text");

            System.out.println("Match #" + (j + 1) + " | Score: " + score);

                context.append(matchedText).append("\n");

        }

        System.out.println("Sending context to LLM"+context);

        if (context.length() == 0) {
            return "Sorry, the retrieved matches were not strong enough to answer your question.";
        }

        // ✅ Step 7: Ask LLM
        return askLLM(context.toString(), question);

    } catch (Exception e) {
        e.printStackTrace();
        return "Error while processing URL: " + e.getMessage();
    }
}



    private List<String> splitText(String text, int chunkSize, int overlap) {
        String[] words = text.split("\\s+");
        List<String> chunks = new ArrayList<>();
        for (int i = 0; i < words.length; i += (chunkSize - overlap)) {
            int end = Math.min(i + chunkSize, words.length);
            chunks.add(String.join(" ", Arrays.copyOfRange(words, i, end)));
        }
        return chunks;
    }

    private String askLLM(String context, String question) throws Exception {
        JSONObject body = new JSONObject();
        body.put("message", question);
        body.put("documents", new JSONArray().put(new JSONObject().put("id", "doc1").put("text", context)));
        body.put("temperature", 0.1);
        body.put("stream", false);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.cohere.ai/v1/chat"))
                .header("Authorization", "Bearer 3a19B6a68opXSRpS8ZWzY2vFoOpooAZzIkpV020s")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("Cohere Chat API Response:");
        System.out.println(response.body());

        if (response.statusCode() != 200) {
            return "Cohere API returned error code: " + response.statusCode() + "\n" + response.body();
        }

        JSONObject res = new JSONObject(response.body());

        if (res.has("text")) {
            return res.getString("text");
        } else if (res.has("message")) {
            return "Cohere returned: " + res.getString("message");
        } else {
            return "Unexpected response structure:\n" + response.body();
        }
    }

    public String answerFromExistingData(String companyName, String question) {
        try {
            JSONArray questionEmbedding = embedder.getEmbedding(question);
            JSONArray matches = vectorDb.queryTopK(questionEmbedding, 3, companyName);

            StringBuilder context = new StringBuilder();
            for (int j = 0; j < matches.length(); j++) {
                context.append(matches.getJSONObject(j).getJSONObject("metadata").getString("text")).append("\n");
            }

            return askLLM(context.toString(), question);

        } catch (Exception e) {
            e.printStackTrace();
            return "Error while processing your question: " + e.getMessage();
        }
    }
public String uploadandask(MultipartFile file, String question, String companyName) {
    try {
        if (file == null || file.isEmpty()) {
            return "Uploaded file is null or empty.";
        }

        System.out.println("Received file: " + file.getOriginalFilename());
        System.out.println("Size: " + file.getSize());
        System.out.println("Content Type: " + file.getContentType());

        // ✅ Extract text from PDF
        String text = crawler.extractTextFromPDFFile(file);
        System.out.println("Extracted text length: " + text.length());

        // ✅ Chunking
        List<String> chunks = splitText(text, 200, 40); // Improved chunk size & overlap
        String documentId = UUID.randomUUID().toString();

        int i = 1;
        for (String chunk : chunks) {
            JSONArray embedding = embedder.getEmbedding(chunk);
            String vectorId = companyName + "_" + documentId + "_chunk_" + i++;

            Map<String, String> metadata = Map.of(
                    "companyName", companyName,
                    "documentId", documentId,
                    "text", chunk
            );

            vectorDb.upsertVector(vectorId, embedding, metadata);
        }

        System.out.println("Stored " + chunks.size() + " chunks to vector DB.");

        // ✅ If no question, return success
        if (question == null || question.trim().isEmpty()) {
            return "PDF uploaded and indexed successfully. No question provided.";
        }

        // ✅ Embed the question
        JSONArray questionEmbedding = embedder.getEmbedding(question);

        // ✅ Retrieve top-k matching chunks filtered by company name
        JSONArray matches = vectorDb.queryTopK(questionEmbedding, 5, companyName);
        if (matches.length() == 0) {
            return "Sorry, no relevant information found for your question.";
        }

        // ✅ Build context with score filtering
        StringBuilder context = new StringBuilder();
        for (int j = 0; j < matches.length(); j++) {
            JSONObject match = matches.getJSONObject(j);
            double score = match.getDouble("score");
            String matchedText = match.getJSONObject("metadata").getString("text");

            System.out.println("Match #" + (j + 1) + " | Score: " + score);
             // ✅ use slightly higher threshold for relevance
                context.append(matchedText).append("\n");

        }

        if (context.length() == 0) {
            return "Sorry, the retrieved chunks were not relevant enough to answer your question.";
        }

        // ✅ Ask the LLM
        System.out.println("Sending context to LLM:\n" + context);
        String response = askLLM(context.toString(), question);

        return response != null && !response.isBlank()
                ? response
                : "LLM could not generate a response.";

    } catch (Exception e) {
        e.printStackTrace();
        return "Error while processing PDF: " + e.getMessage();
    }
}


}
