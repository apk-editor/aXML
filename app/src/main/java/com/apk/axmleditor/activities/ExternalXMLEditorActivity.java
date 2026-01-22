package com.apk.axmleditor.activities;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.apk.axml.aXMLDecoder;
import com.apk.axml.serializableItems.XMLEntry;

import com.apk.axmleditor.BaseActivity;
import com.apk.axmleditor.R;
import com.apk.axmleditor.Utils.Async;
import com.apk.axmleditor.dialogs.ProgressDialog;
import com.apk.axmleditor.adapters.XMLEditorAdapter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textview.MaterialTextView;

import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/*
 * Created by APK Explorer & Editor <apkeditor@protonmail.com> on January 05, 2026
 */
public class ExternalXMLEditorActivity extends BaseActivity {

    private MaterialButton mSave;
    private RecyclerView mRecyclerView;
    private List<XMLEntry> mXMLEntries = null;
    private String mName = null, mSearchText = null;
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
        mRecyclerView = findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        mName = getIntent().getStringExtra(NAME_INTENT);
        Uri mUri = getIntent().getData();

        File imageFile = new File(getExternalCacheDir(), "image.png");
        Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
        if (imageFile.exists() && bitmap != null) {
            icon.setImageBitmap(bitmap);
        }

        if (mName != null) {
            mTitle.setText(mName);
            mTitle.setVisibility(View.VISIBLE);
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

        getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (mSearchText != null && !mSearchText.trim().isEmpty()) {
                    mSearchText = null;
                    mSearch.setText(null);
                    return;
                }
                if (mSave.getVisibility() == View.VISIBLE) {
                    new MaterialAlertDialogBuilder(ExternalXMLEditorActivity.this)
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
            private final Activity activity = ExternalXMLEditorActivity.this;
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
                    try {
                        mXMLEntries = new aXMLDecoder(getContentResolver().openInputStream(uri), null).decode();
                    } catch (XmlPullParserException | IOException ignored) {
                    }
                }

                if (mXMLEntries != null && !mXMLEntries.isEmpty()) {
                    mAdapter = new XMLEditorAdapter(filteredData(mXMLEntries), mXMLEntries, null, uri, null, searchText, mName, mSave, activity);
                }
            }

            @Override
            public void onPostExecute() {
                progressDialog.dismissDialog();
                mSearchText = searchText;
                mRecyclerView.setAdapter(mAdapter);
                mRecyclerView.setVisibility(View.VISIBLE);
            }
        };
    }

}