package com.apk.axmleditor.Utils;

import android.app.Activity;

import com.apk.axml.aXMLEncoder;
import com.apk.axml.serializableItems.ResEntry;
import com.apk.axml.serializableItems.XMLEntry;
import com.apk.axmleditor.R;
import com.apk.axmleditor.dialogs.ProgressDialog;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Objects;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

/*
 * Created by APK Explorer & Editor <apkeditor@protonmail.com> on January 05, 2025
 */
public class XMLEditor {

    public static boolean isXMLValid(String xmlString) {
        try {
            SAXParserFactory.newInstance().newSAXParser().getXMLReader().parse(new InputSource(new StringReader(xmlString)));
            return true;
        } catch (ParserConfigurationException | SAXException | IOException e) {
            return false;
        }
    }

    public static String getExt(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return null;
        }
        String normalized = filePath.replace("\\", "/");

        int lastSlash = normalized.lastIndexOf("/");
        String fileName = (lastSlash == -1) ? normalized : normalized.substring(lastSlash + 1);

        int lastDot = fileName.lastIndexOf(".");
        if (lastDot == -1 || lastDot == fileName.length() - 1) {
            return null;
        }
        return fileName.substring(lastDot + 1);
    }

    public static String xmlEntriesToXML(List<XMLEntry> xmlEntries, List<ResEntry> resEntries) {
        StringBuilder sb = new StringBuilder();

        for (XMLEntry xmlEntry : xmlEntries) {
            if (!xmlEntry.isEmpty() && !xmlEntry.getTag().trim().equals("android:debuggable") && !xmlEntry.getTag().trim().equals("android:testOnly")) {
                if (resEntries != null && !resEntries.isEmpty()) {
                    sb.append(xmlEntry.getText(resEntries)).append("\n");
                } else {
                    sb.append(xmlEntry.getText()).append("\n");
                }
            }
        }

        return sb.toString().trim();
    }

    public static Async encodeToBinaryXML(String xmlString, String packageName, String fileName, Activity activity) {
        return new Async() {
            boolean invalid = false;
            private ProgressDialog progressDialog;
            @Override
            public void onPreExecute() {
                progressDialog = new ProgressDialog(activity);
                progressDialog.setTitle(activity.getString(R.string.saving));
                progressDialog.setIcon(R.mipmap.ic_launcher);
                progressDialog.startDialog();
            }

            @Override
            public void doInBackground() {
                if (XMLEditor.isXMLValid(xmlString)) {
                    invalid = false;
                    File outputXML;
                    if (packageName != null) {
                        outputXML = new File(Utils.getExportPath(activity), packageName + "/" + fileName);
                        Objects.requireNonNull(outputXML.getParentFile()).mkdirs();
                    } else {
                        outputXML = new File(Utils.getExportPath(activity), fileName.replace(getExt(fileName), "-aXMLEditor.xml"));
                    }
                    try (FileOutputStream fos = new FileOutputStream(outputXML)) {
                        aXMLEncoder aXMLEncoder = new aXMLEncoder();
                        byte[] bs = aXMLEncoder.encodeString(xmlString, activity);
                        fos.write(bs);
                    } catch (IOException | XmlPullParserException ignored) {
                    }
                } else {
                    invalid = true;
                }
            }

            @Override
            public void onPostExecute() {
                progressDialog.dismissDialog();
                if (invalid) {
                    Utils.toast(activity.getString(R.string.xml_corrupted_toast), activity).show();
                }
                activity.finish();
            }
        };
    }

}