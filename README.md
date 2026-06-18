# Word Coach 📖🎤

A simple, **offline** Android app for learning tough English words — built with
a beginner-friendly, large-text design.

It was made for a parent who wants to learn difficult English words, hear how
they are pronounced, and practice saying them.

## Features

- **Tough words with simple meanings** — each word is shown in large, easy-to-read
  text with an easy phonetic respelling (e.g. *Ephemeral → ih-FEM-er-ul*), a
  plain-English meaning, and an example sentence.
- **🔊 Hear it** — tap to replay the correct pronunciation as many times as you
  like, using the phone's built-in Text-to-Speech (works offline).
- **🎤 Speak & get scored** — tap the microphone, say the word, and the app rates
  your pronunciation from **1 to 5 stars** with friendly feedback.
- **Fully offline** — the word list is bundled inside the app, and both
  text-to-speech and speech recognition run on-device. No internet needed.
- **Easy navigation** — large Back / Shuffle / Next buttons.

## How pronunciation scoring works

When you speak, the on-device speech recognizer produces its best guesses. The
app compares each guess to the target word using:

1. **Edit distance** (how close the spelling is), and
2. **A Soundex phonetic code** (how close the *sound* is).

The combined similarity is mapped to a 1–5 star rating. See
[`PronunciationScorer.kt`](app/src/main/java/com/wordcoach/app/speech/PronunciationScorer.kt).

## Project structure

```
app/src/main/
  assets/words.json                  # the offline word list
  java/com/wordcoach/app/
    MainActivity.kt                  # entry point + mic permission handling
    data/                            # Word model + repository (loads words.json)
    speech/                          # TTS, speech recognition, scoring
    ui/                              # Compose UI, ViewModel, theme
```

## Building

Requirements: Android SDK (API 34), JDK 17.

```bash
./gradlew assembleDebug
```

The installable APK is created at:

```
app/build/outputs/apk/debug/app-debug.apk
```

Copy this file to an Android phone and open it to install (you may need to allow
"install from unknown sources").

## First-time setup on the phone (one time, needs internet once)

The app itself is offline, but the phone needs its English voices installed:

- **To hear words:** Settings → Language & input → Text-to-speech → install/enable
  the English voice. Most phones already have this.
- **To be scored when speaking:** Make sure Google's offline speech model for
  English is downloaded (Settings → Language & input → On-device speech).

After that, everything works without internet.

## Notes

- `minSdk` 24 (Android 7.0+), `targetSdk` 34.
- Speech recognition prefers the on-device recognizer on Android 13+ and falls
  back to offline-preferred recognition on older versions. On a small number of
  devices without any offline speech model, scoring may be unavailable while the
  word list and pronunciation replay still work.
