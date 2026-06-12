package me.sshcrack.live_voice_translate;

import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class PlayerTranslateSession {

    private static final long IDLE_TIMEOUT_MS = 5000;
    private static final int MAX_QUEUED_FRAMES = 100;

    private final UUID playerId;
    private final GeminiTranslateClientImpl geminiClient;
    private final ConcurrentLinkedQueue<short[]> translatedFrames = new ConcurrentLinkedQueue<>();
    private final AtomicInteger queuedFrameCount = new AtomicInteger();
    private volatile long lastAudioTimestamp;

    public PlayerTranslateSession(UUID playerId, String apiKey, String targetLanguage) {
        this.playerId = playerId;
        this.geminiClient = new GeminiTranslateClientImpl(apiKey, targetLanguage, this);
        this.lastAudioTimestamp = System.currentTimeMillis();
    }

    public void connect() {
        LiveVoiceTranslate.LOGGER.info("[Session {}] Connecting to Gemini...", playerId);
        geminiClient.connect();
    }

    public void feedAudio(short[] pcm48k) {
        lastAudioTimestamp = System.currentTimeMillis();
        short[] pcm16k = AudioResampler.resample(pcm48k, 48000, 16000);
        geminiClient.addPromptAudio(pcm16k);
    }

    public void enqueueTranslatedFrame(short[] pcm48k) {
        if (queuedFrameCount.get() >= MAX_QUEUED_FRAMES) {
            translatedFrames.poll();
        } else {
            queuedFrameCount.incrementAndGet();
        }
        translatedFrames.add(pcm48k);
    }

    public short[] pollTranslatedFrame() {
        short[] frame = translatedFrames.poll();
        if (frame != null) {
            queuedFrameCount.decrementAndGet();
        }
        return frame;
    }

    public boolean isIdle() {
        return (System.currentTimeMillis() - lastAudioTimestamp) > IDLE_TIMEOUT_MS;
    }

    public boolean isConnectedToSocket() {
        return isConnected();
    }

    public boolean isConnected() {
        return geminiClient.isOpen() && geminiClient.isSetupComplete();
    }

    public void close() {
        geminiClient.close();
    }

    public UUID getPlayerId() {
        return playerId;
    }
}
