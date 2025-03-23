package com.apk.axml.ARSCUtils;

import android.annotation.TargetApi;
import android.os.Build;

import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ResTablePackageChunk {

    public static final int RES_TABLE_TYPE_SPEC_TYPE = 0x0202;
    public static final int RES_TABLE_TYPE_TYPE = 0x0201;
    public ChunkHeader header;
    public long pkgId;
    public String packageName;
    public long typeStringOffset;
    public long lastPublicType;
    public long keyStringOffset;
    public long lastPublicKey;
    public ResStringPoolChunk typeStringPool;
    public ResStringPoolChunk keyStringPool;
    public List<BaseTypeChunk> typeChunks;
    public Map<Integer, List<BaseTypeChunk>> typeInfoIndexer;

    public static ResTablePackageChunk parseFrom(PositionInputStream mStreamer, ResStringPoolChunk stringChunk) throws IOException {
        ResTablePackageChunk chunk = new ResTablePackageChunk();
        chunk.header = ChunkHeader.parseFrom(mStreamer);
        chunk.pkgId = Utils.readInt(mStreamer);
        chunk.packageName = Utils.readString16(mStreamer, 128 * 2);
        chunk.typeStringOffset = Utils.readInt(mStreamer);
        chunk.lastPublicType = Utils.readInt(mStreamer);
        chunk.keyStringOffset = Utils.readInt(mStreamer);
        chunk.lastPublicKey = Utils.readInt(mStreamer);

        mStreamer.seek(chunk.typeStringOffset);
        chunk.typeStringPool = ResStringPoolChunk.parseFrom(mStreamer);
        mStreamer.seek(chunk.keyStringOffset);
        chunk.keyStringPool = ResStringPoolChunk.parseFrom(mStreamer);

        mStreamer.seek(chunk.keyStringOffset + chunk.keyStringPool.header.chunkSize);
        chunk.typeChunks = new ArrayList<>();
        StringBuilder logInfo = new StringBuilder();
        while (mStreamer.available() > 0) {
            logInfo.setLength(0);
            ChunkHeader header = ChunkHeader.parseFrom(mStreamer);

            BaseTypeChunk typeChunk = null;
            if (header.type == RES_TABLE_TYPE_SPEC_TYPE) {
                mStreamer.seek(mStreamer.getPosition() - ChunkHeader.LENGTH);
                typeChunk = ResTableTypeSpecChunk.parseFrom(mStreamer);
            } else if (header.type == RES_TABLE_TYPE_TYPE) {
                mStreamer.seek(mStreamer.getPosition() - ChunkHeader.LENGTH);
                typeChunk = ResTableTypeInfoChunk.parseFrom(mStreamer);
            }
            if (typeChunk != null) {
                logInfo.append(typeChunk.getChunkName()).append(" ")
                        .append(String.format("type=%s ", typeChunk.getType()))
                        .append(String.format("count=%s ", typeChunk.getEntryCount()));
            } else {
                logInfo.append("None TableTypeSpecType or TableTypeType!!");
            }

            if (typeChunk != null) {
                chunk.typeChunks.add(typeChunk);
            }
        }

        chunk.createResourceIndex();
        for (int i = 0; i < chunk.typeChunks.size(); ++i) {
            chunk.typeChunks.get(i).translateValues(stringChunk, chunk.typeStringPool, chunk.keyStringPool);
        }

        return chunk;
    }

    private void createResourceIndex() {
        typeInfoIndexer = new HashMap<>();
        for (BaseTypeChunk typeChunk : typeChunks) {
            List<BaseTypeChunk> typeList = typeInfoIndexer.get(typeChunk.getTypeId());
            if (typeList == null) {
                typeList = new ArrayList<>();
                typeInfoIndexer.put(typeChunk.getTypeId(), typeList);
                typeChunk.getTypeId();
            }
            typeList.add(typeChunk);
        }
    }

    public ResTableEntry getResource(int resId) {
        int typeId = (resId & 0x00ff0000) >> 16;
        List<BaseTypeChunk> typeList = typeInfoIndexer.get(typeId);
        for (int i = 1; i < Objects.requireNonNull(typeList).size(); ++i) {
            if (typeList.get(i) instanceof ResTableTypeInfoChunk) {
                ResTableEntry entry = ((ResTableTypeInfoChunk) typeList.get(i)).getResource(resId);
                if (entry != null) {
                    return entry;
                }
            }
        }
        return null;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public String buildEntry2String() {
        StringBuilder builder = new StringBuilder();
        builder.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>").append(System.lineSeparator());
        builder.append("<resources>").append(System.lineSeparator());

        for (int i = 0; i < typeChunks.size(); ++i) {
            if (typeChunks.get(i) instanceof ResTableTypeSpecChunk) {
                List<ResTableTypeInfoChunk> typeInfos = new ArrayList<>();
                for (int j = i + 1; j < typeChunks.size(); ++j) {
                    if (typeChunks.get(j) instanceof ResTableTypeInfoChunk) {
                        typeInfos.add((ResTableTypeInfoChunk) typeChunks.get(j));
                    } else {
                        break;
                    }
                }
                i += typeInfos.size();
                String entry = ResTableTypeInfoChunk.uniqueEntries2String((int) pkgId & 0xff, typeStringPool, typeInfos);
                builder.append("\t").append(entry).append(System.lineSeparator());
            }
        }

        builder.append("</resources>");
        return builder.toString();
    }
}