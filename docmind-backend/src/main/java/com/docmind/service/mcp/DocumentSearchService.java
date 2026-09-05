package com.docmind.service.mcp;

import com.docmind.entity.Document;
import com.docmind.entity.DocumentStatus;
import com.docmind.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentSearchService {

    private final DocumentRepository documentRepository;

    public List<Document> search(String filenameKeyword, LocalDate uploadedAfter, DocumentStatus status) {
        LocalDateTime uploadedAfterDateTime = uploadedAfter == null ? null : uploadedAfter.atStartOfDay();
        return documentRepository.findAll().stream()
                .filter(doc -> filenameKeyword == null
                        || doc.getFilename().toLowerCase().contains(filenameKeyword.toLowerCase()))
                .filter(doc -> uploadedAfterDateTime == null || doc.getUploadedAt().isAfter(uploadedAfterDateTime))
                .filter(doc -> status == null || doc.getStatus() == status)
                .toList();
    }
}
