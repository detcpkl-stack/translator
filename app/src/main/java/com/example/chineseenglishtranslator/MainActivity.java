package com.example.chineseenglishtranslator;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.speech.RecognizerIntent;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;

import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;
import com.google.mlkit.common.model.DownloadConditions;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int SPEECH_REQUEST = 1001;
    private EditText input;
    private TextView output, status, history, sourceLabel, targetLabel;
    private Button translateButton;
    private Spinner directionSpinner;
    private Translator translator;
    private boolean chineseToEnglish = true;
    private SharedPreferences prefs;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("translator_history", MODE_PRIVATE);
        buildScreen();
        createTranslator();
    }

    private GradientDrawable shape(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        return d;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(null, Typeface.BOLD);
        return t;
    }

    private LinearLayout vertical() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    private LinearLayout.LayoutParams lp(int width, int height) {
        return new LinearLayout.LayoutParams(width, height);
    }

    private void buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout page = vertical();
        page.setPadding(22, 28, 22, 28);
        page.setBackgroundColor(Color.rgb(246, 248, 252));
        scroll.addView(page);
        setContentView(scroll);

        TextView title = text("中文  ⇄  English", 27, Color.WHITE, true);
        TextView subtitle = text("CHINESE TO ENGLISH TRANSLATOR", 12, Color.WHITE, true);
        LinearLayout hero = vertical();
        hero.setPadding(22, 24, 22, 24);
        hero.setBackground(shape(Color.rgb(36, 76, 155), 24));
        hero.addView(title);
        subtitle.setPadding(0, 8, 0, 0);
        hero.addView(subtitle);
        page.addView(hero, lp(-1, -2));

        TextView directionLabel = text("Translation direction", 16, Color.rgb(35, 48, 70), true);
        directionLabel.setPadding(2, 22, 0, 8);
        page.addView(directionLabel);

        directionSpinner = new Spinner(this);
        String[] directions = {"Chinese → English", "English → Chinese"};
        ArrayAdapter<String> directionAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, directions);
        directionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        directionSpinner.setAdapter(directionAdapter);
        directionSpinner.setBackground(shape(Color.WHITE, 14));
        page.addView(directionSpinner, lp(-1, 50));
        directionSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                boolean newDirection = (position == 0);
                if (newDirection != chineseToEnglish) {
                    chineseToEnglish = newDirection;
                    createTranslator();
                    input.setText("");
                    output.setText("Your translation will appear here.");
                    status.setText("Direction changed. Ready to translate.");
                }
                updateLanguageLabels();
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { }
        });

        sourceLabel = text("Chinese (中文) — source text", 16, Color.rgb(35, 48, 70), true);
        sourceLabel.setPadding(2, 16, 0, 8);
        page.addView(sourceLabel);
        input = new EditText(this);
        input.setHint("在这里输入中文…  /  Enter Chinese text here…");
        input.setTextSize(17);
        input.setGravity(Gravity.TOP);
        input.setMinLines(5);
        input.setPadding(16, 15, 16, 15);
        input.setBackground(shape(Color.WHITE, 18));
        page.addView(input, lp(-1, -2));

        LinearLayout actions = new LinearLayout(this);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        Button voice = makeButton("🎤 Speak", Color.rgb(230, 237, 249), Color.rgb(35, 65, 120));
        Button clear = makeButton("Clear", Color.rgb(230, 237, 249), Color.rgb(35, 65, 120));
        actions.addView(voice, new LinearLayout.LayoutParams(0, 50, 1));
        Space gap = new Space(this);
        actions.addView(gap, lp(10, 1));
        actions.addView(clear, new LinearLayout.LayoutParams(0, 50, 1));
        LinearLayout.LayoutParams ap = lp(-1, 50); ap.topMargin = 12;
        page.addView(actions, ap);
        voice.setOnClickListener(v -> startVoice());
        clear.setOnClickListener(v -> { input.setText(""); output.setText("Your English translation will appear here."); status.setText("Ready"); });

        translateButton = makeButton("Translate to English  →", Color.rgb(35, 91, 190), Color.WHITE);
        LinearLayout.LayoutParams bp = lp(-1, 54); bp.topMargin = 14;
        page.addView(translateButton, bp);
        translateButton.setOnClickListener(v -> translateText());

        status = text("Ready", 13, Color.GRAY, false);
        status.setPadding(3, 12, 0, 8);
        page.addView(status);

        targetLabel = text("English translation", 16, Color.rgb(35, 48, 70), true);
        targetLabel.setPadding(2, 12, 0, 8);
        page.addView(targetLabel);
        output = text("Your English translation will appear here.", 17, Color.rgb(30, 40, 55), false);
        output.setMinHeight(125);
        output.setGravity(Gravity.TOP);
        output.setPadding(16, 16, 16, 16);
        output.setBackground(shape(Color.WHITE, 18));
        page.addView(output, lp(-1, -2));

        LinearLayout bottom = new LinearLayout(this);
        bottom.setOrientation(LinearLayout.HORIZONTAL);
        Button copy = makeButton("Copy", Color.rgb(230, 237, 249), Color.rgb(35, 65, 120));
        Button share = makeButton("Share", Color.rgb(230, 237, 249), Color.rgb(35, 65, 120));
        bottom.addView(copy, new LinearLayout.LayoutParams(0, 48, 1));
        Space gap2 = new Space(this); bottom.addView(gap2, lp(10, 1));
        bottom.addView(share, new LinearLayout.LayoutParams(0, 48, 1));
        LinearLayout.LayoutParams cp = lp(-1, 48); cp.topMargin = 12;
        page.addView(bottom, cp);
        copy.setOnClickListener(v -> copyResult());
        share.setOnClickListener(v -> shareResult());

        TextView histTitle = text("Recent translations", 17, Color.rgb(35, 48, 70), true);
        histTitle.setPadding(2, 24, 0, 8);
        page.addView(histTitle);
        history = text("", 13, Color.DKGRAY, false);
        history.setPadding(14, 12, 14, 12);
        history.setBackground(shape(Color.WHITE, 16));
        page.addView(history, lp(-1, -2));
        refreshHistory();
    }

    private void createTranslator() {
        if (translator != null) {
            translator.close();
        }
        String sourceLanguage = chineseToEnglish
                ? TranslateLanguage.CHINESE : TranslateLanguage.ENGLISH;
        String targetLanguage = chineseToEnglish
                ? TranslateLanguage.ENGLISH : TranslateLanguage.CHINESE;
        TranslatorOptions options = new TranslatorOptions.Builder()
                .setSourceLanguage(sourceLanguage)
                .setTargetLanguage(targetLanguage)
                .build();
        translator = Translation.getClient(options);
    }

    private void updateLanguageLabels() {
        if (sourceLabel == null || targetLabel == null) return;
        if (chineseToEnglish) {
            sourceLabel.setText("Chinese (中文) — source text");
            targetLabel.setText("English translation");
            input.setHint("在这里输入中文…  /  Enter Chinese text here…");
            translateButton.setText("Translate to English  →");
        } else {
            sourceLabel.setText("English — source text");
            targetLabel.setText("Chinese (中文) translation");
            input.setHint("Enter English text here…");
            translateButton.setText("Translate to Chinese  →");
        }
    }

    private Button makeButton(String label, int bg, int fg) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(14);
        b.setAllCaps(false);
        b.setTypeface(null, Typeface.BOLD);
        b.setTextColor(fg);
        b.setBackground(shape(bg, 16));
        return b;
    }

    private void translateText() {
        String source = input.getText().toString().trim();
        if (source.isEmpty()) {
            input.setError("Please enter Chinese text first");
            return;
        }
        translateButton.setEnabled(false);
        status.setText("Preparing Chinese-English translation model…");
        DownloadConditions conditions = new DownloadConditions.Builder().build();
        translator.downloadModelIfNeeded(conditions)
            .addOnSuccessListener(unused -> {
                status.setText("Translating…");
                translator.translate(source)
                    .addOnSuccessListener(result -> {
                        output.setText(result);
                        status.setText("Translation complete");
                        saveHistory(source, result);
                        translateButton.setEnabled(true);
                    })
                    .addOnFailureListener(e -> fail("Translation failed: " + e.getMessage()));
            })
            .addOnFailureListener(e -> fail("Could not download translation model. Connect to the internet and try again. " + e.getMessage()));
    }

    private void fail(String message) {
        status.setText(message);
        translateButton.setEnabled(true);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void startVoice() {
        try {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, chineseToEnglish ? "zh-CN" : "en-US");
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, chineseToEnglish ? "Speak Chinese" : "Speak English");
            startActivityForResult(intent, SPEECH_REQUEST);
        } catch (Exception e) {
            Toast.makeText(this, "Voice recognition is not available on this device.", Toast.LENGTH_LONG).show();
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SPEECH_REQUEST && resultCode == RESULT_OK && data != null) {
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty()) input.setText(results.get(0));
        }
    }

    private void copyResult() {
        String value = output.getText().toString();
        if (value.isEmpty() || value.startsWith("Your English")) return;
        ClipboardManager clipboard = (ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText("English translation", value));
        Toast.makeText(this, "Translation copied", Toast.LENGTH_SHORT).show();
    }

    private void shareResult() {
        String value = output.getText().toString();
        if (value.isEmpty() || value.startsWith("Your English")) return;
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_TEXT, value);
        startActivity(Intent.createChooser(send, "Share translation"));
    }

    private void saveHistory(String chinese, String english) {
        String old = prefs.getString("items", "");
        String entry = chinese.replace("\n", " ") + "\n→ " + english.replace("\n", " ");
        String updated = entry + (old.isEmpty() ? "" : "\n\n" + old);
        String[] lines = updated.split("\n\n");
        StringBuilder limited = new StringBuilder();
        for (int i = 0; i < Math.min(10, lines.length); i++) {
            if (i > 0) limited.append("\n\n");
            limited.append(lines[i]);
        }
        prefs.edit().putString("items", limited.toString()).apply();
        refreshHistory();
    }

    private void refreshHistory() {
        String saved = prefs.getString("items", "");
        history.setText(saved.isEmpty() ? "Your recent translations will be shown here." : saved);
    }

    @Override protected void onDestroy() {
        if (translator != null) translator.close();
        super.onDestroy();
    }
}
