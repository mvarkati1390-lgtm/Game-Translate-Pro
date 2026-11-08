# Game Translate Pro

Android app for translating Finglish player chat and visible game text into Persian.

## Implemented in 0.2.0
- Persian RTL Compose interface with Translation, Recording, and Downloads tabs.
- Offline starter Finglish phrase dictionary.
- Editable input, translate action, clear action, and selectable example phrases.
- Unknown words are preserved rather than guessed.

## Example phrases
- `salam` -> `سلام`
- `chetori?` -> `چطوری؟`
- `bia team beshim` -> `بیا تیم بشیم`
- `where are you` -> `کجا هستی؟`

## Not implemented yet
- Real-time OCR capture from other apps.
- Floating overlay permission and translation bubble.
- MediaProjection screen recording and saved-video browser.
- General-purpose online translation for unknown words.

The app must request Android screen-capture and overlay permissions explicitly before those features can work. Some apps/games may block capture.
