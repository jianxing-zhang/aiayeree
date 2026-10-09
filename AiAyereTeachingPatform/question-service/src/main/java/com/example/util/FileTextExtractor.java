package com.example.util;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

/**
 * 从上传的文档（.docx / .pdf / .txt）中抽取纯文本。
 */
public class FileTextExtractor {

    public static String extract(byte[] data, String filename) {
        if (data == null || data.length == 0) return "";
        String ext = filename == null ? "" : filename.toLowerCase();
        try {
            if (ext.endsWith(".docx")) {
                return extractDocx(data);
            } else if (ext.endsWith(".pdf")) {
                return extractPdf(data);
            } else if (ext.endsWith(".txt") || ext.endsWith(".md")) {
                return new String(data, StandardCharsets.UTF_8);
            } else {
                // 兜底尝试按文本读取
                return new String(data, StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            throw new RuntimeException("文档解析失败：" + e.getMessage(), e);
        }
    }

    private static String extractDocx(byte[] data) throws Exception {
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(data))) {
            StringBuilder sb = new StringBuilder();
            for (XWPFParagraph p : doc.getParagraphs()) {
                String text = p.getText();
                if (text != null && !text.isBlank()) sb.append(text).append("\n");
            }
            return sb.toString();
        }
    }

    private static String extractPdf(byte[] data) throws Exception {
        try (PDDocument doc = PDDocument.load(data)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(doc);
        }
    }
}