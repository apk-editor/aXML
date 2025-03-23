package com.apk.axml.ARSCUtils;

public abstract class BaseTypeChunk {

    public abstract String getChunkName();

    public abstract long getEntryCount();

    public abstract String getType();

    public abstract int getTypeId();

    public abstract void translateValues(ResStringPoolChunk globalStringPool,
                                ResStringPoolChunk typeStringPool,
                                ResStringPoolChunk keyStringPool);
}