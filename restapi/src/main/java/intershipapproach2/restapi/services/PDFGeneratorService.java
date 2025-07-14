package intershipapproach2.restapi.services;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

// PDFGeneratorService.java
@Service
public class PDFGeneratorService {

    public File generatePdf(String text, String userId, String fileName) throws IOException {
        PDDocument document = new PDDocument();
        PDPage page = new PDPage();
        document.addPage(page);

        PDPageContentStream contentStream = new PDPageContentStream(document, page);
        contentStream.beginText();
        contentStream.setFont(PDType1Font.HELVETICA, 12);
        contentStream.setLeading(14.5f);
        contentStream.newLineAtOffset(25, 700);
        contentStream.showText(text);
        contentStream.endText();
        contentStream.close();

        File file = new File("storage/" + userId + "/" + fileName + ".pdf");
        file.getParentFile().mkdirs();
        document.save(file);
        document.close();
        return file;
    }
}
