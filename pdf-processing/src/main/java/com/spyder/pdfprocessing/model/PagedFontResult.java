package com.spyder.pdfprocessing.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
public class PagedFontResult {
    private String fullText;
    private Map<Integer, String> pageTexts;
    private Map<Integer, List<FontAwareTextStripper.TextWithFont>> pageFontElements;
    private int totalPages;
}