package com.apk.axmleditor.fragments;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.apk.axml.APKParser;
import com.apk.axmleditor.R;
import com.apk.axmleditor.Utils.Async;
import com.apk.axmleditor.Utils.FilesViewModel;
import com.apk.axmleditor.adapters.FilesAdapter;
import com.apk.axmleditor.dialogs.ProgressDialog;
import com.apk.axmleditor.serializable.FilesEntry;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/*
 * Created by APK Explorer & Editor <apkeditor@protonmail.com> on January 05, 2025
 */
public class FilesFragment extends Fragment {

    private FilesAdapter adapter;
    private List<FilesEntry> filesEntries;
    private RecyclerView recyclerView;
    private String packageName, searchTxt = null;
    private Uri uri;

    public FilesFragment() {
    }

    public static FilesFragment newInstance(String key, String packageName, Uri uri) {
        FilesFragment fragment = new FilesFragment();

        Bundle args = new Bundle();
        args.putString("uri", uri.toString());
        args.putString("packageName", packageName);
        args.putString("key", key);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FilesViewModel viewModel = new ViewModelProvider(requireActivity()).get(FilesViewModel.class);
        String key = null;

        if (getArguments() != null) {
            packageName = getArguments().getString("packageName");
            String uriString = getArguments().getString("uri");
            key = getArguments().getString("key");
            if (uriString != null) {
                uri = Uri.parse(uriString);
            }
        }

        filesEntries = viewModel.getFiles(key);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View mRootView = inflater.inflate(R.layout.fragment_files, container, false);

        MaterialAutoCompleteTextView mSearch = mRootView.findViewById(R.id.search);
        recyclerView = mRootView.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.addItemDecoration(new DividerItemDecoration(requireActivity(), DividerItemDecoration.VERTICAL));

        mSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                loadUI(s.toString().toLowerCase()).execute();
            }
        });

        loadUI(searchTxt).execute();

        requireActivity().getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (!isAdded()) return;
                if (searchTxt != null && !searchTxt.trim().isEmpty()) {
                    searchTxt = null;
                    mSearch.setText(null);
                    return;
                }
                new MaterialAlertDialogBuilder(requireActivity())
                        .setIcon(R.mipmap.ic_launcher)
                        .setTitle(R.string.exist_project_title)
                        .setMessage(getString(R.string.exist_project_message, new APKParser().getPackageName()))
                        .setCancelable(false)
                        .setNeutralButton(R.string.cancel, (dialogInterface, i) -> {
                        })
                        .setPositiveButton(R.string.exit, (dialogInterface, i) -> requireActivity().finish())
                        .show();
            }
        });

        return mRootView;
    }

    private Async loadUI(String searchText) {
        return new Async() {
            private ProgressDialog progressDialog;

            @Override
            public void onPreExecute() {
                progressDialog = new ProgressDialog(requireActivity());
                progressDialog.setProgressStatus(R.string.loading);
                progressDialog.startDialog();
                recyclerView.setVisibility(View.GONE);
            }

            public List<FilesEntry> filteredData(List<FilesEntry> filesData) {
                List<FilesEntry> filteredData;
                if (searchText == null || searchText.isEmpty()) {
                    filteredData = filesData;
                } else {
                    filteredData = new ArrayList<>();
                    for (FilesEntry entry : filesData) {
                        if (entry.getName().toLowerCase(Locale.getDefault()).contains(searchText)) {
                            filteredData.add(entry);
                        }
                    }
                }
                return filteredData;
            }

            @Override
            public void doInBackground() {
                adapter = new FilesAdapter(filteredData(filesEntries), uri, packageName, updateFile);
            }

            @Override
            public void onPostExecute() {
                if (!isAdded()) return;
                progressDialog.dismissDialog();
                searchTxt = searchText;
                recyclerView.setAdapter(adapter);
                recyclerView.setVisibility(View.VISIBLE);
            }
        };
    }

    private final ActivityResultLauncher<Intent> updateFile = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Intent intent = result.getData();
                    int position = intent.getIntExtra("position", RecyclerView.NO_POSITION);
                    if (position != RecyclerView.NO_POSITION) {
                        adapter.notifyItemChanged(position);
                    }
                }
            }
    );
    
}