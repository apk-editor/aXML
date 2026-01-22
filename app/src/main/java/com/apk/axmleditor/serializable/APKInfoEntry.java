package com.apk.axmleditor.serializable;

import android.graphics.drawable.Drawable;

import java.io.Serializable;

/*
 * Created by APK Explorer & Editor <apkeditor@protonmail.com> on January 05, 2026
 */
public class APKInfoEntry implements Serializable {

    private final boolean clickable;
    private final Drawable icon;
    private final String title, description;

    public APKInfoEntry(Drawable icon, String title, String description, boolean clickable) {
        this.icon = icon;
        this.title = title;
        this.description = description;
        this.clickable = clickable;
    }

    public boolean isClickable() {
        return clickable;
    }

    public Drawable getIcon() {
        return icon;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

}