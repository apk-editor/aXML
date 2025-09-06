package com.apk.axml.serializableItems;

import java.io.Serializable;

/*
 * Created by APK Explorer & Editor <apkeditor@protonmail.com> on Sept. 06, 2025
 */
public class ResEntry implements Serializable {

    private final int resourceId;
    private final String name;
    private final String value;

    public ResEntry(int resourceId, String name, String value) {
        this.resourceId = resourceId;
        this.name = name;
        this.value = value;
    }

    public int getResourceId() { return resourceId; }

    public String getName() { return name; }

    public String getResAttr() {
        return "@" + String.format("%08X", resourceId);
    }

    public String getValue() { return value; }

}