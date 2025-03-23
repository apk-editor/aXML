package com.apk.axml.ARSCUtils;

import java.io.IOException;

public class ResTableMapEntry extends ResTableEntry {

    public ResTableRef parent;
    public long count;
    public ResTableMap[] resTableMaps;

    public static ResTableMapEntry parseFrom(PositionInputStream mStreamer) throws IOException {
        ResTableMapEntry entry = new ResTableMapEntry();
        ResTableEntry.parseFrom(mStreamer, entry);

        entry.parent = ResTableRef.parseFrom(mStreamer);
        entry.count = Utils.readInt(mStreamer);

        entry.resTableMaps = new ResTableMap[(int) entry.count];
        for (int i = 0; i < entry.count; ++i) {
            entry.resTableMaps[i] = ResTableMap.parseFrom(mStreamer);
        }

        return entry;
    }

    @Override
    public void translateValues(ResStringPoolChunk globalStringPool,
            ResStringPoolChunk typeStringPool,
            ResStringPoolChunk keyStringPool) {
        super.translateValues(globalStringPool, typeStringPool, keyStringPool);
        for (ResTableMap resTableMap : resTableMaps) {
            resTableMap.translateValues(globalStringPool);
        }
    }
}