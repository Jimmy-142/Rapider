# Rapider

Rapider is an Android app (Java) for rapid serial visual presentation (RSVP) reading from PDF files.

## What it does

- Imports a PDF using the system file picker.
- Extracts words and displays one word at a time in a fixed location.
- Highlights the center letter in each word to anchor eye focus.
- Lets the user control playback speed with a WPM slider.

## Tech stack

- Java + Android SDK (min SDK 24)
- Material Components UI
- `pdfbox-android` for PDF text extraction

## Run

1. Open this project in Android Studio (Hedgehog or newer).
2. Let Gradle sync and install missing SDKs if prompted.
3. Run the `app` configuration on an Android device/emulator.
4. Tap **Pick PDF**, then **Play**.
