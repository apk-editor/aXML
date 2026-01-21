package com.apk.axmleditor.Utils;

import android.content.Context;
import android.os.Environment;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Toast;

import com.apk.axmleditor.R;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

/*
 * Created by APK Explorer & Editor <apkeditor@protonmail.com> on January 05, 2025
 */
public class Utils {

    public static boolean isBinaryXML(String name) {
        return name.equalsIgnoreCase("AndroidManifest.xml") ||
                name.contains("res/") && name.endsWith(".xml");
    }

    public static boolean isImageFile(String name) {
        return name.endsWith(".png") ||
                name.endsWith(".webp") ||
                name.endsWith(".bmp") ||
                name.endsWith(".jpg") ||
                name.endsWith(".jpeg");
    }

    public static boolean isTextFile(String name) {
        return name.endsWith(".txt") ||
                name.endsWith(".json") ||
                name.endsWith(".properties") ||
                !isBinaryXML(name) && name.endsWith(".xml") ||
                name.endsWith(".version") ||
                name.endsWith(".html") ||
                name.endsWith(".sh") ||
                name.endsWith(".mf") ||
                name.endsWith(".sf") ||
                name.endsWith(".ini");
    }

    public static File getExportPath(Context context) {
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                context.getString(R.string.app_name));
        if (!file.exists()) {
            file.mkdirs();
        }
        return file;
    }

    public static String read(InputStream inputStream) throws IOException {
        BufferedInputStream bis = new BufferedInputStream(inputStream);
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        for (int result = bis.read(); result != -1; result = bis.read()) {
            buf.write((byte) result);
        }
        return buf.toString("UTF-8");
    }

    public static Toast toast(int resID, Context context) {
        return Toast.makeText(context, context.getString(resID), Toast.LENGTH_LONG);
    }

    public static Toast toast(String message, Context context) {
        return Toast.makeText(context, message, Toast.LENGTH_LONG);
    }

    public static void copyStream(InputStream inputStream, File dest) {
        try (FileOutputStream outputStream = new FileOutputStream(dest, false)) {
            copyStream(Objects.requireNonNull(inputStream), outputStream);
            inputStream.close();
        } catch (IOException ignored) {}
    }

    private static void copyStream(InputStream from, OutputStream to) throws IOException {
        byte[] buf = new byte[1024 * 1024];
        int len;
        while ((len = from.read(buf)) > 0) {
            to.write(buf, 0, len);
        }
    }

    public static void create(String text, File path) {
        try {
            FileWriter writer = new FileWriter(path);
            writer.write(text);
            writer.close();
        } catch (IOException ignored) {
        }
    }

    public static void setSlideInAnimation(final View viewToAnimate, int position) {
        if (position > -1) {
            viewToAnimate.setTranslationY(50f);
            viewToAnimate.setAlpha(0f);

            viewToAnimate.animate()
                    .translationY(0f)
                    .alpha(1f)
                    .setDuration(150)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        } else {
            viewToAnimate.setTranslationY(0f);
            viewToAnimate.setAlpha(1f);
        }
    }

}