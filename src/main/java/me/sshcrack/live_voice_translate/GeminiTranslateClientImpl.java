package me.sshcrack.live_voice_translate;

import me.sshcrack.gemini_live_lib.GeminiLiveTranslateClient;
import me.sshcrack.gemini_live_lib.gson.ClientMessages;
import me.sshcrack.gemini_live_lib.gson.RealtimeInput;

public class GeminiTranslateClientImpl extends GeminiLiveTranslateClient {

    private final PlayerTranslateSession session;

    public GeminiTranslateClientImpl(String apiKey, String targetLanguage, PlayerTranslateSession session) {
        super(apiKey, targetLanguage, true, null);
        this.session = session;
    }

    @Override
    public void addPromptAudio(short[] audio16k) {
        byte[] bytes = AudioResampler.shortsToLittleEndianBytes(audio16k);
        RealtimeInput input = new RealtimeInput();
        input.audio = new RealtimeInput.Blob("audio/pcm;rate=16000", bytes);
        send(ClientMessages.input(input));
    }

    @Override
    public void onTranslatedAudio(byte[] audio, int sampleRate) {
        short[] pcm24k = AudioResampler.littleEndianBytesToShorts(audio);
        short[] pcm48k = AudioResampler.resample(pcm24k, sampleRate, 48000);
        session.enqueueTranslatedFrame(pcm48k);
    }

    @Override
    public void onQuotaExceeded() {
        LiveVoiceTranslate.LOGGER.warn("Gemini API quota exceeded for session {}", session.getPlayerId());
    }

    @Override
    public void onError(Exception ex) {
        LiveVoiceTranslate.LOGGER.error("WebSocket error for session {}", session.getPlayerId(), ex);
    }
}
