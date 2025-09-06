package com.apk.axml.serializableItems;

import java.io.Serializable;
import java.util.List;

/*
 * Created by APK Explorer & Editor <apkeditor@protonmail.com> on Sept. 06, 2025
 */
public class XMLItems implements Serializable {

    private final String mTag;
    private final String mMiddleTag, mEndTag;
    private String mValue;

    public XMLItems(String tag, String middleTag, String value, String endTag) {
        this.mTag = tag;
        this.mMiddleTag = middleTag;
        this.mValue = value;
        this.mEndTag = endTag;
    }

    public boolean isBoolean() {
        return mValue.equalsIgnoreCase("true") || mValue.equalsIgnoreCase("false");
    }

    public boolean isChecked() {
        return isBoolean() && mValue.equalsIgnoreCase("true");
    }

    public String getText() {
        return mTag + mMiddleTag + mValue + mEndTag;
    }

    public String getText(List<ResEntry> resourceMap) {
        return mTag + mMiddleTag + getAttrValue(resourceMap, mValue) + mEndTag;
    }

    public String getAttrValue(List<ResEntry> resourceMap, String stringResource) {
        if (stringResource == null || !stringResource.startsWith("@")) {
            return stringResource;
        }

        for (ResEntry entry : resourceMap) {
            if (stringResource.equals(entry.getName())) {
                return entry.getResAttr();
            }
        }

        return stringResource;
    }

    public String getValue(List<ResEntry> resourceMap) {
        if (mValue == null || !mValue.startsWith("@")) {
            return mValue;
        }

        for (ResEntry entry : resourceMap) {
            if (mValue.equals(entry.getName())) {
                return entry.getValue();
            }
        }

        return mValue;
    }

    public String getTag() {
        return mTag;
    }

    public String getMiddleTag() {
        return mMiddleTag;
    }

    public String getValue() {
        return mValue;
    }

    public String getEndTag() {
        return mEndTag;
    }

    public void setValue(String value) {
        mValue = value;
    }

}