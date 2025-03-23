package com.apk.axml.ARSCUtils;


import java.io.IOException;

public class ResTableConfig {

    public long size;
    public int mcc, mnc;
    public long imsi;
    public int language, country;
    public long locale;
    public int orientation, touchScreen, density;
    public long screenType;
    public int keyboard, navigation, inputFlags, inputPad0;
    public long input;
    public int screenWidth, screenHeight;
    public long screenSize;
    public int sdkVersion, minorVersion;
    public long version;
    public int screenLayout, uiModeByte, smallestScreenWidthDp;
    public long screenConfig;
    public int screenWidthDp, screenHeightDp;
    public long screenSizeDp;
    public byte[] localeScript;
    public byte[] localeVariant;

    public static ResTableConfig parseFrom(PositionInputStream mStreamer) throws IOException {
        ResTableConfig config = new ResTableConfig();
        long cursor = mStreamer.getPosition();
        long start = cursor;

        config.size = Utils.readInt(mStreamer);
        cursor += 4;

        config.mcc = Utils.readShort(mStreamer);
        config.mnc = Utils.readShort(mStreamer);
        mStreamer.seek(cursor);
        config.imsi = Utils.readInt(mStreamer);
        cursor += 4;

        config.language = Utils.readShort(mStreamer);
        config.country = Utils.readShort(mStreamer);
        mStreamer.seek(cursor);
        config.locale = Utils.readInt(mStreamer);
        cursor += 4;

        config.orientation = Utils.readUInt8(mStreamer);
        config.touchScreen = Utils.readUInt8(mStreamer);
        config.density = Utils.readShort(mStreamer);
        mStreamer.seek(cursor);
        config.screenType = Utils.readInt(mStreamer);
        cursor += 4;

        config.keyboard = Utils.readUInt8(mStreamer);
        config.navigation = Utils.readUInt8(mStreamer);
        config.inputFlags = Utils.readUInt8(mStreamer);
        config.inputPad0 = Utils.readUInt8(mStreamer);
        mStreamer.seek(cursor);
        config.input = Utils.readInt(mStreamer);
        cursor += 4;

        config.screenWidth = Utils.readShort(mStreamer);
        config.screenHeight = Utils.readShort(mStreamer);
        mStreamer.seek(cursor);
        config.screenSize = Utils.readInt(mStreamer);
        cursor += 4;

        config.sdkVersion = Utils.readShort(mStreamer);
        config.minorVersion = Utils.readShort(mStreamer);
        mStreamer.seek(cursor);
        config.version = Utils.readInt(mStreamer);
        cursor += 4;

        config.screenLayout = Utils.readUInt8(mStreamer);
        config.uiModeByte = Utils.readUInt8(mStreamer);
        config.smallestScreenWidthDp = Utils.readShort(mStreamer);
        mStreamer.seek(cursor);
        config.screenConfig = Utils.readInt(mStreamer);
        cursor += 4;

        config.screenWidthDp = Utils.readShort(mStreamer);
        config.screenHeightDp = Utils.readShort(mStreamer);
        mStreamer.seek(cursor);
        config.screenSizeDp = Utils.readInt(mStreamer);
        {
            byte[] buf;
            buf = new byte[4];
            mStreamer.read(buf);
            config.localeScript = buf;
            buf = new byte[8];
            mStreamer.read(buf);
            config.localeVariant = buf;
        }

        mStreamer.seek(start + config.size);

        return config;
    }
}