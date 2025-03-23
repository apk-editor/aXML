package com.apk.axml.ARSCUtils;

import android.annotation.TargetApi;
import android.os.Build;

import java.io.IOException;

import java.util.List;

public class ResTableTypeInfoChunk extends BaseTypeChunk {

    public static final long NO_ENTRY = 0xffffffffL;

    public ChunkHeader header;
    public int typeId;
    public int res0;
    public int res1;
    public long entryCount;
    public long entriesStart;
    public ResTableConfig resConfig;
    public long[] entryOffsets;
    public ResTableEntry[] tableEntries;

    public static ResTableTypeInfoChunk parseFrom(PositionInputStream mStreamer) throws IOException {
        ResTableTypeInfoChunk chunk = new ResTableTypeInfoChunk();
        long start = mStreamer.getPosition();
        chunk.header = ChunkHeader.parseFrom(mStreamer);
        chunk.typeId = Utils.readUInt8(mStreamer);
        chunk.res0 = Utils.readUInt8(mStreamer);
        chunk.res1 = Utils.readShort(mStreamer);
        chunk.entryCount = Utils.readInt(mStreamer);
        chunk.entriesStart = Utils.readInt(mStreamer);
        chunk.resConfig = ResTableConfig.parseFrom(mStreamer);

        chunk.entryOffsets = new long[(int) chunk.entryCount];
        for (int i = 0; i < chunk.entryCount; ++i) {
            chunk.entryOffsets[i] = Utils.readInt(mStreamer);
        }

        chunk.tableEntries = new ResTableEntry[(int) chunk.entryCount];
        mStreamer.seek(start + chunk.entriesStart);
        for (int i = 0; i < chunk.entryCount; ++i) {
            if (chunk.entryOffsets[i] == NO_ENTRY || chunk.entryOffsets[i] == -1) {
                continue;
            }

            long cursor = mStreamer.getPosition();
            ResTableEntry entry = ResTableEntry.parseFrom(mStreamer);

            mStreamer.seek(cursor);
            if (entry.flags == ResTableEntry.FLAG_COMPLEX) {
                entry = ResTableMapEntry.parseFrom(mStreamer);
            } else {
                entry = ResTableValueEntry.parseFrom(mStreamer);
            }
            entry.entryId = i;
            chunk.tableEntries[i] = entry;
        }

        return chunk;
    }

    @Override
    public String getChunkName() {
        return "ResTableTypeInfoChunk";
    }

    @Override
    public long getEntryCount() {
        return entryCount;
    }

    @Override
    public String getType() {
        return String.format("0x%s", (typeId));
    }

    public int getTypeId() {
        return typeId;
    }

    @Override
    public void translateValues(ResStringPoolChunk globalStringPool, ResStringPoolChunk typeStringPool, ResStringPoolChunk keyStringPool) {
        for (ResTableEntry entry : tableEntries) {
            if (entry != null) {
                entry.translateValues(globalStringPool, typeStringPool, keyStringPool);
            }
        }
    }

    public ResTableEntry getResource(int resId) {
        int entryId = resId & 0x0000ffff;
        for (ResTableEntry entry : tableEntries) {
            if (entry.entryId == entryId) {
                return entry;
            }
        }
        return null;
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    public static String uniqueEntries2String(int packageId,
                                              ResStringPoolChunk typeStringPool,
                                              List<ResTableTypeInfoChunk> typeInfos) {
        StringBuilder builder = new StringBuilder();

        int configCount = typeInfos.size();
        int entryCount;
        try {
            entryCount = (int) typeInfos.get(0).entryCount;
        } catch (Exception e) {
            entryCount = 0;
        }

        for (int i = 0; i < entryCount; ++i) {
            for (int j = 0; j < configCount; ++j) {
                String entryStr = typeInfos.get(j).buildEntry2String(i, packageId, typeStringPool);
                if (entryStr != null && !entryStr.isEmpty()) {
                    builder.append(entryStr);
                    break;
                }
            }
        }
        return builder.toString();
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public String buildEntry2String(int entryId, int packageId, ResStringPoolChunk typeStringPool) {
        for (ResTableEntry entry : tableEntries) {
            String typeStr = typeStringPool.getString(typeId - 1);
            if (entry != null) {
                if (entry.entryId == entryId) {
                    return ("<public id=\"0x" + String.format("%02x", packageId) + String.format("%02x", typeId) + String.format("%04x", entryId) + "\" type=\"" + typeStr + "\" " + entry + "/>\"" + System.lineSeparator());
                }
            }
        }
        return null;
    }
}