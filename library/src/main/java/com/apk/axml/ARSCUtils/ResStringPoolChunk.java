package com.apk.axml.ARSCUtils;

import java.io.IOException;

import java.util.ArrayList;
import java.util.List;

public class ResStringPoolChunk {

    private static final int UTF8_FLAG = 1 << 8;
    public ChunkHeader header;
    public long stringCount;
    public long styleCount;
    public long flags;
    public long stringsStart;
    public long stylesStart;
    public long[] stringOffsetArray;
    public long[] styleOffsetArray;
    public List<String> strings;
    public List<String> styles;

    public static ResStringPoolChunk parseFrom(PositionInputStream mStreamer) throws IOException {
        long baseCursor = mStreamer.getPosition();

        ResStringPoolChunk chunk = new ResStringPoolChunk();
        chunk.header = ChunkHeader.parseFrom(mStreamer);
        chunk.stringCount = Utils.readInt(mStreamer);
        chunk.styleCount = Utils.readInt(mStreamer);
        chunk.flags = Utils.readInt(mStreamer);
        chunk.stringsStart = Utils.readInt(mStreamer);
        chunk.stylesStart = Utils.readInt(mStreamer);

        boolean utf8 = (chunk.flags & UTF8_FLAG) != 0;

        long[] strOffsets = chunk.stringOffsetArray = new long[(int) chunk.stringCount];
        long[] styleOffsets = chunk.styleOffsetArray = new long[(int) chunk.styleCount];
        List<String> strings = chunk.strings = new ArrayList<>((int) chunk.stringCount);
        List<String> styles = chunk.styles = new ArrayList<>((int) chunk.styleCount);

        for (int i = 0; i < chunk.stringCount; ++i) {
            strOffsets[i] = Utils.readInt(mStreamer);
        }
        for (int i = 0; i < chunk.styleCount; ++i) {
            styleOffsets[i] = Utils.readInt(mStreamer);
        }
        for (int i = 0; i < chunk.stringCount; ++i) {
            long start = baseCursor + chunk.stringsStart + strOffsets[i];
            mStreamer.seek(start);

            if (utf8) {
                int strlen = Utils.readUInt8(mStreamer);
                int len = Utils.readUInt8(mStreamer);
                String str = Utils.readString(mStreamer, len);
                strings.add(str);
            } else {
                int len = Utils.readShort(mStreamer);
                String str = Utils.readString16(mStreamer, len * 2);
                strings.add(str);
            }
        }
        for (int i = 0; i < chunk.styleCount; ++i) {
            long start = baseCursor + chunk.stylesStart + styleOffsets[i];
            mStreamer.seek(start);
            int len = (Utils.readShort(mStreamer) & 0x7f00) >> 8;
            String str = Utils.readString(mStreamer, len);
            styles.add(str);
        }

        return chunk;
    }

    public String getString(int idx) {
        try{
            return strings != null && idx < strings.size() ? strings.get(idx) : null;
        } catch (Exception e) {
            return null;
        }
    }

    public String getStyle(int idx) {
        return styles != null && idx < styles.size() ? styles.get(idx) : null;
    }

}