package com.apk.axmleditor.activities;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.apk.axml.APKParser;
import com.apk.axmleditor.R;
import com.apk.axmleditor.Utils.Async;
import com.apk.axmleditor.dialogs.ProgressDialog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Objects;

/*
 * Created by APK Explorer & Editor <apkeditor@protonmail.com> on January 26, 2026
 */
public class APKPickerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getIntent().getData() != null) {
            new Async() {
                private final Activity activity = APKPickerActivity.this;
                private ProgressDialog progressDialog;
                private String fileName, packageName = null;
                @Override
                public void onPreExecute() {
                    progressDialog = new ProgressDialog(activity);
                    progressDialog.setProgressStatus(R.string.loading);
                    progressDialog.startDialog();
                    new File(getExternalCacheDir(), "image.png").delete();
                }

                private String getFileName(ContentResolver resolver, Uri uri) {
                    String result = null;
                    if ("content".equals(uri.getScheme())) {
                        try (Cursor cursor = resolver.query(uri, null, null, null, null)) {
                            if (cursor != null && cursor.moveToFirst()) {
                                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                                if (nameIndex != -1) {
                                    result = cursor.getString(nameIndex);
                                }
                            }
                        }
                    }

                    if (result == null) {
                        result = uri.getLastPathSegment();
                    }

                    return result;
                }

                public Bitmap drawableToBitmap(Drawable drawable) {
                    if (drawable == null) return null;
                    if (drawable instanceof BitmapDrawable) {
                        return ((BitmapDrawable) drawable).getBitmap();
                    }

                    int width = drawable.getIntrinsicWidth() > 0 ? drawable.getIntrinsicWidth() : 1;
                    int height = drawable.getIntrinsicHeight() > 0 ? drawable.getIntrinsicHeight() : 1;
                    Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(bitmap);
                    drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                    drawable.draw(canvas);
                    return bitmap;
                }

                private void drawableToFile(Drawable drawable) throws IOException {
                    if (drawable == null) return;
                    Bitmap bitmap = drawableToBitmap(drawable);
                    FileOutputStream fos = new FileOutputStream(new File(getExternalCacheDir(), "image.png"));
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                    fos.close();
                }

                @Override
                public void doInBackground() {
                    new File(getExternalCacheDir(), "image.png").delete();
                    fileName = getFileName(getContentResolver(), Objects.requireNonNull(getIntent().getData()));
                    if (fileName != null && fileName.endsWith(".apk")) {
                        APKParser mAPKParser = new APKParser();
                        mAPKParser.parse(Objects.requireNonNull(getIntent().getData()), activity);

                        if (mAPKParser.isParsed()) {
                            packageName = mAPKParser.getPackageName();
                            fileName = mAPKParser.getAppName();
                            try {
                                drawableToFile(mAPKParser.getAppIcon());
                            } catch (IOException ignored) {}
                        }
                    }
                }

                @Override
                public void onPostExecute() {
                    progressDialog.dismissDialog();
                    if (getIntent().getData() != null) {
                        Intent intent = new Intent(activity, APKExplorerActivity.class);
                        intent.putExtra(APKExplorerActivity.PACKAGE_NAME_INTENT, packageName);
                        intent.putExtra(APKExplorerActivity.NAME_INTENT, fileName);
                        intent.setData(getIntent().getData());
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        startActivity(intent);
                    }
                    activity.finish();
                }
            }.execute();
        }
    }

}