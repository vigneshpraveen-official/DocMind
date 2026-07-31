package com.docmind.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SourceReference {
    private Long documentId;
    private String filename;
    private Integer page;
}
