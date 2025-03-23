package com.apk.axml.ARSCUtils;

import java.io.IOException;

public class ResTableRef {

    public long ident;

    public static ResTableRef parseFrom(PositionInputStream mStreamer) throws IOException {
        ResTableRef ref = new ResTableRef();
        ref.ident = Utils.readInt(mStreamer);
        return ref;
    }

    @Override
    public String toString() {
        return String.format("%s: 0x%s", "ident", (ident));
    }
}