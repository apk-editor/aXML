package com.apk.axml;

import android.os.Build;

import androidx.annotation.RequiresApi;

import com.apk.axml.aXMLUtils.AndroidAttrRegistry;
import com.apk.axml.aXMLUtils.ManifestNode;
import com.apk.axml.aXMLUtils.Utils;
import com.apk.axml.serializables.AttributeEntry;
import com.apk.axml.serializables.XMLEntry;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/*
 * Created by APK Explorer & Editor <apkeditor@protonmail.com> on September 03, 2026
 */
public class aXMLEncoder {

    private static final String ANDROID_NS = "http://schemas.android.com/apk/res/android";

    public aXMLEncoder() {
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public byte[] encode(List<XMLEntry> xmlEntries) throws Exception {
        return encodeString(Utils.decodeAsString(xmlEntries));
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public byte[] encode(String xmlContent) throws Exception {
        return encodeString(xmlContent);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private byte[] encodeString(String xmlContent) throws Exception {
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);
        XmlPullParser parser = factory.newPullParser();
        parser.setInput(new StringReader(xmlContent));

        ManifestNode root = null;
        Deque<ManifestNode> stack = new ArrayDeque<>();
        Map<String, String> namespaces = new LinkedHashMap<>();

        int eventType = parser.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                int count = parser.getNamespaceCount(parser.getDepth());
                int prev = parser.getNamespaceCount(parser.getDepth() - 1);
                for (int i = prev; i < count; i++) {
                    String prefix = parser.getNamespacePrefix(i);
                    String uri = parser.getNamespaceUri(i);
                    if (prefix != null && uri != null) {
                        namespaces.put(prefix, uri);
                    }
                }

                String tagNs = parser.getNamespace();
                if (tagNs != null && tagNs.isEmpty()) tagNs = null;
                ManifestNode node = new ManifestNode(tagNs, parser.getName(), parser.getLineNumber());

                for (int i = 0; i < parser.getAttributeCount(); i++) {
                    String ns = parser.getAttributeNamespace(i);
                    if (ns != null && ns.isEmpty()) ns = null;
                    String name = parser.getAttributeName(i);
                    String val = parser.getAttributeValue(i);

                    if ("xmlns".equals(name) || (ns != null && ns.startsWith("http://www.w3.org/2000/xmlns/"))) {
                        continue;
                    }

                    int resId = 0;
                    if (ANDROID_NS.equals(ns)) {
                        resId = AndroidAttrRegistry.getAttrId(name);
                    }

                    node.getAttributes().add(new AttributeEntry(ns, name, val, resId));
                }

                if (root == null) {
                    root = node;
                } else {
                    Objects.requireNonNull(stack.peek()).getChildren().add(node);
                }
                stack.push(node);

            } else if (eventType == XmlPullParser.END_TAG) {
                stack.pop();
            }
            eventType = parser.next();
        }

        if (root == null) throw new IllegalArgumentException("No root XML element found");

        return compileBinary(root, namespaces);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private static byte[] compileBinary(ManifestNode root, Map<String, String> namespaces) throws Exception {
        List<String> pool = new ArrayList<>();
        Map<String, Integer> poolMap = new HashMap<>();
        List<Integer> resMap = new ArrayList<>();

        // 1. Collect all attributes that have resource IDs
        Map<String, Integer> attrIdMap = new HashMap<>();
        for (AttributeEntry a : collectAttributes(root)) {
            if (a.getResId() != 0) {
                attrIdMap.put(a.getName(), a.getResId());
            }
        }

        // 2. Sort attributes strictly by resource ID ascending
        List<Map.Entry<String, Integer>> sortedAttrs = new ArrayList<>(attrIdMap.entrySet());
        Collections.sort(sortedAttrs, (e1, e2) -> {
            int v1 = e1.getValue() != null ? e1.getValue() : 0;
            int v2 = e2.getValue() != null ? e2.getValue() : 0;
            return Integer.compare(v1, v2);
        });

        for (Map.Entry<String, Integer> entry : sortedAttrs) {
            int idx = pool.size();
            pool.add(entry.getKey());
            poolMap.put(entry.getKey(), idx);
            resMap.add(entry.getValue() != null ? entry.getValue() : 0);
        }

        // 3. Add default empty string and namespace strings
        addPoolString(pool, poolMap, "");
        for (Map.Entry<String, String> e : namespaces.entrySet()) {
            addPoolString(pool, poolMap, e.getKey());
            addPoolString(pool, poolMap, e.getValue());
        }

        // 4. Collect non-resource strings
        collectOtherStrings(root, pool, poolMap);

        // 5. Build binary chunks
        byte[] poolChunk = buildPool(pool);
        byte[] resMapChunk = buildResMap(resMap);

        ByteArrayOutputStream body = new ByteArrayOutputStream();

        // START_NAMESPACE
        for (Map.Entry<String, String> e : namespaces.entrySet()) {
            int prefixIdx = getRequiredInt(poolMap, e.getKey());
            int uriIdx = getRequiredInt(poolMap, e.getValue());
            writeNs(body, 0x00100100, prefixIdx, uriIdx, 1);
        }

        // Element nodes
        writeElement(body, root, poolMap);

        // END_NAMESPACE
        for (Map.Entry<String, String> e : namespaces.entrySet()) {
            int prefixIdx = getRequiredInt(poolMap, e.getKey());
            int uriIdx = getRequiredInt(poolMap, e.getValue());
            writeNs(body, 0x00100101, prefixIdx, uriIdx, root.getLine());
        }

        byte[] bodyBytes = body.toByteArray();

        ByteBuffer doc = ByteBuffer.allocate(8 + poolChunk.length + resMapChunk.length + bodyBytes.length);
        doc.order(ByteOrder.LITTLE_ENDIAN);
        doc.putInt(0x00080003); // RES_XML_TYPE
        doc.putInt(doc.capacity());
        doc.put(poolChunk);
        doc.put(resMapChunk);
        doc.put(bodyBytes);

        return doc.array();
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private static byte[] buildPool(List<String> strings) throws Exception {
        ByteArrayOutputStream stringData = new ByteArrayOutputStream();
        int[] offsets = new int[strings.size()];
        int offset = 0;

        for (int i = 0; i < strings.size(); i++) {
            offsets[i] = offset;
            String s = strings.get(i);
            byte[] utf16 = s.getBytes(StandardCharsets.UTF_16LE);
            int charLen = s.length();

            if ((charLen & 0xFFFF8000) != 0) {
                int high = ((charLen >> 16) & 0x7FFF) | 0x8000;
                stringData.write(high & 0xFF);
                stringData.write((high >> 8) & 0xFF);
                int low = charLen & 0xFFFF;
                stringData.write(low & 0xFF);
                stringData.write((low >> 8) & 0xFF);
                offset += 4;
            } else {
                stringData.write(charLen & 0xFF);
                stringData.write((charLen >> 8) & 0xFF);
                offset += 2;
            }

            stringData.write(utf16);
            stringData.write(0);
            stringData.write(0);

            offset += utf16.length + 2;
        }

        int rawLen = stringData.size();
        int pad = (4 - (rawLen % 4)) % 4;
        for (int i = 0; i < pad; i++) stringData.write(0);

        int headerSize = 28;
        int chunkSize = headerSize + (offsets.length * 4) + stringData.size();

        ByteBuffer bb = ByteBuffer.allocate(chunkSize);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.putInt(0x001C0001); // RES_STRING_POOL_TYPE
        bb.putInt(chunkSize);
        bb.putInt(strings.size());
        bb.putInt(0);
        bb.putInt(0);
        bb.putInt(headerSize + (offsets.length * 4));
        bb.putInt(0);

        for (int off : offsets) bb.putInt(off);
        bb.put(stringData.toByteArray());

        return bb.array();
    }

    private static byte[] buildResMap(List<Integer> resIds) {
        if (resIds.isEmpty()) return new byte[0];
        ByteBuffer bb = ByteBuffer.allocate(8 + (resIds.size() * 4));
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.putInt(0x00080180); // RES_XML_RESOURCE_MAP_TYPE
        bb.putInt(bb.capacity());
        for (int id : resIds) bb.putInt(id);
        return bb.array();
    }

    private static int getInt(Map<String, Integer> map, String key) {
        if (key == null) return -1;
        Integer val = map.get(key);
        return val != null ? val : -1;
    }

    private static int getRequiredInt(Map<String, Integer> map, String key) {
        Integer val = map.get(key);
        if (val == null) {
            throw new IllegalStateException("Missing expected key in string pool: " + key);
        }
        return val;
    }

    private static List<AttributeEntry> collectAttributes(ManifestNode node) {
        List<AttributeEntry> list = new ArrayList<>(node.getAttributes());
        for (ManifestNode c : node.getChildren()) {
            list.addAll(collectAttributes(c));
        }
        return list;
    }

    private static void addPoolString(List<String> pool, Map<String, Integer> map, String s) {
        if (s == null) return;
        if (!map.containsKey(s)) {
            map.put(s, pool.size());
            pool.add(s);
        }
    }

    private static void collectOtherStrings(ManifestNode node, List<String> pool, Map<String, Integer> map) {
        addPoolString(pool, map, node.getNs());
        addPoolString(pool, map, node.getName());
        for (AttributeEntry a : node.getAttributes()) {
            addPoolString(pool, map, a.getNs());
            addPoolString(pool, map, a.getName());
            addPoolString(pool, map, a.getRawValue());
        }
        for (ManifestNode child : node.getChildren()) {
            collectOtherStrings(child, pool, map);
        }
    }

    private static void writeNs(ByteArrayOutputStream out, int type, int prefixIdx, int uriIdx, int line) throws Exception {
        ByteBuffer bb = ByteBuffer.allocate(24);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.putInt(type);
        bb.putInt(24);
        bb.putInt(line);
        bb.putInt(-1);
        bb.putInt(prefixIdx);
        bb.putInt(uriIdx);
        out.write(bb.array());
    }

    private static void writeElement(ByteArrayOutputStream out, ManifestNode node, Map<String, Integer> map) throws Exception {
        Collections.sort(node.getAttributes(), (a1, a2) -> {
            if (a1.getResId() != 0 && a2.getResId() != 0) {
                return Integer.compare(a1.getResId(), a2.getResId());
            }
            if (a1.getResId() != 0) return -1;
            if (a2.getResId() != 0) return 1;

            int id1 = getInt(map, a1.getName());
            int id2 = getInt(map, a2.getName());
            return Integer.compare(id1, id2);
        });

        int totalSize = 36 + (node.getAttributes().size() * 20);

        ByteBuffer start = ByteBuffer.allocate(totalSize);
        start.order(ByteOrder.LITTLE_ENDIAN);
        start.putInt(0x00100102); // RES_XML_START_ELEMENT_TYPE
        start.putInt(totalSize);
        start.putInt(node.getLine());
        start.putInt(-1);
        start.putInt(getInt(map, node.getNs()));
        start.putInt(getRequiredInt(map, node.getName()));
        start.putInt(0x00140014);
        start.putShort((short) node.getAttributes().size());
        start.putShort((short) 0);
        start.putShort((short) 0);
        start.putShort((short) 0);

        for (AttributeEntry a : node.getAttributes()) {
            int rawIdx = getInt(map, a.getRawValue());
            int typedData = a.getData();

            if (a.getType() == 0x03) {
                typedData = rawIdx;
            }

            start.putInt(getInt(map, a.getNs()));
            start.putInt(getRequiredInt(map, a.getName()));
            start.putInt(rawIdx);
            start.putInt((a.getType() << 24) | 0x08);
            start.putInt(typedData);
        }
        out.write(start.array());

        for (ManifestNode child : node.getChildren()) {
            writeElement(out, child, map);
        }

        ByteBuffer end = ByteBuffer.allocate(24);
        end.order(ByteOrder.LITTLE_ENDIAN);
        end.putInt(0x00100103); // RES_XML_END_ELEMENT_TYPE
        end.putInt(24);
        end.putInt(node.getLine());
        end.putInt(-1);
        end.putInt(getInt(map, node.getNs()));
        end.putInt(getRequiredInt(map, node.getName()));
        out.write(end.array());
    }

}