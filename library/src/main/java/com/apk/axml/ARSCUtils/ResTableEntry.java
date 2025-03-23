package com.apk.axml.ARSCUtils;

import java.io.IOException;

public class ResTableEntry {

    public static final int FLAG_COMPLEX = 0x0001;
    public int size;
    public int flags;
    public ResStringPoolRef key;
    public int entryId;
    public String keyStr;

    public static ResTableEntry parseFrom(PositionInputStream mStreamer) throws IOException {
        ResTableEntry entry = new ResTableEntry();
        parseFrom(mStreamer, entry);
        return entry;
    }

    public static void parseFrom(PositionInputStream mStreamer, ResTableEntry entry) throws IOException {
        entry.size = Utils.readShort(mStreamer);
        entry.flags = Utils.readShort(mStreamer);
        entry.key = ResStringPoolRef.parseFrom(mStreamer);
    }
    
    @Override
    public String toString() {
        return " ";
    }

    public void translateValues(ResStringPoolChunk globalStringPool,
                                ResStringPoolChunk typeStringPool,
                                ResStringPoolChunk keyStringPool) {
        keyStr = keyStringPool.getString((int) key.index);
    }
}