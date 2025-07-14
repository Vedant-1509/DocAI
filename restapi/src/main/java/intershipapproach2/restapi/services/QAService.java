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

// QAService.java
@Service
public class QAService {

    @Autowired
    WebCrawlerService crawler;
//    @Autowired
//    PDFGeneratorService pdfService;
    @Autowired
    EmbeddingService embedder;
    @Autowired
    VectorDBService vectorDb;

    public String processUrl(String url, String userId, String question) {
        try {
            String text = crawler.extractTextFromUrl(url);

            // split the text extracted in to chunks which are overlapping it store the chunk with the meta-data
            List<String> chunks = splitText(text, 200, 30);
            int i = 1;

            // here the chunks are embedded and store in vectorDB
            for (String chunk : chunks) {
                JSONArray embedding = embedder.getEmbedding(chunk);
                Map<String, String> metadata = Map.of("user_id", userId, "text", chunk);// this the metadata
                vectorDb.upsertVector(userId + "_chunk_" + i++, embedding, metadata);
            }

            if (question != null && !question.isEmpty()) {
                JSONArray questionEmbedding = embedder.getEmbedding(question);
                JSONArray matches = vectorDb.queryTopK(questionEmbedding, 3, userId);
                StringBuilder context = new StringBuilder();
                for (int j = 0; j < matches.length(); j++) {
                    context.append(matches.getJSONObject(j).getJSONObject("metadata").getString("text")).append("\n");
                }

                return askLLM(context.toString(), question);
            }
            return "PDF indexed successfully.";
        } catch (Exception e) {
            e.printStackTrace();
            return " Failed: " + e.getMessage();
        }
    }
    // this
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
        body.put("temperature", 0.5);
        body.put("stream", false);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.cohere.ai/v1/chat"))
                .header("Authorization", "Bearer 3a19B6a68opXSRpS8ZWzY2vFoOpooAZzIkpV020s")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

        //  Log the response for debugging
        System.out.println(" Cohere Chat API Response:");
        System.out.println(response.body());

        if (response.statusCode() != 200) {
            return " Cohere API returned error code: " + response.statusCode() + "\n" + response.body();
        }

        JSONObject res = new JSONObject(response.body());

        //  Return the LLM-generated answer or error message
        if (res.has("text")) {
            return res.getString("text");
        } else if (res.has("message")) {
            return " Cohere returned: " + res.getString("message");
        } else {
            return " Unexpected response structure:\n" + response.body();
        }
    }

//this service is for if the pdf is already generated and and question is asked over it
    public String answerFromExistingData(String userId, String question) {
        try {
            // Step 1-> Embed the question
            JSONArray questionEmbedding = embedder.getEmbedding(question);

            // Step 2-> Query Pinecone for this user's similar chunks
            JSONArray matches = vectorDb.queryTopK(questionEmbedding, 3, userId);

            // Step 3->Combine top chunks to create context
            StringBuilder context = new StringBuilder();
            for (int j = 0; j < matches.length(); j++) {
                context.append(matches.getJSONObject(j).getJSONObject("metadata").getString("text")).append("\n");
            }

            // Step 4-> Ask LLM
            return askLLM(context.toString(), question);

        } catch (Exception e) {
            e.printStackTrace();
            return " Error while processing your question: " + e.getMessage();
        }
    }

    public String uploadandask(MultipartFile file, String question, String userId) {
        try {
            System.out.println("Received file: " + file.getOriginalFilename());
            System.out.println("Size: " + file.getSize());
            System.out.println("Content Type: " + file.getContentType());

            if (file == null || file.isEmpty()) {
                return "Uploaded file is null or empty";
            }

            String text = crawler.extractTextFromPDFFile(file);
            System.out.println("Extracted text length: " + text.length());

            List<String> chunks = splitText(text, 200, 30);
            int i = 1;
            for (String chunk : chunks) {
                JSONArray embedding = embedder.getEmbedding(chunk);
                Map<String, String> metadata = Map.of("user_id", userId, "text", chunk);
                vectorDb.upsertVector(userId + "_chunk_" + i++, embedding, metadata);
            }

            System.out.println("PDF read and uploaded to vector DB successfully.");

            if (question != null && !question.isEmpty()) {
                JSONArray questionEmbedding = embedder.getEmbedding(question);
                JSONArray matches = vectorDb.queryTopK(questionEmbedding, 3, userId);
                StringBuilder context = new StringBuilder();
                for (int j = 0; j < matches.length(); j++) {
                    context.append(matches.getJSONObject(j).getJSONObject("metadata").getString("text")).append("\n");
                }

                return askLLM(context.toString(), question);
            }

            return "PDF uploaded and indexed successfully, but no question was asked.";

        } catch (Exception e) {
            e.printStackTrace();
            return "Error while processing PDF: " + e.getMessage();
        }
    }

}

