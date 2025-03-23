package com.apk.axml.ARSCUtils;

import java.io.IOException;

public class ResFileHeaderChunk {

    public static final int LENGTH = 12;

    public ChunkHeader header;
    public long packageCount;

    public static ResFileHeaderChunk parseFrom(PositionInputStream mStreamer) throws IOException {
        ResFileHeaderChunk chunk = new ResFileHeaderChunk();
        chunk.header = ChunkHeader.parseFrom(mStreamer);
        chunk.packageCount = Utils.readInt(mStreamer);
        return chunk;
    }
}