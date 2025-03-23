package com.apk.axml.ARSCUtils;

import android.annotation.TargetApi;
import android.os.Build;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class Utils {

    public static short readUInt8(PositionInputStream mStreamer) throws IOException {
        byte[] bytes = new byte[1];
        mStreamer.read(bytes);
        return getUInt8(bytes);
    }

    public static int readShort(PositionInputStream mStreamer) throws IOException {
        byte[] bytes = new byte[2];
        mStreamer.read(bytes);
        return getShort(bytes);
    }

    public static long readInt(PositionInputStream mStreamer) throws IOException {
        byte[] bytes = new byte[4];
        mStreamer.read(bytes);
        return getInt(bytes);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static String readString(PositionInputStream mStreamer, int length) throws IOException {
        byte[] bytes = new byte[length];
        mStreamer.read(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static String readString16(PositionInputStream mStreamer, int length) throws IOException {
        byte[] bytes = new byte[length];
        StringBuilder builder = new StringBuilder();
        mStreamer.read(bytes);
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        byte[] buf_2 = new byte[2];
        while (in.read(buf_2) != -1) {
            int code = getShort(buf_2);
            if (code == 0x00) {
                break;
            } else {
                builder.append((char) code);
            }
        }
        return builder.toString();
    }

    public static short getUInt8(byte[] bytes) {
        return (short) (bytes[0] & 0xFF);
    }

    public static int getShort(byte[] bytes) {
        return (int) (bytes[1] << 8 & 0xff00 | bytes[0] & 0xFF);
    }

    public static long getInt(byte[] bytes) {
        return (long) bytes[3]
                << 24 & 0xff000000L
                | bytes[2]
                << 16 & 0xff0000
                | bytes[1]
                << 8 & 0xff00
                | bytes[0] & 0xFF;
    }
}