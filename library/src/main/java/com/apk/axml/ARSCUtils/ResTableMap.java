package com.apk.axml.ARSCUtils;

import java.io.IOException;

public class ResTableMap {

    public ResTableRef name;
    public ResValue value;

    public static ResTableMap parseFrom(PositionInputStream mStreamer) throws IOException {
        ResTableMap tableMap = new ResTableMap();
        tableMap.name = ResTableRef.parseFrom(mStreamer);
        tableMap.value = ResValue.parseFrom(mStreamer);
        return tableMap;
    }

    @Override
    public String toString() {
        return String.format("%-10s {%s}\n", "name", name.toString()) +
                "value:\n" + value.toString();
    }

    public void translateValues(ResStringPoolChunk globalStringPool) {
        value.translateValues(globalStringPool);
    }
}