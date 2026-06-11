package me.sshcrack.live_voice_translate;

public class AudioResampler {

    public static short[] resample(short[] input, int fromHz, int toHz) {
        if (fromHz == toHz) {
            return input.clone();
        }
        double ratio = (double) fromHz / toHz;
        int outputLength = (int) Math.ceil(input.length / ratio);
        short[] output = new short[outputLength];
        for (int i = 0; i < outputLength; i++) {
            double srcIdx = i * ratio;
            int lo = (int) srcIdx;
            int hi = Math.min(lo + 1, input.length - 1);
            double frac = srcIdx - lo;
            output[i] = (short) (input[lo] * (1.0 - frac) + input[hi] * frac);
        }
        return output;
    }

    public static byte[] shortsToLittleEndianBytes(short[] samples) {
        byte[] bytes = new byte[samples.length * 2];
        for (int i = 0; i < samples.length; i++) {
            bytes[i * 2] = (byte) (samples[i] & 0xFF);
            bytes[i * 2 + 1] = (byte) ((samples[i] >> 8) & 0xFF);
        }
        return bytes;
    }

    public static short[] littleEndianBytesToShorts(byte[] bytes) {
        short[] samples = new short[bytes.length / 2];
        for (int i = 0; i < samples.length; i++) {
            int low = bytes[i * 2] & 0xFF;
            int high = bytes[i * 2 + 1] << 8;
            samples[i] = (short) (low | high);
        }
        return samples;
    }
}
