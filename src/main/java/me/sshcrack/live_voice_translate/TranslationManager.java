package me.sshcrack.live_voice_translate;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class TranslationManager {

    private static TranslationManager INSTANCE;

    private final Map<UUID, PlayerTranslateSession> activeSessions = new HashMap<>();
    private final Queue<PlayerTranslateSession> pendingQueue = new ArrayDeque<>();
    private final ReentrantLock lock = new ReentrantLock();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "TranslationManager-IdleChecker");
        t.setDaemon(true);
        return t;
    });

    private String apiKey;
    private String targetLanguage;
    private int maxSockets;

    private TranslationManager(String apiKey, String targetLanguage, int maxSockets) {
        this.apiKey = apiKey;
        this.targetLanguage = targetLanguage;
        this.maxSockets = maxSockets;
        this.scheduler.scheduleAtFixedRate(this::checkIdleSessions, 5, 1, TimeUnit.SECONDS);
    }

    public static void initialize(String apiKey, String targetLanguage, int maxSockets) {
        INSTANCE = new TranslationManager(apiKey, targetLanguage, maxSockets);
    }

    public static TranslationManager get() {
        return INSTANCE;
    }

    private int getOpenSocketCount() {
        int count = 0;
        for (PlayerTranslateSession s : activeSessions.values()) {
            if (s.isConnected()) count++;
        }
        return count;
    }

    public void feedAudio(UUID playerId, short[] audio48k) {
        lock.lock();
        try {
            PlayerTranslateSession session = activeSessions.get(playerId);
            if (session == null) {
                LiveVoiceTranslate.LOGGER.info("[TM] New session for player {} ({} pending)",
                    playerId, pendingQueue.size());
                session = new PlayerTranslateSession(playerId, apiKey, targetLanguage);
                activeSessions.put(playerId, session);
                if (getOpenSocketCount() < maxSockets) {
                    session.connect();
                } else {
                    LiveVoiceTranslate.LOGGER.info("[TM] Queuing session {} (max {} sockets reached)", playerId, maxSockets);
                    pendingQueue.add(session);
                }
            }
            if (session.isConnected()) {
                session.feedAudio(audio48k);
            }
        } finally {
            lock.unlock();
        }
    }

    public short[] getTranslatedAudio(UUID playerId) {
        lock.lock();
        try {
            PlayerTranslateSession session = activeSessions.get(playerId);
            return session != null ? session.pollTranslatedFrame() : null;
        } finally {
            lock.unlock();
        }
    }

    public void onPlayerSilence(UUID playerId) {
        closeSession(playerId);
    }

    private void closeSession(UUID playerId) {
        lock.lock();
        try {
            PlayerTranslateSession session = activeSessions.remove(playerId);
            if (session != null) {
                session.close();
                LiveVoiceTranslate.LOGGER.info("[TM] Closed session {} ({} open sockets remaining)", playerId, getOpenSocketCount());
                promoteNext();
            }
        } finally {
            lock.unlock();
        }
    }

    private void promoteNext() {
        PlayerTranslateSession next = pendingQueue.poll();
        if (next != null) {
            LiveVoiceTranslate.LOGGER.info("[TM] Promoting pending session {} ({} open sockets)", next.getPlayerId(), getOpenSocketCount());
            next.connect();
        }
    }

    private void checkIdleSessions() {
        lock.lock();
        try {
            activeSessions.entrySet().removeIf(entry -> {
                PlayerTranslateSession session = entry.getValue();
                if (session.isIdle()) {
                    LiveVoiceTranslate.LOGGER.debug("Closing idle translation session for player {}", entry.getKey());
                    session.close();
                    promoteNext();
                    return true;
                }
                return false;
            });
        } finally {
            lock.unlock();
        }
    }

    public void hotReload() {
        lock.lock();
        try {
            LiveVoiceTranslate.LOGGER.info("[TM] Hot-reloading translation manager");
            for (PlayerTranslateSession session : activeSessions.values()) {
                session.close();
            }
            activeSessions.clear();
            pendingQueue.clear();

            ModConfig config = ModConfig.get();
            this.apiKey = config.getApiKey();
            this.targetLanguage = config.getTargetLanguage();
            this.maxSockets = config.getMaxWebSockets();
            LiveVoiceTranslate.LOGGER.info("[TM] Hot-reloaded configuration: lang={}, maxSockets={}",
                targetLanguage, maxSockets);
        } finally {
            lock.unlock();
        }
    }

    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(2, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        lock.lock();
        try {
            for (PlayerTranslateSession session : activeSessions.values()) {
                session.close();
            }
            activeSessions.clear();
            pendingQueue.clear();
        } finally {
            lock.unlock();
        }
    }
}
