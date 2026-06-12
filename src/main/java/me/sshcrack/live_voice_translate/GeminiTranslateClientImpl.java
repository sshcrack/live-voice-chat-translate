package me.sshcrack.live_voice_translate;

import me.sshcrack.gemini_live_lib.GeminiLiveTranslateClient;
import me.sshcrack.gemini_live_lib.gson.ClientMessages;
import me.sshcrack.gemini_live_lib.gson.RealtimeInput;
import me.sshcrack.gemini_live_lib.websocket.handshake.ServerHandshake;

public class GeminiTranslateClientImpl extends GeminiLiveTranslateClient {

    private final PlayerTranslateSession session;

    public GeminiTranslateClientImpl(String apiKey, String targetLanguage, PlayerTranslateSession session) {
        super(apiKey, targetLanguage, true, null);
        this.session = session;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        LiveVoiceTranslate.LOGGER.info("[WS] Session {} connected to Gemini", session.getPlayerId());
        super.onOpen(handshakedata);
    }

    @Override
    public void onSetupComplete() {
        LiveVoiceTranslate.LOGGER.info("[WS] Session {} setup complete, ready for audio", session.getPlayerId());
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        LiveVoiceTranslate.LOGGER.info("[WS] Session {} closed (code={}, reason='{}', remote={})",
            session.getPlayerId(), code, reason, remote);
        super.onClose(code, reason, remote);
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
        short[] pcmShorts = AudioResampler.littleEndianBytesToShorts(audio);
        short[] pcm48k = AudioResampler.resample(pcmShorts, sampleRate, 48000);
        session.enqueueTranslatedFrame(pcm48k);
    }

    @Override
    public void onQuotaExceeded() {
        LiveVoiceTranslate.LOGGER.warn("[WS] Session {} quota exceeded", session.getPlayerId());
    }

    @Override
    public void onError(Exception ex) {
        LiveVoiceTranslate.LOGGER.error("[WS] Session {} error", session.getPlayerId(), ex);
    }
}
