package com.apk.axmleditor.activities;

import static android.view.View.VISIBLE;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.apk.axml.APKParser;
import com.apk.axml.aXMLDecoder;
import com.apk.axml.serializableItems.ResEntry;
import com.apk.axml.serializableItems.XMLEntry;
import com.apk.axmleditor.BaseActivity;
import com.apk.axmleditor.R;
import com.apk.axmleditor.Utils.Async;
import com.apk.axmleditor.Utils.Utils;
import com.apk.axmleditor.adapters.XMLEditorAdapter;
import com.apk.axmleditor.dialogs.ProgressDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textview.MaterialTextView;

import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/*
 * Created by APK Explorer & Editor <apkeditor@protonmail.com> on January 05, 2026
 */
public class XMLEditorActivity extends BaseActivity {

    private boolean mLocallyUpdated = false;
    private MaterialButton mRestoreButton, mSave;
    private RecyclerView mRecyclerView;
    private List<XMLEntry> mXMLEntries = null;
    private String mName = null, mPackageName = null, mSearchText = null;
    public static final String NAME_INTENT = "name";
    private XMLEditorAdapter mAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentViewWithInsets(R.layout.activity_xmleditor, R.id.layout_root);

        AppCompatImageButton icon = findViewById(R.id.icon);
        MaterialAutoCompleteTextView mSearch = findViewById(R.id.search);
        mSave = findViewById(R.id.save);
        MaterialTextView mTitle = findViewById(R.id.title);
        mRestoreButton = findViewById(R.id.restore);
        mRecyclerView = findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        mName = getIntent().getStringExtra(NAME_INTENT);
        Uri mUri = getIntent().getData();

        icon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_xml));

        if (mName != null) {
            mTitle.setText(mName);
            mTitle.setVisibility(VISIBLE);
        } else {
            mTitle.setVisibility(View.GONE);
        }

        loadUI(mUri, mSearchText).execute();

        mSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                loadUI(null, s.toString().toLowerCase()).execute();
            }
        });

        mRestoreButton.setOnClickListener(v -> new Async() {
            private final Activity activity = XMLEditorActivity.this;
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

        getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (mSearchText != null && !mSearchText.trim().isEmpty()) {
                    mSearchText = null;
                    mSearch.setText(null);
                    return;
                }
                if (mSave.getVisibility() == VISIBLE) {
                    new MaterialAlertDialogBuilder(XMLEditorActivity.this)
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

    private Async loadUI(Uri uri, String searchText) {
        return new Async() {
            private final Activity activity = XMLEditorActivity.this;
            private List<ResEntry> resEntries = null;
            private ProgressDialog progressDialog;

            @Override
            public void onPreExecute() {
                progressDialog = new ProgressDialog(activity);
                progressDialog.setProgressStatus(R.string.loading);
                progressDialog.startDialog();
                mRecyclerView.setVisibility(View.GONE);
            }

            public List<XMLEntry> filteredData(List<XMLEntry> data) {
                List<XMLEntry> filteredData;
                if (searchText == null || searchText.isEmpty()) {
                    filteredData = data;
                } else {
                    filteredData = new ArrayList<>();
                    for (XMLEntry entry : data) {
                        if (entry.getText().contains(searchText)) {
                            filteredData.add(entry);
                        }
                    }
                }
                return filteredData;
            }

            @Override
            public void doInBackground() {
                if (uri != null) {
                    APKParser mAPKParser = new APKParser();
                    if (mAPKParser.isParsed() && mAPKParser.getDecodedResources() != null) {
                        resEntries = mAPKParser.getDecodedResources();
                        mPackageName = mAPKParser.getPackageName();
                    }

                    File parentDir = new File(Utils.getExportPath(activity), Objects.requireNonNull(mPackageName));
                    if (parentDir.exists() && new File(parentDir, mName).exists()) {
                        mLocallyUpdated = true;
                        try (FileInputStream fis = new FileInputStream(new File(parentDir, mName))) {
                            mXMLEntries = new aXMLDecoder(fis, resEntries).decode();
                        } catch (IOException | XmlPullParserException ignored) {}
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
                                    mXMLEntries = new aXMLDecoder(zis, resEntries).decode();
                                }

                                zis.closeEntry();
                            }
                        } catch (XmlPullParserException | IOException ignored) {}
                    }
                }

                if (mXMLEntries != null && !mXMLEntries.isEmpty()) {
                    mAdapter = new XMLEditorAdapter(filteredData(mXMLEntries), mXMLEntries, resEntries, uri, mPackageName, searchText, mName, mSave, activity);
                }
            }

            @Override
            public void onPostExecute() {
                progressDialog.dismissDialog();
                mSearchText = searchText;
                mRecyclerView.setAdapter(mAdapter);
                mRecyclerView.setVisibility(VISIBLE);
                if (mLocallyUpdated) {
                    mRestoreButton.setVisibility(VISIBLE);
                }
            }
        };
    }

}