package com.apk.axmleditor.activities;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.content.ContextCompat;

import com.apk.axml.APKParser;
import com.apk.axmleditor.BaseActivity;
import com.apk.axmleditor.R;
import com.apk.axmleditor.Utils.Async;
import com.apk.axmleditor.Utils.Utils;
import com.apk.axmleditor.dialogs.ProgressDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textview.MaterialTextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/*
 * Created by APK Explorer & Editor <apkeditor@protonmail.com> on January 05, 2026
 */
public class TextEditorActivity extends BaseActivity {

    private boolean mLocallyUpdated = false;
    private MaterialButton mRestoreButton, mSave;
    private String mName = null, mPackageName = null, mText = null;
    public static final String NAME_INTENT = "name";
    private MaterialAutoCompleteTextView mTextView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentViewWithInsets(R.layout.activity_texteditor, R.id.layout_root);

        AppCompatImageButton icon = findViewById(R.id.icon);
        mTextView = findViewById(R.id.text);
        mSave = findViewById(R.id.save);
        mRestoreButton = findViewById(R.id.restore);
        MaterialTextView mTitle = findViewById(R.id.title);

        mName = getIntent().getStringExtra(NAME_INTENT);
        Uri mUri = getIntent().getData();

        icon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_txt));

        if (mName != null) {
            mTitle.setText(mName);
            mTitle.setVisibility(VISIBLE);
        } else {
            mTitle.setVisibility(GONE);
        }

        loadUI(mUri).execute();

        mSave.setOnClickListener(v -> new Async() {
            private final Activity activity = TextEditorActivity.this;
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
                Utils.create(mTextView.getText().toString().trim(), outputFile);
            }

            @Override
            public void onPostExecute() {
                progressDialog.dismissDialog();
                activity.finish();
            }
        }.execute());

        mRestoreButton.setOnClickListener(v -> new Async() {
            private final Activity activity = TextEditorActivity.this;
            private ProgressDialog progressDialog;

            @Override
            public void onPreExecute() {
                progressDialog = new ProgressDialog(activity);
                progressDialog.setProgressStatus(R.string.applying_modifications_message);
                progressDialog.startDialog();
            }

            @Override
            public void doInBackground() {
                new File(Utils.getExportPath(activity), mPackageName + "/" + mName).delete();
            }

            @Override
            public void onPostExecute() {
                progressDialog.dismissDialog();
                activity.finish();
            }
        }.execute());

        mTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (mText != null && !mText.isEmpty() && !mText.trim().equals(s.toString().trim())) {
                    mSave.setVisibility(VISIBLE);
                } else {
                    mSave.setVisibility(GONE);
                }
            }
        });

        getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (mSave.getVisibility() == VISIBLE) {
                    new MaterialAlertDialogBuilder(TextEditorActivity.this)
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
            private final Activity activity = TextEditorActivity.this;
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
                            mText = Utils.read(fis);
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
                                    mText = Utils.read(zis);
                                }

                                zis.closeEntry();
                            }
                        } catch (IOException ignored) {}
                    }
                }
            }

            @Override
            public void onPostExecute() {
                if (mLocallyUpdated) {
                    mRestoreButton.setVisibility(VISIBLE);
                }
                if (mText != null && !mText.isEmpty()) {
                    mTextView.setText(mText.trim());
                }
                progressDialog.dismissDialog();
            }
        };
    }

}