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

    private final String apiKey;
    private final String targetLanguage;
    private final int maxSockets;
    private int openSocketCount;

    private TranslationManager(String apiKey, String targetLanguage, int maxSockets) {
        this.apiKey = apiKey;
        this.targetLanguage = targetLanguage;
        this.maxSockets = maxSockets;
        this.openSocketCount = 0;
        this.scheduler.scheduleAtFixedRate(this::checkIdleSessions, 5, 1, TimeUnit.SECONDS);
    }

    public static void initialize(String apiKey, String targetLanguage, int maxSockets) {
        INSTANCE = new TranslationManager(apiKey, targetLanguage, maxSockets);
    }

    public static TranslationManager get() {
        return INSTANCE;
    }

    public void feedAudio(UUID playerId, short[] audio48k) {
        PlayerTranslateSession session;
        lock.lock();
        try {
            session = activeSessions.get(playerId);
            if (session == null) {
                session = new PlayerTranslateSession(playerId, apiKey, targetLanguage);
                activeSessions.put(playerId, session);
                if (openSocketCount < maxSockets) {
                    session.connect();
                    openSocketCount++;
                } else {
                    pendingQueue.add(session);
                }
            }
        } finally {
            lock.unlock();
        }

        if (session != null && session.isConnected()) {
            session.feedAudio(audio48k);
        }
    }

    public short[] getTranslatedAudio(UUID playerId) {
        PlayerTranslateSession session = activeSessions.get(playerId);
        return session != null ? session.pollTranslatedFrame() : null;
    }

    public void onPlayerSilence(UUID playerId) {
        closeSession(playerId);
    }

    private void closeSession(UUID playerId) {
        lock.lock();
        try {
            PlayerTranslateSession session = activeSessions.remove(playerId);
            if (session != null) {
                boolean wasConnected = session.isConnected();
                session.close();
                if (wasConnected) {
                    openSocketCount--;
                }
                promoteNext();
            }
        } finally {
            lock.unlock();
        }
    }

    private void promoteNext() {
        PlayerTranslateSession next = pendingQueue.poll();
        if (next != null) {
            next.connect();
            openSocketCount++;
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
                    openSocketCount--;
                    promoteNext();
                    return true;
                }
                return false;
            });
        } finally {
            lock.unlock();
        }
    }

    public void shutdown() {
        scheduler.shutdown();
        lock.lock();
        try {
            for (PlayerTranslateSession session : activeSessions.values()) {
                session.close();
            }
            activeSessions.clear();
            pendingQueue.clear();
            openSocketCount = 0;
        } finally {
            lock.unlock();
        }
    }
}
