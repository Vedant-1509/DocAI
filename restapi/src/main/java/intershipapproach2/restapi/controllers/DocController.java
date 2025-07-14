package intershipapproach2.restapi.controllers;

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

    // End point to send the URL to web crawl and ask the question
    @PostMapping("/crawl-and-ask")
    public ResponseEntity<String> handleCrawlAndQuestion(
            @RequestParam String url,
            @RequestParam String userId,
            @RequestParam(required = false) String question) {

        String answer = qaService.processUrl(url, userId, question);
        return ResponseEntity.ok(answer);
    }

    //   endpoint to ask question only
    @PostMapping("/ask")
    public ResponseEntity<String> askOnly(
            @RequestParam String userId,
            @RequestParam String question) {

        String answer = qaService.answerFromExistingData(userId, question);
        return ResponseEntity.ok(answer);
    }

    @PostMapping("/ask-from-file")
    public ResponseEntity<String> askFromUploadedFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("question") String question,
            @RequestParam("userId") String userId
    ) throws Exception {

        String answer = qaService.uploadandask(file,question ,userId);
        return ResponseEntity.ok(answer);
    }
}

