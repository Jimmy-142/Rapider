package com.rapider;

import android.content.ContentResolver;
import android.net.Uri;

import androidx.annotation.NonNull;

import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.text.PDFTextStripper;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public final class PdfWordExtractor {

    private PdfWordExtractor() {
    }

    @NonNull
    public static List<String> extractWords(ContentResolver contentResolver, Uri uri) throws IOException {
        List<String> words = new ArrayList<>();

        try (InputStream inputStream = contentResolver.openInputStream(uri);
             PDDocument document = PDDocument.load(inputStream)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            String[] chunks = text.split("\\s+");
            for (String chunk : chunks) {
                String clean = chunk.replaceAll("[^\\p{L}\\p{N}'’-]", "").trim();
                if (!clean.isEmpty()) {
                    words.add(clean);
                }
            }
        }

        return words;
    }
}
