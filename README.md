# 🧩 aXML

![](https://img.shields.io/github/languages/top/apk-editor/aXML)
![](https://img.shields.io/github/contributors/apk-editor/aXML)
![](https://img.shields.io/github/license/apk-editor/aXML)

**aXML** is a pure **Java** library for decoding and encoding Android binary XML (**aXML**) files.  
Originally developed for [APK Explorer & Editor](https://github.com/apk-editor/APK-Explorer-Editor), **aXML** is now a standalone, open-source project — available through [JitPack](https://jitpack.io/#apk-editor/aXML) for developers who want to work directly with Android binary XML formats.

---

## 📦 Installation

### 🪜 Step 1: Add the JitPack repository to your root-level `build.gradle`

```gradle
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```

### 🪜 Step 2: Add the dependency to your app-level `build.gradle`

```gradle
dependencies {
    implementation 'com.github.apk-editor:aXML:Tag'
}
```

> 🔖 **Note:** Replace **`Tag`** with the latest **[commit ID](https://github.com/apk-editor/aXML/commits/master "View latest commits")**.

---

## ⚙️ Components & Usage

### 🧠 aXMLDecoder

The `aXMLDecoder` provides functionality to **decode Android binary XML (aXML)** into:

- A list of serializable [`XMLEntry`](library/src/main/java/com/apk/axml/serializableItems/XMLEntry.java) objects, or
- A **human-readable XML string**

#### Decode into serializable entries
```java
List<XMLEntry> manifestEntries;
try {
    manifestEntries = new aXMLDecoder(xmlStream, resourceEntries).decode();
} catch (XmlPullParserException e) {
    // Handle exception
}
```
---

**Parameters:**
- `xmlStream` — InputStream of the binary aXML file
- `resourceEntries` — List of [`ResEntry`](library/src/main/java/com/apk/axml/serializableItems/ResEntry.java) parsed from `resources.arsc`

#### Decode into readable string
```java
String decodedString;
try {
    decodedString = new aXMLDecoder(xmlStream, resourceEntries).decodeAsString();
} catch (XmlPullParserException e) {
    // Handle exception
}
```
---

#### Decode without resource table
```java
List<XMLEntry> manifestEntries = new aXMLDecoder(xmlStream).decode();
String decodedString = new aXMLDecoder(xmlStream).decodeAsString();
```
---

### 🔐 aXMLEncoder

The `aXMLEncoder` provides functionality to **encode readable XML** into Android’s binary aXML format.

#### Validate XML before encoding
```java
public static boolean isXMLValid(String xmlString) {
    try {
        SAXParserFactory.newInstance()
            .newSAXParser()
            .getXMLReader()
            .parse(new InputSource(new StringReader(xmlString)));
        return true;
    } catch (ParserConfigurationException | SAXException | IOException e) {
        return false;
    }
}
```

#### Encode XML into binary aXML
```java
try (FileOutputStream fos = new FileOutputStream(pathToaXMLFile)) {
    aXMLEncoder encoder = new aXMLEncoder();
    byte[] data = encoder.encodeString(xmlString, context);
    fos.write(data);
} catch (IOException | XmlPullParserException e) {
    // Handle exception
}
```
---

### 📖 ResourceTableParser

The `ResourceTableParser` parses Android resource tables (`resources.arsc`) into serializable [`ResEntry`](library/src/main/java/com/apk/axml/serializableItems/ResEntry.java) objects.

```java
List<ResEntry> resourceEntries;
try {
    resourceEntries = new ResourceTableParser(resStream).parse();
} catch (IOException e) {
    // Handle exception
}
```
---

### 📱 APKParser

The `APKParser` provides a high-level interface to **analyze APK files**, extract metadata, and decode manifests/resources.

```java
APKParser apkParser = new APKParser();
apkParser.parse(pathToAPK, context);

if (apkParser.isParsed()) {
    Drawable appIcon = apkParser.getAppIcon();
    List<ResEntry> resourceEntries = apkParser.getDecodedResources();
    List<XMLEntry> manifestEntries = apkParser.getManifest();
    List<String> permissions = apkParser.getPermissions();
    long apkSize = apkParser.getAPKSize();
    String apkCertificate = apkParser.getCertificate();
    String appName = apkParser.getAppName();
    String versionName = apkParser.getVersionName();
    String compiledSDKVersion = apkParser.getCompiledSDKVersion();
    String decodedManifest = apkParser.getManifestAsString();
    String packageName = apkParser.getPackageName();
    String targetSDKVersion = apkParser.getTargetSDKVersion();
    String versionCode = apkParser.getVersionCode();
}
```
---

## 📜 License

```
Copyright (C) 2023-2025 APK Explorer & Editor <apkeditor@protonmail.com>

aXML is free software: you can redistribute it and/or modify it
under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License,
or (at your option) any later version.

aXML is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <https://www.gnu.org/licenses/>.
```

[![GNU GPLv3](https://www.gnu.org/graphics/gplv3-127x51.png)](https://www.gnu.org/licenses/gpl-3.0.en.html)

---

## 👥 Contributions

We welcome community contributions!  
To contribute:
1. Fork this repository
2. Create a feature branch (`feature/your-feature`)
3. Commit and push your changes
4. Open a Pull Request 🎉

---

💡 *Thank you for supporting open-source Android development with [aXML](https://github.com/apk-editor/aXML)!*