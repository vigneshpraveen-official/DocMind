package com.docmind.dto;

import com.docmind.entity.Document;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class DocumentSummaryResponse {
    private Long id;
    private String filename;
    private String status;
    private LocalDateTime uploadedAt;

    public static DocumentSummaryResponse from(Document document) {
        return new DocumentSummaryResponse(
                document.getId(),
                document.getFilename(),
                document.getStatus().name(),
                document.getUploadedAt()
        );
    }
}
