package com.apk.axml.ARSCUtils;

import java.io.IOException;

public class ResTableValueEntry extends ResTableEntry {

    public ResValue resValue;       

    public static ResTableValueEntry parseFrom(PositionInputStream mStreamer) throws IOException {
        ResTableValueEntry entry = new ResTableValueEntry();
        ResTableEntry.parseFrom(mStreamer, entry);
        entry.resValue = ResValue.parseFrom(mStreamer);
        return entry;
    }

    @Override
    public void translateValues(ResStringPoolChunk globalStringPool,
                                ResStringPoolChunk typeStringPool,
                                ResStringPoolChunk keyStringPool) {
        super.translateValues(globalStringPool, typeStringPool, keyStringPool);
        resValue.translateValues(globalStringPool);
    }
    
    @Override
    public String toString() {
        return super.toString() + "name=\""+keyStr+"\"  data=\""+resValue.toString()+"\"";
    }
    
}