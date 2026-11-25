# Chinese English Translator (Android / Java)

A simple Android app that translates between Chinese and English using Google ML Kit on-device translation.

## Features
- Chinese to English and English to Chinese text translation
- First-use translation model download
- Translation can work offline after the model is downloaded
- Chinese or English speech input (uses the device's speech-recognition service)
- Copy and share translated text
- Stores the latest 10 translations locally

## Build with GitHub Actions (no Android Studio required)
1. Create a GitHub repository.
2. Upload all files and folders from this project to the repository root.
3. Open the **Actions** tab and enable workflows if GitHub asks.
4. Select **Build Android APK** and click **Run workflow**, or push a commit.
5. When the workflow completes, open the run and download the artifact named `Chinese-English-Translator-APK`.
6. Extract the artifact and install `app-debug.apk` on your Android phone. You may need to allow installation from that source.

## Requirements
- Android 6.0+ (minSdk 23); designed for Android 12+
- Internet connection for the first translation-model download
- Microphone / speech-recognition service for voice input

## Notes
- The translation model is downloaded by ML Kit when first needed. The model is roughly 30 MB.
- Voice recognition depends on a speech-recognition service installed on the phone and may need internet.
- This is a debug APK for testing, not a Play Store release.
