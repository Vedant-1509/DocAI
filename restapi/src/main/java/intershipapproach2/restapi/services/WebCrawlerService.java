package intershipapproach2.restapi.services;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;
import org.springframework.stereotype.Service;

import java.io.IOException;

// WebCrawlerService.java
@Service
public class WebCrawlerService {

    public String extractTextFromUrl(String url) throws IOException {
        Document doc = Jsoup.connect(url).get();
        return doc.body().text();
    }

    public String extractTextFromPDFFile(MultipartFile file) throws Exception {
        try (InputStream inputStream = file.getInputStream();
             PDDocument document = PDDocument.load(inputStream)) {

            PDFTextStripper pdfStripper = new PDFTextStripper();
            return pdfStripper.getText(document);
        }
    }

}

