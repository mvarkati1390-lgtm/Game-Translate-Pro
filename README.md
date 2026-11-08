# Game Translate Pro

## Implemented in 0.3.0
- Persian RTL Compose interface and offline starter Finglish dictionary.
- Android MediaProjection consent flow for screen recording.
- Foreground service declared with mediaProjection type and persistent notification.
- MP4 screen recordings saved to app-specific Movies/GameTranslatePro.
- Basic in-app list of saved recordings.

## Limitations
- Android asks for screen-capture consent each time.
- Protected games/apps may block capture or show a black frame.
- Recordings are app-specific and removed when the app is uninstalled.
- Playback, share and delete actions are not implemented in the downloads list yet.
- OCR from captured frames and a floating translation overlay remain pending.
- Unknown phrases require a larger dictionary or a translation service.

Build with Android Studio, JDK 17 and compile SDK 35.
