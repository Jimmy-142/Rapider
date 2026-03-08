package com.rapider;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.ColorInt;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final int MIN_WPM = 100;
    private static final int DEFAULT_WPM = 300;

    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

    private final List<String> words = new ArrayList<>();
    private int currentIndex = 0;
    private int currentWpm = DEFAULT_WPM;
    private boolean isPlaying = false;

    private TextView wordText;
    private TextView wpmLabel;
    private MaterialButton playPauseButton;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (!isPlaying || words.isEmpty()) {
                return;
            }

            if (currentIndex >= words.size()) {
                pausePlayback();
                return;
            }

            wordText.setText(buildAnchoredWord(words.get(currentIndex)));
            currentIndex++;

            long delay = Math.max(30L, 60_000L / Math.max(MIN_WPM, currentWpm));
            uiHandler.postDelayed(this, delay);
        }
    };

    private ActivityResultLauncher<String> openDocumentLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PDFBoxResourceLoader.init(getApplicationContext());
        setContentView(R.layout.activity_main);

        wordText = findViewById(R.id.word_text);
        wpmLabel = findViewById(R.id.wpm_label);
        playPauseButton = findViewById(R.id.play_pause_button);
        MaterialButton pickPdfButton = findViewById(R.id.pick_pdf_button);
        SeekBar wpmSeek = findViewById(R.id.wpm_seek);

        setupOpenDocumentLauncher();
        updateWpmLabel();

        wpmSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                currentWpm = MIN_WPM + progress;
                updateWpmLabel();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // no-op
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // no-op
            }
        });

        pickPdfButton.setOnClickListener(v -> openDocumentLauncher.launch("application/pdf"));

        playPauseButton.setOnClickListener(v -> {
            if (isPlaying) {
                pausePlayback();
                return;
            }

            if (words.isEmpty()) {
                Toast.makeText(this, "Load a PDF first", Toast.LENGTH_SHORT).show();
                return;
            }

            startPlayback();
        });
    }

    private void setupOpenDocumentLauncher() {
        openDocumentLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                this::handlePdfSelection
        );
    }

    private void handlePdfSelection(Uri uri) {
        if (uri == null) {
            return;
        }

        pausePlayback();
        wordText.setText("Loading…");

        ioExecutor.execute(() -> {
            try {
                List<String> extracted = PdfWordExtractor.extractWords(getContentResolver(), uri);
                uiHandler.post(() -> {
                    words.clear();
                    words.addAll(extracted);
                    currentIndex = 0;
                    if (words.isEmpty()) {
                        wordText.setText(R.string.empty_state);
                        Toast.makeText(this, "No readable text found in PDF", Toast.LENGTH_SHORT).show();
                    } else {
                        wordText.setText(buildAnchoredWord(words.get(0)));
                        Toast.makeText(this, "Loaded " + words.size() + " words", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (IOException ex) {
                uiHandler.post(() -> {
                    wordText.setText(R.string.empty_state);
                    Toast.makeText(this, "Failed to open PDF: " + ex.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void startPlayback() {
        isPlaying = true;
        playPauseButton.setText(R.string.pause);
        uiHandler.post(tick);
    }

    private void pausePlayback() {
        isPlaying = false;
        playPauseButton.setText(R.string.play);
        uiHandler.removeCallbacks(tick);
    }

    private void updateWpmLabel() {
        wpmLabel.setText(getString(R.string.wpm, currentWpm));
    }

    private CharSequence buildAnchoredWord(String rawWord) {

        if (rawWord == null) {
            return "";
        }

        String word = rawWord.trim();

        int pivot = Math.max(0, (word.length() - 1) / 2);
        @ColorInt int accentColor = getColor(R.color.accent);

        SpannableStringBuilder builder = new SpannableStringBuilder(word);
        builder.setSpan(
                new ForegroundColorSpan(accentColor),
                pivot,
                Math.min(word.length(), pivot + 1),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        return builder;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        pausePlayback();
        ioExecutor.shutdownNow();
    }
}
