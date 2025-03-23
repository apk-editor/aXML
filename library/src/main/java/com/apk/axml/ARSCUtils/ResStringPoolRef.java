package com.apk.axml.ARSCUtils;

import java.io.IOException;

public class ResStringPoolRef {

    public long index;

    public static ResStringPoolRef parseFrom(PositionInputStream mStreamer) throws IOException {
        ResStringPoolRef ref = new ResStringPoolRef();
        ref.index = Utils.readInt(mStreamer);
        return ref;
    }
}