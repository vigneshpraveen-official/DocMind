package com.docmind.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DocumentUploadResponse {
    private Long documentId;
    private String filename;
    private String status;
    private String message;
    private int chunkCount;
}
