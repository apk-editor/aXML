package com.apk.axml.aXMLUtils;

import com.apk.axml.serializableItems.XMLEntry;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/*
 * Created by APK Explorer & Editor <apkeditor@protonmail.com> on Sept. 06, 2025
 */
public class Utils {

    public static byte[] toByteArray(InputStream in) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192]; int n;
        while ((n = in.read(buf)) != -1) baos.write(buf,0,n);
        return baos.toByteArray();
    }

    public static String decodeAsString(List<XMLEntry> xmlEntries) {
        StringBuilder sb = new StringBuilder();
        for (XMLEntry xmlEntry : xmlEntries) {
            sb.append(xmlEntry.getText()).append("\n");
        }
        return sb.toString().trim();
    }

}