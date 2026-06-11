/*? if devtools {*/
package me.sshcrack.live_voice_translate.devtools;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;

public class WavAudio {

    public static class Result {
        public final short[] samples;
        public final int sampleRate;

        public Result(short[] samples, int sampleRate) {
            this.samples = samples;
            this.sampleRate = sampleRate;
        }
    }

    public static Result read(Path path) throws IOException {
        byte[] all = Files.readAllBytes(path);
        ByteBuffer buf = ByteBuffer.wrap(all).order(ByteOrder.LITTLE_ENDIAN);

        byte[] riff = new byte[4];
        buf.get(riff);
        if (riff[0] != 'R' || riff[1] != 'I' || riff[2] != 'F' || riff[3] != 'F') {
            throw new IOException("Not a RIFF file");
        }
        buf.getInt(); // file size - 8
        byte[] wave = new byte[4];
        buf.get(wave);
        if (wave[0] != 'W' || wave[1] != 'A' || wave[2] != 'V' || wave[3] != 'E') {
            throw new IOException("Not a WAVE file");
        }

        int sampleRate = 0;
        int bitsPerSample = 0;
        int numChannels = 0;
        short[] samples = null;

        while (buf.remaining() >= 8) {
            byte[] chunkId = new byte[4];
            buf.get(chunkId);
            int chunkSize = buf.getInt();

            String id = new String(chunkId, java.nio.charset.StandardCharsets.US_ASCII);
            if (id.equals("fmt ")) {
                int audioFormat = buf.getShort() & 0xFFFF;
                numChannels = buf.getShort() & 0xFFFF;
                sampleRate = buf.getInt();
                buf.getInt(); // byteRate
                buf.getShort(); // blockAlign
                bitsPerSample = buf.getShort() & 0xFFFF;
                if (audioFormat != 1) {
                    throw new IOException("Only PCM WAV supported, got format " + audioFormat);
                }
                if (bitsPerSample != 16) {
                    throw new IOException("Only 16-bit WAV supported, got " + bitsPerSample);
                }
                // skip any extra fmt bytes
                int fmtRead = 16;
                while (fmtRead < chunkSize) {
                    buf.get();
                    fmtRead++;
                }
            } else if (id.equals("data")) {
                int bytesPerSample = bitsPerSample / 8;
                int totalSamples = chunkSize / bytesPerSample;
                samples = new short[totalSamples / numChannels];
                for (int i = 0; i < samples.length; i++) {
                    // average channels if stereo
                    long sum = 0;
                    for (int ch = 0; ch < numChannels; ch++) {
                        sum += buf.getShort();
                    }
                    samples[i] = (short) (sum / numChannels);
                }
            } else {
                // skip unknown chunks
                long toSkip = chunkSize;
                if (chunkSize % 2 != 0) toSkip++; // padding byte
                buf.position(buf.position() + (int) toSkip);
            }
        }

        if (samples == null) {
            throw new IOException("No data chunk found in WAV file");
        }

        return new Result(samples, sampleRate);
    }

    public static void write(Path path, short[] samples, int sampleRate) throws IOException {
        int bytesPerSample = 2;
        int numChannels = 1;
        int dataSize = samples.length * bytesPerSample;
        int fileSize = 36 + dataSize;

        ByteBuffer buf = ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN);

        buf.put((byte) 'R');
        buf.put((byte) 'I');
        buf.put((byte) 'F');
        buf.put((byte) 'F');
        buf.putInt(fileSize);
        buf.put((byte) 'W');
        buf.put((byte) 'A');
        buf.put((byte) 'V');
        buf.put((byte) 'E');

        buf.put((byte) 'f');
        buf.put((byte) 'm');
        buf.put((byte) 't');
        buf.put((byte) ' ');
        buf.putInt(16);
        buf.putShort((short) 1); // PCM
        buf.putShort((short) numChannels);
        buf.putInt(sampleRate);
        buf.putInt(sampleRate * numChannels * bytesPerSample);
        buf.putShort((short) (numChannels * bytesPerSample));
        buf.putShort((short) (bytesPerSample * 8));

        buf.put((byte) 'd');
        buf.put((byte) 'a');
        buf.put((byte) 't');
        buf.put((byte) 'a');
        buf.putInt(dataSize);

        for (short sample : samples) {
            buf.putShort(sample);
        }

        try (OutputStream out = Files.newOutputStream(path)) {
            out.write(buf.array());
        }
    }
}
/*?}*/
