package com.apk.axml.utils;

import java.io.IOException;

public class EndNameSpaceChunk extends Chunk<EndNameSpaceChunk.H> {

    public class H extends Chunk.NodeHeader {
        public H() {
            super(ChunkType.XmlEndNamespace);
            size = 0x18;
        }
    }
    public StartNameSpaceChunk start;
    public EndNameSpaceChunk(Chunk parent, StartNameSpaceChunk start) {
        super(parent);
        this.start=start;
    }
    @Override
    public void writeEx(IntWriter w) throws IOException {
        start.writeEx(w);
    }

}