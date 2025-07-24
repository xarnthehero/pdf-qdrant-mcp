package com.spyder.model;

import lombok.Getter;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FontAwareTextStripper extends PDFTextStripper {
    @Getter
    private List<TextWithFont> textElements;
    private StringBuilder currentLine;
    private float currentFontSize;
    private boolean newLine;

    public FontAwareTextStripper() throws IOException {
        super();
        this.textElements = new ArrayList<>();
        this.currentLine = new StringBuilder();
        this.newLine = true;
    }

    @Override
    protected void processTextPosition(TextPosition text) {
        float fontSize = text.getFontSizeInPt();
        String textContent = text.getUnicode();

        if (newLine || Math.abs(fontSize - currentFontSize) > 0.1) {
            if (!currentLine.isEmpty()) {
                textElements.add(new TextWithFont(currentLine.toString().trim(), currentFontSize));
                currentLine = new StringBuilder();
            }
            currentFontSize = fontSize;
            newLine = false;
        }

        currentLine.append(textContent);

        if (textContent.equals("\n") || textContent.equals("\r")) {
            newLine = true;
        }

        super.processTextPosition(text);
    }

    @Override
    protected void endPage(PDPage page) throws IOException {
        if (!currentLine.isEmpty()) {
            textElements.add(new TextWithFont(currentLine.toString().trim(), currentFontSize));
            currentLine = new StringBuilder();
        }
        newLine = true;
        super.endPage(page);
    }

    public record TextWithFont(String text, float fontSize) {

        public boolean isHeader() {
            return fontSize >= 22.0f && !text.trim().isEmpty();
        }

        public String getCleanText() {
            return deduplicateText(text);
        }

        public static String deduplicateText(String text) {
            if (text == null || text.length() <= 1) {
                return text;
            }

            // Check if text is exactly duplicated (like "FORGE FORGE" -> "FORGE")
            String trimmed = text.trim();
            String[] words = trimmed.split("\\s+");

            // If we have even number of words, check if first half equals second half
            if (words.length > 1 && words.length % 2 == 0) {
                int halfLength = words.length / 2;
                boolean isDuplicated = true;

                for (int i = 0; i < halfLength; i++) {
                    if (!words[i].equals(words[i + halfLength])) {
                        isDuplicated = false;
                        break;
                    }
                }

                if (isDuplicated) {
                    StringBuilder result = new StringBuilder();
                    for (int i = 0; i < halfLength; i++) {
                        if (i > 0) result.append(" ");
                        result.append(words[i]);
                    }
                    return result.toString();
                }
            }

            // Check for character-level duplication (like "AABBCC" -> "ABC")
            StringBuilder cleaned = new StringBuilder();
            char prevChar = '\0';

            for (char c : trimmed.toCharArray()) {
                if (c != prevChar || !Character.isLetter(c)) {
                    cleaned.append(c);
                    prevChar = c;
                }
            }

            return cleaned.toString();
        }

        @Override
        public String toString() {
            return String.format("[%.1fpt] %s", fontSize, text);
        }
    }
}