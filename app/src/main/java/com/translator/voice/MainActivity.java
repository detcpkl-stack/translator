package com.translator.voice;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.translate.*;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity {

    private TextView sourceLabel;
    private TextView targetLabel;
    private TextView statusText;
    private TextView historyText;

    private EditText inputText;
    private TextView outputText;

    private Button micButton;
    private Button translateButton;
    private Button speakButton;
    private Button swapButton;

    private Translator translator;
    private TextToSpeech tts;
    private SpeechRecognizer speechRecognizer;

    private boolean englishToChinese = true;
    private boolean ttsReady = false;
    private boolean isListening = false;

    private final ArrayList<String> history = new ArrayList<>();

    private static final int AUDIO_PERMISSION = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        createInterface();
        initializeTTS();
        requestAudioPermission();

        updateLanguages();
    }

    private void createInterface() {

        ScrollView scroll = new ScrollView(this);

        LinearLayout main = new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);
        main.setPadding(22, 24, 22, 24);
        main.setBackgroundColor(0xFFF4F6FC);

        scroll.addView(main);

        TextView title = new TextView(this);
        title.setText("VOICE TRANSLATOR");
        title.setTextSize(25);
        title.setTextColor(0xFF172554);
        title.setGravity(Gravity.CENTER);
        title.setTypeface(null, 1);

        main.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("English ↔ 中文 Chinese");
        subtitle.setTextSize(17);
        subtitle.setTextColor(0xFF6750A4);
        subtitle.setGravity(Gravity.CENTER);
        addSpace(main, 8);
        main.addView(subtitle);

        addSpace(main, 22);

        LinearLayout languageRow = new LinearLayout(this);
        languageRow.setGravity(Gravity.CENTER);
        languageRow.setOrientation(LinearLayout.HORIZONTAL);

        sourceLabel = new TextView(this);
        targetLabel = new TextView(this);

        styleLanguage(sourceLabel);
        styleLanguage(targetLabel);

        swapButton = new Button(this);
        swapButton.setText("⇄");

        languageRow.addView(sourceLabel,
                new LinearLayout.LayoutParams(0, 65, 1));

        languageRow.addView(swapButton);

        languageRow.addView(targetLabel,
                new LinearLayout.LayoutParams(0, 65, 1));

        main.addView(languageRow);

        swapButton.setOnClickListener(v -> {
            englishToChinese = !englishToChinese;
            updateLanguages();
            inputText.setText("");
            outputText.setText("");
            statusText.setText("Language direction changed");
        });

        addSpace(main, 20);

        TextView inputTitle = new TextView(this);
        inputTitle.setText("YOUR SPEECH / INPUT TEXT");
        inputTitle.setTextSize(15);
        inputTitle.setTextColor(0xFF172554);
        inputTitle.setTypeface(null, 1);
        main.addView(inputTitle);

        inputText = new EditText(this);
        inputText.setHint("Speak or type here...");
        inputText.setTextSize(18);
        inputText.setMinLines(4);
        inputText.setGravity(Gravity.TOP);
        inputText.setPadding(15, 15, 15, 15);
        inputText.setBackgroundColor(0xFFFFFFFF);

        main.addView(inputText);

        addSpace(main, 12);

        micButton = new Button(this);
        micButton.setText("🎤  SPEAK NOW");

        main.addView(micButton);

        micButton.setOnClickListener(v -> startListening());

        addSpace(main, 10);

        translateButton = new Button(this);
        translateButton.setText("TRANSLATE");

        main.addView(translateButton);

        translateButton.setOnClickListener(v -> translateText());

        addSpace(main, 20);

        TextView resultTitle = new TextView(this);
        resultTitle.setText("TRANSLATED RESULT");
        resultTitle.setTextSize(15);
        resultTitle.setTextColor(0xFF172554);
        resultTitle.setTypeface(null, 1);

        main.addView(resultTitle);

        addSpace(main, 8);

        outputText = new TextView(this);
        outputText.setText("Your translation will appear here.");
        outputText.setTextSize(21);
        outputText.setTextColor(0xFF172554);
        outputText.setPadding(18, 25, 18, 25);
        outputText.setMinHeight(110);
        outputText.setBackgroundColor(0xFFFFFFFF);

        main.addView(outputText);

        addSpace(main, 12);

        speakButton = new Button(this);
        speakButton.setText("🔊  PLAY TRANSLATED AUDIO");

        main.addView(speakButton);

        speakButton.setOnClickListener(v -> speakTranslation());

        addSpace(main, 12);

        statusText = new TextView(this);
        statusText.setText("Ready to translate");
        statusText.setTextSize(14);
        statusText.setGravity(Gravity.CENTER);
        statusText.setTextColor(0xFF6750A4);

        main.addView(statusText);

        addSpace(main, 25);

        TextView historyTitle = new TextView(this);
        historyTitle.setText("TRANSLATION HISTORY");
        historyTitle.setTextSize(17);
        historyTitle.setTypeface(null, 1);
        historyTitle.setTextColor(0xFF172554);

        main.addView(historyTitle);

        historyText = new TextView(this);
        historyText.setText("No translations yet.");
        historyText.setTextSize(14);
        historyText.setTextColor(0xFF334155);

        main.addView(historyText);

        Button clearButton = new Button(this);
        clearButton.setText("CLEAR ALL");

        main.addView(clearButton);

        clearButton.setOnClickListener(v -> {
            history.clear();
            historyText.setText("No translations yet.");
            inputText.setText("");
            outputText.setText("");
            statusText.setText("Cleared");
        });

        setContentView(scroll);
    }

    private void styleLanguage(TextView view) {
        view.setTextSize(15);
        view.setTextColor(0xFFFFFFFF);
        view.setGravity(Gravity.CENTER);
        view.setBackgroundColor(0xFF6750A4);
        view.setPadding(5, 5, 5, 5);
    }

    private void addSpace(LinearLayout layout, int height) {
        View space = new View(this);
        layout.addView(space,
                new LinearLayout.LayoutParams(
                        1, height));
    }

    private void updateLanguages() {

        if (englishToChinese) {
            sourceLabel.setText("English 🇬🇧");
            targetLabel.setText("中文 Chinese 🇨🇳");
        } else {
            sourceLabel.setText("中文 Chinese 🇨🇳");
            targetLabel.setText("English 🇬🇧");
        }
    }

    private void initializeTTS() {

        tts = new TextToSpeech(this, result -> {

            if (result == TextToSpeech.SUCCESS) {

                ttsReady = true;

                setTTSLanguage();
            }
        });
    }

    private void setTTSLanguage() {

        if (tts == null || !ttsReady) return;

        Locale language = englishToChinese
                ? Locale.SIMPLIFIED_CHINESE
                : Locale.US;

        int result = tts.setLanguage(language);

        if (result == TextToSpeech.LANG_MISSING_DATA ||
                result == TextToSpeech.LANG_NOT_SUPPORTED) {

            statusText.setText(
                    "Speech voice not installed. Check TTS language settings.");
        }
    }

    private void requestAudioPermission() {

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    AUDIO_PERMISSION);
        }
    }

    private void startListening() {

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {

            requestAudioPermission();
            return;
        }

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {

            statusText.setText(
                    "Speech recognition is unavailable on this device.");
            return;
        }

        try {

            if (speechRecognizer != null) {
                speechRecognizer.destroy();
            }

            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);

            speechRecognizer.setRecognitionListener(
                    new android.speech.RecognitionListener() {

                @Override
                public void onReadyForSpeech(Bundle params) {
                    statusText.setText("Listening...");
                    isListening = true;
                    micButton.setText("Listening...");
                }

                @Override
                public void onBeginningOfSpeech() {}

                @Override
                public void onRmsChanged(float rmsdB) {}

                @Override
                public void onBufferReceived(byte[] buffer) {}

                @Override
                public void onEndOfSpeech() {
                    isListening = false;
                    micButton.setText("🎤 SPEAK NOW");
                }

                @Override
                public void onError(int error) {
                    isListening = false;
                    micButton.setText("🎤 SPEAK NOW");
                    statusText.setText(
                            "Speech recognition error: " + error);
                }

                @Override
                public void onResults(Bundle results) {

                    ArrayList<String> matches =
                            results.getStringArrayList(
                                    SpeechRecognizer.RESULTS_RECOGNITION);

                    if (matches != null && !matches.isEmpty()) {
                        inputText.setText(matches.get(0));
                        statusText.setText("Speech captured successfully");
                        translateText();
                    }
                }

                @Override
                public void onPartialResults(Bundle partialResults) {}

                @Override
                public void onEvent(int eventType, Bundle params) {}
            });

            Intent intent = new Intent(
                    RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

            intent.putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

            intent.putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE,
                    englishToChinese ? "en-US" : "zh-CN");

            intent.putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,
                    englishToChinese ? "en-US" : "zh-CN");

            intent.putExtra(
                    RecognizerIntent.EXTRA_MAX_RESULTS, 1);

            speechRecognizer.startListening(intent);

        } catch (Exception e) {
            statusText.setText("Microphone error: " + e.getMessage());
        }
    }

    private void translateText() {

        String text = inputText.getText().toString().trim();

        if (text.isEmpty()) {
            statusText.setText("Please speak or enter some text.");
            return;
        }

        translateButton.setEnabled(false);
        statusText.setText("Preparing translation model...");

        if (translator != null) {
            translator.close();
        }

        TranslatorOptions options =
                new TranslatorOptions.Builder()
                        .setSourceLanguage(
                                englishToChinese
                                        ? TranslateLanguage.ENGLISH
                                        : TranslateLanguage.CHINESE)
                        .setTargetLanguage(
                                englishToChinese
                                        ? TranslateLanguage.CHINESE
                                        : TranslateLanguage.ENGLISH)
                        .build();

        translator = Translation.getClient(options);

        DownloadConditions conditions =
                new DownloadConditions.Builder().build();

        translator.downloadModelIfNeeded(conditions)

                .addOnSuccessListener(unused -> {

                    statusText.setText("Translating...");

                    translator.translate(text)

                            .addOnSuccessListener(translated -> {

                                outputText.setText(translated);

                                String record =
                                        "Input: " + text +
                                        "\nOutput: " + translated;

                                history.add(0, record);

                                updateHistory();

                                statusText.setText(
                                        "Translation completed!");

                                translateButton.setEnabled(true);

                                speakText(translated);
                            })

                            .addOnFailureListener(error -> {

                                statusText.setText(
                                        "Translation failed: " +
                                        error.getMessage());

                                translateButton.setEnabled(true);
                            });
                })

                .addOnFailureListener(error -> {

                    statusText.setText(
                            "Model download failed. Check internet: " +
                            error.getMessage());

                    translateButton.setEnabled(true);
                });
    }

    private void updateHistory() {

        StringBuilder builder = new StringBuilder();

        int limit = Math.min(history.size(), 10);

        for (int i = 0; i < limit; i++) {

            builder.append(i + 1)
                    .append(". ")
                    .append(history.get(i))
                    .append("\n\n");
        }

        historyText.setText(builder.toString());
    }

    private void speakText(String text) {

        if (!ttsReady || tts == null) {
            statusText.setText(
                    "Translation ready. Text-to-speech is not ready.");
            return;
        }

        setTTSLanguage();

        tts.speak(
                text,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "translator_audio");
    }

    private void speakTranslation() {

        String text = outputText.getText().toString();

        if (text.isEmpty() ||
                text.equals("Your translation will appear here.")) {

            statusText.setText("No translated text to play.");
            return;
        }

        speakText(text);
    }

    @Override
    protected void onDestroy() {

        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }

        if (translator != null) {
            translator.close();
        }

        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }

        super.onDestroy();
    }
}
