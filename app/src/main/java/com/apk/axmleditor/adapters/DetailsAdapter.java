package com.apk.axmleditor.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.apk.axmleditor.R;
import com.apk.axmleditor.Utils.Utils;
import com.apk.axmleditor.serializable.APKInfoEntry;
import com.google.android.material.textview.MaterialTextView;

import java.util.List;

/*
 * Created by APK Explorer & Editor <apkeditor@protonmail.com> on January 05, 2025
 */
public class DetailsAdapter extends RecyclerView.Adapter<DetailsAdapter.ViewHolder> {

    private final List<APKInfoEntry> data;

    public DetailsAdapter(List<APKInfoEntry> data) {
        this.data = data;
    }

    @NonNull
    @Override
    public DetailsAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View rowItem = LayoutInflater.from(parent.getContext()).inflate(R.layout.recycle_view_details, parent, false);
        return new ViewHolder(rowItem);
    }

    @Override
    public void onBindViewHolder(@NonNull DetailsAdapter.ViewHolder holder, int position) {
        holder.title.setText(data.get(position).getTitle());
        holder.description.setText(data.get(position).getDescription());
        if (data.get(position).getIcon() != null) {
            holder.icon.setImageDrawable(data.get(position).getIcon());
        } else {
            holder.icon.setImageDrawable(ContextCompat.getDrawable(holder.icon.getContext(), R.drawable.ic_shield));
        }
        Utils.setSlideInAnimation(holder.title, position);
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final AppCompatImageButton icon;
        private final MaterialTextView title, description;

        public ViewHolder(View view) {
            super(view);
            this.icon = view.findViewById(R.id.icon);
            this.title = view.findViewById(R.id.title);
            this.description = view.findViewById(R.id.description);
        }
    }

}