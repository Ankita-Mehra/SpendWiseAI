# SpendWiseAI

An AI-powered Android app that turns receipt photos into organised expenses.(Kotlin, Jetpack Compose, Firebase AI Logic, Gemini, ML Kit)
It uses on-device OCR (ML Kit) and Gemini (through Firebase AI Logic), with a UI built in Jetpack Compose.

<img width="575" height="1280" alt="HomeScreen" src="https://github.com/user-attachments/assets/fd709b58-c36a-432d-8f1e-4512815782bb" />

<img width="575" height="1280" alt="ReceiptScreen" src="https://github.com/user-attachments/assets/7b31778d-3698-4f20-8504-32f985efa530" />




## Features

- Scan a receipt with the camera, or pick one from the gallery
- Automatically extracts the **store, date, total and category**
- Review and correct the AI result before saving
- Add expenses manually
- Monthly spending total with a breakdown by category
- Delete expenses you no longer need

## How it works

1. **ML Kit** reads the text from the receipt photo on the device (free, works offline).
2. **Gemini** (via Firebase AI Logic) converts that raw text into structured JSON: store, date, total, category.
3. The app **validates** the AI output (date format, allowed categories, non-negative total).
4. You **review** the result on an editable form, then save it.

```
Photo -> ML Kit OCR -> raw text -> Gemini (JSON) -> validation -> review screen -> save
```

## Tech stack

- Kotlin, Jetpack Compose, Material 3
- MVVM with Kotlin coroutines and StateFlow
- Gemini (`gemini-3.8-flash`) through Firebase AI Logic
- ML Kit Text Recognition (on-device)
- Firebase App Check
- Local JSON-file storage (easy to swap for Room)
- Min SDK 26

## Project structure

```
com.example.receiptai
├── MainActivity.kt
├── ReceiptApplication.kt      Firebase and App Check setup
├── ai/
│   └── ReceiptScanner.kt      OCR + Gemini + validation
├── data/
│   ├── Expense.kt
│   └── ExpenseStore.kt        JSON-file persistence
└── ui/
    ├── ExpenseViewModel.kt    scan state: Idle -> Scanning -> Ready / Failed
    ├── ReceiptApp.kt          root composable
    ├── HomeScreen.kt          summary, scan buttons, expense list
    ├── ReviewScreen.kt        editable form for the AI result
    └── AppTheme.kt
```

## Setup

This app calls Gemini through your own Firebase project (free on the Spark plan).

### 1. Clone and open

```bash
git clone https://github.com/YOUR_USERNAME/ReceiptAI.git
```

Open it in Android Studio and let Gradle sync.

### 2. Create a Firebase project

1. Go to the [Firebase console](https://console.firebase.google.com) and create a project.
2. Add an Android app with the your desired package name.
3. Download `google-services.json` and put it in the `app/` folder.
   This file is git-ignored on purpose. See `app/google-services.json.example` for the format.

### 3. Enable Gemini

1. In the Firebase console open **AI Logic -> Get started**.
2. Choose the **Gemini Developer API**.

No API key goes into the code. Firebase manages it for you.

### 4. Set up App Check

Firebase requires App Check for AI Logic.

1. Go to **App Check -> APIs -> Firebase AI Logic -> Set up**.
2. Baseline protection: **Enforced**. Replay protection: **Disabled**.
3. Run the app once from Android Studio, then search Logcat for `DebugAppCheckProvider` and copy the debug token.
4. In **App Check -> Apps -> your app -> Manage debug tokens**, add that token.

Debug builds use the App Check debug provider. Release builds use Play Integrity.
Never commit your debug token.

### 5. Run

Use a real device, or an emulator with internet. On an emulator, drag a receipt image onto it and use **From gallery**.


## Security and privacy

- No API keys in the app. Gemini is reached through Firebase AI Logic.
- App Check blocks requests that don't come from your app.
- `google-services.json` and debug tokens are git-ignored.
- OCR runs on the device, but the recognised receipt text is sent to Google's Gemini API.

## Roadmap

- Replace the JSON store with Room
- Extract line items from receipts
- CSV export and monthly charts
- Edit existing expenses
- Ask questions about your expenses ("How much did I spend on food?")

## License

Add a license of your choice (for example MIT) in a `LICENSE` file.
