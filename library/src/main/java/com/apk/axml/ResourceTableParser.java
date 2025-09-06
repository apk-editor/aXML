package com.apk.axml;

import android.annotation.TargetApi;
import android.os.Build;

import com.apk.axml.serializableItems.ResEntry;
import com.apk.axml.aXMLUtils.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/*
 * Created by APK Explorer & Editor <apkeditor@protonmail.com> on Sept. 06, 2025
 */
public class ResourceTableParser {

    public static final int RES_STRING_POOL_TYPE = 0x0001;
    public static final int RES_TABLE_TYPE = 0x0002;
    public static final int RES_TABLE_PACKAGE_TYPE = 0x0200;
    public static final int RES_TABLE_TYPE_TYPE = 0x0201;
    public static final int RES_TABLE_TYPE_SPEC_TYPE = 0x0202;
    public static final int UTF8_FLAG = 1 << 8;

    private final byte[] data;

    public ResourceTableParser(InputStream resStream) throws IOException {
        this.data = Utils.toByteArray(resStream);
    }

    public List<ResEntry> parse() {
        LE le = new LE(data);
        List<ResEntry> out = new ArrayList<>();

        // Read RES_TABLE header
        ChunkHeader tableHeader = ChunkHeader.read(le);
        if (tableHeader.type != RES_TABLE_TYPE) {
            throw new IllegalStateException("Not a RES_TABLE file");
        }

        le.u32(); // packageCount - we don't use it, but must consume it
        int tableChunkEnd = tableHeader.start + tableHeader.size;

        StringPool globalPool = null;

        while (le.pos < tableChunkEnd) {
            ChunkHeader hdr = ChunkHeader.peek(le);
            try {
                if (hdr.type == RES_STRING_POOL_TYPE) {
                    globalPool = StringPool.read(le);
                } else if (hdr.type == RES_TABLE_PACKAGE_TYPE) {
                    parsePackage(le, out, globalPool);
                } else {
                    ChunkHeader.skip(le);
                }
            } catch (IllegalStateException ex) {
                // Malformed chunk — skip it if possible, otherwise bail out
                int skipTo = hdr.start + hdr.size;
                if (skipTo > le.a.length) break;
                le.seek(skipTo);
            }
        }
        return out;
    }

    private void parsePackage(LE le, List<ResEntry> out, StringPool globalPool) {
        ChunkHeader pkgHdr = ChunkHeader.read(le);
        int pkgStart = pkgHdr.start, pkgEnd = pkgHdr.start + pkgHdr.size;

        int packageId = le.u32();
        String packageName = sanitizePackage(le.utf16FixedLengthString(256));
        int typeStringsOffset = le.u32();
        le.u32(); // lastPublicType
        int keyStringsOffset = le.u32();
        le.u32(); // lastPublicKey
        if (le.pos + 4 <= pkgEnd && le.hasRemaining(4)) le.u32(); // optional aapt2

        int save = le.pos;

        StringPool typePool = null, keyPool = null;
        if (typeStringsOffset != 0) {
            int p = pkgStart + typeStringsOffset;
            if (p >= pkgStart && p < pkgEnd) {
                le.seek(p);
                typePool = StringPool.read(le);
            }
        }
        if (keyStringsOffset != 0) {
            int p = pkgStart + keyStringsOffset;
            if (p >= pkgStart && p < pkgEnd) {
                le.seek(p);
                keyPool = StringPool.read(le);
            }
        }

        le.seek(save);

        while (le.pos < pkgEnd) {
            if (le.pos + 8 > pkgEnd) break;
            ChunkHeader hdr = ChunkHeader.peek(le);
            if (hdr.start < pkgStart || hdr.start + hdr.size > pkgEnd) break;

            if (hdr.type == RES_TABLE_TYPE_SPEC_TYPE) {
                ChunkHeader.skip(le);
            } else if (hdr.type == RES_TABLE_TYPE_TYPE) {
                parseTypeChunk(le, packageId, packageName, typePool, keyPool, globalPool, out);
            } else {
                ChunkHeader.skip(le);
            }
        }

        le.seek(pkgEnd);
    }

    private void parseTypeChunk(LE le, int packageId, @SuppressWarnings("unused") String _packageName,
                                StringPool typePool, StringPool keyPool,
                                StringPool globalPool,
                                List<ResEntry> out) {

        ChunkHeader typeHdr = ChunkHeader.read(le);
        if (typeHdr.type != RES_TABLE_TYPE_TYPE) {
            le.seek(typeHdr.start + typeHdr.size);
            return;
        }

        final int typeChunkStart = typeHdr.start;
        final int typeChunkEnd = typeHdr.start + typeHdr.size;

        // Basic header
        int typeId = le.u8(); le.skip(3);
        int entryCount = le.u32();
        int entriesStart = le.u32();

        // entriesStart must point inside chunk (at or after header)
        if (entriesStart < typeHdr.headerSize || typeChunkStart + entriesStart > typeChunkEnd) {
            le.seek(typeHdr.start + typeHdr.size);
            return;
        }

        int configSize = le.u32();
        if (configSize < 4 || !le.hasRemaining(configSize - 4)) {
            le.seek(typeHdr.start + typeHdr.size);
            return;
        }
        le.skip(configSize - 4);

        // Offsets array must fit; clamp entryCount if necessary
        int offsetsStartPos = le.pos;
        long maxOffsetsBytes = (long)typeChunkEnd - (long)offsetsStartPos;
        long neededOffsetsBytes = (long)entryCount * 4L;
        if (neededOffsetsBytes > maxOffsetsBytes) {
            entryCount = (int)Math.max(0, maxOffsetsBytes / 4L);
        }

        int[] entryOffsets = new int[entryCount];
        for (int i = 0; i < entryCount; i++) entryOffsets[i] = le.s32();

        String typeName = (typePool != null && (typeId - 1) >= 0 && (typeId - 1) < typePool.strings.size())
                ? typePool.strings.get(typeId - 1)
                : String.format(Locale.getDefault(), "type%d", typeId);

        final int MIN_ENTRY_HEADER = 8;

        for (int entryIndex = 0; entryIndex < entryCount; entryIndex++) {
            int rel = entryOffsets[entryIndex];
            if (rel == -1) continue;

            long entryPosL = (long) typeChunkStart + (long) entriesStart + (long) rel;
            if (entryPosL < typeChunkStart || entryPosL > Integer.MAX_VALUE) continue;
            int entryPos = (int) entryPosL;

            if (entryPos < typeChunkStart || entryPos + MIN_ENTRY_HEADER > typeChunkEnd) continue;

            int savePos = le.pos;
            le.seek(entryPos);

            // Validate we can read the header
            if (!le.hasRemaining(2 + 2 + 4)) { le.seek(savePos); continue; }

            int entrySize = le.u16();
            int flags = le.u16();
            int keyIndex = le.u32();

            if (entrySize < MIN_ENTRY_HEADER || entryPos + entrySize > typeChunkEnd) { le.seek(savePos); continue; }

            String name = (keyPool != null && keyIndex >= 0 && keyIndex < keyPool.strings.size())
                    ? keyPool.strings.get(keyIndex)
                    : "entry" + entryIndex;

            int resId = ((packageId & 0xFF) << 24) | ((typeId & 0xFF) << 16) | (entryIndex & 0xFFFF);

            String value = null;

            // Simple entry: Res_value immediately after header
            if ((flags & 0x0001) == 0) {
                // Res_value is: u16 size, u8 res0, u8 type, u32 data
                if (le.hasRemaining(2 + 1 + 1 + 4)) {
                    le.skip(2); // valueSize (we don't need it)
                    le.skip(1); // res0
                    int valueType = le.u8();
                    int valueData = le.u32();
                    if (valueType == 3 && globalPool != null && valueData >= 0 && valueData < globalPool.strings.size()) {
                        value = globalPool.strings.get(valueData);
                    }
                }
            } else {
                // Complex (map) entry. We don't fully parse maps here; skip them safely.
                // The entrySize includes header + map entries. We've already validated entryPos+entrySize <= chunk end.
                // So just leave value = null and continue.
            }

            out.add(new ResEntry(resId, "@" + typeName + "/" + name, value));

            le.seek(savePos);
        }

        le.seek(typeHdr.start + typeHdr.size);
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    private static String sanitizePackage(String pkg) {
        if (pkg == null) return "app";
        int nul = pkg.indexOf('\0');
        String s = (nul >= 0) ? pkg.substring(0, nul) : pkg;
        s = s.trim();
        return s.isEmpty() ? "app" : s;
    }

    private static final class ChunkHeader {
        final int type, headerSize, size, start;
        private ChunkHeader(int type,int headerSize,int size,int start) {
            this.type = type;
            this.headerSize = headerSize;
            this.size = size;
            this.start = start;
        }
        static ChunkHeader read(LE le){
            int start = le.pos;
            int type = le.u16();
            int headerSize = le.u16();
            int size = le.u32();
            // ensure header lies within buffer and declared size is sane
            if (size <= 0 || headerSize < 8) throw new IllegalStateException("Bad chunk header");
            le.requireAbsolute(start, size);
            return new ChunkHeader(type, headerSize, size, start);
        }
        static ChunkHeader peek(LE le) {
            int save=le.pos;
            ChunkHeader h=read(le);
            le.seek(save);
            return h;
        }
        static void skip(LE le) {
            ChunkHeader h=read(le);
            le.seek(h.start+h.size);
        }
    }

    private static final class StringPool {
        final List<String> strings;
        private StringPool(List<String> strings) {
            this.strings=strings;
        }
        static StringPool read(LE le) {
            ChunkHeader hdr = ChunkHeader.read(le);
            if (hdr.type != RES_STRING_POOL_TYPE) throw new IllegalStateException("Expected STRING_POOL");
            int stringCount = le.u32(), styleCount = le.u32(), flags = le.u32();
            int stringsStart = le.u32(); le.u32(); // stylesStart (unused) - consume
            boolean utf8 = (flags & UTF8_FLAG) != 0;

            int[] stringOffsets = new int[stringCount];
            for (int i = 0; i < stringCount; i++) stringOffsets[i] = le.u32();
            for (int i = 0; i < styleCount; i++) le.u32();

            int base = hdr.start + stringsStart;
            List<String> out = new ArrayList<>(stringCount);
            for (int i = 0; i < stringCount; i++) {
                int off = base + stringOffsets[i];
                if (off < hdr.start || off >= hdr.start + hdr.size) {
                    out.add("");
                    continue;
                }
                int save = le.pos;
                le.seek(off);
                String s = utf8 ? readUtf8(le) : readUtf16(le);
                out.add(s);
                le.seek(save);
            }
            le.seek(hdr.start + hdr.size);
            return new StringPool(out);
        }
        @TargetApi(Build.VERSION_CODES.KITKAT)
        private static String readUtf8(LE le) {
            int _utf16len = readLength8Safe(le); // length in utf16 chars (not always used)
            int utf8len = readLength8Safe(le);
            if (!le.hasRemaining(utf8len + 1)) return "";
            byte[] b = le.bytes(utf8len);
            le.u8(); // trailing 0
            return new String(b, StandardCharsets.UTF_8);
        }
        private static int readLength8Safe(LE le) {
            int a = le.u8();
            if ((a & 0x80) == 0) return a;
            return ((a & 0x7F) << 7) | (le.u8() & 0x7F);
        }
        @TargetApi(Build.VERSION_CODES.KITKAT)
        private static String readUtf16(LE le) {
            int u16len = readLength16Safe(le);
            if (!le.hasRemaining(u16len * 2 + 2)) return "";
            byte[] b = le.bytes(u16len * 2);
            le.u16(); // trailing 0
            return new String(b, StandardCharsets.UTF_16LE);
        }
        private static int readLength16Safe(LE le) {
            int a = le.u16();
            if ((a & 0x8000) == 0) return a;
            int b = le.u16();
            return ((a & 0x7FFF) << 16) | b;
        }
    }

    private static final class LE {
        final byte[] a; int pos;

        LE(byte[] a) {
            this.a = a; this.pos = 0;
        }

        private void require(int n) {
            if (n < 0 || pos + n > a.length) throw new IllegalStateException("Require failed: need=" + n + " pos=" + pos + " len=" + a.length);
        }

        private void requireAbsolute(int p,int n) {
            if (p < 0 || n < 0 || p + n > a.length) throw new IllegalStateException("RequireAbsolute failed: p=" + p + " need=" + n + " len=" + a.length);
        }

        boolean hasRemaining(int n) {
            return n >= 0 && pos + n <= a.length;
        }

        void seek(int p) {
            requireAbsolute(p,0); pos = p;
        }

        void skip(int n) {
            require(n); pos += n;
        }

        int u8() {
            require(1); return a[pos++] & 0xFF;
        }

        int u16() {
            require(2); int v = (a[pos] & 0xFF) | ((a[pos+1] & 0xFF) << 8); pos += 2; return v;
        }

        int s32() {
            return u32();
        }

        int u32() {
            require(4); long v = (a[pos] & 0xFFL) | ((a[pos+1] & 0xFFL) << 8) | ((a[pos+2] & 0xFFL) << 16) | ((a[pos+3] & 0xFFL) << 24); pos += 4; return (int) v;
        }

        @TargetApi(Build.VERSION_CODES.GINGERBREAD)
        byte[] bytes(int len) {
            require(len); byte[] b = Arrays.copyOfRange(a, pos, pos + len); pos += len; return b;
        }

        String utf16FixedLengthString(int byteLen) {
            // be forgiving if truncated
            if (byteLen < 0) byteLen = 0;
            if (!hasRemaining(byteLen)) byteLen = Math.max(0, a.length - pos);
            int end = pos + byteLen;
            StringBuilder sb = new StringBuilder();
            while (pos + 1 < end) {
                int ch = u16();
                if (ch == 0) break;
                sb.append((char) ch);
            }
            pos = end;
            return sb.toString();
        }
    }

}