/*? if devtools {*/
/*package me.sshcrack.live_voice_translate.devtools;

import de.maxhenkel.voicechat.api.Position;
import de.maxhenkel.voicechat.api.VoicechatClientApi;
import de.maxhenkel.voicechat.api.audiochannel.ClientLocationalAudioChannel;
import me.sshcrack.live_voice_translate.AudioResampler;
import me.sshcrack.live_voice_translate.LiveVoiceTranslate;
import me.sshcrack.live_voice_translate.ModConfig;
import me.sshcrack.live_voice_translate.TranslationManager;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class DevTranslateRunner {

    private static DevTranslateRunner INSTANCE;

    private static final int FRAME_SIZE = 960;
    private static final long FRAME_INTERVAL_MS = 20;
    private static final long DRAIN_TIMEOUT_MS = 5000;
    private static final float CHANNEL_DISTANCE = 32.0f;
    private static final double SOURCE_RADIUS = 4.0;
    private static final double EAR_HEIGHT = 1.5;

    private VoicechatClientApi api;
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> tickTask;
    private final List<TestSource> sources = new ArrayList<>();

    public static DevTranslateRunner get() {
        if (INSTANCE == null) {
            INSTANCE = new DevTranslateRunner();
        }
        return INSTANCE;
    }

    public void onVoicechatConnected(VoicechatClientApi api) {
        if (sources.size() > 0) {
            LiveVoiceTranslate.LOGGER.info("DevTranslateRunner: test already running");
            return;
        }
        if (!ModConfig.get().isEnabled()) {
            LiveVoiceTranslate.LOGGER.warn("DevTranslateRunner: translation disabled in config");
            return;
        }

        this.api = api;
        Path devtestDir = Paths.get("config", LiveVoiceTranslate.MOD_ID, "devtest");
        if (!Files.exists(devtestDir)) {
            LiveVoiceTranslate.LOGGER.info("DevTranslateRunner: no devtest directory at {}", devtestDir.toAbsolutePath());
            return;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(devtestDir, "*.wav")) {
            for (Path wavPath : stream) {
                TestSource source = loadSource(wavPath);
                if (source != null) {
                    sources.add(source);
                }
            }
        } catch (IOException e) {
            LiveVoiceTranslate.LOGGER.error("DevTranslateRunner: failed to scan devtest directory", e);
        }

        if (sources.isEmpty()) {
            LiveVoiceTranslate.LOGGER.info("DevTranslateRunner: no WAV files found in devtest directory");
            return;
        }

        LiveVoiceTranslate.LOGGER.info("DevTranslateRunner: loaded {} test source(s)", sources.size());

        Position origin = api.createPosition(0, 0, 0);
        for (TestSource source : sources) {
            source.channel = api.createLocationalAudioChannel(source.uuid, origin);
            source.channel.setDistance(CHANNEL_DISTANCE);
        }

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "DevTranslateRunner");
            t.setDaemon(true);
            return t;
        });

        AtomicInteger idleCounter = new AtomicInteger();
        tickTask = scheduler.scheduleAtFixedRate(() -> {
            updatePositions();

            boolean anyRemaining = false;
            for (TestSource source : sources) {
                if (source.nextFrameIdx < source.totalFrames) {
                    feedFrame(source);
                    anyRemaining = true;
                }
                drainTranslated(source);
            }

            if (!anyRemaining) {
                boolean anyOutput = false;
                for (TestSource source : sources) {
                    short[] frame;
                    int played = 0;
                    while ((frame = TranslationManager.get().getTranslatedAudio(source.uuid)) != null) {
                        source.channel.play(frame);
                        anyOutput = true;
                        played++;
                    }
                    if (played > 0) {
                        LiveVoiceTranslate.LOGGER.info("[DevTools] Played {} translated frame(s) for source '{}'", played, source.name);
                    }
                }
                if (anyOutput) {
                    idleCounter.set(0);
                } else if (idleCounter.incrementAndGet() >= DRAIN_TIMEOUT_MS / 500) {
                    LiveVoiceTranslate.LOGGER.info("[DevTools] All sources drained, stopping");
                    stop();
                }
            } else {
                idleCounter.set(0);
            }
        }, 1000, FRAME_INTERVAL_MS, TimeUnit.MILLISECONDS);

        LiveVoiceTranslate.LOGGER.info("DevTranslateRunner: started feeding {} source(s)", sources.size());
    }

    private void updatePositions() {
        var player = Minecraft.getInstance().player;
        if (player == null) return;

        double px = player.getX();
        double py = player.getY() + EAR_HEIGHT;
        double pz = player.getZ();

        int count = sources.size();
        for (int i = 0; i < count; i++) {
            double angle = 2 * Math.PI * i / count;
            Position pos = api.createPosition(
                px + SOURCE_RADIUS * Math.sin(angle),
                py,
                pz + SOURCE_RADIUS * Math.cos(angle)
            );
            sources.get(i).channel.setLocation(pos);
        }
    }

    private void feedFrame(TestSource source) {
        int start = source.nextFrameIdx * FRAME_SIZE;
        int end = Math.min(start + FRAME_SIZE, source.pcm.length);
        short[] frame = Arrays.copyOfRange(source.pcm, start, end);
        if (source.nextFrameIdx % 50 == 0) {
            LiveVoiceTranslate.LOGGER.info("[DevTools] Feeding frame {}/{} for '{}'",
                source.nextFrameIdx + 1, source.totalFrames, source.name);
        }
        TranslationManager.get().feedAudio(source.uuid, frame);
        source.nextFrameIdx++;
    }

    private void drainTranslated(TestSource source) {
        short[] frame;
        while ((frame = TranslationManager.get().getTranslatedAudio(source.uuid)) != null) {
            source.channel.play(frame);
        }
    }

    public void onVoicechatDisconnected() {
        stop();
    }

    private void stop() {
        if (tickTask != null) {
            tickTask.cancel(false);
            tickTask = null;
        }
        if (scheduler != null) {
            scheduler.shutdown();
            scheduler = null;
        }
        for (TestSource source : sources) {
            TranslationManager.get().onPlayerSilence(source.uuid);
        }
        int count = sources.size();
        sources.clear();
        api = null;
        LiveVoiceTranslate.LOGGER.info("DevTranslateRunner: stopped, cleaned up {} source(s)", count);
    }

    private TestSource loadSource(Path wavPath) {
        try {
            WavAudio.Result wav = WavAudio.read(wavPath);
            short[] pcm = wav.samples;
            if (wav.sampleRate != 48000) {
                pcm = AudioResampler.resample(pcm, wav.sampleRate, 48000);
            }

            String filename = wavPath.getFileName().toString().toLowerCase();
            String base = filename.contains(".") ? filename.substring(0, filename.lastIndexOf('.')) : filename;
            String lang = "en";
            if (base.contains("_")) {
                String[] parts = base.split("_");
                String candidate = parts[parts.length - 1];
                if (candidate.matches("[a-z]{2}(-[a-zA-Z0-9]+)?")) {
                    lang = candidate;
                }
            }

            UUID uuid = UUID.nameUUIDFromBytes(("devtest:" + filename).getBytes());
            int totalFrames = (int) Math.ceil((double) pcm.length / FRAME_SIZE);

            double duration = pcm.length / 48000.0;
            LiveVoiceTranslate.LOGGER.info("DevTranslateRunner: loaded '{}' (lang={}, {}s, {} frames, uuid={})", filename, lang, String.format("%.1f", duration), totalFrames, uuid);

            return new TestSource(uuid, base, lang, pcm, totalFrames);
        } catch (IOException e) {
            LiveVoiceTranslate.LOGGER.error("DevTranslateRunner: failed to load {}", wavPath, e);
            return null;
        }
    }

    private static class TestSource {
        final UUID uuid;
        final String name;
        final String language;
        final short[] pcm;
        final int totalFrames;
        int nextFrameIdx;
        ClientLocationalAudioChannel channel;

        TestSource(UUID uuid, String name, String language, short[] pcm, int totalFrames) {
            this.uuid = uuid;
            this.name = name;
            this.language = language;
            this.pcm = pcm;
            this.totalFrames = totalFrames;
            this.nextFrameIdx = 0;
        }
    }
}
*//*?}*/
