package com.apk.axml.ARSCUtils;

import java.io.ByteArrayInputStream;

import java.io.IOException;

public class ArscFile {

    private static final int RES_TABLE_TYPE = 0x0002;
    private static final int RES_STRING_POOL_TYPE = 0x0001;
    private static final int RES_TABLE_PACKAGE_TYPE = 0x0200;

    public ResFileHeaderChunk arscHeader;
    public ResStringPoolChunk resStringPoolChunk;
    public ResTablePackageChunk resTablePackageChunk;

    public ArscFile() {
    }

    public void parse(byte[] sBuf) throws IOException {
        ByteArrayInputStream mStreamer = new ByteArrayInputStream(sBuf);

        byte[] headBytes;
        byte[] chunkBytes;
        long cursor = 0;
        ChunkHeader header;

        chunkBytes = new byte[ResFileHeaderChunk.LENGTH];
        mStreamer.read(chunkBytes, 0, chunkBytes.length);
        header = ChunkHeader.parseFrom(new PositionInputStream(new ByteArrayInputStream(chunkBytes)));
        if (header.type != RES_TABLE_TYPE) {
            return;
        }

        mStreamer.reset();
        chunkBytes = new byte[header.headerSize];
        cursor += mStreamer.read(chunkBytes, 0, chunkBytes.length);
        arscHeader = ResFileHeaderChunk.parseFrom(new PositionInputStream(new ByteArrayInputStream(chunkBytes)));

        do {
            headBytes = new byte[ChunkHeader.LENGTH];
            cursor += mStreamer.read(headBytes, 0, headBytes.length);
            header = ChunkHeader.parseFrom(new PositionInputStream(new ByteArrayInputStream(headBytes)));

            chunkBytes = new byte[(int) header.chunkSize];
            System.arraycopy(headBytes, 0, chunkBytes, 0, ChunkHeader.LENGTH);
            cursor += mStreamer.read(chunkBytes, ChunkHeader.LENGTH, (int) header.chunkSize - ChunkHeader.LENGTH);

            switch (header.type) {
                case RES_STRING_POOL_TYPE:
                    resStringPoolChunk = ResStringPoolChunk.parseFrom(new PositionInputStream(new ByteArrayInputStream(chunkBytes)));
                    break;
                case RES_TABLE_PACKAGE_TYPE:
                    resTablePackageChunk = ResTablePackageChunk.parseFrom(new PositionInputStream(new ByteArrayInputStream(chunkBytes)), resStringPoolChunk);
                    break;
                default:
                    break;
            }

        } while (cursor < sBuf.length);
    }

    @Override
    public String toString() {
        return String.valueOf(arscHeader) + '\n' +
                resStringPoolChunk + '\n' +
                resTablePackageChunk + '\n';
    }

    public String buildPublicXml() {
        return resTablePackageChunk.buildEntry2String();
    }

    public ResTableEntry getResource(int resId) {
        long pkgId = (resId & 0xff000000L) >> 24;
        if (resTablePackageChunk.pkgId == pkgId) {
            return resTablePackageChunk.getResource(resId);
        } else {
            return null;
        }
    }
}