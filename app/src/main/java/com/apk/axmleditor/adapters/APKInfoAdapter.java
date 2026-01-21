package com.apk.axmleditor.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.apk.axmleditor.R;
import com.apk.axmleditor.Utils.Utils;
import com.apk.axmleditor.dialogs.DetailsDialog;
import com.apk.axmleditor.serializable.APKInfoEntry;
import com.google.android.material.textview.MaterialTextView;

import java.util.List;

/*
 * Created by APK Explorer & Editor <apkeditor@protonmail.com> on January 05, 2025
 */
public class APKInfoAdapter extends RecyclerView.Adapter<APKInfoAdapter.ViewHolder> {

    private final List<APKInfoEntry> data;

    public APKInfoAdapter(List<APKInfoEntry> data) {
        this.data = data;
    }

    @NonNull
    @Override
    public APKInfoAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View rowItem = LayoutInflater.from(parent.getContext()).inflate(R.layout.recycle_view_apkinfo, parent, false);
        return new ViewHolder(rowItem);
    }

    @Override
    public void onBindViewHolder(@NonNull APKInfoAdapter.ViewHolder holder, int position) {
        holder.title.setText(data.get(position).getTitle());
        holder.description.setText(data.get(position).getDescription());
        Utils.setSlideInAnimation(holder.itemView, position);
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private final MaterialTextView title, description;

        public ViewHolder(View view) {
            super(view);
            view.setOnClickListener(this);
            this.title = view.findViewById(R.id.title);
            this.description = view.findViewById(R.id.description);
        }

        @Override
        public void onClick(View view) {
            boolean clickable = data.get(getBindingAdapterPosition()).isClickable();
            String title = data.get(getBindingAdapterPosition()).getTitle();
            if (clickable) {
                new DetailsDialog(title, view.getContext());
            }
        }
    }

}