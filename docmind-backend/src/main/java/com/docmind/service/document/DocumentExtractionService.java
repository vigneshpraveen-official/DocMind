package com.docmind.service.document;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class DocumentExtractionService {

    public Map<Integer, String> extractTextFromPDF(MultipartFile file) throws IOException {
        Map<Integer, String> pageTexts = new HashMap<>();

        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();

            for (int pageNum = 1; pageNum <= document.getNumberOfPages(); pageNum++) {
                stripper.setStartPage(pageNum);
                stripper.setEndPage(pageNum);

                String pageText = stripper.getText(document);
                pageTexts.put(pageNum, pageText);

                log.debug("Extracted {} chars from page {}", pageText.length(), pageNum);
            }
        } catch (IOException e) {
            log.error("Failed to extract text from PDF: {}", e.getMessage());
            throw e;
        }

        log.info("Successfully extracted text from {} pages", pageTexts.size());
        return pageTexts;
    }
}
