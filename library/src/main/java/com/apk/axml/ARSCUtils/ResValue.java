package com.apk.axml.ARSCUtils;

import java.io.IOException;
import java.util.Locale;

public class ResValue {

    public final static int TYPE_REFERENCE = 0x01;
    public final static int TYPE_ATTRIBUTE = 0x02;
    public final static int TYPE_STRING = 0x03;
    public final static int TYPE_FLOAT = 0x04;
    public final static int TYPE_DIMENSION = 0x05;
    public final static int TYPE_FRACTION = 0x06;
    public static final int TYPE_DYNAMIC_REFERENCE = 0x07;
    public final static int TYPE_INT_DEC = 0x10;
    public final static int TYPE_INT_HEX = 0x11;
    public final static int TYPE_INT_BOOLEAN = 0x12;
    public final static int TYPE_INT_COLOR_ARGB8 = 0x1c;
    public final static int TYPE_INT_COLOR_RGB8 = 0x1d;
    public final static int TYPE_INT_COLOR_ARGB4 = 0x1e;
    public final static int TYPE_INT_COLOR_RGB4 = 0x1f;
    public static final int COMPLEX_UNIT_PX = 0;
    public static final int COMPLEX_UNIT_DIP = 1;
    public static final int COMPLEX_UNIT_SP = 2;
    public static final int COMPLEX_UNIT_PT = 3;
    public static final int COMPLEX_UNIT_IN = 4;
    public static final int COMPLEX_UNIT_MM = 5;
    public static final int COMPLEX_UNIT_SHIFT = 0;
    public static final int COMPLEX_UNIT_MASK = 15;
    public static final int COMPLEX_UNIT_FRACTION = 0;
    public static final int COMPLEX_UNIT_FRACTION_PARENT = 1;
    public int size;
    public int res0;
    public int dataType;
    public long data;
    public String dataStr;

    public static ResValue parseFrom(PositionInputStream mStreamer) throws IOException {
        ResValue value = new ResValue();
        value.size = Utils.readShort(mStreamer);
        value.res0 = Utils.readUInt8(mStreamer);
        value.dataType = Utils.readUInt8(mStreamer);
        value.data = Utils.readInt(mStreamer);
        return value;
    }

    public void translateValues(ResStringPoolChunk globalStringPool) {
        dataStr = getDataStr(globalStringPool);
    }

    public String getDataStr(ResStringPoolChunk stringPool) {
        String resStr;
        switch (dataType) {
            case TYPE_REFERENCE:
                resStr = String.format("@%s/0x%08x", getPackage(data), data);
                break;
            case TYPE_ATTRIBUTE:
                resStr = String.format("?%s/0x%08x", getPackage(data), data);
                break;
            case TYPE_STRING:
                resStr = stringPool.getString((int) data);
                break;
            case TYPE_FLOAT:
                resStr = String.valueOf(Float.intBitsToFloat((int) data));
                break;
            case TYPE_DIMENSION:
                resStr = complexToFloat((int) data) + getDimenUnit(data);
                break;
            case TYPE_FRACTION:
                resStr = complexToFloat((int) data) + getFractionUnit(data);
                break;
            case TYPE_DYNAMIC_REFERENCE:
                resStr = "TYPE_DYNAMIC_REFERENCE";
                break;
            case TYPE_INT_DEC:
                resStr = String.format(Locale.getDefault(), "%d", data);
                break;
            case TYPE_INT_HEX:
                resStr = String.format("0x%08x", data);
                break;
            case TYPE_INT_BOOLEAN:
                resStr = data == 0 ? "false" : "true";
                break;
            case TYPE_INT_COLOR_ARGB8:
                resStr = String.format("#%08x", data);
                break;
            case TYPE_INT_COLOR_RGB8:
                resStr = String.format("#ff%06x", 0xffffff & data);
                break;
            case TYPE_INT_COLOR_ARGB4:
                resStr = String.format("#%04x", 0xffff & data);
                break;
            case TYPE_INT_COLOR_RGB4:
                resStr = String.format("#f%03x", 0x0fff & data);
                break;
            default:
                resStr = String.format("<0x%08x, type 0x%08x>", data, dataType);
                break;
        }
        return resStr;
    }

    private static String getPackage(long id) {
        if (id >>> 24 == 1) {
            return "android:";
        }
        return "";
    }

    public static float complexToFloat(int complex) {
        return (float) (complex & 0xFFFFFF00) * RADIX_MULTS[(complex>>4) & 3];
    }

    private static final float[] RADIX_MULTS ={
            0.00390625F,3.051758E-005F,1.192093E-007F,4.656613E-010F
    };

    private static String getDimenUnit(long data) {
        switch ((int) (data >> COMPLEX_UNIT_SHIFT & COMPLEX_UNIT_MASK)) {
            case COMPLEX_UNIT_PX: return "px";
            case COMPLEX_UNIT_DIP: return "dp";
            case COMPLEX_UNIT_SP: return "sp";
            case COMPLEX_UNIT_PT: return "pt";
            case COMPLEX_UNIT_IN: return "in";
            case COMPLEX_UNIT_MM: return "mm";
            default: return " (unknown unit)";
        }
    }

    private static String getFractionUnit(long data) {
        switch ((int) (data >> COMPLEX_UNIT_SHIFT & COMPLEX_UNIT_MASK)) {
            case COMPLEX_UNIT_FRACTION: return "%";
            case COMPLEX_UNIT_FRACTION_PARENT: return "%p";
            default: return " (unknown unit)";
        }
    }
    
    @Override
    public String toString() {
        return dataStr;
    }
}