package me.sshcrack.live_voice_translate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class TranslationManager {

    private static TranslationManager INSTANCE;

    private record QueuedSession(PlayerTranslateSession session, double[] position) {}

    private final Map<UUID, PlayerTranslateSession> activeSessions = new HashMap<>();
    private final List<QueuedSession> pendingList = new ArrayList<>();
    private final Map<UUID, double[]> playerPositions = new HashMap<>();
    private final ReentrantLock lock = new ReentrantLock();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "TranslationManager-IdleChecker");
        t.setDaemon(true);
        return t;
    });

    private final String apiKey;
    private final String targetLanguage;
    private final int maxSockets;

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

    public void feedAudio(UUID playerId, short[] audio48k, double[] position) {
        if (position != null) {
            playerPositions.put(playerId, position);
        }
        feedAudio(playerId, audio48k);
    }

    public void feedAudio(UUID playerId, short[] audio48k) {
        lock.lock();
        try {
            PlayerTranslateSession session = activeSessions.get(playerId);
            if (session == null) {
                LiveVoiceTranslate.LOGGER.info("[TM] New session for player {} ({} pending)",
                    playerId, pendingList.size());
                session = new PlayerTranslateSession(playerId, apiKey, targetLanguage);
                activeSessions.put(playerId, session);
                if (getOpenSocketCount() < maxSockets) {
                    session.connect();
                } else {
                    LiveVoiceTranslate.LOGGER.info("[TM] Queuing session {} (max {} sockets reached)", playerId, maxSockets);
                    double[] pos = playerPositions.get(playerId);
                    pendingList.add(new QueuedSession(session, pos));
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
                playerPositions.remove(playerId);
                LiveVoiceTranslate.LOGGER.info("[TM] Closed session {} ({} open sockets remaining)", playerId, getOpenSocketCount());
                promoteNext();
            }
        } finally {
            lock.unlock();
        }
    }

    private void promoteNext() {
        if (pendingList.isEmpty()) return;

        var player = net.minecraft.client.Minecraft.getInstance().player;
        if (player == null) {
            LiveVoiceTranslate.LOGGER.warn("[TM] Local player is null, falling back to FIFO promotion");
            var entry = pendingList.remove(0);
            entry.session.connect();
            return;
        }

        pendingList.sort(Comparator.comparingDouble(e -> {
            double[] pos = e.position;
            if (pos == null) return Double.MAX_VALUE;
            double dx = pos[0] - player.getX();
            double dy = pos[1] - player.getY();
            double dz = pos[2] - player.getZ();
            return dx * dx + dy * dy + dz * dz;
        }));

        var nearest = pendingList.remove(0);
        LiveVoiceTranslate.LOGGER.info("[TM] Promoting pending session {} ({} open sockets)", nearest.session.getPlayerId(), getOpenSocketCount());
        nearest.session.connect();
    }

    private void checkIdleSessions() {
        lock.lock();
        try {
            activeSessions.entrySet().removeIf(entry -> {
                PlayerTranslateSession session = entry.getValue();
                if (session.isIdle()) {
                    LiveVoiceTranslate.LOGGER.debug("Closing idle translation session for player {}", entry.getKey());
                    session.close();
                    playerPositions.remove(entry.getKey());
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
            pendingList.clear();
            playerPositions.clear();
        } finally {
            lock.unlock();
        }
    }
}
