package com.apk.axmleditor;

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
import android.os.Handler;
import android.provider.OpenableColumns;
import android.view.View;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.apk.axml.APKParser;
import com.apk.axml.aXMLDecoder;
import com.apk.axmleditor.Utils.Async;
import com.apk.axmleditor.activities.APKExplorerActivity;
import com.apk.axmleditor.dialogs.AboutDialog;
import com.apk.axmleditor.dialogs.ProgressDialog;
import com.apk.axmleditor.Utils.Utils;
import com.apk.axmleditor.activities.ExternalXMLEditorActivity;
import com.google.android.material.button.MaterialButton;

import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Objects;

/*
 * Created by APK Explorer & Editor <apkeditor@protonmail.com> on January 05, 2026
 */
public class MainActivity extends AppCompatActivity {

    private boolean exit = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        View rootView = getWindow().getDecorView().findViewById(android.R.id.content);
        MaterialButton infoButton = findViewById(R.id.info_button);

        rootView.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            String[] mimeTypes = {
                    "application/vnd.android.package-archive",
                    "application/xml",
                    "text/plain",
                    "text/xml"
            };
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            explorerFilePicker.launch(intent);
        });

        infoButton.setOnClickListener(v -> new AboutDialog(this));

        getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (exit) {
                    exit = false;
                    finish();
                } else {
                    Utils.toast(R.string.exit_confirmation_message, MainActivity.this).show();
                    exit = true;
                    new Handler().postDelayed(() -> exit = false, 2000);
                }
            }
        });
    }

    private final ActivityResultLauncher<Intent> explorerFilePicker = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();

                    new Async() {
                        private final Activity activity = MainActivity.this;
                        private int fileType;
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
                            fileName = getFileName(getContentResolver(), Objects.requireNonNull(data.getData()));
                            if (fileName != null) {
                                if (fileName.endsWith(".apk")) {
                                    fileType = 0;
                                    APKParser mAPKParser = new APKParser();
                                    mAPKParser.parse(Objects.requireNonNull(data.getData()), activity);

                                    if (mAPKParser.isParsed()) {
                                        packageName = mAPKParser.getPackageName();
                                        fileName = mAPKParser.getAppName();
                                        try {
                                            drawableToFile(mAPKParser.getAppIcon());
                                        } catch (IOException ignored) {}
                                    }
                                } else {
                                    try {
                                        new aXMLDecoder(getContentResolver().openInputStream(data.getData()), null).decode();
                                        fileType = 1;
                                    } catch (XmlPullParserException | IOException e) {
                                        fileType = 2;
                                    }
                                }
                            }
                        }

                        @Override
                        public void onPostExecute() {
                            progressDialog.dismissDialog();
                            if (fileType == 2) {
                                Utils.toast(R.string.file_type_invalid_toast, activity).show();
                                return;
                            }
                            if (data.getData() != null) {
                                Intent intent;
                                if (fileType == 1) {
                                    intent = new Intent(activity, ExternalXMLEditorActivity.class);
                                    intent.putExtra(ExternalXMLEditorActivity.NAME_INTENT, fileName);
                                } else {
                                    intent = new Intent(activity, APKExplorerActivity.class);
                                    intent.putExtra(APKExplorerActivity.PACKAGE_NAME_INTENT, packageName);
                                    intent.putExtra(APKExplorerActivity.NAME_INTENT, fileName);
                                }
                                intent.setData(data.getData());
                                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                startActivity(intent);
                            }
                        }
                    }.execute();
                }
            }
    );

}