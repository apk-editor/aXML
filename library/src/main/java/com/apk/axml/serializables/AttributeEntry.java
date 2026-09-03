package com.apk.axml.serializables;

import java.io.Serializable;

/*
 * Created by APK Explorer & Editor <apkeditor@protonmail.com> on September 03, 2026
 */
public class AttributeEntry implements Serializable {

    private final int resId, type, data;
    private final String ns, name, rawValue;

    public AttributeEntry(String ns, String name, String rawValue, int resId) {
        this.ns = ns;
        this.name = name;
        this.rawValue = rawValue;
        this.resId = resId;

        if (rawValue != null && (rawValue.startsWith("@") || rawValue.startsWith("?"))) {
            boolean isAttr = rawValue.startsWith("?");
            this.type = isAttr ? 0x02 : 0x01; // TYPE_ATTRIBUTE : TYPE_REFERENCE
            this.data = parseRefOrInt(rawValue.substring(1));
        } else if ("true".equalsIgnoreCase(rawValue) || "false".equalsIgnoreCase(rawValue)) {
            this.type = 0x12; // TYPE_INT_BOOLEAN
            this.data = "true".equalsIgnoreCase(rawValue) ? 1 : 0;
        } else if (rawValue != null && rawValue.startsWith("#")) {
            this.type = rawValue.length() <= 7 ? 0x1d : 0x1c; // TYPE_INT_COLOR_RGB8 : TYPE_INT_COLOR_ARGB8
            this.data = (int) Long.parseLong(rawValue.substring(1), 16);
        } else if (rawValue != null && (rawValue.startsWith("0x") || rawValue.startsWith("0X"))) {
            this.type = 0x11; // TYPE_INT_HEX
            this.data = (int) Long.parseLong(rawValue.substring(2), 16);
        } else if (rawValue != null && rawValue.matches("^-?\\d+$")) {
            this.type = 0x10; // TYPE_INT_DEC
            this.data = Integer.parseInt(rawValue);
        } else {
            this.type = 0x03; // TYPE_STRING
            this.data = 0; // Handled during element serialization via rawValue pool index
        }
    }

    public int getData() {
        return data;
    }

    public int getResId() {
        return resId;
    }

    public int getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getNs() {
        return ns;
    }

    public String getRawValue() {
        return rawValue;
    }

    private static int parseRefOrInt(String s) {
        try {
            if (s.startsWith("0x") || s.startsWith("0X")) {
                return (int) Long.parseLong(s.substring(2), 16);
            }
            if (s.length() == 8 && s.matches("^[0-9a-fA-F]+$")) {
                return (int) Long.parseLong(s, 16);
            }
            return (int) Long.parseLong(s, 10);
        } catch (Exception e) {
            return 0;
        }
    }

}