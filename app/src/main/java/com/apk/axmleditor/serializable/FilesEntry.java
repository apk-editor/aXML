package com.apk.axmleditor.serializable;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.Serializable;

/*
 * Created by APK Explorer & Editor <apkeditor@protonmail.com> on January 05, 2026
 */
public class FilesEntry implements Serializable {

    private final byte[] iconData;
    private final String name;

    public FilesEntry(byte[] iconData, String name) {
        this.iconData = iconData;
        this.name = name;
    }

    public Bitmap getIcon() {
        if (iconData == null) return null;
        return BitmapFactory.decodeByteArray(iconData, 0, iconData.length);
    }

    public String getName() {
        return name;
    }

}