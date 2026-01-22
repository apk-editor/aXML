package com.apk.axmleditor.adapters;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.apk.axmleditor.R;
import com.apk.axmleditor.Utils.Async;
import com.apk.axmleditor.Utils.Utils;
import com.apk.axmleditor.activities.ImageViewerActivity;
import com.apk.axmleditor.activities.TextEditorActivity;
import com.apk.axmleditor.activities.XMLEditorActivity;
import com.apk.axmleditor.dialogs.ProgressDialog;
import com.apk.axmleditor.serializable.FilesEntry;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textview.MaterialTextView;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/*
 * Created by APK Explorer & Editor <apkeditor@protonmail.com> on January 05, 2026
 */
public class FilesAdapter extends RecyclerView.Adapter<FilesAdapter.ViewHolder> {

    private final ActivityResultLauncher<Intent> updateFile;
    private final List<FilesEntry> data;
    private final String packageName;
    private static Uri fileUri = null;

    public FilesAdapter(List<FilesEntry> data, Uri uri, String packageName, ActivityResultLauncher<Intent> updateFile) {
        this.data = data;
        if (fileUri == null && uri != null) {
            fileUri = uri;
        }
        this.packageName = packageName;
        this.updateFile = updateFile;
    }

    @NonNull
    @Override
    public FilesAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View rowItem = LayoutInflater.from(parent.getContext()).inflate(R.layout.recycle_view_files, parent, false);
        return new ViewHolder(rowItem);
    }

    @Override
    public void onBindViewHolder(@NonNull FilesAdapter.ViewHolder holder, int position) {
        holder.text.setText(data.get(position).getName());
        if (data.get(position).getIcon() != null) {
            if (new File(Utils.getExportPath(holder.icon.getContext()), packageName + "/" + data.get(position).getName()).exists()) {
                holder.icon.setImageDrawable(Drawable.createFromPath(Utils.getExportPath(holder.icon.getContext()) + "/" + packageName + "/" + data.get(position).getName()));
            } else {
                holder.icon.setImageBitmap(data.get(position).getIcon());
            }
        } else {
            if (data.get(position).getName().endsWith(".xml")) {
                holder.icon.setImageDrawable(ContextCompat.getDrawable(holder.icon.getContext(), R.drawable.ic_xml));
            } else if (Utils.isTextFile(data.get(position).getName())) {
                holder.icon.setImageDrawable(ContextCompat.getDrawable(holder.icon.getContext(), R.drawable.ic_txt));
            } else {
                holder.icon.setImageDrawable(ContextCompat.getDrawable(holder.icon.getContext(), R.drawable.ic_file));
            }
        }
        Utils.setSlideInAnimation(holder.text, position);
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {

        private final AppCompatImageButton icon;
        private final MaterialTextView text;

        public ViewHolder(View view) {
            super(view);
            view.setOnClickListener(this);
            view.setOnLongClickListener(this);
            this.icon = view.findViewById(R.id.icon);
            this.text = view.findViewById(R.id.text);
        }

        @Override
        public void onClick(View view) {
            int position = getBindingAdapterPosition();
            if (position != RecyclerView.NO_POSITION) {
                Intent intent = null;
                if (Utils.isBinaryXML(data.get(position).getName().toLowerCase())) {
                    intent = new Intent(view.getContext(), XMLEditorActivity.class);
                    intent.putExtra(XMLEditorActivity.NAME_INTENT, data.get(position).getName());
                } else if (Utils.isTextFile(data.get(position).getName().toLowerCase())) {
                    intent = new Intent(view.getContext(), TextEditorActivity.class);
                    intent.putExtra(TextEditorActivity.NAME_INTENT, data.get(position).getName());
                } else if (Utils.isImageFile(data.get(position).getName().toLowerCase())) {
                    intent = new Intent(view.getContext(), ImageViewerActivity.class);
                    intent.putExtra(ImageViewerActivity.NAME_INTENT, data.get(position).getName());
                    intent.putExtra(ImageViewerActivity.POSITION_INTENT, position);
                }
                if (intent != null) {
                    intent.setData(fileUri);
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    updateFile.launch(intent);
                }
            }
        }

        @Override
        public boolean onLongClick(View view) {
            int position = getBindingAdapterPosition();
            if (position == RecyclerView.NO_POSITION) return false;
            if (new File(Utils.getExportPath(view.getContext()), packageName + "/" + data.get(position).getName()).exists()) return true;
            new MaterialAlertDialogBuilder(view.getContext())
                    .setIcon(R.mipmap.ic_launcher)
                    .setTitle(R.string.file_save_question)
                    .setMessage(" - " + data.get(position).getName().trim())
                    .setNegativeButton(R.string.cancel, (dialogInterface, i) -> {
                    })
                    .setPositiveButton(R.string.save, (dialogInterface, i) -> new Async() {
                                private ProgressDialog progressDialog;
                                @Override
                                public void onPreExecute() {
                                    progressDialog = new ProgressDialog(view.getContext());
                                    progressDialog.setTitle(view.getContext().getString(R.string.saving));
                                    progressDialog.setIcon(R.mipmap.ic_launcher);
                                    progressDialog.startDialog();
                                }

                                @Override
                                public void doInBackground() {
                                    File outFile = new File(Utils.getExportPath(view.getContext()), packageName + "/" + data.get(position).getName());
                                    Objects.requireNonNull(outFile.getParentFile()).mkdirs();
                                    try (ZipInputStream zis = new ZipInputStream(view.getContext().getContentResolver().openInputStream(fileUri))) {
                                        ZipEntry entry;
                                        while ((entry = zis.getNextEntry()) != null) {
                                            if (entry.isDirectory()) {
                                                continue;
                                            }
                                            if (entry.getName().equals(entry.getName())) {
                                                Utils.copyStream(zis, outFile);
                                            }
                                            zis.closeEntry();
                                        }
                                    } catch (IOException ignored) {}
                                }

                                @Override
                                public void onPostExecute() {
                                    progressDialog.dismissDialog();
                                    new MaterialAlertDialogBuilder(view.getContext())
                                            .setIcon(R.mipmap.ic_launcher)
                                            .setTitle(R.string.app_name)
                                            .setMessage(view.getContext().getString(R.string.file_saved_message, packageName))
                                            .setNegativeButton(R.string.cancel, (dialogInterface, i) -> {
                                            }).show();
                                }
                            }.execute()
                    ).show();
            return true;
        }
    }

}