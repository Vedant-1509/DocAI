package intershipapproach2.restapi.controllers;


import intershipapproach2.restapi.APIResponse.ApiResponse;
import intershipapproach2.restapi.services.QAService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class DocController {

    @Autowired
    private QAService qaService;

    @PostMapping("/crawl-and-ask")
    public ResponseEntity<ApiResponse> handleCrawlAndQuestion(
            @RequestParam String url,
            @RequestParam String userId,
            @RequestParam(required = false) String question) {

        try {
            String answer = qaService.processUrl(url, userId, question);
            return ResponseEntity.ok(new ApiResponse(true, "Answer generated from crawled URL", answer));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ApiResponse(false, "Failed to process URL", null));
        }
    }

    @PostMapping("/ask")
    public ResponseEntity<ApiResponse> askOnly(
            @RequestParam String userId,
            @RequestParam String question) {

        try {
            String answer = qaService.answerFromExistingData(userId, question);
            return ResponseEntity.ok(new ApiResponse(true, "Answer fetched from stored data", answer));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ApiResponse(false, "Failed to answer question", null));
        }
    }

    @PostMapping("/ask-from-file")
    public ResponseEntity<ApiResponse> askFromUploadedFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("question") String question,
            @RequestParam("userId") String userId) {

        try {
            String answer = qaService.uploadandask(file, question, userId);
            return ResponseEntity.ok(new ApiResponse(true, "Answer generated from uploaded file", answer));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ApiResponse(false, "Failed to process file", null));
        }
    }
}
