package me.sshcrack.live_voice_translate;

import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

public class PlayerTranslateSession {

    private static final long IDLE_TIMEOUT_MS = 5000;

    private final UUID playerId;
    private final GeminiTranslateClientImpl geminiClient;
    private final ConcurrentLinkedQueue<short[]> translatedFrames = new ConcurrentLinkedQueue<>();
    private volatile long lastAudioTimestamp;
    private volatile boolean connected;

    public PlayerTranslateSession(UUID playerId, String apiKey, String targetLanguage) {
        this.playerId = playerId;
        this.geminiClient = new GeminiTranslateClientImpl(apiKey, targetLanguage, this);
        this.lastAudioTimestamp = System.currentTimeMillis();
    }

    public void connect() {
        LiveVoiceTranslate.LOGGER.info("[Session {}] Connecting to Gemini...", playerId);
        geminiClient.connect();
        connected = true;
    }

    public void feedAudio(short[] pcm48k) {
        lastAudioTimestamp = System.currentTimeMillis();
        LiveVoiceTranslate.LOGGER.debug("[Session {}] Feeding {} audio samples", playerId, pcm48k.length);
        short[] pcm16k = AudioResampler.resample(pcm48k, 48000, 16000);
        geminiClient.addPromptAudio(pcm16k);
    }

    public void enqueueTranslatedFrame(short[] pcm48k) {
        LiveVoiceTranslate.LOGGER.debug("[Session {}] Received translated frame ({} samples)", playerId, pcm48k.length);
        translatedFrames.add(pcm48k);
    }

    public short[] pollTranslatedFrame() {
        return translatedFrames.poll();
    }

    public boolean isIdle() {
        return (System.currentTimeMillis() - lastAudioTimestamp) > IDLE_TIMEOUT_MS;
    }

    public boolean isConnected() {
        return connected && geminiClient.isOpen() && geminiClient.isSetupComplete();
    }

    public void close() {
        connected = false;
        geminiClient.close();
    }

    public UUID getPlayerId() {
        return playerId;
    }
}
