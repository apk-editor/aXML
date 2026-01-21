package com.apk.axmleditor.activities;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.apk.axml.APKParser;
import com.apk.axmleditor.R;
import com.apk.axmleditor.Utils.Async;
import com.apk.axmleditor.Utils.Utils;
import com.apk.axmleditor.dialogs.ProgressDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textview.MaterialTextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/*
 * Created by APK Explorer & Editor <apkeditor@protonmail.com> on January 05, 2025
 */
public class ImageViewerActivity extends AppCompatActivity {

    private AppCompatImageView mImageView;
    private boolean mLocallyUpdated = false;
    private Drawable mDrawable = null;
    private int mPosition;
    private MaterialButton mRestoreButton;
    private String mName = null, mPackageName = null;
    public static final String NAME_INTENT = "name", POSITION_INTENT = "position";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_imageviewer);

        AppCompatImageButton icon = findViewById(R.id.icon);
        mImageView = findViewById(R.id.image);
        MaterialButton replaceButton = findViewById(R.id.save);
        mRestoreButton = findViewById(R.id.restore);
        MaterialTextView mTitle = findViewById(R.id.title);

        mName = getIntent().getStringExtra(NAME_INTENT);
        mPosition = getIntent().getIntExtra(POSITION_INTENT, RecyclerView.NO_POSITION);
        Uri mUri = getIntent().getData();

        icon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_image));
        replaceButton.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_replace));
        replaceButton.setText(getString(R.string.replace_image));
        replaceButton.setVisibility(VISIBLE);

        if (mName != null) {
            mTitle.setText(mName);
            mTitle.setVisibility(VISIBLE);
        } else {
            mTitle.setVisibility(GONE);
        }

        loadUI(mUri).execute();

        mRestoreButton.setOnClickListener(v -> new Async() {
            private final Activity activity = ImageViewerActivity.this;
            private Intent result;
            private ProgressDialog progressDialog;

            @Override
            public void onPreExecute() {
                progressDialog = new ProgressDialog(activity);
                progressDialog.setProgressStatus(R.string.applying_modifications_message);
                progressDialog.startDialog();
                result = activity.getIntent();
            }

            @Override
            public void doInBackground() {
                new File(Utils.getExportPath(activity), mPackageName + "/" + mName).delete();
            }

            @Override
            public void onPostExecute() {
                progressDialog.dismissDialog();
                result.putExtra("position", mPosition);
                setResult(Activity.RESULT_OK, result);
                activity.finish();
            }
        }.execute());

        replaceButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            String[] mimeTypes = {
                    "image/png",
                    "image/jpeg",
                    "image/gif",
                    "image/bmp",
                    "image/heif",
                    "image/heic",
                    "image/webp"
            };
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            imagePicker.launch(intent);
        });

        getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (mDrawable != null && mDrawable != mImageView.getDrawable()) {
                    new MaterialAlertDialogBuilder(ImageViewerActivity.this)
                            .setIcon(R.mipmap.ic_launcher)
                            .setTitle(R.string.discard_file_message)
                            .setNegativeButton(R.string.cancel, (dialogInterface, i) -> {
                            })
                            .setPositiveButton(R.string.discard, (dialogInterface, i) -> finish())
                            .show();
                    return;
                }
                finish();
            }
        });
    }

    private Async loadUI(Uri uri) {
        return new Async() {
            private final Activity activity = ImageViewerActivity.this;
            private ProgressDialog progressDialog;

            @Override
            public void onPreExecute() {
                progressDialog = new ProgressDialog(activity);
                progressDialog.setProgressStatus(R.string.loading);
                progressDialog.startDialog();
            }

            @Override
            public void doInBackground() {
                if (uri != null) {
                    APKParser mAPKParser = new APKParser();
                    if (mAPKParser.isParsed() && mAPKParser.getDecodedResources() != null) {
                        mPackageName = mAPKParser.getPackageName();
                    }

                    File parentDir = new File(Utils.getExportPath(activity), Objects.requireNonNull(mPackageName));
                    if (parentDir.exists() && new File(parentDir, mName).exists()) {
                        mLocallyUpdated = true;
                        try (FileInputStream fis = new FileInputStream(new File(parentDir, mName))) {
                            mDrawable = Drawable.createFromStream(fis, null);
                        } catch (IOException ignored) {}
                    } else {
                        mLocallyUpdated = false;
                        try (ZipInputStream zis = new ZipInputStream(getContentResolver().openInputStream(uri))) {
                            ZipEntry entry;

                            while ((entry = zis.getNextEntry()) != null) {

                                // Skip directories
                                if (entry.isDirectory()) {
                                    continue;
                                }

                                if (entry.getName().equals(mName)) {
                                    mDrawable = Drawable.createFromStream(zis, null);
                                }

                                zis.closeEntry();
                            }
                        } catch (IOException ignored) {}
                    }
                }
            }

            @Override
            public void onPostExecute() {
                progressDialog.dismissDialog();
                if (mDrawable != null) {
                    mImageView.setImageDrawable(mDrawable);
                }
                if (mLocallyUpdated) {
                    mRestoreButton.setVisibility(VISIBLE);
                }
            }
        };
    }

    private final ActivityResultLauncher<Intent> imagePicker = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();

                    new Async() {
                        private final Activity activity = ImageViewerActivity.this;
                        private Bitmap bitmap = null;
                        private Intent result;
                        private ProgressDialog progressDialog;
                        @Override
                        public void onPreExecute() {
                            progressDialog = new ProgressDialog(activity);
                            progressDialog.setProgressStatus(R.string.loading);
                            progressDialog.startDialog();
                        }

                        private Bitmap loadBitmap(Uri uri) {
                            try {
                                return MediaStore.Images.Media.getBitmap(activity.getContentResolver(), uri);
                            } catch (IOException ignored) {
                            }
                            return null;
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

                        private void drawableToFile(Drawable drawable, File outputFile) throws IOException {
                            if (drawable == null) return;

                            if (mDrawable == null) return;

                            int width = mDrawable.getIntrinsicWidth() > 0 ? mDrawable.getIntrinsicWidth() : 1;
                            int height = mDrawable.getIntrinsicHeight() > 0 ? mDrawable.getIntrinsicHeight() : 1;

                            // Convert drawable → bitmap
                            Bitmap bitmap = drawableToBitmap(drawable);

                            // Scale bitmap to desired width & height
                            Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);

                            // Save to file
                            FileOutputStream fos = new FileOutputStream(outputFile);
                            scaledBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                            fos.close();
                        }

                        @Override
                        public void doInBackground() {
                            result = activity.getIntent();
                            Uri uri = data.getData();
                            if (uri != null) {
                                bitmap = loadBitmap(uri);
                            }
                        }

                        @Override
                        public void onPostExecute() {
                            progressDialog.dismissDialog();
                            if (bitmap != null) {
                                mImageView.setImageBitmap(bitmap);
                                new MaterialAlertDialogBuilder(ImageViewerActivity.this)
                                        .setIcon(mImageView.getDrawable())
                                        .setTitle(R.string.replace_image_question)
                                        .setCancelable(false)
                                        .setNegativeButton(R.string.restore_original, (dialogInterface, i) -> mImageView.setImageDrawable(mDrawable))
                                        .setPositiveButton(R.string.replace, (dialogInterface, i) -> new Async() {
                                                    private final Activity activity = ImageViewerActivity.this;
                                                    private ProgressDialog progressDialog;

                                                    @Override
                                                    public void onPreExecute() {
                                                        progressDialog = new ProgressDialog(activity);
                                                        progressDialog.setProgressStatus(R.string.applying_modifications_message);
                                                        progressDialog.startDialog();
                                                    }

                                                    @Override
                                                    public void doInBackground() {
                                                        File outputFile = new File(Utils.getExportPath(activity), mPackageName + "/" + mName);
                                                        Objects.requireNonNull(outputFile.getParentFile()).mkdirs();
                                                        try {
                                                            drawableToFile(mImageView.getDrawable(), outputFile);
                                                        } catch (IOException ignored) {
                                                        }
                                                    }

                                                    @Override
                                                    public void onPostExecute() {
                                                        progressDialog.dismissDialog();
                                                        result.putExtra("position", mPosition);
                                                        setResult(Activity.RESULT_OK, result);
                                                        activity.finish();
                                                    }
                                                }.execute()
                                        ).show();
                            }
                        }
                    }.execute();
                }
            }
    );

}