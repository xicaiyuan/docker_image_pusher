package com.wetype.voicebubble;

import android.os.SystemClock;

final class AppState {
    private AppState() {}

    static volatile boolean captureOpen = false;
    static volatile boolean calibrationOpen = false;
    static volatile boolean voiceSeen = false;
    static volatile boolean voiceEnded = false;
    static volatile long voiceSeenAt = 0L;
    static volatile long lastVoiceActiveAt = 0L;
    static volatile long recognitionSeenAt = 0L;

    static void resetVoice() {
        voiceSeen = false;
        voiceEnded = false;
        voiceSeenAt = 0L;
        lastVoiceActiveAt = 0L;
        recognitionSeenAt = 0L;
    }

    static void markVoiceSeen() {
        long now = SystemClock.elapsedRealtime();
        voiceSeen = true;
        voiceEnded = false;
        lastVoiceActiveAt = now;
        if (voiceSeenAt == 0L) voiceSeenAt = now;
    }

    static void markVoiceEnded() {
        if (voiceSeen) {
            voiceEnded = true;
            recognitionSeenAt = SystemClock.elapsedRealtime();
        }
    }
}
