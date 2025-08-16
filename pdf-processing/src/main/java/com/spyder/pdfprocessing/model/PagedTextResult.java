package com.spyder.pdfprocessing.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
@AllArgsConstructor
public class PagedTextResult {
    private String fullText;
    private Map<Integer, String> pageTexts;
    private int totalPages;
}