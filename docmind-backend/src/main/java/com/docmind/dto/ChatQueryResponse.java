package com.docmind.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ChatQueryResponse {
    private String answer;
    private List<SourceReference> sources;
}
