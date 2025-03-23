package com.apk.axml;

import com.apk.axml.ARSCUtils.ARSCFile;
import com.apk.axml.ARSCUtils.ResTableEntry;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/*
 * Created by APK Explorer & Editor <apkeditor@protonmail.com> on January 22, 2023
 * Based on the original work of @ibilux (https://github.com/ibilux/ArscResourcesParser)
 */
public class ARSCDecoder {

    private final InputStream inputStream;

    public ARSCDecoder(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    private byte[] getBytes() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        int next = inputStream.read();
        while (next > -1) {
            bos.write(next);
            next = inputStream.read();
        }
        bos.flush();
        byte[] result = bos.toByteArray();
        bos.close();
        return result;
    }

    private ARSCFile arscFile() {
        try {
            ARSCFile arscFile = new ARSCFile();
            arscFile.parse(getBytes());
            return arscFile;
        } catch (IOException | OutOfMemoryError ignored) {
            return null;
        }
    }

    public String getPublicXML() {
        ARSCFile arscFile = arscFile();
        if (arscFile != null) {
            return Objects.requireNonNull(arscFile).buildPublicXml();
        } else {
            return null;
        }
    }

    public String getStringByResID(String resId) {
        ResTableEntry res = Objects.requireNonNull(arscFile()).getResource(Integer.decode(resId));
        if (res != null) {
            return res.toString();
        } else {
            return "Resource ID 0x" + String.format("%04x", Integer.decode(resId)) + " cannot be found.";
        }
    }

}