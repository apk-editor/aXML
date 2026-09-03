package com.apk.axml.aXMLUtils;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/*
 * Created by APK Explorer & Editor <apkeditor@protonmail.com> on September 03, 2026
 */
public final class AndroidAttrRegistry {

	private static final Map<String, Integer> ATTR_IDS = new HashMap<>();

	private AndroidAttrRegistry() {
	}

	static {
		try {
			Class<?> rAttr = Class.forName("android.R$attr");
			for (Field f : rAttr.getFields()) {
				if (f.getType() == int.class) {
					ATTR_IDS.put(f.getName(), f.getInt(null));
				}
			}
		} catch (Throwable ignored) {}

		putIfAbsent("theme", 0x01010000);
		putIfAbsent("label", 0x01010001);
		putIfAbsent("icon", 0x01010002);
		putIfAbsent("name", 0x01010003);
		putIfAbsent("permission", 0x01010006);
		putIfAbsent("protectionLevel", 0x01010009);
		putIfAbsent("enabled", 0x0101000e);
		putIfAbsent("debuggable", 0x0101000f);
		putIfAbsent("exported", 0x01010010);
		putIfAbsent("multiprocess", 0x01010013);
		putIfAbsent("excludeFromRecents", 0x01010017);
		putIfAbsent("authorities", 0x01010018);
		putIfAbsent("configChanges", 0x0101001f);
		putIfAbsent("value", 0x01010024);
		putIfAbsent("resource", 0x01010025);
		putIfAbsent("mimeType", 0x01010026);
		putIfAbsent("minSdkVersion", 0x0101020c);
		putIfAbsent("versionCode", 0x0101021b);
		putIfAbsent("versionName", 0x0101021c);
		putIfAbsent("targetSdkVersion", 0x01010270);
		putIfAbsent("testOnly", 0x01010272);
		putIfAbsent("allowBackup", 0x01010280);
		putIfAbsent("required", 0x0101028e);
		putIfAbsent("supportsRtl", 0x010103af);
		putIfAbsent("fullBackupContent", 0x0101048d);
		putIfAbsent("extractNativeLibs", 0x010104ea);
		putIfAbsent("directBootAware", 0x01010505);
		putIfAbsent("compileSdkVersion", 0x01010572);
		putIfAbsent("compileSdkVersionCodename", 0x01010573);
		putIfAbsent("appComponentFactory", 0x0101057a);
		putIfAbsent("dataExtractionRules", 0x0101063c);
	}

	private static void putIfAbsent(String key, int id) {
		if (!ATTR_IDS.containsKey(key)) {
			ATTR_IDS.put(key, id);
		}
	}

	public static int getAttrId(String name) {
		Integer id = ATTR_IDS.get(name);
		return id != null ? id : 0;
	}

}