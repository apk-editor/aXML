package com.apk.axml.serializableItems;

import java.io.Serializable;
import java.util.List;

/*
 * Created by APK Explorer & Editor <apkeditor@protonmail.com> on Sept. 06, 2025
 */
public class XMLEntry implements Serializable {

    private final String mTag;
    private final String mMiddleTag, mEndTag;
    private String mValue;

    public XMLEntry(String tag, String middleTag, String value, String endTag) {
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

    private boolean isDecoded(String stringResource) {
        if (stringResource == null || stringResource.trim().isEmpty()) {
            return false;
        }
        return stringResource.startsWith("@") || stringResource.startsWith("?@") || stringResource.startsWith("res/");
    }

    public String getText() {
        return mTag + mMiddleTag + mValue + mEndTag;
    }

    public String getText(List<ResEntry> resourceMap) {
        return mTag + mMiddleTag + getAttrValue(resourceMap, mValue) + mEndTag;
    }

    public String getAttrValue(List<ResEntry> resourceMap, String stringResource) {
        if (!isDecoded(stringResource)) {
            return stringResource;
        }

        for (ResEntry entry : resourceMap) {
            if (stringResource.equals(entry.getName()) || stringResource.equals(entry.getValue())) {
                return entry.getResAttr();
            }
        }

        return stringResource;
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
