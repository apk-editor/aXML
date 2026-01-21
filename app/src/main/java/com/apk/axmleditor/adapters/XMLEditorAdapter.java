package com.apk.axmleditor.adapters;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.apk.axml.serializableItems.ResEntry;
import com.apk.axml.serializableItems.XMLEntry;

import com.apk.axmleditor.R;
import com.apk.axmleditor.Utils.Async;
import com.apk.axmleditor.activities.ImageViewerActivity;
import com.apk.axmleditor.activities.TextEditorActivity;
import com.apk.axmleditor.activities.XMLEditorActivity;
import com.apk.axmleditor.dialogs.ProgressDialog;
import com.apk.axmleditor.Utils.Utils;
import com.apk.axmleditor.Utils.XMLEditor;
import com.apk.axmleditor.dialogs.XMLEditorDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textview.MaterialTextView;

import java.util.List;
import java.util.Objects;

/*
 * Created by APK Explorer & Editor <apkeditor@protonmail.com> on January 05, 2025
 */
public class XMLEditorAdapter extends RecyclerView.Adapter<XMLEditorAdapter.ViewHolder> {

    private final Activity activity;
    private final List<XMLEntry> data, originalData;
    private final List<ResEntry> resourceMap;
    private final MaterialButton saveButton;
    private final String fileName, packageName, searchWord;
    private static boolean isModified = false;
    private static Uri fileUri = null;

    public XMLEditorAdapter(List<XMLEntry> data, List<XMLEntry> originalData, List<ResEntry> resourceMap, Uri uri, String packageName, String searchWord, String fileName, MaterialButton saveButton, Activity activity) {
        this.data = data;
        this.originalData = originalData;
        this.resourceMap = resourceMap;
        if (fileUri == null && uri != null) {
            fileUri = uri;
        }
        this.packageName = packageName;
        this.searchWord = searchWord;
        this.fileName = fileName;
        this.saveButton = saveButton;
        this.activity = activity;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View rowItem = LayoutInflater.from(parent.getContext()).inflate(R.layout.recycle_view_xmleditor, parent, false);
        return new ViewHolder(rowItem);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (searchWord == null || data.get(position).getText().contains(searchWord)) {
            holder.mText.setAlpha(data.get(position).getValue().isEmpty() ? (float) 0.5 : 1);
            holder.mText.setText(data.get(position).getText());
            holder.mText.setVisibility(VISIBLE);
        } else {
            holder.mText.setVisibility(GONE);
        }
        Utils.setSlideInAnimation(holder.itemView, position);

        saveButton.setOnClickListener(v -> XMLEditor.encodeToBinaryXML(XMLEditor.xmlEntriesToXML(originalData, resourceMap), packageName, fileName, activity).execute());
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {

        private final MaterialTextView mText;

        public ViewHolder(View view) {
            super(view);
            view.setOnClickListener(this);
            view.setOnLongClickListener(this);
            this.mText = view.findViewById(R.id.text);
        }

        @Override
        public void onClick(View view) {
            int position = getBindingAdapterPosition();
            if (position == RecyclerView.NO_POSITION) return;
            if (data.get(position).getValue().isEmpty()) return;
            if (data.get(position).getValue().startsWith("res/")) {
                Intent intent = null;
                if (Utils.isBinaryXML(data.get(position).getValue().toLowerCase())) {
                    intent = new Intent(view.getContext(), XMLEditorActivity.class);
                    intent.putExtra(XMLEditorActivity.NAME_INTENT, data.get(position).getValue());
                } else if (Utils.isTextFile(data.get(position).getValue().toLowerCase())) {
                    intent = new Intent(view.getContext(), TextEditorActivity.class);
                    intent.putExtra(TextEditorActivity.NAME_INTENT, data.get(position).getValue());
                } else if (Utils.isImageFile(data.get(position).getValue())) {
                    intent = new Intent(view.getContext(), ImageViewerActivity.class);
                    intent.putExtra(ImageViewerActivity.NAME_INTENT, data.get(position).getValue());
                    intent.putExtra(ImageViewerActivity.POSITION_INTENT, RecyclerView.NO_POSITION);
                }
                Objects.requireNonNull(intent).setData(fileUri);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                view.getContext().startActivity(intent);
            } else {
                launchEditorDialog(position, view.getContext());
            }
        }

        @Override
        public boolean onLongClick(View view) {
            int position = getBindingAdapterPosition();
            if (position == RecyclerView.NO_POSITION) return false;
            if (data.get(position).getValue().isEmpty()) return true;
            new MaterialAlertDialogBuilder(view.getContext())
                    .setIcon(R.mipmap.ic_launcher)
                    .setTitle(R.string.remove_line_question)
                    .setMessage(data.get(position).getText().trim())
                    .setNegativeButton(R.string.cancel, (dialogInterface, i) -> {
                    })
                    .setPositiveButton(R.string.remove_line, (dialogInterface, i) ->
                            removeCurrentLine(position, view.getContext()).execute())
                    .show();
            return true;
        }
    }

    private void launchEditorDialog(int position, Context context) {
        new XMLEditorDialog(data.get(position), context) {

            @Override
            public void modifyLine(String newValue) {
                modify(newValue, position, context).execute();
            }

            @Override
            public void removeLine() {
                removeCurrentLine(position, context).execute();
            }
        };
    }

    private Async modify(String newValue, int position, Context context) {
        return new Async() {
            private boolean invalid = false;
            private ProgressDialog progressDialog;
            @Override
            public void onPreExecute() {
                progressDialog = new ProgressDialog(context);
                progressDialog.setTitle(context.getString(R.string.applying_modifications_message));
                progressDialog.setIcon(R.mipmap.ic_launcher);
                progressDialog.startDialog();
            }

            @Override
            public void doInBackground() {
                int positionOriginal = RecyclerView.NO_POSITION;

                for (int i = 0; i < originalData.size(); i++) {
                    if (originalData.get(i).getId().equals(data.get(position).getId())) {
                        positionOriginal = i;
                        break;
                    }
                }

                if (positionOriginal == RecyclerView.NO_POSITION) return;

                String oldValue = data.get(position).getValue();
                data.get(position).setValue(newValue);
                originalData.get(positionOriginal).setValue(newValue);

                if (XMLEditor.isXMLValid(XMLEditor.xmlEntriesToXML(originalData, resourceMap))) {
                    if (!isModified) {
                        isModified = true;
                    }
                    invalid = false;
                } else {
                    data.get(position).setValue(oldValue);
                    originalData.get(positionOriginal).setValue(oldValue);
                    invalid = true;
                }
            }

            @Override
            public void onPostExecute() {
                progressDialog.dismissDialog();
                if (invalid) {
                    Utils.toast(context.getString(R.string.xml_corrupted_toast), context).show();
                } else {
                    saveButton.setVisibility(isModified ? VISIBLE : GONE);
                    notifyItemChanged(position);
                }
            }
        };
    }

    private Async removeCurrentLine(int position, Context context) {
        return new Async() {
            private boolean invalid = false;
            private ProgressDialog progressDialog;

            @Override
            public void onPreExecute() {
                progressDialog = new ProgressDialog(context);
                progressDialog.setTitle(context.getString(R.string.applying_modifications_message));
                progressDialog.setIcon(R.mipmap.ic_launcher);
                progressDialog.startDialog();
            }

            @Override
            public void doInBackground() {
                int positionOriginal = RecyclerView.NO_POSITION;

                for (int i = 0; i < originalData.size(); i++) {
                    if (originalData.get(i).getId().equals(data.get(position).getId())) {
                        positionOriginal = i;
                        break;
                    }
                }
                if (positionOriginal == RecyclerView.NO_POSITION) return;

                XMLEntry target = data.get(position);
                if (target.getEndTag().trim().isEmpty()) {
                    data.set(position, new XMLEntry("", "", "", ""));
                    originalData.remove(positionOriginal);
                } else {
                    XMLEntry entry = new XMLEntry("", "", "", data.get(position).getEndTag().replace("\"", ""));
                    data.set(position, entry);
                    originalData.set(positionOriginal, entry);
                }

                if (XMLEditor.isXMLValid(XMLEditor.xmlEntriesToXML(originalData, resourceMap))) {
                    if (!isModified) {
                        isModified = true;
                    }
                    invalid = false;
                } else {
                    data.set(position, target);
                    originalData.set(positionOriginal, target);
                    invalid = true;
                }
            }

            @Override
            public void onPostExecute() {
                progressDialog.dismissDialog();
                if (invalid) {
                    Utils.toast(context.getString(R.string.xml_corrupted_toast), context).show();
                } else {
                    saveButton.setVisibility(isModified ? VISIBLE : GONE);
                    notifyItemChanged(position);
                }
            }
        };
    }

}